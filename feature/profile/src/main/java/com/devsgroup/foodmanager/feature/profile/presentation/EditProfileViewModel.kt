package com.devsgroup.foodmanager.feature.profile.presentation

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.AuthException
import com.devsgroup.foodmanager.core.model.CookingAttitude
import com.devsgroup.foodmanager.core.model.ProfileException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Digits in the email verification code the server mails out (see the auth API). */
private const val EMAIL_CODE_LENGTH = 6

data class EditProfileUiState(
    val username: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val statusEmoji: String? = null,
    val age: String = "",
    val cookingAttitude: CookingAttitude? = null,
    val bio: String = "",
    val usernameError: String? = null,
    val isSaving: Boolean = false,
    // --- Email: changed via a separate, server-verified two-step flow (send code → confirm) ---
    val email: String = "",
    val emailError: String? = null,
    val emailVerified: Boolean = false,
    val isSendingCode: Boolean = false,
    val codeSent: Boolean = false,
    val emailCode: String = "",
    val emailCodeError: String? = null,
    val isVerifyingCode: Boolean = false,
    val emailStatus: String? = null,
) {
    val canSave: Boolean get() = username.isNotBlank() && !isSaving

    /** The "send code" action lights up only for a fresh, well-formed, unconfirmed address. */
    val canSendCode: Boolean
        get() = email.isNotBlank() && emailError == null && !emailVerified && !isSendingCode

    val canVerifyCode: Boolean
        get() = emailCode.length == EMAIL_CODE_LENGTH && !isVerifyingCode
}

/** Loads the current [ProfileRepository] profile into an editable form and saves it back. */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val profile = profileRepository.profile().first()
                _uiState.update {
                    it.copy(
                        username = profile.username.removePrefix("@"),
                        name = profile.name,
                        avatarUrl = profile.avatarUrl,
                        statusEmoji = profile.statusEmoji,
                        age = profile.age?.toString().orEmpty(),
                        cookingAttitude = profile.cookingAttitude,
                        bio = profile.bio.orEmpty(),
                        // A stored email was verified when it was saved, so start in the confirmed state.
                        email = profile.email.orEmpty(),
                        emailVerified = !profile.email.isNullOrBlank(),
                    )
                }
            } catch (e: ProfileException) {
                // Leave the form at its blank defaults — Сохранить will retry the fetch via updateProfile.
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null) }
    }

    fun onAvatarChange(uri: String) {
        _uiState.update { it.copy(avatarUrl = uri) }
    }

    fun onAvatarDelete() {
        _uiState.update { it.copy(avatarUrl = null) }
    }

    fun onStatusEmojiSelect(emoji: String) {
        _uiState.update {
            it.copy(statusEmoji = if (it.statusEmoji == emoji) null else emoji)
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onAgeChange(value: String) {
        if (value.isEmpty() || value.all(Char::isDigit)) {
            _uiState.update { it.copy(age = value) }
        }
    }

    fun onCookingAttitudeSelect(attitude: CookingAttitude) {
        _uiState.update {
            it.copy(cookingAttitude = if (it.cookingAttitude == attitude) null else attitude)
        }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bio = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = validateEmail(value),
                // Editing the address invalidates any pending or previously confirmed verification.
                emailVerified = false,
                codeSent = false,
                emailCode = "",
                emailCodeError = null,
                emailStatus = null,
            )
        }
    }

    /** Step 1: ask the server to email a 6-digit code to the entered address. */
    fun onSendEmailCode() {
        val email = _uiState.value.email.trim()
        val error = validateEmail(email, required = true)
        if (error != null) {
            _uiState.update { it.copy(emailError = error) }
            return
        }
        _uiState.update { it.copy(isSendingCode = true, emailError = null, emailStatus = null) }
        viewModelScope.launch {
            try {
                authRepository.addEmail(email)
                _uiState.update {
                    it.copy(
                        isSendingCode = false,
                        codeSent = true,
                        emailCode = "",
                        emailCodeError = null,
                        emailStatus = "Код отправлен на $email",
                    )
                }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isSendingCode = false, emailError = e.toSendMessage()) }
            }
        }
    }

    fun onEmailCodeChange(value: String) {
        val digits = value.filter(Char::isDigit).take(EMAIL_CODE_LENGTH)
        _uiState.update { it.copy(emailCode = digits, emailCodeError = null) }
    }

    /** Step 2: confirm the pending email with the code the user received, then persist it. */
    fun onVerifyEmailCode() {
        val state = _uiState.value
        if (state.emailCode.length != EMAIL_CODE_LENGTH) return
        _uiState.update { it.copy(isVerifyingCode = true, emailCodeError = null) }
        viewModelScope.launch {
            try {
                authRepository.verifyEmailByCode(state.emailCode)
                // Confirmed server-side — commit it onto the profile independently of "Сохранить".
                val current = profileRepository.profile().first()
                profileRepository.updateProfile(current.copy(email = state.email.trim()))
                _uiState.update {
                    it.copy(
                        isVerifyingCode = false,
                        codeSent = false,
                        emailVerified = true,
                        emailCode = "",
                        emailStatus = null,
                    )
                }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isVerifyingCode = false, emailCodeError = e.toVerifyMessage()) }
            }
        }
    }

    fun onResendEmailCode() = onSendEmailCode()

    fun onSave(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Придумайте никнейм") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val current = profileRepository.profile().first()
            profileRepository.updateProfile(
                current.copy(
                    username = "@${state.username.trim().removePrefix("@")}",
                    name = state.name.trim().ifBlank { current.name },
                    avatarUrl = state.avatarUrl,
                    statusEmoji = state.statusEmoji,
                    age = state.age.toIntOrNull(),
                    cookingAttitude = state.cookingAttitude,
                    bio = state.bio.trim().ifBlank { null },
                ),
            )
            _uiState.update { it.copy(isSaving = false) }
            onSaved()
        }
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

    private fun AuthException.toSendMessage(): String = when (this) {
        is AuthException.Conflict -> "Эта почта уже занята"
        is AuthException.Network -> "Нет подключения к интернету"
        is AuthException.Unauthorized -> "Сессия истекла, войдите заново"
        else -> message ?: "Не удалось отправить код"
    }

    private fun AuthException.toVerifyMessage(): String = when (this) {
        is AuthException.InvalidRequest -> "Неверный или устаревший код"
        is AuthException.Network -> "Нет подключения к интернету"
        else -> message ?: "Не удалось подтвердить почту"
    }
}
