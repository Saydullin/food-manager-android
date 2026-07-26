package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.datastore.SettingsPreferencesDataSource
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists app preferences via [SettingsPreferencesDataSource] so notifications/theme/
 * language choices survive across app launches, not just the process lifetime.
 */
@Singleton
class DefaultSettingsRepository @Inject constructor(
    private val settingsPrefs: SettingsPreferencesDataSource,
) : SettingsRepository {

    override fun notificationsEnabled(): Flow<Boolean> = settingsPrefs.notificationsEnabled

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        settingsPrefs.setNotificationsEnabled(enabled)
    }

    override fun darkThemeEnabled(): Flow<Boolean> = settingsPrefs.darkThemeEnabled

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        settingsPrefs.setDarkThemeEnabled(enabled)
    }

    override fun language(): Flow<AppLanguage> = settingsPrefs.language.map { name ->
        name?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.systemDefault()
    }

    override suspend fun setLanguage(language: AppLanguage) {
        settingsPrefs.setLanguage(language.name)
    }
}
