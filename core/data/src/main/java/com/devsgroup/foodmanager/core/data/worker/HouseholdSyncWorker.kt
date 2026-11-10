package com.devsgroup.foodmanager.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.mapper.toPutRequest
import com.devsgroup.foodmanager.core.database.dao.EaterDao
import com.devsgroup.foodmanager.core.database.entity.EaterEntity
import com.devsgroup.foodmanager.core.datastore.KitchenPreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.PantryPreferencesDataSource
import com.devsgroup.foodmanager.core.network.household.HouseholdApi
import com.devsgroup.foodmanager.core.network.household.model.PutKitchenRequestDto
import com.devsgroup.foodmanager.core.network.household.model.PutPantryRequestDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

/**
 * Replays cook-dinner edits made while the server was unreachable: household upserts
 * and deletes, the kitchen, and the pantry. Every endpoint is a full replace (or an
 * idempotent delete), so sending the latest local state is equivalent to replaying the
 * edits that produced it, and a duplicate run is harmless.
 */
@HiltWorker
class HouseholdSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eaterDao: EaterDao,
    private val kitchenPrefs: KitchenPreferencesDataSource,
    private val pantryPrefs: PantryPreferencesDataSource,
    private val householdApi: HouseholdApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Everything is attempted even after a failure, so one stuck item doesn't hold
        // back unrelated edits.
        var retry = false
        for (eater in eaterDao.pending()) {
            if (!push(eater)) retry = true
        }
        if (kitchenPrefs.isPendingSync() && !pushKitchen()) retry = true
        if (pantryPrefs.isPendingSync() && !pushPantry()) retry = true
        return if (retry) Result.retry() else Result.success()
    }

    /** Returns false if [eater] still needs a later retry, true once it's settled either way. */
    private suspend fun push(eater: EaterEntity): Boolean = attempt("eater ${eater.id}", onRejected = {
        // The server will never accept this as-is: drop the local edit and let the next
        // refresh bring back the server's copy.
        if (eater.deleted) eaterDao.delete(eater.id) else eaterDao.markSynced(eater.id, eater.localVersion)
    }) {
        if (eater.deleted) {
            householdApi.deleteEater(eater.id)
            eaterDao.deleteTombstone(eater.id, eater.localVersion)
        } else {
            householdApi.putEater(eater.id, eater.toDomain().toPutRequest())
            eaterDao.markSynced(eater.id, eater.localVersion)
        }
    }

    private suspend fun pushKitchen(): Boolean {
        val codes = kitchenPrefs.appliances.first() ?: return true
        return attempt("kitchen", onRejected = { kitchenPrefs.markSyncedIfUnchanged(codes) }) {
            householdApi.putKitchen(PutKitchenRequestDto(codes.toList()))
            kitchenPrefs.markSyncedIfUnchanged(codes)
        }
    }

    private suspend fun pushPantry(): Boolean {
        val ids = pantryPrefs.currentItems().mapTo(LinkedHashSet()) { it.id }
        return attempt("pantry", onRejected = { pantryPrefs.markSyncedIfUnchanged(ids) }) {
            householdApi.putPantry(PutPantryRequestDto(ids.toList()))
            pantryPrefs.markSyncedIfUnchanged(ids)
        }
    }

    /** Runs [push]; true = settled (sent, or rejected for good), false = retry later. */
    private suspend fun attempt(what: String, onRejected: suspend () -> Unit, push: suspend () -> Unit): Boolean =
        try {
            push()
            true
        } catch (e: IOException) {
            Log.w(TAG, "No connectivity, will retry $what later", e)
            false
        } catch (e: HttpException) {
            if (e.code() in 500..599) {
                Log.w(TAG, "Server error syncing $what, will retry", e)
                false
            } else {
                Log.e(TAG, "Dropping unsyncable $what", e)
                onRejected()
                true
            }
        }

    private companion object {
        const val TAG = "HouseholdSyncWorker"
    }
}
