package com.devsgroup.foodmanager

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.devsgroup.foodmanager.core.data.worker.OutboxScheduler
import com.devsgroup.foodmanager.core.data.worker.ProfileSyncScheduler
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FoodManagerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var outboxScheduler: OutboxScheduler

    @Inject lateinit var profileSyncScheduler: ProfileSyncScheduler

    @Inject lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Catches anything left in the outbox from a process death before its worker ran.
        outboxScheduler.scheduleSync()
        // Likewise for taste-profile edits made offline: the worker no-ops when nothing's queued.
        profileSyncScheduler.scheduleSync()

        // Applies the stored language preference on launch and re-applies it the instant
        // it changes (e.g. from the profile screen), recreating activities to match.
        applicationScope.launch {
            settingsRepository.language().collect { language ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
            }
        }
    }
}
