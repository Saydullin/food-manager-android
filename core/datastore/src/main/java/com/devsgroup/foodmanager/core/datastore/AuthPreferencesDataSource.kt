package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devsgroup.foodmanager.core.common.auth.AccountBanNotifier
import com.devsgroup.foodmanager.core.common.auth.AuthTokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the auth session (access + refresh tokens) in Preferences DataStore and
 * exposes them as flows. Also implements [AuthTokenProvider] so the network layer's
 * interceptor can read the current access token synchronously.
 */
@Singleton
class AuthPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AuthTokenProvider, AccountBanNotifier {

    val accessTokenFlow: Flow<String?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[ACCESS_TOKEN] }

    val refreshTokenFlow: Flow<String?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[REFRESH_TOKEN] }

    /**
     * The server-issued id of this device's registration, set by register()/addDevice()/
     * confirmRecovery(). Survives [clear] (i.e. logout) — it identifies the device's
     * signing key independent of the session, so a later login on the same device can
     * pass it back to `verify()`.
     */
    val deviceIdFlow: Flow<String?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[DEVICE_ID] }

    /**
     * The registered account's username, set by [register] so the profile screen can
     * default to it. Survives [clear], mirroring [deviceIdFlow] — it identifies the
     * account on this device rather than the session.
     */
    val usernameFlow: Flow<String?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[USERNAME] }

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when a silent token refresh (see the network `Authenticator`) failed outright. */
    val sessionExpired: Flow<Unit> = _sessionExpired.asSharedFlow()

    /**
     * True once the server has rejected a request with `ACCOUNT_BANNED` (see the
     * network response interceptor). Persisted rather than in-memory so the ban
     * still shows after a process restart, without requiring another failed call.
     */
    val accountBannedFlow: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[ACCOUNT_BANNED] ?: false }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit {
            it[ACCESS_TOKEN] = accessToken
            it[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun updateDeviceId(deviceId: String) {
        dataStore.edit { it[DEVICE_ID] = deviceId }
    }

    suspend fun updateUsername(username: String) {
        dataStore.edit { it[USERNAME] = username }
    }

    /** Clears the session — call on logout. */
    suspend fun clear() {
        dataStore.edit {
            it.remove(ACCESS_TOKEN)
            it.remove(REFRESH_TOKEN)
        }
    }

    /**
     * Synchronous read for the OkHttp auth interceptor, which runs off the main
     * thread. DataStore keeps its values in an in-memory cache after the first
     * read, so blocking here only touches disk once per process.
     */
    override fun accessToken(): String? = runBlocking {
        runCatching { accessTokenFlow.first() }.getOrNull()
    }

    override fun refreshToken(): String? = runBlocking {
        runCatching { refreshTokenFlow.first() }.getOrNull()
    }

    override fun storeTokens(accessToken: String, refreshToken: String) {
        runBlocking { updateTokens(accessToken, refreshToken) }
    }

    override fun onSessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }

    override fun onAccountBanned() {
        runBlocking { dataStore.edit { it[ACCOUNT_BANNED] = true } }
    }

    /**
     * Syncs the ban flag from the authoritative `isBanned` field on `GET /users/me`,
     * so a lifted ban actually clears the tag instead of being stuck true forever
     * (the interceptor-driven [onAccountBanned] above only ever sets it, never unsets it).
     */
    suspend fun updateAccountBanned(banned: Boolean) {
        dataStore.edit { it[ACCOUNT_BANNED] = banned }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
        val DEVICE_ID = stringPreferencesKey("auth_device_id")
        val USERNAME = stringPreferencesKey("auth_username")
        val ACCOUNT_BANNED = booleanPreferencesKey("auth_account_banned")
    }
}
