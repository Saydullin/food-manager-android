package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.SwipeAction
import kotlinx.coroutines.flow.Flow

/**
 * Contract for accessing foods and recording user reactions. Implemented in
 * :core:data by coordinating the local (Room) and remote (Retrofit) sources.
 */
interface FoodRepository {

    /** The cached feed of dishes not yet swiped, in the server's newest-first order. */
    fun feed(): Flow<List<Food>>

    /**
     * Fetches the next page of the feed (using the cursor from the last page) and
     * merges it into the cache backing [feed]. Returns whether more pages remain.
     * [lang] is an optional BCP-47 tag; the server falls back to `en`, then to any
     * available translation, never omitting a dish for a missing translation.
     */
    suspend fun loadNextFeedPage(lang: String? = null): Boolean

    /** Foods the user has liked / saved. */
    fun likedFoods(): Flow<List<Food>>

    fun food(id: String): Flow<Food?>

    /**
     * Records a swipe verdict and drops the dish out of [feed] once the server
     * confirms it. [reason]/[reasonDetail] are only meaningful for [SwipeAction.DISLIKE]
     * (see the server's dislike-reason codes) and ignored otherwise.
     */
    suspend fun recordSwipe(foodId: String, action: SwipeAction, reason: String? = null, reasonDetail: String? = null)

    /** Removes a food from [likedFoods] (the profile screen's "✕" on a liked dish). */
    suspend fun unlikeFood(foodId: String)
}
