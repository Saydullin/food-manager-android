package com.devsgroup.foodmanager.core.model

/**
 * Domain models for the device-key (passkey-style) auth backend. Every login is
 * proven by signing a server nonce with an Ed25519 device key held in the Android
 * Keystore — these types describe what the auth API returns, framework-free.
 */

/** An access + refresh token pair issued for a (user, device) session. */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/** The authenticated account. */
data class AuthUser(
    val id: String,
    val username: String,
    val email: String?,
    val emailVerified: Boolean,
)

/** A device (Ed25519 key) registered to authenticate for an account. */
data class AuthDevice(
    val id: String,
    val label: String?,
    /** ISO-8601 timestamps as returned by the server; null when not provided. */
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
)

/** A login challenge whose [nonce] the client signs with its device key. */
data class AuthChallenge(
    val challengeId: String,
    val nonce: String,
)

/**
 * A freshly-started session. Returned by register (with [user]) and by recovery
 * confirm (which issues a new device + tokens but no user payload, so [user] is null).
 */
data class AuthSession(
    val user: AuthUser?,
    val device: AuthDevice,
    val tokens: AuthTokens,
)
