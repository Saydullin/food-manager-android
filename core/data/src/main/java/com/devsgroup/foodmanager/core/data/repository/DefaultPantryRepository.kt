package com.devsgroup.foodmanager.core.data.repository

import android.util.Log
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.worker.HouseholdSyncScheduler
import com.devsgroup.foodmanager.core.datastore.PantryPreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.StoredPantryItem
import com.devsgroup.foodmanager.core.domain.repository.PantryRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.model.Ingredient
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.network.household.HouseholdApi
import com.devsgroup.foodmanager.core.network.household.model.PutPantryRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore copy of `/users/me/pantry` (ids + names, so it renders offline), local-first. */
@Singleton
class DefaultPantryRepository @Inject constructor(
    private val pantryPrefs: PantryPreferencesDataSource,
    private val householdApi: HouseholdApi,
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: HouseholdSyncScheduler,
    private val json: Json,
) : PantryRepository {

    override fun items(): Flow<List<Ingredient>> = pantryPrefs.items.map { stored ->
        stored.map { Ingredient(id = it.id, name = it.name) }.sortedBy { it.name.lowercase() }
    }

    override suspend fun setItems(items: List<Ingredient>) {
        val distinct = items.distinctBy { it.id }
        val ids = distinct.mapTo(LinkedHashSet()) { it.id }
        pantryPrefs.setItems(distinct.map { StoredPantryItem(it.id, it.name) })
        try {
            accountApiCall(json, TAG) { householdApi.putPantry(PutPantryRequestDto(ids.toList()), lang()) }
            pantryPrefs.markSyncedIfUnchanged(ids)
        } catch (e: ProfileException) {
            // Ids come from the server's own catalog, so a rejection means one was deleted
            // meanwhile; the next refresh brings back what the server actually has.
            Log.i(TAG, "Pantry push failed: ${e.message}")
            if (e.isRetryable()) syncScheduler.scheduleSync() else pantryPrefs.markSyncedIfUnchanged(ids)
        }
    }

    override suspend fun search(query: String): List<Ingredient> {
        val trimmed = query.trim()
        return accountApiCall(json, TAG) {
            householdApi.searchIngredients(search = trimmed.ifEmpty { null }, lang = lang()).ingredients
        }.map { it.toDomain() }
    }

    override suspend fun refresh() {
        if (pantryPrefs.isPendingSync()) {
            syncScheduler.scheduleSync()
            return
        }
        val server = accountApiCall(json, TAG) { householdApi.getPantry(lang()).items }
        if (!pantryPrefs.isPendingSync()) {
            pantryPrefs.saveFromServer(server.map { StoredPantryItem(it.ingredientId, it.name) })
        }
    }

    private suspend fun lang(): String = settingsRepository.language().first().tag

    private companion object {
        const val TAG = "DefaultPantryRepo"
    }
}
