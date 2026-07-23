package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodRecommendation
import com.devsgroup.foodmanager.core.model.SwipeAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Placeholder implementation. Wire the Room DAO (:core:database) and the Retrofit
 * API (:core:network) here to make this offline-first (local as source of truth,
 * remote as sync).
 */
class DefaultFoodRepository @Inject constructor(
    // TODO: inject FoodDao and FoodApi once the sources are implemented.
) : FoodRepository {

    override fun recommendedFoods(): Flow<List<FoodRecommendation>> = flowOf(emptyList())

    override fun likedFoods(): Flow<List<Food>> = flowOf(emptyList())

    override fun food(id: String): Flow<Food?> = flowOf(null)

    override suspend fun recordSwipe(foodId: String, action: SwipeAction) {
        // TODO: persist locally, then enqueue remote sync.
    }

    override suspend fun unlikeFood(foodId: String) {
        // TODO: persist locally once liked foods are backed by Room.
    }
}
