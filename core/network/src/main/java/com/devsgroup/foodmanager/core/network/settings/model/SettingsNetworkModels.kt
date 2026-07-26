package com.devsgroup.foodmanager.core.network.settings.model

import kotlinx.serialization.Serializable

/**
 * Wire models for the Food Manager settings API. Field names match the server JSON
 * one-to-one (camelCase). `theme` is one of `"LIGHT"`, `"DARK"`, `"SYSTEM"`; `language`
 * is a BCP-47 tag (e.g. `"en"`, `"ru"`). The settings object is treated as open by the
 * server (more fields may be added over time), so unknown keys are simply ignored by
 * the shared [kotlinx.serialization.json.Json] config.
 */

// ---- Requests ---------------------------------------------------------------

/** Partial update — send only the fields that changed; the rest keep their current value. */
@Serializable
data class UpdateSettingsRequest(
    val language: String? = null,
    val theme: String? = null,
    val pushNotificationsEnabled: Boolean? = null,
)

// ---- Responses --------------------------------------------------------------

@Serializable
data class SettingsDto(
    val language: String,
    val theme: String,
    val pushNotificationsEnabled: Boolean,
)

/** Both GET and PATCH `/users/me/settings` return this envelope. */
@Serializable
data class SettingsResponse(val settings: SettingsDto)
