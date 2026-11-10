package com.devsgroup.foodmanager.core.model

/** A dish/food item the user can be shown and can react to. */
data class Food(
    val id: String,
    val name: String,
    val description: String?,
    /** The full recipe article/instructions, as HTML; separate from the short [description]. */
    val content: String?,
    /** A code from the server's `GET /cuisines` catalog (e.g. `"ITALIAN"`); open-ended, nullable. */
    val cuisine: String?,
    /** 1..7 photo URLs, ordered (first = cover); empty if none. */
    val images: List<String>,
    val nutrition: Nutrition?,
    val tags: FoodTags,
    val createdAt: String,
    val updatedAt: String,
    /** BCP-47 tag the name/description actually resolved to (server-side translation fallback). */
    val language: String = "en",
    /** Every language this dish is translated into. */
    val availableLanguages: List<String> = emptyList(),
    /**
     * The recipe's ingredient lines, in authored order. Empty when unknown — the Room
     * cache doesn't store them, so a dish read back from the feed cache has none.
     */
    val ingredients: List<FoodIngredient> = emptyList(),
)

/** One ingredient line of a recipe: a catalog ingredient and how much of it. */
data class FoodIngredient(
    val ingredientId: String,
    val name: String,
    val amount: Double,
    /** Free text from the recipe author, e.g. `"g"`, `"ml"`, `"pcs"`. */
    val unit: String,
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
    /** Equipment the dish needs, as [KitchenAppliance] names (`"OVEN"`); empty = nothing special. */
    val appliances: Set<String> = emptySet(),
)

/**
 * Mirrors the server's fixed `GET /cuisines` catalog, in its `sortOrder`. Each entry's
 * `name` *is* the wire code, which is what makes these safe to send straight through as
 * taste preferences — the feed's ranking matches a preference against cuisine codes, so
 * a value that isn't a real code simply never scores.
 */
enum class Cuisine {
    ITALIAN,
    JAPANESE,
    CHINESE,
    MEXICAN,
    INDIAN,
    THAI,
    FRENCH,
    AMERICAN,
    MEDITERRANEAN,
    KOREAN,
}

/**
 * The allergens a dish can be tagged with, as the server's UPPER_SNAKE codes (its
 * `food_allergens` columns). Deliberately the *complete* list rather than a common
 * subset: this backs the onboarding allergy picker, and an allergen missing from the
 * picker is one the user cannot declare — and therefore one the feed can't filter on.
 */
enum class Allergen {
    MILK,
    EGGS,
    PEANUTS,
    TREE_NUTS,
    SOY,
    WHEAT,
    GLUTEN,
    FISH,
    SHELLFISH,
    SESAME,
    MUSTARD,
    CELERY,
    LUPIN,
    MOLLUSCS,
    SULFITES,
}

/** Mirrors the server's fixed `GET /diets` catalog (see `add_diets` migration) exactly — an entry here that the server doesn't know sends an unrecognized code and 400s. */
enum class DietaryTag {
    VEGETARIAN,
    VEGAN,
    HALAL,
    LACTOSE_FREE,
    GLUTEN_FREE,
}
