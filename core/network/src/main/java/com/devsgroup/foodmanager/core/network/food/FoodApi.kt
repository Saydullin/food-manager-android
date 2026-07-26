package com.devsgroup.foodmanager.core.network.food

import com.devsgroup.foodmanager.core.network.food.model.DeleteInteractionResponseDto
import com.devsgroup.foodmanager.core.network.food.model.FeedResponseDto
import com.devsgroup.foodmanager.core.network.food.model.FoodDetailResponseDto
import com.devsgroup.foodmanager.core.network.food.model.InteractionRequestDto
import com.devsgroup.foodmanager.core.network.food.model.InteractionResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for the food feed & swipe backend (all paths are relative to the
 * `/api/` base URL). All endpoints are protected; the bearer token is attached
 * automatically by the auth interceptor whenever a session exists, so no @Header
 * params here.
 */
interface FoodApi {

    @GET("foods/feed")
    suspend fun getFeed(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null,
        @Query("lang") lang: String? = null,
    ): FeedResponseDto

    @GET("foods/{foodId}")
    suspend fun getFood(
        @Path("foodId") foodId: String,
        @Query("lang") lang: String? = null,
    ): FoodDetailResponseDto

    @POST("foods/{foodId}/interactions")
    suspend fun recordInteraction(
        @Path("foodId") foodId: String,
        @Body body: InteractionRequestDto,
    ): InteractionResponseDto

    @DELETE("foods/{foodId}/interactions")
    suspend fun deleteInteraction(@Path("foodId") foodId: String): DeleteInteractionResponseDto
}
