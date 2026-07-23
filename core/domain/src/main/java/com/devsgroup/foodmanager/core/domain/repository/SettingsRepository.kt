package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow

/** Contract for app-wide user preferences (notifications, theme, language). */
interface SettingsRepository {

    fun notificationsEnabled(): Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)

    fun darkThemeEnabled(): Flow<Boolean>

    suspend fun setDarkThemeEnabled(enabled: Boolean)

    fun language(): Flow<AppLanguage>

    suspend fun setLanguage(language: AppLanguage)
}
