package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation: holds the user's display profile for the process lifetime.
 * Back this with Preferences DataStore (:core:datastore) to persist it across sessions,
 * and seed it from the registered account once auth exposes the current user.
 */
@Singleton
class DefaultProfileRepository @Inject constructor() : ProfileRepository {

    // Seeded with sample data until registration captures a real display name.
    private val profile = MutableStateFlow(
        UserProfile(name = "Мария", username = "@chef_maria"),
    )

    override fun profile(): Flow<UserProfile> = profile.asStateFlow()

    override suspend fun updateProfile(profile: UserProfile) {
        this.profile.value = profile
    }
}
