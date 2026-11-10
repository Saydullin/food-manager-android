package com.devsgroup.foodmanager.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A household member (not the account owner — "me" is the profile). Local copy of
 * `/users/me/household/eaters`, written first and synced in the background.
 *
 * Set-shaped fields are delimiter-joined codes, same convention as [FoodEntity] — the
 * join/split lives in `:core:data`'s mapper.
 *
 * Sync bookkeeping:
 * - [pendingSync] — the local row is newer than the server's (a PUT, or with [deleted]
 *   a DELETE, still has to go out).
 * - [deleted] — a tombstone: hidden from the UI, kept until the server confirms the delete.
 * - [localVersion] — bumped on every local edit, so a push only clears [pendingSync] if
 *   no newer edit landed while it was in flight.
 */
@Entity(tableName = "eaters")
data class EaterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sex: String?,
    val age: Int?,
    val likesCsv: String,
    val avoidCsv: String,
    val allergensCsv: String,
    val dietsCsv: String,
    val medicalCsv: String,
    val createdAt: Long,
    val pendingSync: Boolean = false,
    val deleted: Boolean = false,
    val localVersion: Long = 0,
)
