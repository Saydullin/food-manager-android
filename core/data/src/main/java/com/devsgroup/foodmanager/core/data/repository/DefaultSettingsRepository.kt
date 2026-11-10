package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.common.di.Dispatcher
import com.devsgroup.foodmanager.core.common.di.FoodDispatcher
import com.devsgroup.foodmanager.core.data.mapper.isDarkTheme
import com.devsgroup.foodmanager.core.data.mapper.toAppLanguage
import com.devsgroup.foodmanager.core.data.mapper.toThemeValue
import com.devsgroup.foodmanager.core.datastore.SettingsPreferencesDataSource
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.model.AppLanguage
import com.devsgroup.foodmanager.core.network.settings.SettingsApi
import com.devsgroup.foodmanager.core.network.settings.model.SettingsDto
import com.devsgroup.foodmanager.core.network.settings.model.UpdateSettingsRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs the signed-in user's settings with the real `/users/me/settings` endpoints
 * ([SettingsApi]), syncing the server's copy on first read and after every update.
 *
 * Reads never wait on the network: the first collector gets the last-known local
 * values ([SettingsPreferencesDataSource], or defaults) at once, and the server's copy
 * replaces them when it arrives. That matters because the app-level splash waits on
 * [darkThemeEnabled] — blocking it on the fetch held the splash for the full OkHttp
 * connect timeout (~10 s) whenever the server was unreachable.
 */
@Singleton
class DefaultSettingsRepository @Inject constructor(
    private val settingsApi: SettingsApi,
    private val settingsPrefs: SettingsPreferencesDataSource,
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    private val cache = MutableStateFlow<SettingsDto?>(null)

    /** One server fetch per process; reset on failure so the next reader retries. */
    private val serverFetchStarted = AtomicBoolean(false)

    /** Bumped by every local edit, so a fetch that started before it can't overwrite it. */
    private val localEdits = AtomicInteger(0)

    private val settingsFlow: Flow<SettingsDto> = channelFlow {
        if (cache.value == null) cache.value = localSnapshot()
        // Tied to this collector's scope: if nobody is listening any more, there's no
        // one to deliver the result to, and the next collector starts a fresh attempt.
        if (serverFetchStarted.compareAndSet(false, true)) launch { refreshFromServer() }
        cache.filterNotNull().collect { send(it) }
    }

    override fun notificationsEnabled(): Flow<Boolean> = settingsFlow.map { it.pushNotificationsEnabled }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        updateSettings(UpdateSettingsRequest(pushNotificationsEnabled = enabled))
    }

    override fun darkThemeEnabled(): Flow<Boolean> = settingsFlow.map { it.isDarkTheme() }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        updateSettings(UpdateSettingsRequest(theme = enabled.toThemeValue()))
    }

    override fun language(): Flow<AppLanguage> = settingsFlow.map { it.toAppLanguage() }

    override suspend fun setLanguage(language: AppLanguage) {
        updateSettings(UpdateSettingsRequest(language = language.tag))
    }

    /** Adopts the server's copy, unless a local edit landed while the request was in flight. */
    private suspend fun refreshFromServer() {
        val editsBefore = localEdits.get()
        try {
            val server = apiCall { settingsApi.mySettings().settings }
            if (localEdits.get() == editsBefore) {
                cache.value = server
                persistLocally(server)
            }
        } catch (e: CancellationException) {
            serverFetchStarted.set(false)
            throw e
        } catch (e: IOException) {
            // Offline: keep showing the local values; a later reader tries again.
            serverFetchStarted.set(false)
        } catch (e: HttpException) {
            serverFetchStarted.set(false)
        }
    }

    /**
     * Applies [request] to the local cache immediately so the UI (a switch, a radio
     * choice) flips right away instead of waiting on the network round trip, then
     * syncs it to the server in the background. On failure the optimistic value is
     * kept — same offline-friendly stance as [refreshFromServer].
     */
    private suspend fun updateSettings(request: UpdateSettingsRequest) {
        localEdits.incrementAndGet()
        val current = cache.value ?: localSnapshot()
        val optimistic = current.copy(
            language = request.language ?: current.language,
            theme = request.theme ?: current.theme,
            pushNotificationsEnabled = request.pushNotificationsEnabled ?: current.pushNotificationsEnabled,
        )
        cache.value = optimistic
        persistLocally(optimistic)
        try {
            val updated = apiCall { settingsApi.updateSettings(request).settings }
            cache.value = updated
            persistLocally(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Offline — keep the optimistic value, same as a failed refresh.
        } catch (e: HttpException) {
            // Server rejected it — keep the optimistic value rather than reverting mid-interaction.
        }
    }

    /** What the app showed before this session ever talked to the server, for offline use. */
    private suspend fun localSnapshot(): SettingsDto = SettingsDto(
        language = settingsPrefs.language.first() ?: AppLanguage.systemDefault().tag,
        theme = settingsPrefs.darkThemeEnabled.first().toThemeValue(),
        pushNotificationsEnabled = settingsPrefs.notificationsEnabled.first(),
    )

    private suspend fun persistLocally(settings: SettingsDto) {
        settingsPrefs.setLanguage(settings.language)
        settingsPrefs.setDarkThemeEnabled(settings.isDarkTheme())
        settingsPrefs.setNotificationsEnabled(settings.pushNotificationsEnabled)
    }

    private suspend fun <T> apiCall(block: suspend () -> T): T = withContext(ioDispatcher) { block() }
}
