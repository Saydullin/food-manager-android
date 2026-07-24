package com.devsgroup.foodmanager.core.database.di

import android.content.Context
import androidx.room3.Room
import com.devsgroup.foodmanager.core.database.FoodDatabase
import com.devsgroup.foodmanager.core.database.dao.FeedMetaDao
import com.devsgroup.foodmanager.core.database.dao.FoodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesFoodDatabase(@ApplicationContext context: Context): FoodDatabase =
        Room.databaseBuilder(context, FoodDatabase::class.java, "food-manager.db").build()

    @Provides
    fun providesFoodDao(database: FoodDatabase): FoodDao = database.foodDao()

    @Provides
    fun providesFeedMetaDao(database: FoodDatabase): FeedMetaDao = database.feedMetaDao()
}
