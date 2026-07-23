package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devsgroup.foodmanager.core.common.auth.AuthTokenProvider
import kotlinx.coroutines.flow.Flow
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
) : AuthTokenProvider {

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

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
        val DEVICE_ID = stringPreferencesKey("auth_device_id")
        val USERNAME = stringPreferencesKey("auth_username")
    }
}
