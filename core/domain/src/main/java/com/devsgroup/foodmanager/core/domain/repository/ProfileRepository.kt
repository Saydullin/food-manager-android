package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Contract for the signed-in user's own display profile (name, username, bio, ...). */
interface ProfileRepository {

    fun profile(): Flow<UserProfile>

    suspend fun updateProfile(profile: UserProfile)
}
