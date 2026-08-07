package com.devsgroup.foodmanager.feature.auth.presentation

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.AuthException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Digits in the email verification code the server mails out (see the auth API). */
private const val EMAIL_CODE_LENGTH = 4

/**
 * State for the dedicated "Подтвердите почту" screen — reached right after
 * registration, and re-entered on every later launch until the account's email is
 * verified (see the app-level start-destination gate). [isChangingEmail] reveals an
 * inline form to correct a mistyped address without leaving this screen.
 */
data class EmailConfirmationUiState(
    val email: String = "",
    val code: String = "",
    val codeError: String? = null,
    val isConfirming: Boolean = false,
    val isResending: Boolean = false,
    val resendMessage: String? = null,
    /** Whether [resendMessage] reports a failure — it carries both outcomes. */
    val resendFailed: Boolean = false,
    val isChangingEmail: Boolean = false,
    val newEmail: String = "",
    val newEmailError: String? = null,
    val isSubmittingNewEmail: Boolean = false,
) {
    val canConfirm: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && !isConfirming

    val canSubmitNewEmail: Boolean
        get() = newEmail.isNotBlank() && newEmailError == null && !isSubmittingNewEmail
}

@HiltViewModel
class EmailConfirmationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailConfirmationUiState())
    val uiState: StateFlow<EmailConfirmationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // profile() only emits once something is cached (it filters nulls), so ask
            // before collecting — on an account this device has never fetched, awaiting
            // it would hang here and leave the address blank with no explanation.
            if (!profileRepository.hasCachedProfile()) return@launch
            val email = runCatching { profileRepository.profile().first().email }.getOrNull()
            if (!email.isNullOrBlank()) {
                _uiState.update { it.copy(email = email) }
            }
        }
    }

    fun onCodeChange(value: String) {
        val digits = value.filter(Char::isDigit).take(EMAIL_CODE_LENGTH)
        _uiState.update { it.copy(code = digits, codeError = null) }
    }

    /** Confirms the mailed code, pulls the now-verified profile, then hands off. */
    fun onConfirm(onConfirmed: () -> Unit) {
        val state = _uiState.value
        if (state.code.length != EMAIL_CODE_LENGTH) return

        _uiState.update { it.copy(isConfirming = true, codeError = null) }
        viewModelScope.launch {
            try {
                authRepository.verifyEmailByCode(state.code)
            } catch (e: AuthException) {
                _uiState.update { it.copy(isConfirming = false, codeError = e.toVerifyMessage()) }
                return@launch
            }
            // The account is verified server-side now; fetch so the stored profile — and
            // the app-level gate reading it — agree. Best-effort on purpose: the
            // verification already succeeded, so a failure here must not strand the user
            // on this screen. The next launch resolves it against the server anyway.
            runCatching { profileRepository.refresh() }
            onConfirmed()
        }
    }

    /** Re-sends the verification code to the address currently on file. */
    fun onResendCode() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            // Nothing to send to, and silently doing nothing here is what made this
            // screen a dead end: no code arrives, no error explains why. Open the
            // change-email form instead — supplying an address is the way out.
            _uiState.update {
                it.copy(
                    resendMessage = "Мы не знаем вашего адреса — укажите его ниже",
                    resendFailed = true,
                    isChangingEmail = true,
                )
            }
            return
        }

        _uiState.update {
            it.copy(isResending = true, resendMessage = null, resendFailed = false, codeError = null)
        }
        viewModelScope.launch {
            try {
                authRepository.addEmail(email)
                _uiState.update {
                    it.copy(isResending = false, resendMessage = "Код отправлен повторно", code = "")
                }
            } catch (e: AuthException) {
                _uiState.update {
                    it.copy(isResending = false, resendMessage = e.toSendMessage(), resendFailed = true)
                }
            }
        }
    }

    fun onToggleChangeEmail() {
        _uiState.update {
            it.copy(
                isChangingEmail = !it.isChangingEmail,
                newEmail = "",
                newEmailError = null,
            )
        }
    }

    fun onNewEmailChange(value: String) {
        _uiState.update { it.copy(newEmail = value, newEmailError = validateEmail(value)) }
    }

    /** Changes the on-file address and re-sends a code to it, replacing the pending one. */
    fun onSubmitNewEmail() {
        val state = _uiState.value
        val error = validateEmail(state.newEmail, required = true)
        if (error != null) {
            _uiState.update { it.copy(newEmailError = error) }
            return
        }

        val newEmail = state.newEmail.trim()
        _uiState.update { it.copy(isSubmittingNewEmail = true, newEmailError = null) }
        viewModelScope.launch {
            try {
                authRepository.addEmail(newEmail)
                // addEmail moves the address (and clears verification) server-side, so a
                // fetch is all it takes to mirror that locally — best-effort, since the
                // code is already on its way and the user needs this screen either way.
                runCatching { profileRepository.refresh() }
                _uiState.update {
                    it.copy(
                        email = newEmail,
                        isSubmittingNewEmail = false,
                        isChangingEmail = false,
                        newEmail = "",
                        code = "",
                        codeError = null,
                        resendMessage = "Код отправлен на новый адрес",
                        resendFailed = false,
                    )
                }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isSubmittingNewEmail = false, newEmailError = e.toSendMessage()) }
            }
        }
    }

    /** Escape hatch for a user who can't confirm this account's address — starts over. */
    fun onLogout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    private fun AuthException.toSendMessage(): String = when (this) {
        is AuthException.Network -> "Нет подключения к интернету"
        else -> message ?: "Не удалось отправить код"
    }

    private fun AuthException.toVerifyMessage(): String = when (this) {
        is AuthException.InvalidRequest -> "Неверный или устаревший код"
        is AuthException.Network -> "Нет подключения к интернету"
        else -> message ?: "Не удалось подтвердить почту"
    }

    /** Empty is fine while typing (button stays disabled); required only gates submit. */
    private fun validateEmail(value: String, required: Boolean = false): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> if (required) "Введите адрес почты" else null
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Проверьте адрес почты"
            else -> null
        }
    }
}
