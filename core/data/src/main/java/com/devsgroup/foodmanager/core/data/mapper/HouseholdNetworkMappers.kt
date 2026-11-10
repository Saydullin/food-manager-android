package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.Ingredient
import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.Sex
import com.devsgroup.foodmanager.core.network.household.model.EaterDto
import com.devsgroup.foodmanager.core.network.household.model.IngredientDto
import com.devsgroup.foodmanager.core.network.household.model.KitchenDto
import com.devsgroup.foodmanager.core.network.household.model.PantryItemDto
import com.devsgroup.foodmanager.core.network.household.model.PutEaterRequestDto
import com.devsgroup.foodmanager.core.network.household.model.PutKitchenRequestDto

/**
 * Maps the cook-dinner household wire models (`:core:network`) to domain models and back.
 * The Room-side mappers for the same types live in `EaterMappers.kt`.
 */

/**
 * Decodes server codes into enum values, dropping any this build does not know (a newer
 * server may add codes) instead of throwing — a single unknown code must not fail a whole
 * list response.
 */
internal inline fun <reified T : Enum<T>> Iterable<String>.toKnownEnumSet(): Set<T> {
    val known = enumValues<T>().associateBy { it.name }
    return mapNotNullTo(mutableSetOf()) { known[it] }
}

/** Single-value counterpart of [toKnownEnumSet]; unknown or null codes become null. */
internal inline fun <reified T : Enum<T>> String?.toKnownEnumOrNull(): T? =
    this?.let { code -> enumValues<T>().firstOrNull { it.name == code } }

/** Server eaters are never the account owner (that is the profile), hence `isSelf = false`. */
fun EaterDto.toDomain() = Eater(
    id = id,
    name = name,
    isSelf = false,
    sex = sex.toKnownEnumOrNull<Sex>(),
    age = age,
    likes = likes.toSet(),
    avoid = avoid.toSet(),
    allergens = allergens.toKnownEnumSet<Allergen>(),
    diets = diets.toKnownEnumSet<DietaryTag>(),
    medical = medicalRestrictions.toKnownEnumSet<MedicalRestriction>(),
)

fun Eater.toPutRequest() = PutEaterRequestDto(
    name = name,
    sex = sex?.name,
    age = age,
    likes = likes.toList(),
    avoid = avoid.toList(),
    allergens = allergens.map { it.name },
    diets = diets.map { it.name },
    medicalRestrictions = medical.map { it.name },
)

/** `null` means the user has not answered yet (the cue to ask); an empty set is a real answer. */
fun KitchenDto.toDomain(): Set<KitchenAppliance>? = appliances?.toKnownEnumSet<KitchenAppliance>()

fun Set<KitchenAppliance>.toPutKitchenRequest() = PutKitchenRequestDto(appliances = map { it.name })

fun IngredientDto.toDomain() = Ingredient(id = id, name = name)

fun PantryItemDto.toDomain() = Ingredient(id = ingredientId, name = name)
