package com.devsgroup.foodmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Whether the app should open in the auth flow or straight into the app. [Loading] is
 * the brief window while the persisted session is read from DataStore — the UI holds a
 * neutral splash until it resolves so the start destination is picked exactly once.
 */
sealed interface AppUiState {
    data object Loading : AppUiState
    data class Ready(val isLoggedIn: Boolean) : AppUiState
}

/**
 * App-level gate over [AuthRepository.isLoggedIn]. It drives only the *initial* start
 * destination — the NavHost is composed after this resolves, so the graph is built once
 * and later token changes don't rebuild it.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = authRepository.isLoggedIn
        .map { AppUiState.Ready(isLoggedIn = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState.Loading,
        )
}
