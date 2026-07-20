package com.devsgroup.foodmanager.core.model

/**
 * Typed failures the auth repository can raise. The `:core:domain` contract is a
 * pure-Kotlin module and can't reference `DataResult` (which lives in the Android
 * `:core:common` module), so auth operations signal failure by throwing one of
 * these instead. Callers (feature ViewModels) map them to UI state.
 */
sealed class AuthException(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** No connectivity / I/O failure reaching the server. */
    class Network(cause: Throwable?) : AuthException("Network error", cause)

    /** 400 — malformed request (e.g. invalid public key, bad verification code). */
    class InvalidRequest(message: String?) : AuthException(message)

    /** 401 / 403 — bad signature, expired/used challenge, invalid or missing session. */
    class Unauthorized(message: String?) : AuthException(message)

    /** 404 — unknown username/device (server returns this generically). */
    class NotFound(message: String?) : AuthException(message)

    /** 409 — username, email, or device public key already taken. */
    class Conflict(message: String?) : AuthException(message)

    /** Anything else (5xx, serialization, unexpected). */
    class Unknown(message: String?, cause: Throwable?) : AuthException(message, cause)
}
