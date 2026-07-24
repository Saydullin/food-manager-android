package com.devsgroup.foodmanager.core.network.interceptor

import com.devsgroup.foodmanager.core.common.auth.AuthTokenProvider
import com.devsgroup.foodmanager.core.network.auth.AuthApi
import com.devsgroup.foodmanager.core.network.auth.model.RefreshRequest
import com.devsgroup.foodmanager.core.network.di.RefreshApi
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Silently refreshes an expired access token on a 401 and retries the request once.
 * Uses a [RefreshApi]-qualified [AuthApi] backed by its own bare OkHttpClient (no
 * [AuthInterceptor], no authenticator) so refreshing never re-enters this class —
 * the main client (which this authenticator is attached to) would otherwise form a
 * DI cycle: authenticator -> AuthApi -> Retrofit -> OkHttpClient -> authenticator.
 *
 * Concurrent 401s are deduped: only the first caller performs the network refresh
 * (guarded by [lock]); everyone else re-checks [AuthTokenProvider.accessToken] after
 * acquiring the lock and, if it already moved past the token their request failed
 * with, simply retries with the new one instead of refreshing again.
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    @RefreshApi private val refreshAuthApi: AuthApi,
    private val tokenProvider: AuthTokenProvider,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith("auth/refresh")) return null
        if (responseCount(response) >= 2) return null

        // Only requests that actually carried our bearer token are worth refreshing for.
        // A 401 with no Authorization header (e.g. a bad signature on `verify`, before any
        // session exists) is unrelated to token expiry and must not trigger a refresh or a
        // forced logout.
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (failedToken.isNullOrBlank()) return null

        val newAccessToken = synchronized(lock) {
            val currentToken = tokenProvider.accessToken()
            if (currentToken != null && currentToken != failedToken) {
                currentToken
            } else {
                refreshSynchronously()
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun refreshSynchronously(): String? {
        val refreshToken = tokenProvider.refreshToken() ?: run {
            Log.e(TAG, "No refresh token available, forcing logout")
            tokenProvider.onSessionExpired()
            return null
        }
        return try {
            val tokens = runBlocking { refreshAuthApi.refresh(RefreshRequest(refreshToken)) }
            tokenProvider.storeTokens(tokens.accessToken, tokens.refreshToken)
            tokens.accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed, forcing logout", e)
            tokenProvider.onSessionExpired()
            null
        }
    }

    private companion object {
        const val TAG = "TokenRefreshAuthenticator"
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
