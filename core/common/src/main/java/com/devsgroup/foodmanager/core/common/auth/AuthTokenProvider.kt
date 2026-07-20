package com.devsgroup.foodmanager.core.common.auth

/**
 * Read-only view of the current session's access token, consumed by the OkHttp
 * auth interceptor in `:core:network`. It is implemented by the DataStore-backed
 * token store in `:core:datastore` and bound via Hilt — this seam lets the network
 * layer attach the bearer header without depending on the persistence layer.
 */
interface AuthTokenProvider {
    /** The current bearer access token, or null when signed out. */
    fun accessToken(): String?
}
