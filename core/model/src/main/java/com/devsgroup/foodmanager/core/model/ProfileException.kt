package com.devsgroup.foodmanager.core.model

/**
 * Typed failures the profile repository can raise. Mirrors [AuthException] for the
 * same reason: `:core:domain` is a pure-Kotlin module and can't reference `DataResult`
 * (which lives in the Android `:core:common` module), so profile operations signal
 * failure by throwing one of these instead. Callers (feature ViewModels) map them to
 * UI state — e.g. [Conflict] for a username already taken by another account.
 */
sealed class ProfileException(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** No connectivity / I/O failure reaching the server. */
    class Network(cause: Throwable?) : ProfileException("Network error", cause)

    /** 400 — malformed request (e.g. invalid imageUrl, out-of-range age). */
    class InvalidRequest(message: String?) : ProfileException(message)

    /** 401 / 403 — invalid or missing session. */
    class Unauthorized(message: String?) : ProfileException(message)

    /** 404 — unknown account (server returns this generically). */
    class NotFound(message: String?) : ProfileException(message)

    /** 409 — the requested username is already taken by another account. */
    class Conflict(message: String?) : ProfileException(message)

    /** Anything else (5xx, serialization, unexpected). */
    class Unknown(message: String?, cause: Throwable?) : ProfileException(message, cause)
}
