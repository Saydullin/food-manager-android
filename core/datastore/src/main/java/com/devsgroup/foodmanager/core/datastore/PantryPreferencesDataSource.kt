package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** One stored pantry item: the catalog ingredient id plus its display name. */
@Serializable
data class StoredPantryItem(val id: String, val name: String)

/**
 * What the user has at home — the local copy of `/users/me/pantry`. Names are stored
 * alongside ids so the list renders offline. [isPendingSync] marks a local edit the
 * server hasn't received yet (same idea as the kitchen and the profile's taste lists).
 */
@Singleton
class PantryPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val items: Flow<List<StoredPantryItem>> = prefs.map { it[ITEMS].decode() }

    suspend fun isPendingSync(): Boolean = prefs.first()[PENDING] == true

    suspend fun currentItems(): List<StoredPantryItem> = items.first()

    /** A local edit: stored and flagged for the background push. */
    suspend fun setItems(items: List<StoredPantryItem>) {
        dataStore.edit {
            it[ITEMS] = items.encode()
            it[PENDING] = true
        }
    }

    /** Clears the pending flag only if the stored ids are still the ones just pushed. */
    suspend fun markSyncedIfUnchanged(syncedIds: Set<String>) {
        dataStore.edit { prefs ->
            if (prefs[ITEMS].decode().mapTo(HashSet()) { it.id } == syncedIds) prefs.remove(PENDING)
        }
    }

    /** Adopts the server's copy. */
    suspend fun saveFromServer(items: List<StoredPantryItem>) {
        dataStore.edit {
            it[ITEMS] = items.encode()
            it.remove(PENDING)
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(ITEMS)
            it.remove(PENDING)
        }
    }

    private fun List<StoredPantryItem>.encode(): String = JSON.encodeToString(SERIALIZER, this)

    private fun String?.decode(): List<StoredPantryItem> =
        if (this.isNullOrEmpty()) emptyList() else runCatching { JSON.decodeFromString(SERIALIZER, this) }.getOrDefault(emptyList())

    private companion object {
        val ITEMS = stringPreferencesKey("pantry_items")
        val PENDING = booleanPreferencesKey("pantry_items_pending")
        val JSON = Json { ignoreUnknownKeys = true }
        val SERIALIZER = ListSerializer(StoredPantryItem.serializer())
    }
}
