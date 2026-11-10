package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's kitchen equipment, as appliance codes — the local copy of
 * `/users/me/kitchen`. A missing key means "never asked" and is distinct from an empty
 * set ("asked, nothing selected"); the dinner flow shows the equipment step only in the
 * first case. [isPendingSync] marks a local edit the server hasn't received yet.
 */
@Singleton
class KitchenPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val appliances: Flow<Set<String>?> = prefs.map { it[APPLIANCES] }

    suspend fun isPendingSync(): Boolean = prefs.first()[PENDING] == true

    /** A local edit: stored and flagged for the background push. */
    suspend fun setAppliances(codes: Set<String>) {
        dataStore.edit {
            it[APPLIANCES] = codes
            it[PENDING] = true
        }
    }

    /** Clears the pending flag only if the stored value is still the one just pushed. */
    suspend fun markSyncedIfUnchanged(synced: Set<String>) {
        dataStore.edit { if (it[APPLIANCES] == synced) it.remove(PENDING) }
    }

    /** Adopts the server's copy (`null` = never answered). */
    suspend fun saveFromServer(codes: Set<String>?) {
        dataStore.edit {
            if (codes == null) it.remove(APPLIANCES) else it[APPLIANCES] = codes
            it.remove(PENDING)
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(APPLIANCES)
            it.remove(PENDING)
        }
    }

    private companion object {
        val APPLIANCES = stringSetPreferencesKey("kitchen_appliances")
        val PENDING = booleanPreferencesKey("kitchen_appliances_pending")
    }
}
