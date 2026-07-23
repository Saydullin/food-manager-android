package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation: holds app preferences for the process lifetime. Back this
 * with Preferences DataStore (:core:datastore) to persist them across sessions.
 */
@Singleton
class DefaultSettingsRepository @Inject constructor() : SettingsRepository {

    private val notificationsEnabled = MutableStateFlow(true)

    // The app's identity is the dark, bold look, so dark theme starts on.
    private val darkThemeEnabled = MutableStateFlow(true)

    private val language = MutableStateFlow(AppLanguage.RUSSIAN)

    override fun notificationsEnabled(): Flow<Boolean> = notificationsEnabled.asStateFlow()

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
    }

    override fun darkThemeEnabled(): Flow<Boolean> = darkThemeEnabled.asStateFlow()

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        darkThemeEnabled.value = enabled
    }

    override fun language(): Flow<AppLanguage> = language.asStateFlow()

    override suspend fun setLanguage(language: AppLanguage) {
        this.language.value = language
    }
}
