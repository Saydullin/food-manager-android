package com.devsgroup.foodmanager.core.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues [ProfileSyncWorker] to replay queued taste-profile edits once the device has connectivity. */
@Singleton
class ProfileSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, TimeUnit.SECONDS.toMillis(30), TimeUnit.MILLISECONDS)
            .build()
        // KEEP: an already-scheduled run reads the newest stored values when it fires, so a
        // further edit made before then needs no enqueue of its own.
        WorkManager.getInstance(context).enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val SYNC_WORK_NAME = "profile-preferences-sync"
    }
}
