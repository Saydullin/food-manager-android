package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.database.entity.FoodEntity
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodTags
import com.devsgroup.foodmanager.core.model.Nutrition

/**
 * Maps the domain [Food] to/from its cached Room shape ([FoodEntity]). List-shaped
 * fields (images, tag categories) are packed into a single delimited column instead
 * of reaching for Room's TypeConverter API — the join/split lives entirely here.
 */
private const val LIST_DELIMITER = "|"

private fun List<String>.toCsv(): String = joinToString(LIST_DELIMITER)
private fun String.fromCsv(): List<String> = if (isEmpty()) emptyList() else split(LIST_DELIMITER)

fun FoodEntity.toDomain() = Food(
    id = id,
    name = name,
    description = description,
    cuisine = cuisine,
    images = imagesCsv.fromCsv(),
    nutrition = if (calories == null && servings == null && protein == null && fat == null && carbs == null) {
        null
    } else {
        Nutrition(calories = calories, servings = servings, protein = protein, fat = fat, carbs = carbs)
    },
    tags = FoodTags(
        allergens = allergensCsv.fromCsv().toSet(),
        dietaryRestrictions = dietaryRestrictionsCsv.fromCsv().toSet(),
        intolerances = intolerancesCsv.fromCsv().toSet(),
        features = featuresCsv.fromCsv().toSet(),
        diets = dietsCsv.fromCsv().toSet(),
    ),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** [feedPosition]: the dish's position in the server's feed order, or `null` if it's not (or no longer) in the feed. */
fun Food.toEntity(feedPosition: Int?) = FoodEntity(
    id = id,
    name = name,
    description = description,
    cuisine = cuisine,
    imagesCsv = images.toCsv(),
    calories = nutrition?.calories,
    servings = nutrition?.servings,
    protein = nutrition?.protein,
    fat = nutrition?.fat,
    carbs = nutrition?.carbs,
    allergensCsv = tags.allergens.toList().toCsv(),
    dietaryRestrictionsCsv = tags.dietaryRestrictions.toList().toCsv(),
    intolerancesCsv = tags.intolerances.toList().toCsv(),
    featuresCsv = tags.features.toList().toCsv(),
    dietsCsv = tags.diets.toList().toCsv(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    feedPosition = feedPosition,
)
