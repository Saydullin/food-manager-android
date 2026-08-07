package com.devsgroup.foodmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the signed-in user's own [UserProfile] in Preferences DataStore so the
 * Profile screen has something to show offline (e.g. right after registration, or on
 * a later launch with no connectivity) instead of only relying on the in-memory,
 * process-lifetime cache in `DefaultProfileRepository`.
 *
 * This is also the durable store behind offline taste-profile editing: each
 * [ProfilePreferenceList] carries a "pending sync" flag marking that its local value is
 * newer than the server's, so `ProfileSyncWorker` can replay it once connectivity returns.
 */
@Singleton
class ProfilePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val profileFlow: Flow<UserProfile?> = preferences.map { it.toUserProfile() }

    /** The lists whose local value hasn't reached the server yet. */
    val pendingSyncListsFlow: Flow<Set<ProfilePreferenceList>> = preferences.map { prefs ->
        ProfilePreferenceList.entries.filterTo(mutableSetOf()) { prefs[it.pendingKey] == true }
    }

    /**
     * Whether this device has taken the signed-in account through the onboarding
     * questions. Stored locally rather than derived from the server because "I have no
     * diets and no allergies" is a perfectly good set of answers, and it is
     * indistinguishable on the wire from never having been asked — deriving it would
     * re-ask those users forever. Cleared with the rest of the profile on logout, so the
     * next account to sign in gets asked for itself.
     */
    val onboardingCompleteFlow: Flow<Boolean> = preferences.map { it[ONBOARDING_COMPLETE] == true }

    suspend fun setOnboardingComplete() {
        dataStore.edit { prefs -> prefs[ONBOARDING_COMPLETE] = true }
    }

    suspend fun currentProfile(): UserProfile? = profileFlow.first()

    suspend fun pendingSyncLists(): Set<ProfilePreferenceList> = pendingSyncListsFlow.first()

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
            for (list in ProfilePreferenceList.entries) {
                prefs[list.valueKey] = profile.valuesOf(list).encode()
            }
        }
    }

    /**
     * Replaces a single list's stored value, recording whether it still needs pushing to
     * the server. Touches only that list, so it can't clobber a sibling list that's
     * mid-edit or still queued.
     */
    suspend fun saveList(list: ProfilePreferenceList, values: List<String>, pendingSync: Boolean) {
        dataStore.edit { prefs ->
            prefs[list.valueKey] = values.encode()
            if (pendingSync) prefs[list.pendingKey] = true else prefs.remove(list.pendingKey)
        }
    }

    /**
     * Marks [list] as delivered, but only if its stored value is still the [synced] one the
     * server just accepted. An edit made while that push was in flight leaves the flag set
     * instead, so the newer value gets a push of its own rather than being silently counted
     * as delivered (and then overwritten by the server's older copy on the next refresh).
     *
     * Returns whether the flag was actually cleared.
     */
    suspend fun clearPendingSyncIfUnchanged(list: ProfilePreferenceList, synced: List<String>): Boolean {
        var cleared = false
        dataStore.edit { prefs ->
            cleared = prefs[list.valueKey] == synced.encode()
            if (cleared) prefs.remove(list.pendingKey)
        }
        return cleared
    }

    /**
     * Wipes the stored profile and any queued edits — call on logout, so the next account
     * to sign in on this device neither sees the previous user's profile nor pushes their
     * leftover pending lists up to its own account.
     */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(NAME)
            prefs.remove(USERNAME)
            prefs.remove(EMAIL)
            prefs.remove(EMAIL_VERIFIED)
            prefs.remove(AVATAR_URL)
            prefs.remove(STATUS_EMOJI)
            prefs.remove(AGE)
            prefs.remove(COOKING_ATTITUDE)
            prefs.remove(BIO)
            prefs.remove(ONBOARDING_COMPLETE)
            for (list in ProfilePreferenceList.entries) {
                prefs.remove(list.valueKey)
                prefs.remove(list.pendingKey)
            }
        }
    }

    private fun String?.putOrRemove(prefs: MutablePreferences, key: Preferences.Key<String>) {
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
            foodPreferences = this[FOOD_PREFERENCES].decodeList(),
            foodExceptions = this[FOOD_EXCEPTIONS].decodeList(),
            diets = this[DIETS].decodeList(),
        )
    }

    private val ProfilePreferenceList.valueKey: Preferences.Key<String>
        get() = when (this) {
            ProfilePreferenceList.FOOD_PREFERENCES -> FOOD_PREFERENCES
            ProfilePreferenceList.FOOD_EXCEPTIONS -> FOOD_EXCEPTIONS
            ProfilePreferenceList.DIETS -> DIETS
        }

    private val ProfilePreferenceList.pendingKey: Preferences.Key<Boolean>
        get() = when (this) {
            ProfilePreferenceList.FOOD_PREFERENCES -> FOOD_PREFERENCES_PENDING
            ProfilePreferenceList.FOOD_EXCEPTIONS -> FOOD_EXCEPTIONS_PENDING
            ProfilePreferenceList.DIETS -> DIETS_PENDING
        }

    /**
     * Tags are free-form user text, so they're stored JSON-encoded rather than delimiter-joined
     * (no escaping to get wrong) and as a list rather than a `stringSet` (which would shuffle the
     * user's chips on every launch).
     */
    private fun List<String>.encode(): String = JSON.encodeToString(STRING_LIST, this)

    private fun String?.decodeList(): List<String> = when {
        isNullOrBlank() -> emptyList()
        else -> runCatching { JSON.decodeFromString(STRING_LIST, this) }.getOrDefault(emptyList())
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
        val ONBOARDING_COMPLETE = booleanPreferencesKey("profile_onboarding_complete")

        val FOOD_PREFERENCES = stringPreferencesKey("profile_food_preferences")
        val FOOD_EXCEPTIONS = stringPreferencesKey("profile_food_exceptions")
        val DIETS = stringPreferencesKey("profile_diets")

        val FOOD_PREFERENCES_PENDING = booleanPreferencesKey("profile_food_preferences_pending")
        val FOOD_EXCEPTIONS_PENDING = booleanPreferencesKey("profile_food_exceptions_pending")
        val DIETS_PENDING = booleanPreferencesKey("profile_diets_pending")

        val JSON = Json
        val STRING_LIST = ListSerializer(String.serializer())
    }
}
