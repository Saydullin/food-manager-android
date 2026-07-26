package com.devsgroup.foodmanager.core.network.interceptor

import com.devsgroup.foodmanager.core.common.auth.AccountBanNotifier
import com.devsgroup.foodmanager.core.network.auth.model.ErrorResponse
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Watches every response for the server's `{ "error": { "code": "ACCOUNT_BANNED" } }`
 * envelope and records the ban via [AccountBanNotifier]. A ban can be returned by any
 * endpoint (not just profile/auth ones), so this is applied centrally at the OkHttp
 * layer rather than duplicated in each repository's per-call error mapping.
 */
class AccountBanInterceptor @Inject constructor(
    private val notifier: AccountBanNotifier,
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful && response.errorCode() == ACCOUNT_BANNED_CODE) {
            notifier.onAccountBanned()
        }
        return response
    }

    private fun Response.errorCode(): String? = runCatching {
        val body = peekBody(MAX_PEEK_BYTES).string()
        if (body.isBlank()) null else json.decodeFromString(ErrorResponse.serializer(), body).error.code
    }.getOrNull()

    private companion object {
        const val ACCOUNT_BANNED_CODE = "ACCOUNT_BANNED"
        const val MAX_PEEK_BYTES = 4096L
    }
}
