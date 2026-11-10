package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.IngredientSource

/**
 * Dishes for tonight from `GET /dinner/suggestions`. The matching rules (allergies,
 * diets, doctor's restrictions, kitchen equipment, pantry) live on the server only —
 * a second copy on the client already drifted once.
 */
interface DinnerRepository {

    /**
     * Network-only; throws [com.devsgroup.foodmanager.core.model.ProfileException] on
     * failure (e.g. `Network` when offline). The returned dishes are also put in the
     * local food cache so the details screen can open them.
     */
    suspend fun suggestions(
        eaterIds: Set<String>,
        includeSelf: Boolean,
        source: IngredientSource,
        lang: String?,
    ): List<DinnerSuggestion>
}
