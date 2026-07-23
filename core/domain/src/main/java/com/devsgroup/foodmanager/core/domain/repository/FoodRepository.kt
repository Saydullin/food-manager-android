package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodRecommendation
import com.devsgroup.foodmanager.core.model.SwipeAction
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accessing foods and recording user reactions. Implemented in
 * :core:data by coordinating the local (Room) and remote (Retrofit) sources.
 */
interface FoodRepository {

    /** The personalized deck to swipe through, ordered by match score. */
    fun recommendedFoods(): Flow<List<FoodRecommendation>>

    /** Foods the user has liked / saved. */
    fun likedFoods(): Flow<List<Food>>

    fun food(id: String): Flow<Food?>

    /** Persists a swipe and lets the recommender learn from it. */
    suspend fun recordSwipe(foodId: String, action: SwipeAction)

    /** Removes a food from [likedFoods] (the profile screen's "✕" on a liked dish). */
    suspend fun unlikeFood(foodId: String)
}
