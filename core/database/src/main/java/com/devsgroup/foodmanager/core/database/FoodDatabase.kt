package com.devsgroup.foodmanager.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.devsgroup.foodmanager.core.database.dao.EaterDao
import com.devsgroup.foodmanager.core.database.dao.FeedMetaDao
import com.devsgroup.foodmanager.core.database.dao.FoodDao
import com.devsgroup.foodmanager.core.database.dao.OutboxDao
import com.devsgroup.foodmanager.core.database.entity.EaterEntity
import com.devsgroup.foodmanager.core.database.entity.FeedMetaEntity
import com.devsgroup.foodmanager.core.database.entity.FoodEntity
import com.devsgroup.foodmanager.core.database.entity.OutboxEntryEntity

@Database(
    entities = [FoodEntity::class, FeedMetaEntity::class, OutboxEntryEntity::class, EaterEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun feedMetaDao(): FeedMetaDao
    abstract fun outboxDao(): OutboxDao
    abstract fun eaterDao(): EaterDao
}
