package com.devsgroup.foodmanager.core.model

/**
 * The learned preferences that drive personalized recommendations. Built from
 * onboarding answers and continuously refined by the user's swipes.
 */
data class TasteProfile(
    val favoriteCuisines: Set<Cuisine> = emptySet(),
    val dietaryTags: Set<DietaryTag> = emptySet(),
    val dislikedCuisines: Set<Cuisine> = emptySet(),
)

/** The user's reaction to a food card in the discovery deck. */
enum class SwipeAction {
    LIKE,
    DISLIKE,
    SUPER_LIKE,
}

/** A food paired with the confidence that the user will like it. */
data class FoodRecommendation(
    val food: Food,
    /** 0.0..1.0 match score derived from the [TasteProfile]. */
    val matchScore: Float,
)
