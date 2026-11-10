package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodIngredient
import com.devsgroup.foodmanager.core.model.FoodTags
import com.devsgroup.foodmanager.core.model.Nutrition
import com.devsgroup.foodmanager.core.network.food.model.FoodDto
import com.devsgroup.foodmanager.core.network.food.model.FoodIngredientDto
import com.devsgroup.foodmanager.core.network.food.model.FoodTagsDto
import com.devsgroup.foodmanager.core.network.food.model.NutritionDto
import kotlin.math.roundToInt

/** Maps food wire models (`:core:network`) to domain models (`:core:model`). */

fun FoodDto.toDomain() = Food(
    id = id,
    name = name,
    description = description,
    content = content,
    cuisine = cuisine,
    images = images,
    nutrition = nutrition?.toDomain(),
    tags = tags.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    language = language,
    availableLanguages = availableLanguages,
    ingredients = ingredients.map { it.toDomain() },
)

/** The server stores macros as floats; the domain keeps whole grams. */
internal fun NutritionDto.toDomain() = Nutrition(
    calories = calories,
    servings = servings,
    protein = protein?.roundToInt(),
    fat = fat?.roundToInt(),
    carbs = carbs?.roundToInt(),
)

internal fun FoodIngredientDto.toDomain() = FoodIngredient(
    ingredientId = ingredientId,
    name = name,
    amount = amount,
    unit = unit,
)

internal fun FoodTagsDto.toDomain() = FoodTags(
    allergens = allergens.toSet(),
    dietaryRestrictions = dietaryRestrictions.toSet(),
    intolerances = intolerances.toSet(),
    features = features.toSet(),
    diets = diets.toSet(),
    appliances = appliances.toSet(),
)
