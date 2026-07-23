package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.TasteProfileRepository
import com.devsgroup.foodmanager.core.model.TasteProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation: holds the taste profile for the process lifetime. Back this
 * with Preferences DataStore (:core:datastore) to persist it across sessions.
 */
@Singleton
class DefaultTasteProfileRepository @Inject constructor() : TasteProfileRepository {

    // Seeded with a couple of sample tags so the profile screen has something to show
    // before onboarding/manual editing populates it for real.
    private val tasteProfile = MutableStateFlow(
        TasteProfile(favoriteTags = setOf("Острое", "Итальянская")),
    )
    private val onboardingComplete = MutableStateFlow(false)

    override fun tasteProfile(): Flow<TasteProfile> = tasteProfile.asStateFlow()

    override suspend fun updateTasteProfile(profile: TasteProfile) {
        tasteProfile.value = profile
    }

    override fun isOnboardingComplete(): Flow<Boolean> = onboardingComplete.asStateFlow()

    override suspend fun setOnboardingComplete(complete: Boolean) {
        onboardingComplete.value = complete
    }
}
