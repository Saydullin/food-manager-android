package com.devsgroup.foodmanager.core.network.household

import com.devsgroup.foodmanager.core.network.household.model.DeleteEaterResponseDto
import com.devsgroup.foodmanager.core.network.household.model.EaterResponseDto
import com.devsgroup.foodmanager.core.network.household.model.EatersResponseDto
import com.devsgroup.foodmanager.core.network.household.model.IngredientsResponseDto
import com.devsgroup.foodmanager.core.network.household.model.KitchenResponseDto
import com.devsgroup.foodmanager.core.network.household.model.PantryResponseDto
import com.devsgroup.foodmanager.core.network.household.model.PutEaterRequestDto
import com.devsgroup.foodmanager.core.network.household.model.PutKitchenRequestDto
import com.devsgroup.foodmanager.core.network.household.model.PutPantryRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for the cook-dinner household backend: the people the user cooks for, their
 * kitchen equipment, the pantry and the ingredient catalog (paths are relative to the `/api/`
 * base URL). Every endpoint is protected; the bearer token is attached automatically by the
 * auth interceptor, so no @Header params here. All writes are replace-in-full `PUT`s so they
 * can be queued offline and replayed.
 */
interface HouseholdApi {

    @GET("users/me/household/eaters")
    suspend fun listEaters(): EatersResponseDto

    /** [eaterId] is a client-generated UUID, so replaying an offline create can't duplicate. */
    @PUT("users/me/household/eaters/{eaterId}")
    suspend fun putEater(
        @Path("eaterId") eaterId: String,
        @Body body: PutEaterRequestDto,
    ): EaterResponseDto

    @DELETE("users/me/household/eaters/{eaterId}")
    suspend fun deleteEater(@Path("eaterId") eaterId: String): DeleteEaterResponseDto

    @GET("users/me/kitchen")
    suspend fun getKitchen(): KitchenResponseDto

    @PUT("users/me/kitchen")
    suspend fun putKitchen(@Body body: PutKitchenRequestDto): KitchenResponseDto

    @GET("users/me/pantry")
    suspend fun getPantry(@Query("lang") lang: String? = null): PantryResponseDto

    @PUT("users/me/pantry")
    suspend fun putPantry(
        @Body body: PutPantryRequestDto,
        @Query("lang") lang: String? = null,
    ): PantryResponseDto

    @GET("ingredients")
    suspend fun searchIngredients(
        @Query("search") search: String?,
        @Query("lang") lang: String?,
    ): IngredientsResponseDto
}
