package com.devsgroup.foodmanager.core.network.profile

import com.devsgroup.foodmanager.core.network.profile.model.EditProfileRequest
import com.devsgroup.foodmanager.core.network.profile.model.ProfileResponse
import com.devsgroup.foodmanager.core.network.profile.model.SetDietsRequest
import com.devsgroup.foodmanager.core.network.profile.model.SetFoodExceptionsRequest
import com.devsgroup.foodmanager.core.network.profile.model.SetFoodPreferencesRequest
import com.devsgroup.foodmanager.core.network.profile.model.SetProfileImageRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Retrofit surface for the profile backend (all paths are relative to the `/api/`
 * base URL). Every endpoint here is protected; the bearer token is attached
 * automatically by the auth interceptor whenever a session exists, so no @Header
 * params here.
 */
interface ProfileApi {

    @GET("users/me")
    suspend fun myProfile(): ProfileResponse

    @PATCH("users/me")
    suspend fun editProfile(@Body body: EditProfileRequest): ProfileResponse

    @PUT("users/me/image")
    suspend fun setProfileImage(@Body body: SetProfileImageRequest): ProfileResponse

    @DELETE("users/me/image")
    suspend fun removeProfileImage(): ProfileResponse

    @PUT("users/me/food-preferences")
    suspend fun setFoodPreferences(@Body body: SetFoodPreferencesRequest): ProfileResponse

    @PUT("users/me/food-exceptions")
    suspend fun setFoodExceptions(@Body body: SetFoodExceptionsRequest): ProfileResponse

    @PUT("users/me/diets")
    suspend fun setDiets(@Body body: SetDietsRequest): ProfileResponse

    /**
     * Latches this account as onboarded. Takes no body and is idempotent, so it's safe to
     * re-send — which the repository does when a completion recorded offline hasn't
     * reached the server yet.
     */
    @POST("users/me/onboarding-complete")
    suspend fun completeOnboarding(): ProfileResponse
}
