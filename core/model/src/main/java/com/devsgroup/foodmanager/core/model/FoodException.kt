package com.devsgroup.foodmanager.core.model

/**
 * Typed failures the food repository can raise. Mirrors [AuthException]/[ProfileException]
 * for the same reason: `:core:domain` is a pure-Kotlin module and can't reference `DataResult`
 * (which lives in the Android `:core:common` module), so food operations signal failure by
 * throwing one of these instead.
 */
sealed class FoodException(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** No connectivity / I/O failure reaching the server. */
    class Network(cause: Throwable?) : FoodException("Network error", cause)

    /** 400 — malformed request (e.g. `INVALID_CURSOR`, a malformed dish id). */
    class InvalidRequest(message: String?) : FoodException(message)

    /** 401 / 403 — invalid or missing session. */
    class Unauthorized(message: String?) : FoodException(message)

    /** 404 `FOOD_NOT_FOUND` — unknown dish id. */
    class NotFound(message: String?) : FoodException(message)

    /** Anything else (5xx, serialization, unexpected). */
    class Unknown(message: String?, cause: Throwable?) : FoodException(message, cause)
}
