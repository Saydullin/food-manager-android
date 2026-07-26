package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.AppLanguage
import com.devsgroup.foodmanager.core.network.settings.model.SettingsDto

/** The domain layer only exposes a light/dark toggle (no tri-state theme yet). */
private const val THEME_DARK = "DARK"

internal fun SettingsDto.toAppLanguage(): AppLanguage = AppLanguage.fromTag(language)

internal fun SettingsDto.isDarkTheme(): Boolean = theme == THEME_DARK

internal fun Boolean.toThemeValue(): String = if (this) THEME_DARK else "LIGHT"
