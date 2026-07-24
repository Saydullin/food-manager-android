package com.devsgroup.foodmanager.core.model

/** A dish/food item the user can be shown and can react to. */
data class Food(
    val id: String,
    val name: String,
    val description: String?,
    /** A code from the server's `GET /cuisines` catalog (e.g. `"ITALIAN"`); open-ended, nullable. */
    val cuisine: String?,
    /** 1..7 photo URLs, ordered (first = cover); empty if none. */
    val images: List<String>,
    val nutrition: Nutrition?,
    val tags: FoodTags,
    val createdAt: String,
    val updatedAt: String,
)

/** Per-serving nutrition (КБЖУ); any field may be unknown. */
data class Nutrition(
    val calories: Int?,
    val servings: Int?,
    val protein: Int?,
    val fat: Int?,
    val carbs: Int?,
)

/** Each category is a set of UPPER_SNAKE server codes (e.g. `"SPICY"`, `"FISH"`). */
data class FoodTags(
    val allergens: Set<String> = emptySet(),
    val dietaryRestrictions: Set<String> = emptySet(),
    val intolerances: Set<String> = emptySet(),
    val features: Set<String> = emptySet(),
    val diets: Set<String> = emptySet(),
)

enum class Cuisine {
    ITALIAN,
    JAPANESE,
    MEXICAN,
    INDIAN,
    CHINESE,
    MEDITERRANEAN,
    AMERICAN,
    THAI,
    FRENCH,
    OTHER,
}

enum class DietaryTag {
    VEGETARIAN,
    VEGAN,
    GLUTEN_FREE,
    DAIRY_FREE,
    HALAL,
    KOSHER,
    LOW_CARB,
    SPICY,
}
