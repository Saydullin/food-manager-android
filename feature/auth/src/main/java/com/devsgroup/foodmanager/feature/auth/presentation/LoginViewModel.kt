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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Digits in the login code the server mails to a device with no existing key. */
private const val LOGIN_CODE_LENGTH = 4

/**
 * Form state for "С возвращением" — re-authenticating this device. The fast path needs
 * only a nickname: the account's proof is the Ed25519 key this device already holds
 * from a prior registration. [needsEmailLogin] flips on only once that fast path can't
 * work here (a fresh install / new device with no key yet) — from there the flow falls
 * back to username + email + a mailed one-time code, ending with [codeSent] revealing
 * the code field.
 *
 * [canSubmit] gates the primary button until the nickname is present, matching the
 * muted-until-ready button in the mock.
 */
data class LoginUiState(
    val username: String = "",
    val usernameError: String? = null,
    val isLoading: Boolean = false,
    val submitError: String? = null,
    // --- Fallback: revealed only when this device has no signing key yet ---
    val needsEmailLogin: Boolean = false,
    val email: String = "",
    val emailError: String? = null,
    val isSendingCode: Boolean = false,
    val codeSent: Boolean = false,
    val code: String = "",
    val codeError: String? = null,
    val isConfirmingCode: Boolean = false,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && usernameError == null && !isLoading

    /** The "send code" action lights up only for a well-formed address. */
    val canSendCode: Boolean
        get() = email.isNotBlank() && emailError == null && !isSendingCode

    val canConfirmCode: Boolean
        get() = code.length == LOGIN_CODE_LENGTH && !isConfirmingCode
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceKeyRepository: DeviceKeyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = null,
                submitError = null,
                // A new nickname deserves a fresh attempt at the fast device-key path.
                needsEmailLogin = false,
                email = "",
                emailError = null,
                codeSent = false,
                code = "",
                codeError = null,
            )
        }
    }

    /**
     * Re-authenticates this device for [LoginUiState.username]: requests a login
     * challenge, signs its nonce with the device's Keystore key, and submits the
     * signature. A successful verify() persists the session, which flips
     * [AuthRepository.isLoggedIn] — the app-level gate observing it then hands off
     * via [onAuthenticated]. If this device holds no signing key yet, reveals the
     * email step instead of failing outright.
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
                _uiState.update { it.copy(isLoading = false, needsEmailLogin = true) }
                return@launch
            }

            try {
                val challenge = authRepository.requestChallenge(state.username.trim())
                val signature = deviceKeyRepository.sign(challenge.nonce)
                authRepository.verify(challenge.challengeId, signature, deviceId)
                onAuthenticated()
            } catch (e: AuthException) {
                if (e is AuthException.Unauthorized) {
                    // This device's key belongs to a different account (e.g. after
                    // logging out of a second account) — fall back to email login
                    // instead of a dead-end error.
                    _uiState.update { it.copy(isLoading = false, needsEmailLogin = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, submitError = null).applyAuthError(e) }
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                submitError = null,
                // Editing the address invalidates any code already sent for the old one.
                codeSent = false,
                code = "",
                codeError = null,
            )
        }
    }

    /** Fallback step 1: requests a one-time login code mailed to the account's on-file address. */
    fun onSendLoginCode() {
        val state = _uiState.value
        val emailError = validateEmail(state.email, required = true)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update { it.copy(isSendingCode = true, emailError = null) }
        viewModelScope.launch {
            try {
                authRepository.requestLoginCode(state.username.trim(), state.email.trim())
                _uiState.update {
                    it.copy(isSendingCode = false, codeSent = true, code = "", codeError = null)
                }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isSendingCode = false, emailError = e.toSendCodeMessage()) }
            }
        }
    }

    fun onResendLoginCode() = onSendLoginCode()

    fun onCodeChange(value: String) {
        val digits = value.filter(Char::isDigit).take(LOGIN_CODE_LENGTH)
        _uiState.update { it.copy(code = digits, codeError = null) }
    }

    /**
     * Fallback step 2: confirms the emailed code, registering this device's freshly
     * generated Keystore key as a new device and starting a session.
     */
    fun onConfirmLoginCode(onAuthenticated: () -> Unit) {
        val state = _uiState.value
        if (state.code.length != LOGIN_CODE_LENGTH) return

        _uiState.update { it.copy(isConfirmingCode = true, codeError = null) }
        viewModelScope.launch {
            try {
                // This device may still hold a key from a previously logged-out
                // account — discard it so the server doesn't reject a public key
                // already registered elsewhere.
                deviceKeyRepository.deleteDeviceKey()
                val publicKey = deviceKeyRepository.getOrCreatePublicKey()
                authRepository.confirmLoginCode(
                    username = state.username.trim(),
                    email = state.email.trim(),
                    code = state.code,
                    newPublicKey = publicKey,
                )
                onAuthenticated()
            } catch (e: AuthException) {
                _uiState.update { it.copy(isConfirmingCode = false, codeError = e.toConfirmCodeMessage()) }
            }
        }
    }

    private fun LoginUiState.applyAuthError(e: AuthException): LoginUiState = when (e) {
        is AuthException.NotFound -> copy(usernameError = "Пользователь не найден")
        is AuthException.Network -> copy(submitError = "Нет подключения к интернету")
        else -> copy(submitError = e.message ?: "Не удалось войти")
    }

    /** Empty is fine while typing (button stays disabled); a malformed address reads as an error. */
    private fun validateEmail(value: String, required: Boolean = false): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> if (required) "Введите адрес почты" else null
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Проверьте адрес почты"
            else -> null
        }
    }

    private fun AuthException.toSendCodeMessage(): String = when (this) {
        is AuthException.Network -> "Нет подключения к интернету"
        else -> message ?: "Не удалось отправить код"
    }

    private fun AuthException.toConfirmCodeMessage(): String = when (this) {
        is AuthException.InvalidRequest -> "Неверный или истёкший код"
        is AuthException.Network -> "Нет подключения к интернету"
        else -> message ?: "Не удалось подтвердить код"
    }
}
