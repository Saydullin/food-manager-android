package com.devsgroup.foodmanager.core.model

/** A dish/food item the user can be shown and can react to. */
data class Food(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val cuisine: Cuisine,
    val dietaryTags: Set<DietaryTag>,
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
