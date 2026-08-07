package com.devsgroup.foodmanager.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devsgroup.foodmanager.core.data.repository.setPreferenceList
import com.devsgroup.foodmanager.core.datastore.ProfilePreferenceList
import com.devsgroup.foodmanager.core.datastore.ProfilePreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.valuesOf
import com.devsgroup.foodmanager.core.network.profile.ProfileApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

/**
 * Replays taste-profile edits (favourites, exceptions, diets) the user made while the
 * server was unreachable.
 *
 * Unlike the interaction outbox there's nothing to order or accumulate here: every list is
 * a full-replace endpoint, so sending its latest stored value is exactly equivalent to
 * replaying each add/remove that produced it — which also makes a duplicate run harmless.
 */
@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profilePrefs: ProfilePreferencesDataSource,
    private val profileApi: ProfileApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = profilePrefs.pendingSyncLists()
        if (pending.isEmpty()) return Result.success()
        val profile = profilePrefs.currentProfile() ?: return Result.success()

        var retry = false
        for (list in pending) {
            // Every list is attempted even after one fails, so an unrelated queued edit
            // isn't held back by whichever list the server is currently unhappy with.
            if (!push(list, profile.valuesOf(list))) retry = true
        }
        return if (retry) Result.retry() else Result.success()
    }

    /** Returns false if [list] still needs a later retry, true once it's settled either way. */
    private suspend fun push(list: ProfilePreferenceList, values: List<String>): Boolean {
        try {
            profileApi.setPreferenceList(list, values)
            // Settles the flag only if the stored list is still the one just sent. An edit
            // made mid-flight leaves it queued and reports "not settled", so this run ends
            // in a retry that picks up the newer value.
            return profilePrefs.clearPendingSyncIfUnchanged(list, values)
        } catch (e: IOException) {
            Log.w(TAG, "No connectivity, will retry $list later", e)
            return false
        } catch (e: HttpException) {
            if (e.code() in 500..599) {
                Log.w(TAG, "Server error syncing $list, will retry", e)
                return false
            }
            // 4xx means the server will never accept this list as-is (e.g. a diet code it
            // no longer knows) — stop retrying and let the next profile refresh pull its copy.
            Log.e(TAG, "Dropping unsyncable $list", e)
            profilePrefs.clearPendingSyncIfUnchanged(list, values)
            return true
        }
    }

    private companion object {
        const val TAG = "ProfileSyncWorker"
    }
}
