package com.devsgroup.foodmanager.core.network.profile

import com.devsgroup.foodmanager.core.network.profile.model.EditProfileRequest
import com.devsgroup.foodmanager.core.network.profile.model.ProfileResponse
import com.devsgroup.foodmanager.core.network.profile.model.SetProfileImageRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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
}
