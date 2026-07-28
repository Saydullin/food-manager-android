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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs the signed-in user's settings with the real `/users/me/settings` endpoints
 * ([SettingsApi]), syncing the server's copy on first read and after every update.
 * [SettingsPreferencesDataSource] still holds the last-known values so the app has
 * something to show immediately and while offline; if the initial fetch fails, the
 * local values (or their defaults) are used instead of leaving the caller stuck.
 */
@Singleton
class DefaultSettingsRepository @Inject constructor(
    private val settingsApi: SettingsApi,
    private val settingsPrefs: SettingsPreferencesDataSource,
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    private val cache = MutableStateFlow<SettingsDto?>(null)

    private val settingsFlow: Flow<SettingsDto> = cache
        .onStart { if (cache.value == null) cache.value = fetchSettings() }
        .filterNotNull()

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

    private suspend fun fetchSettings(): SettingsDto = try {
        apiCall { settingsApi.mySettings().settings }.also { persistLocally(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        localSnapshot()
    } catch (e: HttpException) {
        localSnapshot()
    }

    /**
     * Applies [request] to the local cache immediately so the UI (a switch, a radio
     * choice) flips right away instead of waiting on the network round trip, then
     * syncs it to the server in the background. On failure the optimistic value is
     * kept — same offline-friendly stance as [fetchSettings].
     */
    private suspend fun updateSettings(request: UpdateSettingsRequest) {
        val current = cache.value ?: fetchSettings()
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
            // Offline — keep the optimistic value, matching fetchSettings' fallback.
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
