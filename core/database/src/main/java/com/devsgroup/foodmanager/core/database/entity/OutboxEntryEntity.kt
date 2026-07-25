package com.devsgroup.foodmanager.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A swipe/interaction waiting to be sent to the server (outbox pattern for offline durability). */
@Entity(tableName = "outbox_entries")
data class OutboxEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: String,
    val action: String,
    val reason: String?,
    val reasonDetail: String?,
)
