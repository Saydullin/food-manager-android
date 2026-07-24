package com.devsgroup.foodmanager.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.devsgroup.foodmanager.core.database.dao.FeedMetaDao
import com.devsgroup.foodmanager.core.database.dao.FoodDao
import com.devsgroup.foodmanager.core.database.entity.FeedMetaEntity
import com.devsgroup.foodmanager.core.database.entity.FoodEntity

@Database(entities = [FoodEntity::class, FeedMetaEntity::class], version = 1, exportSchema = false)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun feedMetaDao(): FeedMetaDao
}
