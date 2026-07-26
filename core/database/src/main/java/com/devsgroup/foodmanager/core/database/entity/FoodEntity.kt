package com.devsgroup.foodmanager.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Cached copy of a dish from `GET /foods/feed` / `GET /foods/:foodId`. List-shaped
 * fields (images, tag categories) are stored as a single delimiter-joined string —
 * the join/split lives in `:core:data`'s entity mapper, next to the DTO mapper.
 *
 * [feedPosition] mirrors the server's keyset feed order so [dao.FoodDao.observeFeed]
 * can reproduce it across process restarts; `null` means the dish isn't currently
 * part of the cached feed (e.g. dropped after a swipe).
 */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val cuisine: String?,
    val imagesCsv: String,
    val calories: Int?,
    val servings: Int?,
    val protein: Int?,
    val fat: Int?,
    val carbs: Int?,
    val allergensCsv: String,
    val dietaryRestrictionsCsv: String,
    val intolerancesCsv: String,
    val featuresCsv: String,
    val dietsCsv: String,
    val createdAt: String,
    val updatedAt: String,
    val feedPosition: Int?,
    val liked: Boolean = false,
)
