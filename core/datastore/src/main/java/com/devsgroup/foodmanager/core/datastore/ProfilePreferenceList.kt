package com.devsgroup.foodmanager.core.datastore

import com.devsgroup.foodmanager.core.model.UserProfile

/**
 * The three taste-profile lists the server exposes as independent full-replace endpoints
 * (`PUT /users/me/food-preferences`, `.../food-exceptions`, `.../diets`). Each is tracked
 * separately for offline sync so a queued diet edit doesn't hold up an unrelated
 * favourites edit — and because the endpoints are whole-list replacements, replaying a
 * queued edit only ever needs the list's *latest* local value, never an ordered log of
 * the individual add/remove taps that produced it.
 */
enum class ProfilePreferenceList {
    FOOD_PREFERENCES,
    FOOD_EXCEPTIONS,
    DIETS,
}

/** This profile's current values for [list]. */
fun UserProfile.valuesOf(list: ProfilePreferenceList): List<String> = when (list) {
    ProfilePreferenceList.FOOD_PREFERENCES -> foodPreferences
    ProfilePreferenceList.FOOD_EXCEPTIONS -> foodExceptions
    ProfilePreferenceList.DIETS -> diets
}

/** A copy of this profile with [list] replaced by [values]. */
fun UserProfile.withValues(list: ProfilePreferenceList, values: List<String>): UserProfile = when (list) {
    ProfilePreferenceList.FOOD_PREFERENCES -> copy(foodPreferences = values)
    ProfilePreferenceList.FOOD_EXCEPTIONS -> copy(foodExceptions = values)
    ProfilePreferenceList.DIETS -> copy(diets = values)
}
