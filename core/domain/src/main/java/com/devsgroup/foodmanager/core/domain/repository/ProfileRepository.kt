package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Contract for the signed-in user's own display profile (name, username, bio, ...). */
interface ProfileRepository {

    /** True once the account's email has been confirmed by a mailed code. Backed by DataStore. */
    val emailVerified: Flow<Boolean>

    /** True once the server has rejected a request with `ACCOUNT_BANNED`. Backed by DataStore. */
    val isBanned: Flow<Boolean>

    /**
     * True once this account has answered the onboarding questions — the app-level gate
     * sends a signed-in user who hasn't into the onboarding flow instead of the feed.
     *
     * Also true for an account that already carries taste data (diets, exceptions or
     * preferences set from the profile screen, or by onboarding on another device): those
     * users have already told us what onboarding would ask, so asking again would be
     * pointless. That's what makes a reinstall or a second device skip the questions.
     */
    val onboardingComplete: Flow<Boolean>

    /**
     * Emits the cached profile immediately (in-memory, or else whatever's persisted
     * in DataStore) without hitting the network — callers that want fresh data call
     * [refresh] alongside this.
     */
    fun profile(): Flow<UserProfile>

    /** True if a profile is already available to show immediately (in-memory or persisted), without a network call. */
    suspend fun hasCachedProfile(): Boolean

    /**
     * Forces a fresh fetch from the server and pushes the result into whatever's
     * currently collecting [profile] — used for refresh-on-launch and pull-to-refresh,
     * where the process-lifetime cache would otherwise mask server-side changes.
     */
    suspend fun refresh()

    suspend fun updateProfile(profile: UserProfile)

    /**
     * Drops any in-memory cached profile so the next [profile] collection re-reads
     * from DataStore/network instead of returning a stale, previously-cached account's
     * data. Needed around account switches (register/logout) — [profile]'s process-
     * lifetime cache has no other way to know the signed-in account changed underneath it.
     */
    fun invalidateCache()

    /**
     * Uploads a raw image file and returns its public http(s) URL, ready to be passed
     * to [updateProfile]'s `avatarUrl` (the server only accepts already-hosted URLs
     * there, so a picked photo must go through this endpoint first).
     */
    suspend fun uploadAvatarImage(bytes: ByteArray, fileName: String, mimeType: String): String

    /** Full-replaces the user's food preferences (free-form strings); the server sorts/dedupes them. */
    suspend fun updateFoodPreferences(preferences: List<String>)

    /** Full-replaces the user's food exceptions (free-form strings); the server sorts/dedupes them. */
    suspend fun updateFoodExceptions(exceptions: List<String>)

    /** Full-replaces the user's selected diets (server-owned enum codes, e.g. "VEGETARIAN"). */
    suspend fun updateDiets(diets: List<String>)

    /**
     * Records that onboarding has been answered, flipping [onboardingComplete]. Called
     * after the answers themselves are saved, so a user who quits part-way through is
     * asked again rather than left with a half-filled profile.
     */
    suspend fun completeOnboarding()
}
