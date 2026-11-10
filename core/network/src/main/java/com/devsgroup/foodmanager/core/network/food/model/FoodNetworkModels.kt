package com.devsgroup.foodmanager.core.network.food.model

import kotlinx.serialization.Serializable

/**
 * Wire models for the food feed & swipe API. Field names match the server JSON
 * one-to-one (camelCase), so no @SerialName is needed. Optional fields default to
 * null/empty; the shared [kotlinx.serialization.json.Json] is configured with
 * `explicitNulls = false`, so null request fields are simply omitted.
 */

// ---- Requests ---------------------------------------------------------------

@Serializable
data class InteractionRequestDto(
    val action: String,
    val reason: String? = null,
    val reasonDetail: String? = null,
)

// ---- Responses --------------------------------------------------------------

@Serializable
data class NutritionDto(
    val calories: Int? = null,
    val servings: Int? = null,
    /**
     * Grams per serving. The server stores these as floats (`24.5`), and decoding that into an
     * `Int` throws, so they are `Double` on the wire and rounded in the mapper.
     */
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
)

@Serializable
data class FoodTagsDto(
    val allergens: List<String> = emptyList(),
    val dietaryRestrictions: List<String> = emptyList(),
    val intolerances: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val diets: List<String> = emptyList(),
    /** Equipment the dish needs, as UPPER_SNAKE codes (`"OVEN"`); `[]` = nothing special. */
    val appliances: List<String> = emptyList(),
)

/** One ingredient line of a recipe; `id` is the line itself, `ingredientId` the catalog entry. */
@Serializable
data class FoodIngredientDto(
    val id: String,
    val ingredientId: String,
    val name: String,
    val amount: Double,
    val unit: String,
)

/** A dish, fully shaped: same payload for feed items and single-detail lookups. */
@Serializable
data class FoodDto(
    val id: String,
    val name: String,
    val description: String? = null,
    /** The full recipe article/instructions, as HTML; separate from the short [description]. */
    val content: String? = null,
    val cuisine: String? = null,
    val images: List<String> = emptyList(),
    val nutrition: NutritionDto? = null,
    val tags: FoodTagsDto = FoodTagsDto(),
    val createdAt: String,
    val updatedAt: String,
    /** The language the name/description actually resolved to (server falls back to `en`, then any translation). */
    val language: String = "en",
    /** Every language this dish is translated into. */
    val availableLanguages: List<String> = emptyList(),
    /** The recipe's ingredient lines in authored order; empty for dishes without a list. */
    val ingredients: List<FoodIngredientDto> = emptyList(),
)

/** GET /foods/feed — keyset (cursor) paginated. */
@Serializable
data class FeedResponseDto(
    val items: List<FoodDto>,
    val nextCursor: String? = null,
    val hasMore: Boolean,
)

@Serializable
data class FoodDetailResponseDto(val food: FoodDto)

@Serializable
data class InteractionDto(
    val foodId: String,
    val action: String,
    val reason: String? = null,
    val reasonDetail: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class InteractionResponseDto(val interaction: InteractionDto)

/** DELETE /foods/:foodId/interactions — idempotent undo, never 404s. */
@Serializable
data class DeleteInteractionResponseDto(val removed: Boolean)
