package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.common.di.Dispatcher
import com.devsgroup.foodmanager.core.common.di.FoodDispatcher
import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.datastore.AuthPreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.ProfilePreferencesDataSource
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.AuthChallenge
import com.devsgroup.foodmanager.core.model.AuthDevice
import com.devsgroup.foodmanager.core.model.AuthException
import com.devsgroup.foodmanager.core.model.AuthSession
import com.devsgroup.foodmanager.core.model.AuthTokens
import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.network.auth.AuthApi
import com.devsgroup.foodmanager.core.network.auth.model.AddDeviceRequest
import com.devsgroup.foodmanager.core.network.auth.model.AddEmailRequest
import com.devsgroup.foodmanager.core.network.auth.model.ChallengeRequest
import com.devsgroup.foodmanager.core.network.auth.model.ConfirmLoginCodeRequest
import com.devsgroup.foodmanager.core.network.auth.model.ConfirmRecoveryRequest
import com.devsgroup.foodmanager.core.network.auth.model.ErrorResponse
import com.devsgroup.foodmanager.core.network.auth.model.LogoutRequest
import com.devsgroup.foodmanager.core.network.auth.model.RefreshRequest
import com.devsgroup.foodmanager.core.network.auth.model.RegisterRequest
import com.devsgroup.foodmanager.core.network.auth.model.RequestLoginCodeRequest
import com.devsgroup.foodmanager.core.network.auth.model.RequestRecoveryRequest
import com.devsgroup.foodmanager.core.network.auth.model.VerifyEmailCodeRequest
import com.devsgroup.foodmanager.core.network.auth.model.VerifyRequest
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the auth backend ([AuthApi]) with the local token store
 * ([AuthPreferencesDataSource]): it maps wire models to domain models, persists the
 * session whenever the server issues tokens, and translates transport/HTTP failures
 * into [AuthException]s.
 */
@Singleton
class DefaultAuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val authPrefs: AuthPreferencesDataSource,
    private val profilePrefs: ProfilePreferencesDataSource,
    private val profileRepository: ProfileRepository,
    private val json: Json,
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override val tokens: Flow<AuthTokens?> =
        combine(authPrefs.accessTokenFlow, authPrefs.refreshTokenFlow) { access, refresh ->
            if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
                AuthTokens(access, refresh)
            } else {
                null
            }
        }

    override val isLoggedIn: Flow<Boolean> =
        authPrefs.accessTokenFlow.map { !it.isNullOrBlank() }

    override val deviceId: Flow<String?> = authPrefs.deviceIdFlow

    override val sessionExpired: Flow<Unit> = authPrefs.sessionExpired

    override suspend fun isUsernameAvailable(username: String): Boolean = apiCall {
        authApi.usernameAvailable(username).available
    }

    override suspend fun register(
        username: String,
        publicKey: String,
        deviceLabel: String?,
        email: String?,
    ): AuthSession = apiCall {
        val response = authApi.register(
            RegisterRequest(username, publicKey, deviceLabel, email),
        )
        authPrefs.updateTokens(response.accessToken, response.refreshToken)
        authPrefs.updateDeviceId(response.device.id)
        authPrefs.updateUsername(response.user.username)
        // Drop any profile cached in memory from a previous account in this same
        // process (e.g. registering a second account after a logout) before seeding
        // the new one — otherwise ProfileRepository.profile() keeps serving the old
        // account's data until the process restarts.
        profileRepository.invalidateCache()
        // Persist a baseline profile so the Profile screen has correct data (username,
        // email) to show on a later offline launch, before any `/users/me` fetch runs.
        profilePrefs.saveProfile(
            UserProfile(
                name = response.user.username,
                username = "@${response.user.username}",
                email = response.user.email ?: email,
                emailVerified = response.user.emailVerified,
            ),
        )
        response.toDomain()
    }

    override suspend fun requestChallenge(username: String): AuthChallenge = apiCall {
        authApi.challenge(ChallengeRequest(username)).toDomain()
    }

    override suspend fun verify(
        challengeId: String,
        signature: String,
        deviceId: String,
    ): AuthTokens = apiCall {
        val response = authApi.verify(VerifyRequest(challengeId, signature, deviceId))
        authPrefs.updateTokens(response.accessToken, response.refreshToken)
        response.toDomain()
    }

    override suspend fun refresh(): AuthTokens {
        val refreshToken = authPrefs.refreshTokenFlow.first()
        if (refreshToken.isNullOrBlank()) {
            Log.e(TAG, "Refresh requested with no active session")
            throw AuthException.Unauthorized("No active session to refresh")
        }
        return apiCall {
            val response = authApi.refresh(RefreshRequest(refreshToken))
            authPrefs.updateTokens(response.accessToken, response.refreshToken)
            response.toDomain()
        }
    }

    override suspend fun logout() {
        val refreshToken = authPrefs.refreshTokenFlow.first()
        if (!refreshToken.isNullOrBlank()) {
            // Best effort: revoke server-side, but always clear the local session.
            runCatching { withContext(ioDispatcher) { authApi.logout(LogoutRequest(refreshToken)) } }
                .onFailure { Log.e(TAG, "Server-side logout failed, clearing local session anyway", it) }
        }
        authPrefs.clear()
        profileRepository.invalidateCache()
        // Also drops any taste-profile edit still queued for sync — replaying this account's
        // pending lists against whoever signs in next would write them to the wrong account.
        profilePrefs.clear()
    }

    override suspend fun addEmail(email: String) = apiCall {
        authApi.addEmail(AddEmailRequest(email)).let { }
    }

    override suspend fun verifyEmailByToken(token: String) = apiCall {
        authApi.verifyEmail(token).let { }
    }

    override suspend fun verifyEmailByCode(code: String) = apiCall {
        authApi.verifyEmailCode(VerifyEmailCodeRequest(code)).let { }
    }

    override suspend fun listDevices(): List<AuthDevice> = apiCall {
        authApi.devices().map { it.toDomain() }
    }

    override suspend fun addDevice(publicKey: String, deviceLabel: String?): AuthDevice = apiCall {
        val device = authApi.addDevice(AddDeviceRequest(publicKey, deviceLabel)).toDomain()
        authPrefs.updateDeviceId(device.id)
        device
    }

    override suspend fun revokeDevice(deviceId: String) = apiCall {
        authApi.revokeDevice(deviceId).let { }
    }

    override suspend fun requestRecovery(username: String?, email: String?) = apiCall {
        authApi.requestRecovery(RequestRecoveryRequest(username, email)).let { }
    }

    override suspend fun confirmRecovery(
        recoveryToken: String,
        newPublicKey: String,
        deviceLabel: String?,
    ): AuthSession = apiCall {
        val response = authApi.confirmRecovery(
            ConfirmRecoveryRequest(recoveryToken, newPublicKey, deviceLabel),
        )
        authPrefs.updateTokens(response.accessToken, response.refreshToken)
        authPrefs.updateDeviceId(response.device.id)
        response.toDomain()
    }

    override suspend fun requestLoginCode(username: String, email: String) = apiCall {
        authApi.requestLoginCode(RequestLoginCodeRequest(username, email)).let { }
    }

    override suspend fun confirmLoginCode(
        username: String,
        email: String,
        code: String,
        newPublicKey: String,
        deviceLabel: String?,
    ): AuthSession = apiCall {
        val response = authApi.confirmLoginCode(
            ConfirmLoginCodeRequest(username, email, code, newPublicKey, deviceLabel),
        )
        authPrefs.updateTokens(response.accessToken, response.refreshToken)
        authPrefs.updateDeviceId(response.device.id)
        authPrefs.updateUsername(response.user.username)
        // Same baseline seed as register(): without it this device has no stored profile
        // at all, and the first `/users/me` merge would write one back with whatever the
        // wire response happens to say about an account this install knows nothing about.
        // Signing in by mailed code also *proves* the address — the server only sends
        // that code to an already-verified email — so `emailVerified` from the response
        // is what stops the email gate from re-asking on a device that never verified.
        profileRepository.invalidateCache()
        profilePrefs.saveProfile(
            UserProfile(
                name = response.user.username,
                username = "@${response.user.username}",
                email = response.user.email ?: email,
                emailVerified = response.user.emailVerified,
            ),
        )
        response.toDomain()
    }

    /** Runs [block] on the IO dispatcher and normalizes failures to [AuthException]. */
    private suspend fun <T> apiCall(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: AuthException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error calling auth API", e)
            throw e.toAuthException()
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling auth API", e)
            throw AuthException.Network(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error calling auth API", e)
            throw AuthException.Unknown(e.message, e)
        }
    }

    private companion object {
        const val TAG = "DefaultAuthRepository"
    }

    private fun HttpException.toAuthException(): AuthException {
        val serverMessage = parseErrorMessage() ?: message()
        return when (code()) {
            400 -> AuthException.InvalidRequest(serverMessage)
            401, 403 -> AuthException.Unauthorized(serverMessage)
            404 -> AuthException.NotFound(serverMessage)
            409 -> AuthException.Conflict(serverMessage)
            else -> AuthException.Unknown(serverMessage, this)
        }
    }

    /** Extracts `error.message` from the server's `{ "error": {...} }` envelope. */
    private fun HttpException.parseErrorMessage(): String? = runCatching {
        val body = response()?.errorBody()?.string()
        if (body.isNullOrBlank()) {
            null
        } else {
            json.decodeFromString(ErrorResponse.serializer(), body).error.message.takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}
