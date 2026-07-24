package com.devsgroup.foodmanager.feature.auth.presentation

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.DeviceKeyRepository
import com.devsgroup.foodmanager.core.model.AuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** Server-side nickname availability, checked once the local length rule passes. */
enum class UsernameAvailability { UNKNOWN, CHECKING, AVAILABLE }

/**
 * Form state for "Создать аккаунт". The nickname is required (and has a minimum
 * length); the email is optional but, when present, must look like an address.
 *
 * [usernameError] / [emailError] carry validation copy for the field helper lines,
 * [usernameAvailability] carries the async "is this nickname taken" check, and
 * [canSubmit] gates the primary button so it only lights up once the form is valid —
 * matching the muted-until-ready button in the mock.
 */
data class RegistrationUiState(
    val username: String = "",
    val email: String = "",
    val usernameError: String? = null,
    val emailError: String? = null,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.UNKNOWN,
    val isLoading: Boolean = false,
    val submitError: String? = null,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() &&
            usernameError == null &&
            emailError == null &&
            !isLoading
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceKeyRepository: DeviceKeyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val usernameQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            usernameQuery
                .debounce(USERNAME_CHECK_DEBOUNCE_MS.milliseconds)
                .filter { it.length >= MIN_USERNAME_LENGTH }
                .distinctUntilChanged()
                .collectLatest(::checkUsernameAvailability)
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = validateUsername(value),
                usernameAvailability = UsernameAvailability.UNKNOWN,
                submitError = null,
            )
        }
        usernameQuery.value = value.trim()
    }

    private suspend fun checkUsernameAvailability(username: String) {
        _uiState.update { it.copy(usernameAvailability = UsernameAvailability.CHECKING) }
        val available = runCatching { authRepository.isUsernameAvailable(username) }.getOrNull()
        _uiState.update {
            when (available) {
                true -> it.copy(usernameAvailability = UsernameAvailability.AVAILABLE)
                false -> it.copy(
                    usernameAvailability = UsernameAvailability.UNKNOWN,
                    usernameError = "Никнейм уже занят",
                )
                null -> it.copy(usernameAvailability = UsernameAvailability.UNKNOWN)
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = validateEmail(value), submitError = null) }
    }

    /**
     * Validates the form and, when it passes, generates this device's signing key
     * (Android Keystore) and registers the account with its public key. Discards any
     * key left over from a previously logged-out account first — reusing it would
     * make the server reject this register() with PUBLIC_KEY_TAKEN, since that key
     * is already registered to the other account. A successful register() persists
     * the session, which flips [AuthRepository.isLoggedIn] — the app-level gate
     * observing it then hands off via [onRegistered].
     */
    fun onCreateAccount(onRegistered: () -> Unit) {
        val state = _uiState.value
        val usernameError = validateUsername(state.username, required = true)
        val emailError = validateEmail(state.email)
        if (usernameError != null || emailError != null) {
            _uiState.update { it.copy(usernameError = usernameError, emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, submitError = null) }
        viewModelScope.launch {
            try {
                deviceKeyRepository.deleteDeviceKey()
                val publicKey = deviceKeyRepository.getOrCreatePublicKey()
                authRepository.register(
                    username = state.username.trim(),
                    publicKey = publicKey,
                    email = state.email.trim().ifBlank { null },
                )
                onRegistered()
            } catch (e: AuthException) {
                _uiState.update { it.copy(isLoading = false, submitError = null).applyAuthError(e) }
            }
        }
    }

    private fun RegistrationUiState.applyAuthError(e: AuthException): RegistrationUiState = when (e) {
        is AuthException.Conflict -> copy(
            usernameError = "Никнейм или почта уже заняты",
            usernameAvailability = UsernameAvailability.UNKNOWN,
        )
        is AuthException.Network -> copy(submitError = "Нет подключения к интернету")
        else -> copy(submitError = e.message ?: "Не удалось создать аккаунт")
    }

    /** Empty is fine while typing (button stays disabled); too-short reads as an error. */
    private fun validateUsername(value: String, required: Boolean = false): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> if (required) "Придумайте никнейм" else null
            trimmed.length < MIN_USERNAME_LENGTH -> "Минимум $MIN_USERNAME_LENGTH символа"
            else -> null
        }
    }

    private fun validateEmail(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> null // optional
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Проверьте адрес почты"
            else -> null
        }
    }

    private companion object {
        const val MIN_USERNAME_LENGTH = 3
        const val USERNAME_CHECK_DEBOUNCE_MS = 400L
    }
}
