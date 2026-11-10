package com.devsgroup.foodmanager.core.data.repository

import android.util.Log
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.mapper.toKnownEnumSet
import com.devsgroup.foodmanager.core.data.mapper.toPutKitchenRequest
import com.devsgroup.foodmanager.core.data.worker.HouseholdSyncScheduler
import com.devsgroup.foodmanager.core.datastore.KitchenPreferencesDataSource
import com.devsgroup.foodmanager.core.domain.repository.KitchenRepository
import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.network.household.HouseholdApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore copy of `/users/me/kitchen`, local-first like the household. */
@Singleton
class DefaultKitchenRepository @Inject constructor(
    private val kitchenPrefs: KitchenPreferencesDataSource,
    private val householdApi: HouseholdApi,
    private val syncScheduler: HouseholdSyncScheduler,
    private val json: Json,
) : KitchenRepository {

    override fun appliances(): Flow<Set<KitchenAppliance>?> =
        kitchenPrefs.appliances.map { it?.toKnownEnumSet<KitchenAppliance>() }

    override suspend fun setAppliances(appliances: Set<KitchenAppliance>) {
        val codes = appliances.mapTo(mutableSetOf()) { it.name }
        kitchenPrefs.setAppliances(codes)
        try {
            accountApiCall(json, TAG) { householdApi.putKitchen(appliances.toPutKitchenRequest()) }
            kitchenPrefs.markSyncedIfUnchanged(codes)
        } catch (e: ProfileException) {
            // A fixed enum list can't really be rejected; whatever it was, the local answer
            // stands and the next push or refresh settles it.
            Log.i(TAG, "Kitchen push failed, queued: ${e.message}")
            if (e.isRetryable()) syncScheduler.scheduleSync() else kitchenPrefs.markSyncedIfUnchanged(codes)
        }
    }

    override suspend fun refresh() {
        if (kitchenPrefs.isPendingSync()) {
            syncScheduler.scheduleSync()
            return
        }
        val server = accountApiCall(json, TAG) { householdApi.getKitchen().kitchen.toDomain() }
        // A local edit made while the request was in flight wins over the older server copy.
        if (!kitchenPrefs.isPendingSync()) {
            kitchenPrefs.saveFromServer(server?.mapTo(mutableSetOf()) { it.name })
        }
    }

    private companion object {
        const val TAG = "DefaultKitchenRepo"
    }
}
