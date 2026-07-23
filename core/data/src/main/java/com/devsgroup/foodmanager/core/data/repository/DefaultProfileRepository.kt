package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.datastore.AuthPreferencesDataSource
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation: holds profile edits for the process lifetime, defaulting
 * to the registered account's username until the user customizes it from the
 * edit-profile screen — that edit then wins over the auth-sourced default.
 * Back this with Preferences DataStore (:core:datastore) to persist edits across sessions.
 */
@Singleton
class DefaultProfileRepository @Inject constructor(
    private val authPrefs: AuthPreferencesDataSource,
) : ProfileRepository {

    private val edited = MutableStateFlow<UserProfile?>(null)

    override fun profile(): Flow<UserProfile> =
        combine(authPrefs.usernameFlow, edited) { username, edited ->
            edited ?: defaultProfile(username)
        }

    override suspend fun updateProfile(profile: UserProfile) {
        edited.value = profile
    }

    private fun defaultProfile(username: String?): UserProfile {
        val handle = username?.trim().orEmpty()
        return UserProfile(name = handle, username = if (handle.isEmpty()) "" else "@$handle")
    }
}
