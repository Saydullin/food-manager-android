package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.Eater
import kotlinx.coroutines.flow.Flow

/**
 * The people the user cooks for. Always contains exactly one self eater
 * ([Eater.isSelf], id [Eater.SELF_ID]).
 *
 * The self eater is not stored here: it's the account profile seen as an eater (sex,
 * age and doctor's restrictions plus the profile's allergies/diets/likes), because
 * that's how the server models "me". Everyone else is synced with
 * `/users/me/household/eaters`, local-first.
 *
 * Failures are [com.devsgroup.foodmanager.core.model.ProfileException]s — this is the
 * account's own data, with the same error vocabulary as the profile.
 */
interface HouseholdRepository {

    /** Self first, then everyone else in the order they were added. */
    fun eaters(): Flow<List<Eater>>

    /** Pulls the server's copy. Edits still waiting to sync keep their local value. */
    suspend fun refresh()

    /**
     * Saves an eater. The self eater goes straight to the profile (online; throws on
     * failure). Anyone else is saved locally first and pushed in the background, so this
     * only throws when the server rejects the eater outright (e.g. too many people) —
     * in which case the local change is rolled back.
     */
    suspend fun upsert(eater: Eater)

    /** Removes an eater, local-first. The self eater can't be removed and is ignored. */
    suspend fun delete(eaterId: String)
}
