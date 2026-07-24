package com.devsgroup.foodmanager.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Single-row table tracking the feed's pagination cursor across app restarts. */
@Entity(tableName = "feed_meta")
data class FeedMetaEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val nextCursor: String?,
    val hasMore: Boolean,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
