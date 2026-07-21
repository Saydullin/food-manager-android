package com.devsgroup.foodmanager.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.DeviceKeyRepository
import com.devsgroup.foodmanager.core.model.AuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Form state for "С возвращением" — re-authenticating this device. There is no
 * password: the account's proof is the Ed25519/RSA key this device already holds
 * from a prior registration, so only a nickname is asked for.
 *
 * [canSubmit] gates the primary button until the nickname is present, matching the
 * muted-until-ready button in the mock.
 */
data class LoginUiState(
    val username: String = "",
    val usernameError: String? = null,
    val isLoading: Boolean = false,
    val submitError: String? = null,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && usernameError == null && !isLoading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceKeyRepository: DeviceKeyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null, submitError = null) }
    }

    /**
     * Re-authenticates this device for [LoginUiState.username]: requests a login
     * challenge, signs its nonce with the device's Keystore key, and submits the
     * signature. A successful verify() persists the session, which flips
     * [AuthRepository.isLoggedIn] — the app-level gate observing it then hands off
     * via [onAuthenticated].
     */
    fun onLogin(onAuthenticated: () -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Введите никнейм") }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            val deviceId = authRepository.deviceId.first()
            if (deviceId == null || !deviceKeyRepository.hasDeviceKey()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        submitError = "На этом устройстве ещё нет ключа входа — сначала создайте аккаунт.",
                    )
                }
                return@launch
            }

            try {
                val challenge = authRepository.requestChallenge(state.username.trim())
                val signature = deviceKeyRepository.sign(challenge.nonce)
                authRepository.verify(challenge.challengeId, signature, deviceId)
                onAuthenticated()
            } catch (e: AuthException) {
                _uiState.update { it.copy(isLoading = false, submitError = null).applyAuthError(e) }
            }
        }
    }

    private fun LoginUiState.applyAuthError(e: AuthException): LoginUiState = when (e) {
        is AuthException.NotFound -> copy(usernameError = "Пользователь не найден")
        is AuthException.Unauthorized -> copy(submitError = "Не удалось подтвердить вход. Попробуйте снова.")
        is AuthException.Network -> copy(submitError = "Нет подключения к интернету")
        else -> copy(submitError = e.message ?: "Не удалось войти")
    }
}
