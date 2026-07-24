package com.devsgroup.foodmanager.core.common.auth

/**
 * Session token access for the OkHttp layer in `:core:network` — read for the auth
 * interceptor, read/write for the refresh authenticator. It is implemented by the
 * DataStore-backed token store in `:core:datastore` and bound via Hilt — this seam
 * lets the network layer attach/rotate the bearer token without depending on the
 * persistence layer.
 */
interface AuthTokenProvider {
    /** The current bearer access token, or null when signed out. */
    fun accessToken(): String?

    /** The current refresh token, or null when signed out. */
    fun refreshToken(): String?

    /** Persists a freshly-issued token pair, e.g. after a silent refresh. */
    fun storeTokens(accessToken: String, refreshToken: String)

    /** Signals that the session could not be refreshed and must be treated as logged out. */
    fun onSessionExpired()
}
