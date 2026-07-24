package com.devsgroup.foodmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Whether the app should open in the auth flow or straight into the app, and which theme
 * it should render in. [Loading] is the brief window while the persisted session is read
 * from DataStore — the UI holds a neutral splash until it resolves so the start
 * destination is picked exactly once.
 */
sealed interface AppUiState {
    data object Loading : AppUiState
    data class Ready(val isLoggedIn: Boolean, val darkThemeEnabled: Boolean) : AppUiState
}

/**
 * App-level gate over [AuthRepository.isLoggedIn] and [SettingsRepository.darkThemeEnabled].
 * [AppUiState.isLoggedIn] drives only the *initial* start destination — the NavHost is
 * composed after this resolves, so the graph is built once and later token changes don't
 * rebuild it. [AppUiState.darkThemeEnabled] is read continuously instead, since the
 * profile screen's theme toggle should switch the whole app live.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        authRepository.isLoggedIn,
        settingsRepository.darkThemeEnabled(),
    ) { isLoggedIn, darkThemeEnabled ->
        AppUiState.Ready(isLoggedIn = isLoggedIn, darkThemeEnabled = darkThemeEnabled)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState.Loading,
        )

    private val _forcedLogoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Tells [com.devsgroup.foodmanager.ui.FoodApp] to navigate back to the auth flow. */
    val forcedLogoutEvents: SharedFlow<Unit> = _forcedLogoutEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionExpired.collect {
                authRepository.logout()
                _forcedLogoutEvents.emit(Unit)
            }
        }
    }
}
