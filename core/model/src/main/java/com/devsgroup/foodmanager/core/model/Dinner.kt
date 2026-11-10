package com.devsgroup.foodmanager.core.model

/** A catalog ingredient (`GET /ingredients`), e.g. as an item in the user's pantry. */
data class Ingredient(
    val id: String,
    /** Resolved to the requested language with the server's usual fallback. */
    val name: String,
)

/** One dish suggested for tonight's table (`GET /dinner/suggestions`). */
data class DinnerSuggestion(
    val food: Food,
    /** Ingredients not in the pantry — the shopping list. Empty when the pantry is empty. */
    val missingIngredients: List<FoodIngredient>,
)
