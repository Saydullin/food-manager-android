package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.KitchenAppliance
import kotlinx.coroutines.flow.Flow

/** What cooking equipment the user has. Synced with `/users/me/kitchen`, local-first. */
interface KitchenRepository {

    /**
     * `null` until the user has answered (or skipped) the question — so the flow knows
     * whether to ask. An empty set is a real answer ("nothing listed").
     */
    fun appliances(): Flow<Set<KitchenAppliance>?>

    /** Saved locally at once, pushed in the background. */
    suspend fun setAppliances(appliances: Set<KitchenAppliance>)

    /** Pulls the server's copy unless a local edit is still waiting to sync. */
    suspend fun refresh()
}
