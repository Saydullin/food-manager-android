package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.FoodIngredient
import com.devsgroup.foodmanager.core.network.dinner.model.DinnerSuggestionDto
import com.devsgroup.foodmanager.core.network.dinner.model.MissingIngredientDto

/** Maps `GET /dinner/suggestions` wire models (`:core:network`) to domain models (`:core:model`). */

fun DinnerSuggestionDto.toDomain() = DinnerSuggestion(
    food = food.toDomain(),
    missingIngredients = missingIngredients.map { it.toDomain() },
)

internal fun MissingIngredientDto.toDomain() = FoodIngredient(
    ingredientId = ingredientId,
    name = name,
    amount = amount,
    unit = unit,
)
