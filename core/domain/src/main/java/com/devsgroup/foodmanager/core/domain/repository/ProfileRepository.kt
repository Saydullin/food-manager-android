package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Contract for the signed-in user's own display profile (name, username, bio, ...). */
interface ProfileRepository {

    /** True once the account's email has been confirmed by a mailed code. Backed by DataStore. */
    val emailVerified: Flow<Boolean>

    /** True once the server has rejected a request with `ACCOUNT_BANNED`. Backed by DataStore. */
    val isBanned: Flow<Boolean>

    fun profile(): Flow<UserProfile>

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
}
