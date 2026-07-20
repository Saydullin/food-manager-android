package com.devsgroup.foodmanager.core.network.interceptor

import com.devsgroup.foodmanager.core.common.auth.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches `Authorization: Bearer <accessToken>` to every request when a session
 * exists. Public endpoints (register, challenge, verify, refresh) simply ignore
 * the header server-side, so a blanket attach is safe and keeps the API clean.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.accessToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
