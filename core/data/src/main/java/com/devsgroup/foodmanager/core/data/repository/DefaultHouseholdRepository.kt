package com.devsgroup.foodmanager.core.data.repository

import android.util.Log
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.mapper.toEntity
import com.devsgroup.foodmanager.core.data.mapper.toKnownEnumSet
import com.devsgroup.foodmanager.core.data.mapper.toPutRequest
import com.devsgroup.foodmanager.core.data.worker.HouseholdSyncScheduler
import com.devsgroup.foodmanager.core.database.dao.EaterDao
import com.devsgroup.foodmanager.core.domain.repository.HouseholdRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.network.household.HouseholdApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Household members live in Room and are synced with `/users/me/household/eaters`
 * local-first, the same way the profile's taste lists are: a write lands locally at
 * once, is pushed immediately when possible, and otherwise queued for
 * [com.devsgroup.foodmanager.core.data.worker.HouseholdSyncWorker].
 *
 * The self eater is synthesized from the profile on every read and written back
 * through [ProfileRepository.updateDinnerSelf].
 */
@Singleton
class DefaultHouseholdRepository @Inject constructor(
    private val eaterDao: EaterDao,
    private val householdApi: HouseholdApi,
    private val profileRepository: ProfileRepository,
    private val syncScheduler: HouseholdSyncScheduler,
    private val json: Json,
) : HouseholdRepository {

    override fun eaters(): Flow<List<Eater>> = combine(
        profileRepository.profile()
            .map { it.toSelfEater() }
            // profile() stays silent until something is cached; never stall the list on it.
            .onStart { emit(Eater(id = Eater.SELF_ID, name = SELF_NAME, isSelf = true)) },
        eaterDao.observeVisible(),
    ) { self, others -> listOf(self) + others.map { it.toDomain() } }

    override suspend fun refresh() {
        val server = accountApiCall(json, TAG) { householdApi.listEaters().eaters }
        val localCreatedAt = server.mapNotNull { eaterDao.get(it.id) }.associate { it.id to it.createdAt }
        eaterDao.replaceSynced(
            server.mapIndexed { index, dto ->
                dto.toDomain().toEntity(
                    // Server order is creation order; keep it even if a timestamp is missing.
                    createdAt = localCreatedAt[dto.id] ?: dto.createdAt.toEpochMillis() ?: index.toLong(),
                    pendingSync = false,
                    localVersion = 0,
                )
            },
        )
        if (eaterDao.pending().isNotEmpty()) syncScheduler.scheduleSync()
    }

    override suspend fun upsert(eater: Eater) {
        if (eater.isSelf || eater.id == Eater.SELF_ID) {
            profileRepository.updateDinnerSelf(eater.sex, eater.age, eater.medical)
            return
        }

        val previous = eaterDao.get(eater.id)
        val entity = eater.toEntity(
            createdAt = previous?.createdAt ?: System.currentTimeMillis(),
            pendingSync = true,
            localVersion = (previous?.localVersion ?: 0) + 1,
        )
        // Pending *before* the request: if the process dies mid-push, it's still queued.
        eaterDao.upsert(entity)

        try {
            accountApiCall(json, TAG) { householdApi.putEater(eater.id, eater.toPutRequest()) }
            eaterDao.markSynced(eater.id, entity.localVersion)
        } catch (e: ProfileException) {
            if (e.isRetryable()) {
                Log.i(TAG, "Queued eater ${eater.id} for later sync")
                syncScheduler.scheduleSync()
                return
            }
            // Rejected for good (e.g. too many people): undo, so the UI doesn't keep
            // showing someone the server will never have.
            if (previous != null) eaterDao.upsert(previous) else eaterDao.delete(eater.id)
            throw e
        }
    }

    override suspend fun delete(eaterId: String) {
        if (eaterId == Eater.SELF_ID) return
        val existing = eaterDao.get(eaterId) ?: return
        val tombstone = existing.copy(deleted = true, pendingSync = true, localVersion = existing.localVersion + 1)
        eaterDao.upsert(tombstone)

        try {
            accountApiCall(json, TAG) { householdApi.deleteEater(eaterId) }
            eaterDao.deleteTombstone(eaterId, tombstone.localVersion)
        } catch (e: ProfileException) {
            // The delete is idempotent server-side, so only a retryable failure keeps the
            // tombstone around; anything else means there's nothing left to delete.
            if (e.isRetryable()) syncScheduler.scheduleSync() else eaterDao.delete(eaterId)
        }
    }

    private fun String?.toEpochMillis(): Long? = this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    private companion object {
        const val TAG = "DefaultHouseholdRepo"
        const val SELF_NAME = "Я"
    }
}

/**
 * The profile as an eater. Allergies and diets are the profile's lists as far as they
 * are catalog codes; any other "exception" is free text and shown as something avoided.
 * Display only — the server does the actual matching from the profile itself.
 */
private fun UserProfile.toSelfEater(): Eater {
    val allergenCodes = Allergen.entries.mapTo(HashSet()) { it.name }
    val (allergyEntries, otherEntries) = foodExceptions.partition { it.trim().uppercase() in allergenCodes }
    return Eater(
        id = Eater.SELF_ID,
        name = "Я",
        isSelf = true,
        sex = sex,
        age = age,
        likes = foodPreferences.toSet(),
        avoid = otherEntries.toSet(),
        allergens = allergyEntries.map { it.trim().uppercase() }.toKnownEnumSet<Allergen>(),
        diets = diets.map { it.trim().uppercase() }.toKnownEnumSet<DietaryTag>(),
        medical = medicalRestrictions,
    )
}
