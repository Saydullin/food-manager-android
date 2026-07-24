package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.AuthChallenge
import com.devsgroup.foodmanager.core.model.AuthDevice
import com.devsgroup.foodmanager.core.model.AuthSession
import com.devsgroup.foodmanager.core.model.AuthTokens
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the device-key auth backend. Implemented in `:core:data` on top of
 * the Retrofit `AuthApi` (`:core:network`) and the DataStore token store
 * (`:core:datastore`).
 *
 * The signing/keygen half of the passkey flow (Android Keystore) is intentionally
 * out of scope here: the crypto-producing steps take the resulting material —
 * [register]/[addDevice] take a `publicKey`, [verify] takes a `signature` — as
 * parameters, so the Keystore integration can be layered on later without touching
 * this contract.
 *
 * Every suspend function persists tokens where the server issues them and throws a
 * [com.devsgroup.foodmanager.core.model.AuthException] subtype on failure.
 */
interface AuthRepository {

    /** Current session tokens, or null when signed out. Backed by DataStore. */
    val tokens: Flow<AuthTokens?>

    /** True while a valid access token is stored. */
    val isLoggedIn: Flow<Boolean>

    /**
     * The id of this device's registration, or null if this device has never
     * registered/added a key. Set by [register]/[addDevice]/[confirmRecovery] and, unlike
     * [tokens], survives [logout] — a later [verify] call on the same device needs it.
     */
    val deviceId: Flow<String?>

    /**
     * Emits when the network layer's silent token-refresh tried to rotate an expired
     * access token and failed (the refresh token itself was rejected, or the refresh
     * call errored). Collectors should force a logout and navigate back to the auth
     * flow — the stored tokens are already unusable.
     */
    val sessionExpired: Flow<Unit>

    /** Whether [username] is free to register. */
    suspend fun isUsernameAvailable(username: String): Boolean

    /**
     * Registers a new account with its first device key and starts a session.
     * [publicKey] is the base64 SPKI DER of the device's Ed25519 public key.
     */
    suspend fun register(
        username: String,
        publicKey: String,
        deviceLabel: String? = null,
        email: String? = null,
    ): AuthSession

    /** Login step 1: request a nonce to sign with the device's private key. */
    suspend fun requestChallenge(username: String): AuthChallenge

    /** Login step 2: submit the signed nonce. On success the session is persisted. */
    suspend fun verify(challengeId: String, signature: String, deviceId: String): AuthTokens

    /** Rotates the stored refresh token for a fresh pair. Throws if not logged in. */
    suspend fun refresh(): AuthTokens

    /** Revokes the stored refresh token server-side (best effort) and clears the local session. */
    suspend fun logout()

    // --- Email (require an active session) ---

    /** Adds or changes the account email and triggers a verification email. */
    suspend fun addEmail(email: String)

    /** Verifies a pending email via the link token from the verification email. */
    suspend fun verifyEmailByToken(token: String)

    /** Verifies a pending email via the 6-digit code from the verification email. */
    suspend fun verifyEmailByCode(code: String)

    // --- Devices (require an active session) ---

    /** Lists the devices registered to the current account. */
    suspend fun listDevices(): List<AuthDevice>

    /** Registers an additional device key for the current account. */
    suspend fun addDevice(publicKey: String, deviceLabel: String? = null): AuthDevice

    /** Revokes (removes) a device and any sessions bound to it. */
    suspend fun revokeDevice(deviceId: String)

    // --- Recovery ---

    /**
     * Requests account recovery. Always succeeds silently (anti-enumeration); the
     * server only emails a link if a verified email is on file. Pass a [username]
     * or [email].
     */
    suspend fun requestRecovery(username: String? = null, email: String? = null)

    /**
     * Confirms recovery with the emailed token, registering [newPublicKey] as a new
     * device and starting a session. The returned [AuthSession.user] is null (the
     * server returns only the new device + tokens).
     */
    suspend fun confirmRecovery(
        recoveryToken: String,
        newPublicKey: String,
        deviceLabel: String? = null,
    ): AuthSession

    /**
     * Requests a login code by [username] + [email] — the code-based counterpart to
     * [requestRecovery], for a device with no existing key (e.g. a reinstall). Always
     * succeeds silently (anti-enumeration); the server only emails the 6-digit code if
     * the username has a verified email on file matching [email].
     */
    suspend fun requestLoginCode(username: String, email: String)

    /**
     * Confirms the emailed login code, registering [newPublicKey] as a new device and
     * starting a session. Unlike [confirmRecovery], the returned [AuthSession.user] IS
     * populated (the server returns `{ user, device, accessToken, refreshToken }`).
     */
    suspend fun confirmLoginCode(
        username: String,
        email: String,
        code: String,
        newPublicKey: String,
        deviceLabel: String? = null,
    ): AuthSession
}
