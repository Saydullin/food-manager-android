package com.devsgroup.foodmanager.core.network.auth

import com.devsgroup.foodmanager.core.network.auth.model.AddDeviceRequest
import com.devsgroup.foodmanager.core.network.auth.model.AddEmailRequest
import com.devsgroup.foodmanager.core.network.auth.model.ChallengeRequest
import com.devsgroup.foodmanager.core.network.auth.model.ChallengeResponse
import com.devsgroup.foodmanager.core.network.auth.model.ConfirmRecoveryRequest
import com.devsgroup.foodmanager.core.network.auth.model.DeviceDto
import com.devsgroup.foodmanager.core.network.auth.model.LogoutRequest
import com.devsgroup.foodmanager.core.network.auth.model.MessageResponse
import com.devsgroup.foodmanager.core.network.auth.model.RecoveryConfirmResponse
import com.devsgroup.foodmanager.core.network.auth.model.RefreshRequest
import com.devsgroup.foodmanager.core.network.auth.model.RegisterRequest
import com.devsgroup.foodmanager.core.network.auth.model.RequestRecoveryRequest
import com.devsgroup.foodmanager.core.network.auth.model.SessionResponse
import com.devsgroup.foodmanager.core.network.auth.model.TokenPairResponse
import com.devsgroup.foodmanager.core.network.auth.model.UsernameAvailabilityResponse
import com.devsgroup.foodmanager.core.network.auth.model.VerifyEmailCodeRequest
import com.devsgroup.foodmanager.core.network.auth.model.VerifyRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for the auth backend (all paths are relative to the `/api/`
 * base URL). Protected endpoints need a bearer token; it is attached automatically
 * by the auth interceptor whenever a session exists, so no @Header params here.
 */
interface AuthApi {

    // --- Register + login flow ---

    @GET("auth/username-available")
    suspend fun usernameAvailable(@Query("username") username: String): UsernameAvailabilityResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): SessionResponse

    @POST("auth/challenge")
    suspend fun challenge(@Body body: ChallengeRequest): ChallengeResponse

    @POST("auth/verify")
    suspend fun verify(@Body body: VerifyRequest): TokenPairResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPairResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest): MessageResponse

    // --- Email ---

    @POST("auth/email/add")
    suspend fun addEmail(@Body body: AddEmailRequest): MessageResponse

    @GET("auth/email/verify")
    suspend fun verifyEmail(@Query("token") token: String): MessageResponse

    @POST("auth/email/verify-code")
    suspend fun verifyEmailCode(@Body body: VerifyEmailCodeRequest): MessageResponse

    // --- Devices ---

    @GET("auth/devices")
    suspend fun devices(): List<DeviceDto>

    @POST("auth/devices/add")
    suspend fun addDevice(@Body body: AddDeviceRequest): DeviceDto

    @DELETE("auth/devices/{deviceId}")
    suspend fun revokeDevice(@Path("deviceId") deviceId: String): MessageResponse

    // --- Recovery ---

    @POST("auth/recovery/request")
    suspend fun requestRecovery(@Body body: RequestRecoveryRequest): MessageResponse

    @POST("auth/recovery/confirm")
    suspend fun confirmRecovery(@Body body: ConfirmRecoveryRequest): RecoveryConfirmResponse
}
