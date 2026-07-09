package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.TasteProfileRepository
import com.devsgroup.foodmanager.core.model.TasteProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Placeholder implementation. Back this with Preferences DataStore (:core:datastore)
 * to persist the user's learned tastes across sessions.
 */
class DefaultTasteProfileRepository @Inject constructor(
    // TODO: inject the DataStore-backed local source.
) : TasteProfileRepository {

    override fun tasteProfile(): Flow<TasteProfile> = flowOf(TasteProfile())

    override suspend fun updateTasteProfile(profile: TasteProfile) {
        // TODO: persist to DataStore.
    }

    override fun isOnboardingComplete(): Flow<Boolean> = flowOf(false)

    override suspend fun setOnboardingComplete(complete: Boolean) {
        // TODO: persist to DataStore.
    }
}
