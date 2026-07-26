package com.devsgroup.foodmanager.core.network.settings

import com.devsgroup.foodmanager.core.network.settings.model.SettingsResponse
import com.devsgroup.foodmanager.core.network.settings.model.UpdateSettingsRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * Retrofit surface for the settings backend (all paths are relative to the `/api/`
 * base URL). Both endpoints are protected; the bearer token is attached automatically
 * by the auth interceptor whenever a session exists, so no @Header params here.
 */
interface SettingsApi {

    @GET("users/me/settings")
    suspend fun mySettings(): SettingsResponse

    @PATCH("users/me/settings")
    suspend fun updateSettings(@Body body: UpdateSettingsRequest): SettingsResponse
}
