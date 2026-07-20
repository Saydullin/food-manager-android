package com.devsgroup.foodmanager.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.devsgroup.foodmanager.core.common.auth.AuthTokenProvider
import com.devsgroup.foodmanager.core.datastore.AuthPreferencesDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    /** Exposes the token store's [AuthTokenProvider] role to the network interceptor. */
    @Binds
    @Singleton
    abstract fun bindsAuthTokenProvider(impl: AuthPreferencesDataSource): AuthTokenProvider

    companion object {
        @Provides
        @Singleton
        fun providesAuthPreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") },
        )
    }
}
