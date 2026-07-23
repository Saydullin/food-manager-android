package com.devsgroup.foodmanager.core.model

/**
 * The learned preferences that drive personalized recommendations. Built from
 * onboarding answers and continuously refined by the user's swipes, but also
 * hand-editable from the profile screen — so likes/avoids are free-text tags
 * rather than a closed [Cuisine] enum.
 */
data class TasteProfile(
    val favoriteTags: Set<String> = emptySet(),
    val avoidTags: Set<String> = emptySet(),
    val dietaryTags: Set<DietaryTag> = emptySet(),
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
