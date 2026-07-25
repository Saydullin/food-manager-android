package com.devsgroup.foodmanager.core.data.repository

import android.util.Log
import com.devsgroup.foodmanager.core.common.di.Dispatcher
import com.devsgroup.foodmanager.core.common.di.FoodDispatcher
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.mapper.toEntity
import com.devsgroup.foodmanager.core.data.worker.OutboxScheduler
import com.devsgroup.foodmanager.core.database.dao.FeedMetaDao
import com.devsgroup.foodmanager.core.database.dao.FoodDao
import com.devsgroup.foodmanager.core.database.dao.OutboxDao
import com.devsgroup.foodmanager.core.database.entity.FeedMetaEntity
import com.devsgroup.foodmanager.core.database.entity.OutboxEntryEntity
import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodException
import com.devsgroup.foodmanager.core.model.SwipeAction
import com.devsgroup.foodmanager.core.network.auth.model.ErrorResponse
import com.devsgroup.foodmanager.core.network.food.FoodApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Coordinates the food feed backend ([FoodApi]) with the local cache ([FoodDao]/
 * [FeedMetaDao]): Room is the single source of truth for [feed]/[food], the network
 * is only used to fetch the next page or record a swipe. Mirrors [DefaultAuthRepository]'s
 * error-mapping shape.
 */
class DefaultFoodRepository @Inject constructor(
    private val foodApi: FoodApi,
    private val foodDao: FoodDao,
    private val feedMetaDao: FeedMetaDao,
    private val outboxDao: OutboxDao,
    private val outboxScheduler: OutboxScheduler,
    private val json: Json,
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : FoodRepository {

    override fun feed(): Flow<List<Food>> =
        foodDao.observeFeed().map { entities -> entities.map { it.toDomain() } }

    override suspend fun loadNextFeedPage(): Boolean = apiCall {
        val cursor = feedMetaDao.get()?.nextCursor
        val response = foodApi.getFeed(limit = FEED_PAGE_SIZE, cursor = cursor)
        var nextPosition = (foodDao.maxFeedPosition() ?: -1) + 1
        val entities = response.items.map { dto -> dto.toDomain().toEntity(feedPosition = nextPosition++) }
        foodDao.upsertAll(entities)
        feedMetaDao.upsert(FeedMetaEntity(nextCursor = response.nextCursor, hasMore = response.hasMore))
        response.hasMore
    }

    override fun likedFoods(): Flow<List<Food>> = flowOf(emptyList())

    override fun food(id: String): Flow<Food?> = foodDao.observeFood(id).map { it?.toDomain() }

    /**
     * Outbox pattern: the swipe is durably queued in Room and the card is dropped from the feed
     * immediately — delivery to the server happens later via [OutboxSyncWorker], so a dropped
     * connection never loses the interaction or bounces the card back into the deck.
     */
    override suspend fun recordSwipe(foodId: String, action: SwipeAction, reason: String?, reasonDetail: String?) {
        withContext(ioDispatcher) {
            outboxDao.insert(OutboxEntryEntity(foodId = foodId, action = action.name, reason = reason, reasonDetail = reasonDetail))
            foodDao.clearFromFeed(foodId)
        }
        outboxScheduler.scheduleSync()
    }

    override suspend fun unlikeFood(foodId: String) {
        // TODO: persist locally once liked foods are backed by Room.
    }

    /** Runs [block] on the IO dispatcher and normalizes failures to [FoodException]. */
    private suspend fun <T> apiCall(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: FoodException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error calling food API", e)
            throw e.toFoodException()
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling food API", e)
            throw FoodException.Network(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error calling food API", e)
            throw FoodException.Unknown(e.message, e)
        }
    }

    private fun HttpException.toFoodException(): FoodException {
        val serverMessage = parseErrorMessage() ?: message()
        return when (code()) {
            400 -> FoodException.InvalidRequest(serverMessage)
            401, 403 -> FoodException.Unauthorized(serverMessage)
            404 -> FoodException.NotFound(serverMessage)
            else -> FoodException.Unknown(serverMessage, this)
        }
    }

    /** Extracts `error.message` from the server's `{ "error": {...} }` envelope. */
    private fun HttpException.parseErrorMessage(): String? = runCatching {
        val body = response()?.errorBody()?.string()
        if (body.isNullOrBlank()) {
            null
        } else {
            json.decodeFromString(ErrorResponse.serializer(), body).error.message.takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private companion object {
        const val TAG = "DefaultFoodRepository"
        const val FEED_PAGE_SIZE = 10
    }
}
