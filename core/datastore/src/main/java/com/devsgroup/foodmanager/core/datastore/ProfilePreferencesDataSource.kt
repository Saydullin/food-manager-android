package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devsgroup.foodmanager.core.model.CookingAttitude
import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the signed-in user's own [UserProfile] in Preferences DataStore so the
 * Profile screen has something to show offline (e.g. right after registration, or on
 * a later launch with no connectivity) instead of only relying on the in-memory,
 * process-lifetime cache in `DefaultProfileRepository`.
 */
@Singleton
class ProfilePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val profileFlow: Flow<UserProfile?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserProfile() }

    suspend fun currentProfile(): UserProfile? = profileFlow.first()

    suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[NAME] = profile.name
            prefs[USERNAME] = profile.username
            prefs[EMAIL_VERIFIED] = profile.emailVerified
            profile.email.putOrRemove(prefs, EMAIL)
            profile.avatarUrl.putOrRemove(prefs, AVATAR_URL)
            profile.statusEmoji.putOrRemove(prefs, STATUS_EMOJI)
            profile.bio.putOrRemove(prefs, BIO)
            val age = profile.age
            if (age != null) prefs[AGE] = age else prefs.remove(AGE)
            profile.cookingAttitude?.name.putOrRemove(prefs, COOKING_ATTITUDE)
        }
    }

    private fun String?.putOrRemove(prefs: androidx.datastore.preferences.core.MutablePreferences, key: Preferences.Key<String>) {
        if (this != null) prefs[key] = this else prefs.remove(key)
    }

    private fun Preferences.toUserProfile(): UserProfile? {
        val name = this[NAME] ?: return null
        val username = this[USERNAME] ?: return null
        return UserProfile(
            name = name,
            username = username,
            email = this[EMAIL],
            emailVerified = this[EMAIL_VERIFIED] ?: false,
            avatarUrl = this[AVATAR_URL],
            statusEmoji = this[STATUS_EMOJI],
            age = this[AGE],
            cookingAttitude = this[COOKING_ATTITUDE]?.let { runCatching { CookingAttitude.valueOf(it) }.getOrNull() },
            bio = this[BIO],
        )
    }

    private companion object {
        val NAME = stringPreferencesKey("profile_name")
        val USERNAME = stringPreferencesKey("profile_username")
        val EMAIL = stringPreferencesKey("profile_email")
        val EMAIL_VERIFIED = booleanPreferencesKey("profile_email_verified")
        val AVATAR_URL = stringPreferencesKey("profile_avatar_url")
        val STATUS_EMOJI = stringPreferencesKey("profile_status_emoji")
        val AGE = intPreferencesKey("profile_age")
        val COOKING_ATTITUDE = stringPreferencesKey("profile_cooking_attitude")
        val BIO = stringPreferencesKey("profile_bio")
    }
}
