package com.devsgroup.foodmanager.core.common.auth

/**
 * Signals that the server rejected a request with the `ACCOUNT_BANNED` error code.
 * Implemented by the DataStore-backed session store in `:core:datastore` and bound
 * via Hilt, mirroring [AuthTokenProvider] — this seam lets the network layer's
 * response interceptor record a ban without depending on the persistence layer.
 * The ban applies account-wide, so any endpoint's response can trigger it.
 */
interface AccountBanNotifier {
    /** Persists that the signed-in account has been suspended by the server. */
    fun onAccountBanned()
}
