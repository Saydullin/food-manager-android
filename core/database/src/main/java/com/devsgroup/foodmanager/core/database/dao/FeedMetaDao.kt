package com.devsgroup.foodmanager.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.devsgroup.foodmanager.core.database.entity.FeedMetaEntity

@Dao
interface FeedMetaDao {

    @Query("SELECT * FROM feed_meta WHERE id = ${FeedMetaEntity.SINGLETON_ID}")
    suspend fun get(): FeedMetaEntity?

    @Upsert
    suspend fun upsert(meta: FeedMetaEntity)
}
