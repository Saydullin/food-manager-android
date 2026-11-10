package com.devsgroup.foodmanager.core.network.dinner

import com.devsgroup.foodmanager.core.network.dinner.model.DinnerSuggestionsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit surface for the cook-dinner suggestions (paths are relative to the `/api/` base
 * URL). Protected; the bearer token is attached automatically by the auth interceptor.
 */
interface DinnerApi {

    /**
     * Dishes that suit every selected eater. Not paginated — it's a short list, not a feed.
     *
     * @param eaterIds comma-joined household eater ids; null when nobody but the owner eats
     * @param source `"ANY"` or `"AT_HOME"` (cook from the pantry)
     * @param maxMissing for `AT_HOME`, how many ingredients may still need buying (server default 2)
     */
    @GET("dinner/suggestions")
    suspend fun suggestions(
        @Query("eaterIds") eaterIds: String?,
        @Query("includeSelf") includeSelf: Boolean,
        @Query("source") source: String,
        @Query("maxMissing") maxMissing: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("lang") lang: String? = null,
    ): DinnerSuggestionsResponseDto
}
