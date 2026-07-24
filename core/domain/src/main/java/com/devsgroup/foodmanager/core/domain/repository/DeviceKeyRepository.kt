package com.devsgroup.foodmanager.core.domain.repository

/**
 * The device's signing key that proves possession for [AuthRepository]'s
 * challenge/verify login step. Implemented in `:core:data` against the Android
 * Keystore — the key never leaves it, so this contract only exposes the
 * operations that can be satisfied without touching key material directly:
 * asking for the public key (to hand the server at register/addDevice time) and
 * signing a server-issued nonce (to prove the private key at login time).
 */
interface DeviceKeyRepository {

    /** True once this device holds a signing key from a prior register()/addDevice() call. */
    suspend fun hasDeviceKey(): Boolean

    /** The base64 SPKI DER public key, generating the device's key pair first if absent. */
    suspend fun getOrCreatePublicKey(): String

    /** Signs [nonce] with the device's private key, base64-encoded. */
    suspend fun sign(nonce: String): String

    /**
     * Discards any existing signing key so the next [getOrCreatePublicKey] mints a
     * fresh one. Needed before registering a brand-new account on a device that
     * already holds a key from a previously logged-out account — the server
     * rejects a public key it has already registered to another account/device.
     */
    suspend fun deleteDeviceKey()
}
