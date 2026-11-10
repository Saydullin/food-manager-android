package com.devsgroup.foodmanager.core.network.dinner.model

import com.devsgroup.foodmanager.core.network.food.model.FoodDto
import kotlinx.serialization.Serializable

/**
 * Wire models for `GET /dinner/suggestions`. Field names match the server JSON one-to-one
 * (camelCase), so no @SerialName is needed. The dish itself is the same [FoodDto] the feed uses.
 */

// ---- Responses --------------------------------------------------------------

/** One line of the shopping list: a recipe ingredient that is not in the pantry. */
@Serializable
data class MissingIngredientDto(
    val ingredientId: String,
    val name: String,
    val amount: Double,
    val unit: String,
)

@Serializable
data class DinnerSuggestionDto(
    val food: FoodDto,
    /** `[]` when the pantry is empty. */
    val missingIngredients: List<MissingIngredientDto> = emptyList(),
)

@Serializable
data class DinnerSuggestionsResponseDto(val suggestions: List<DinnerSuggestionDto> = emptyList())
