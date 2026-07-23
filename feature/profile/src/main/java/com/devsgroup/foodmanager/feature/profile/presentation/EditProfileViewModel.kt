package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.CookingAttitude
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
) {
    val canSave: Boolean get() = username.isNotBlank() && !isSaving
}

/** Loads the current [ProfileRepository] profile into an editable form and saves it back. */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
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
                )
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
}
