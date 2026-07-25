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

/** Enqueues [OutboxSyncWorker] to drain queued interactions once the device has connectivity. */
@Singleton
class OutboxScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, TimeUnit.SECONDS.toMillis(30), TimeUnit.MILLISECONDS)
            .build()
        // KEEP: a sync is already scheduled/running whenever there's anything left to drain.
        WorkManager.getInstance(context).enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val SYNC_WORK_NAME = "outbox-sync"
    }
}
