package com.devsgroup.foodmanager.core.network.profile.model

import kotlinx.serialization.Serializable

/**
 * Wire models for the Food Manager profile API (`/users/me`). Field names match the
 * server JSON one-to-one (camelCase), so no @SerialName is needed. Optional fields
 * default to null; the shared [kotlinx.serialization.json.Json] is configured with
 * `explicitNulls = false`, so null request fields are simply omitted.
 */

// ---- Requests ---------------------------------------------------------------

/**
 * Partial update sent to `PATCH /users/me`. `username`/`age` are only changed when
 * present, so omit them (leave null) to leave those fields untouched; `status` and
 * `description` are always sent (default "") since the server treats an empty string
 * the same as an explicit null for clearing those text fields.
 */
@Serializable
data class EditProfileRequest(
    val username: String? = null,
    val name: String? = null,
    val age: Int? = null,
    val status: String = "",
    val description: String = "",
)

@Serializable
data class SetProfileImageRequest(val imageUrl: String)

/** Body for `PUT /users/me/food-preferences` — full-replace list of free-form strings. */
@Serializable
data class SetFoodPreferencesRequest(val preferences: List<String>)

/** Body for `PUT /users/me/food-exceptions` — full-replace list of free-form strings. */
@Serializable
data class SetFoodExceptionsRequest(val exceptions: List<String>)

/** Body for `PUT /users/me/diets` — full-replace list of server-owned diet enum codes. */
@Serializable
data class SetDietsRequest(val diets: List<String>)

// ---- Responses --------------------------------------------------------------

@Serializable
data class ProfileUserDto(
    val id: String,
    val username: String,
    val name: String? = null,
    val imageUrl: String? = null,
    val age: Int? = null,
    val status: String? = null,
    val description: String? = null,
    val isBanned: Boolean = false,
    /**
     * Whether this *account* has been through the onboarding questions — so they're
     * asked once per user rather than once per install. Defaults to false for a response
     * from a server that predates the field; the worst case is asking once more.
     */
    val onboardingCompleted: Boolean = false,
    val foodPreferences: List<String> = emptyList(),
    val foodExceptions: List<String> = emptyList(),
    val diets: List<String> = emptyList(),
)

/** Envelope shared by GET/PATCH `/users/me` and the image set/remove endpoints. */
@Serializable
data class ProfileResponse(val user: ProfileUserDto)
