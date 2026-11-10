package com.devsgroup.foodmanager.core.network.household.model

import kotlinx.serialization.Serializable

/**
 * Wire models for the cook-dinner household API: eaters, kitchen appliances, pantry and the
 * ingredient catalog. Field names match the server JSON one-to-one (camelCase), so no
 * @SerialName is needed. Enum-like values (sex, allergens, appliances, ...) stay plain strings
 * here; the mappers decode them and drop codes this build does not know.
 */

// ---- Requests ---------------------------------------------------------------

/**
 * Body for `PUT /users/me/household/eaters/{eaterId}` — a full replace, so every list is
 * always sent. `sex`/`age` are left out when null, which the server reads as "unknown".
 */
@Serializable
data class PutEaterRequestDto(
    val name: String,
    val sex: String? = null,
    val age: Int? = null,
    val likes: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val diets: List<String> = emptyList(),
    val medicalRestrictions: List<String> = emptyList(),
)

/** Body for `PUT /users/me/kitchen` — full-replace list of appliance codes (`[]` = "nothing"). */
@Serializable
data class PutKitchenRequestDto(val appliances: List<String>)

/** Body for `PUT /users/me/pantry` — full-replace list of catalog ingredient ids (`[]` empties it). */
@Serializable
data class PutPantryRequestDto(val ingredientIds: List<String>)

// ---- Responses --------------------------------------------------------------

@Serializable
data class EaterDto(
    val id: String,
    val name: String,
    val sex: String? = null,
    val age: Int? = null,
    val likes: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val diets: List<String> = emptyList(),
    val medicalRestrictions: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** GET /users/me/household/eaters — the account owner is not in this list. */
@Serializable
data class EatersResponseDto(val eaters: List<EaterDto> = emptyList())

@Serializable
data class EaterResponseDto(val eater: EaterDto)

/** DELETE /users/me/household/eaters/{eaterId} — idempotent, never 404s. */
@Serializable
data class DeleteEaterResponseDto(val removed: Boolean)

/** `appliances` is null until the user has answered — the client's cue to ask; `[]` is a real answer. */
@Serializable
data class KitchenDto(val appliances: List<String>? = null)

@Serializable
data class KitchenResponseDto(val kitchen: KitchenDto)

@Serializable
data class IngredientDto(
    val id: String,
    val name: String,
)

@Serializable
data class IngredientsResponseDto(val ingredients: List<IngredientDto> = emptyList())

@Serializable
data class PantryItemDto(
    val ingredientId: String,
    val name: String,
)

@Serializable
data class PantryResponseDto(val items: List<PantryItemDto> = emptyList())
