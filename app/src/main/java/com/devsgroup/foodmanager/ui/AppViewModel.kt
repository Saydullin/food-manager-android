package com.devsgroup.foodmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Whether the app should open in the auth flow, the email-confirmation gate, or
 * straight into the app, and which theme it should render in. [Loading] is the brief
 * window while the persisted session is read from DataStore — the UI holds a neutral
 * splash until it resolves so the start destination is picked exactly once.
 */
sealed interface AppUiState {
    data object Loading : AppUiState
    data class Ready(
        val isLoggedIn: Boolean,
        val emailVerified: Boolean,
        /** False until the account has answered (or declined) the onboarding questions. */
        val onboardingComplete: Boolean,
        val darkThemeEnabled: Boolean,
    ) : AppUiState
}

/**
 * App-level gate over [AuthRepository.isLoggedIn], [ProfileRepository.emailVerified],
 * [ProfileRepository.onboardingComplete] and [SettingsRepository.darkThemeEnabled].
 * [AppUiState.isLoggedIn]/[AppUiState.emailVerified]/[AppUiState.onboardingComplete]
 * drive only the *initial* start destination — the NavHost is composed after this
 * resolves, so the graph is built once and later token changes don't rebuild it.
 * (`onboardingComplete` is also read once more, at the moment the auth flow hands off,
 * to pick where a just-registered user lands — see `FoodApp`.)
 * [AppUiState.darkThemeEnabled] is read continuously instead, since the profile
 * screen's theme toggle should switch the whole app live.
 *
 * [Ready] also waits on a minimum splash duration so the splash animation isn't cut
 * short when everything above resolves instantly (e.g. warm DataStore reads).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Emits once, after [SPLASH_MIN_DURATION_MS] — so the splash animation always gets to play out. */
    private val minimumSplashDuration = flow {
        delay(SPLASH_MIN_DURATION_MS)
        emit(Unit)
    }

    val uiState: StateFlow<AppUiState> = combine(
        authRepository.isLoggedIn,
        profileRepository.emailVerified,
        profileRepository.onboardingComplete,
        settingsRepository.darkThemeEnabled(),
        minimumSplashDuration,
    ) { isLoggedIn, emailVerified, onboardingComplete, darkThemeEnabled, _ ->
        AppUiState.Ready(
            isLoggedIn = isLoggedIn,
            emailVerified = emailVerified,
            onboardingComplete = onboardingComplete,
            darkThemeEnabled = darkThemeEnabled,
        )
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

    private companion object {
        const val SPLASH_MIN_DURATION_MS = 1_000L
    }
}
