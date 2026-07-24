package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodTags
import com.devsgroup.foodmanager.core.model.Nutrition
import com.devsgroup.foodmanager.core.network.food.model.FoodDto
import com.devsgroup.foodmanager.core.network.food.model.FoodTagsDto
import com.devsgroup.foodmanager.core.network.food.model.NutritionDto

/** Maps food wire models (`:core:network`) to domain models (`:core:model`). */

fun FoodDto.toDomain() = Food(
    id = id,
    name = name,
    description = description,
    cuisine = cuisine,
    images = images,
    nutrition = nutrition?.toDomain(),
    tags = tags.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun NutritionDto.toDomain() = Nutrition(
    calories = calories,
    servings = servings,
    protein = protein,
    fat = fat,
    carbs = carbs,
)

internal fun FoodTagsDto.toDomain() = FoodTags(
    allergens = allergens.toSet(),
    dietaryRestrictions = dietaryRestrictions.toSet(),
    intolerances = intolerances.toSet(),
    features = features.toSet(),
    diets = diets.toSet(),
)
