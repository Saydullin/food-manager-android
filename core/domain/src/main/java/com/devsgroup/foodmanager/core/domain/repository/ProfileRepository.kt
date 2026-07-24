package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Contract for the signed-in user's own display profile (name, username, bio, ...). */
interface ProfileRepository {

    fun profile(): Flow<UserProfile>

    suspend fun updateProfile(profile: UserProfile)

    /**
     * Uploads a raw image file and returns its public http(s) URL, ready to be passed
     * to [updateProfile]'s `avatarUrl` (the server only accepts already-hosted URLs
     * there, so a picked photo must go through this endpoint first).
     */
    suspend fun uploadAvatarImage(bytes: ByteArray, fileName: String, mimeType: String): String
}
