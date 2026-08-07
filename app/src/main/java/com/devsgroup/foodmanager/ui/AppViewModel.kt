package com.devsgroup.foodmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
 * [Ready] also waits on [startupGate] — a minimum splash duration, so the splash
 * animation isn't cut short when everything above resolves instantly (e.g. warm
 * DataStore reads), plus a server check for the one stored answer that isn't
 * trustworthy on its own (see [resolveEmailVerification]).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /**
     * Emits once the app is ready to commit to a start destination: after
     * [SPLASH_MIN_DURATION_MS] (so the splash animation always gets to play out) and,
     * when it's needed, after [resolveEmailVerification] has had its bounded chance to
     * settle the email gate. Both run concurrently, so the check usually costs nothing
     * beyond the splash the user was already watching.
     */
    private val startupGate = flow {
        coroutineScope {
            val splash = launch { delay(SPLASH_MIN_DURATION_MS) }
            val emailCheck = launch { resolveEmailVerification() }
            splash.join()
            emailCheck.join()
        }
        emit(Unit)
    }

    /**
     * Settles "is this account's email verified?" against the server, but only when the
     * locally stored answer would send the user to the confirmation gate.
     *
     * That local answer is only trustworthy on the device that did the verifying: the
     * flag lives in DataStore, and until it's refreshed from `/users/me` a code login or
     * a reinstall reads a stale "no". The gate that follows is a dead end for such an
     * account — it can only ask for a code nobody sent — so it's worth a fetch before
     * committing to it. Bounded by [EMAIL_CHECK_TIMEOUT_MS] and best-effort: an offline
     * launch keeps the stored answer rather than holding the splash on a dead network.
     */
    private suspend fun resolveEmailVerification() {
        if (!authRepository.isLoggedIn.first()) return
        if (profileRepository.emailVerified.first()) return
        withTimeoutOrNull(EMAIL_CHECK_TIMEOUT_MS) {
            runCatching { profileRepository.refresh() }
        }
    }

    val uiState: StateFlow<AppUiState> = combine(
        authRepository.isLoggedIn,
        profileRepository.emailVerified,
        profileRepository.onboardingComplete,
        settingsRepository.darkThemeEnabled(),
        startupGate,
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

        /** Comfortably longer than a healthy `/users/me`, short enough not to read as a hang. */
        const val EMAIL_CHECK_TIMEOUT_MS = 4_000L
    }
}
