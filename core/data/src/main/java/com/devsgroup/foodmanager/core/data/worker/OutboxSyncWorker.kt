package com.devsgroup.foodmanager.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devsgroup.foodmanager.core.data.repository.UNLIKE_ACTION
import com.devsgroup.foodmanager.core.database.dao.OutboxDao
import com.devsgroup.foodmanager.core.database.entity.OutboxEntryEntity
import com.devsgroup.foodmanager.core.network.food.FoodApi
import com.devsgroup.foodmanager.core.network.food.model.InteractionRequestDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.io.IOException

/**
 * Drains the outbox one entry at a time, oldest first, so a dish's interactions replay to the
 * server in the order the user actually made them.
 */
@HiltWorker
class OutboxSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxDao: OutboxDao,
    private val foodApi: FoodApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        for (entry in outboxDao.getAll()) {
            if (sendEntry(entry) is Result.Retry) return Result.retry()
        }
        return Result.success()
    }

    private suspend fun sendEntry(entry: OutboxEntryEntity): Result {
        try {
            if (entry.action == UNLIKE_ACTION) {
                foodApi.deleteInteraction(entry.foodId)
            } else {
                foodApi.recordInteraction(
                    entry.foodId,
                    InteractionRequestDto(entry.action, entry.reason, entry.reasonDetail),
                )
            }
            outboxDao.delete(entry)
            return Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "No connectivity, will retry outbox sync later", e)
            return Result.retry()
        } catch (e: HttpException) {
            if (e.code() in 500..599) {
                Log.w(TAG, "Server error syncing outbox entry ${entry.id}, will retry", e)
                return Result.retry()
            }
            // 4xx means the server will never accept this entry as-is (e.g. the food was
            // removed) — drop it rather than retrying forever.
            Log.e(TAG, "Dropping unrecoverable outbox entry ${entry.id}", e)
            outboxDao.delete(entry)
            return Result.success()
        }
    }

    private companion object {
        const val TAG = "OutboxSyncWorker"
    }
}
