package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.database.entity.EaterEntity
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.Sex

/** Same delimiter as [FoodEntityMappers]: `avoid` may hold free text, which can contain commas. */
private const val LIST_DELIMITER = "|"

private fun Collection<String>.toCsv(): String = joinToString(LIST_DELIMITER)
private fun String.fromCsv(): List<String> = if (isEmpty()) emptyList() else split(LIST_DELIMITER)

/** Household rows only — the self eater is the profile and never stored here. */
fun EaterEntity.toDomain() = Eater(
    id = id,
    name = name,
    isSelf = false,
    sex = sex.toKnownEnumOrNull<Sex>(),
    age = age,
    likes = likesCsv.fromCsv().toSet(),
    avoid = avoidCsv.fromCsv().toSet(),
    allergens = allergensCsv.fromCsv().toKnownEnumSet<Allergen>(),
    diets = dietsCsv.fromCsv().toKnownEnumSet<DietaryTag>(),
    medical = medicalCsv.fromCsv().toKnownEnumSet<MedicalRestriction>(),
)

fun Eater.toEntity(
    createdAt: Long,
    pendingSync: Boolean,
    localVersion: Long,
    deleted: Boolean = false,
) = EaterEntity(
    id = id,
    name = name,
    sex = sex?.name,
    age = age,
    likesCsv = likes.toCsv(),
    avoidCsv = avoid.toCsv(),
    allergensCsv = allergens.map { it.name }.toCsv(),
    dietsCsv = diets.map { it.name }.toCsv(),
    medicalCsv = medical.map { it.name }.toCsv(),
    createdAt = createdAt,
    pendingSync = pendingSync,
    deleted = deleted,
    localVersion = localVersion,
)
