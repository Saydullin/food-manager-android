package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.TasteProfile
import kotlinx.coroutines.flow.Flow

/** Contract for reading and updating the user's learned taste preferences. */
interface TasteProfileRepository {

    fun tasteProfile(): Flow<TasteProfile>

    suspend fun updateTasteProfile(profile: TasteProfile)

    /** Whether onboarding (initial interest capture) has been completed. */
    fun isOnboardingComplete(): Flow<Boolean>

    suspend fun setOnboardingComplete(complete: Boolean)
}
