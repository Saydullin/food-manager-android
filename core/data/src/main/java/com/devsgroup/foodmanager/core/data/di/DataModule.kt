package com.devsgroup.foodmanager.core.data.di

import com.devsgroup.foodmanager.core.data.repository.DefaultFoodRepository
import com.devsgroup.foodmanager.core.data.repository.DefaultTasteProfileRepository
import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.domain.repository.TasteProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds domain repository contracts to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindsFoodRepository(impl: DefaultFoodRepository): FoodRepository

    @Binds
    @Singleton
    abstract fun bindsTasteProfileRepository(impl: DefaultTasteProfileRepository): TasteProfileRepository
}
