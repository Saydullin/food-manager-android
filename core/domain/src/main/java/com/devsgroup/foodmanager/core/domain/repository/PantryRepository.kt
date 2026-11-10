package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.Ingredient
import kotlinx.coroutines.flow.Flow

/** What the user has at home ("из того, что есть дома"). Synced with `/users/me/pantry`. */
interface PantryRepository {

    /** Alphabetical by name. */
    fun items(): Flow<List<Ingredient>>

    /** Replaces the pantry. Saved locally at once, pushed in the background. */
    suspend fun setItems(items: List<Ingredient>)

    /**
     * Searches the ingredient catalog by name (any language). Network-only; throws a
     * [com.devsgroup.foodmanager.core.model.ProfileException] when offline.
     */
    suspend fun search(query: String): List<Ingredient>

    /** Pulls the server's copy unless a local edit is still waiting to sync. */
    suspend fun refresh()
}
