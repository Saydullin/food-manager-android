package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Persists app-wide preferences (notifications, theme, language) across app launches. */
@Singleton
class SettingsPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val prefsFlow = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val notificationsEnabled: Flow<Boolean> = prefsFlow.map { it[NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    // The app's identity is the dark, bold look, so dark theme starts on.
    val darkThemeEnabled: Flow<Boolean> = prefsFlow.map { it[DARK_THEME_ENABLED] ?: true }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        dataStore.edit { it[DARK_THEME_ENABLED] = enabled }
    }

    val language: Flow<String?> = prefsFlow.map { it[LANGUAGE] }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[LANGUAGE] = language }
    }

    private companion object {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("settings_notifications_enabled")
        val DARK_THEME_ENABLED = booleanPreferencesKey("settings_dark_theme_enabled")
        val LANGUAGE = stringPreferencesKey("settings_language")
    }
}
