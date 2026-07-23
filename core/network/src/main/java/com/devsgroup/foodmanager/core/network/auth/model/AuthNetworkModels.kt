package com.devsgroup.foodmanager.core.network.auth.model

import kotlinx.serialization.Serializable

/**
 * Wire models for the Food Manager auth API. Field names match the server JSON
 * one-to-one (camelCase), so no @SerialName is needed. Optional fields default to
 * null; the shared [kotlinx.serialization.json.Json] is configured with
 * `explicitNulls = false`, so null request fields are simply omitted.
 */

// ---- Requests ---------------------------------------------------------------

@Serializable
data class RegisterRequest(
    val username: String,
    val publicKey: String,
    val deviceLabel: String? = null,
    val email: String? = null,
)

@Serializable
data class ChallengeRequest(val username: String)

@Serializable
data class VerifyRequest(
    val challengeId: String,
    val signature: String,
    val deviceId: String,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class AddEmailRequest(val email: String)

@Serializable
data class VerifyEmailCodeRequest(val code: String)

@Serializable
data class RequestRecoveryRequest(
    val username: String? = null,
    val email: String? = null,
)

@Serializable
data class ConfirmRecoveryRequest(
    val recoveryToken: String,
    val newPublicKey: String,
    val deviceLabel: String? = null,
)

@Serializable
data class RequestLoginCodeRequest(
    val username: String,
    val email: String,
)

@Serializable
data class ConfirmLoginCodeRequest(
    val username: String,
    val email: String,
    val code: String,
    val newPublicKey: String,
    val deviceLabel: String? = null,
)

@Serializable
data class AddDeviceRequest(
    val publicKey: String,
    val deviceLabel: String? = null,
)

// ---- Responses --------------------------------------------------------------

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val emailVerified: Boolean = false,
)

/** Covers the register (id + label), add-device (+ createdAt), and list (+ lastUsedAt) shapes. */
@Serializable
data class DeviceDto(
    val id: String,
    val deviceLabel: String? = null,
    val lastUsedAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class UsernameAvailabilityResponse(val available: Boolean)

/** Register response: account + first device + a live session. */
@Serializable
data class SessionResponse(
    val user: UserDto,
    val device: DeviceDto,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class ChallengeResponse(
    val challengeId: String,
    val nonce: String,
)

/** verify + refresh both return just the token pair. */
@Serializable
data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
)

/** recovery/confirm returns a new device + session, without a user payload. */
@Serializable
data class RecoveryConfirmResponse(
    val device: DeviceDto,
    val accessToken: String,
    val refreshToken: String,
)

/** Generic `{ "message": "..." }` body used by logout, email, revoke, recovery-request. */
@Serializable
data class MessageResponse(val message: String = "")

/** Server error envelope: `{ "error": { "code": "...", "message": "..." } }`. */
@Serializable
data class ErrorResponse(val error: ApiError) {
    @Serializable
    data class ApiError(
        val code: String = "",
        val message: String = "",
    )
}
