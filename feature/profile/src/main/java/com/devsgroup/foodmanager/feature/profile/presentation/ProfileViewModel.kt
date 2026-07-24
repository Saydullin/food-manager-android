package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.domain.repository.GroupRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.domain.repository.RecipeRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.domain.repository.TasteProfileRepository
import com.devsgroup.foodmanager.core.model.AppLanguage
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.model.Recipe
import com.devsgroup.foodmanager.core.model.UserGroup
import com.devsgroup.foodmanager.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** Which bottom sheet (if any) is open. Mutually exclusive, matching the design. */
sealed interface ProfileSheet {
    data class AddFavoriteTag(val input: String = "") : ProfileSheet
    data class AddAvoidTag(val input: String = "") : ProfileSheet
    data class NewGroup(val input: String = "") : ProfileSheet
    data object Diet : ProfileSheet
    data object Language : ProfileSheet
}

data class ProfileUiState(
    val profile: UserProfile = UserProfile(name = "", username = ""),
    val favoriteTags: Set<String> = emptySet(),
    val avoidTags: Set<String> = emptySet(),
    val dietTags: Set<DietaryTag> = emptySet(),
    val groups: List<UserGroup> = emptyList(),
    val myRecipes: List<Recipe> = emptyList(),
    val likedFoods: List<Food> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val sheet: ProfileSheet? = null,
    val toast: String? = null,
    val toastIsError: Boolean = false,
)

/** Ephemeral UI-only state that no repository owns: which sheet is open, the toast. */
private data class LocalUiState(
    val sheet: ProfileSheet? = null,
    val toast: String? = null,
    val toastIsError: Boolean = false,
)

private data class RepositoryState(
    val profile: UserProfile,
    val favoriteTags: Set<String>,
    val avoidTags: Set<String>,
    val dietTags: Set<DietaryTag>,
    val groups: List<UserGroup>,
    val myRecipes: List<Recipe>,
    val likedFoods: List<Food>,
)

private data class SettingsState(
    val notificationsEnabled: Boolean,
    val darkThemeEnabled: Boolean,
    val language: AppLanguage,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val tasteProfileRepository: TasteProfileRepository,
    private val groupRepository: GroupRepository,
    private val recipeRepository: RecipeRepository,
    private val foodRepository: FoodRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())

    private val repositoryState = combine(
        profileRepository.profile()
            .catch { e ->
                if (e !is ProfileException) throw e
                showToast(e.toDisplayMessage(), isError = true)
                emit(UserProfile(name = "", username = ""))
            },
        tasteProfileRepository.tasteProfile(),
        groupRepository.groups(),
        recipeRepository.myRecipes(),
        foodRepository.likedFoods(),
    ) { profile, tasteProfile, groups, recipes, likedFoods ->
        RepositoryState(
            profile = profile,
            favoriteTags = tasteProfile.favoriteTags,
            avoidTags = tasteProfile.avoidTags,
            dietTags = tasteProfile.dietaryTags,
            groups = groups,
            myRecipes = recipes,
            likedFoods = likedFoods,
        )
    }

    private val settingsState = combine(
        settingsRepository.notificationsEnabled(),
        settingsRepository.darkThemeEnabled(),
        settingsRepository.language(),
    ) { notificationsEnabled, darkThemeEnabled, language ->
        SettingsState(notificationsEnabled, darkThemeEnabled, language)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        repositoryState,
        settingsState,
        localState,
    ) { repo, settings, local ->
        ProfileUiState(
            profile = repo.profile,
            favoriteTags = repo.favoriteTags,
            avoidTags = repo.avoidTags,
            dietTags = repo.dietTags,
            groups = repo.groups,
            myRecipes = repo.myRecipes,
            likedFoods = repo.likedFoods,
            notificationsEnabled = settings.notificationsEnabled,
            darkThemeEnabled = settings.darkThemeEnabled,
            language = settings.language,
            sheet = local.sheet,
            toast = local.toast,
            toastIsError = local.toastIsError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    // --- Sheet visibility -------------------------------------------------

    fun onAddFavoriteTagClick() = openSheet(ProfileSheet.AddFavoriteTag())
    fun onAddAvoidTagClick() = openSheet(ProfileSheet.AddAvoidTag())
    fun onNewGroupClick() = openSheet(ProfileSheet.NewGroup())
    fun onDietClick() = openSheet(ProfileSheet.Diet)
    fun onLanguageClick() = openSheet(ProfileSheet.Language)

    fun onDismissSheet() = openSheet(null)

    private fun openSheet(sheet: ProfileSheet?) {
        localState.update { it.copy(sheet = sheet) }
    }

    /** Edits the text input of whichever text-entry sheet is currently open. */
    fun onSheetInputChange(value: String) {
        localState.update { state ->
            val updated = when (val sheet = state.sheet) {
                is ProfileSheet.AddFavoriteTag -> sheet.copy(input = value)
                is ProfileSheet.AddAvoidTag -> sheet.copy(input = value)
                is ProfileSheet.NewGroup -> sheet.copy(input = value)
                ProfileSheet.Diet, ProfileSheet.Language, null -> state.sheet
            }
            state.copy(sheet = updated)
        }
    }

    // --- Taste profile ------------------------------------------------------

    fun onSubmitFavoriteTag() {
        val tag = (localState.value.sheet as? ProfileSheet.AddFavoriteTag)?.input?.trim()
        if (tag.isNullOrBlank()) return
        viewModelScope.launch {
            val current = tasteProfileRepository.tasteProfile().first()
            tasteProfileRepository.updateTasteProfile(current.copy(favoriteTags = current.favoriteTags + tag))
        }
        openSheet(null)
    }

    fun onSubmitAvoidTag() {
        val tag = (localState.value.sheet as? ProfileSheet.AddAvoidTag)?.input?.trim()
        if (tag.isNullOrBlank()) return
        viewModelScope.launch {
            val current = tasteProfileRepository.tasteProfile().first()
            tasteProfileRepository.updateTasteProfile(current.copy(avoidTags = current.avoidTags + tag))
        }
        openSheet(null)
    }

    fun onRemoveFavoriteTag(tag: String) {
        viewModelScope.launch {
            val current = tasteProfileRepository.tasteProfile().first()
            tasteProfileRepository.updateTasteProfile(current.copy(favoriteTags = current.favoriteTags - tag))
        }
    }

    fun onRemoveAvoidTag(tag: String) {
        viewModelScope.launch {
            val current = tasteProfileRepository.tasteProfile().first()
            tasteProfileRepository.updateTasteProfile(current.copy(avoidTags = current.avoidTags - tag))
        }
    }

    fun onToggleDietTag(tag: DietaryTag) {
        viewModelScope.launch {
            val current = tasteProfileRepository.tasteProfile().first()
            val dietTags = if (tag in current.dietaryTags) current.dietaryTags - tag else current.dietaryTags + tag
            tasteProfileRepository.updateTasteProfile(current.copy(dietaryTags = dietTags))
        }
    }

    // --- Groups ---------------------------------------------------------

    fun onSubmitNewGroup() {
        val name = (localState.value.sheet as? ProfileSheet.NewGroup)?.input?.trim()
        if (name.isNullOrBlank()) return
        viewModelScope.launch {
            groupRepository.createGroup(name)
            showToast("Группа «$name» создана")
        }
        openSheet(null)
    }

    fun onDeleteGroup(group: UserGroup) {
        viewModelScope.launch {
            groupRepository.deleteGroup(group.id)
            showToast("Группа «${group.name}» удалена")
        }
    }

    // --- Recipes / liked foods -------------------------------------------

    fun onCreateRecipeClick() {
        showToast("Раздел скоро появится")
    }

    fun onUnlikeFood(food: Food) {
        viewModelScope.launch { foodRepository.unlikeFood(food.id) }
    }

    // --- Settings ---------------------------------------------------------

    fun onToggleNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun onToggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkThemeEnabled(enabled) }
    }

    fun onSelectLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
        openSheet(null)
    }

    fun onHelpClick() {
        showToast("Раздел скоро появится")
    }

    fun onLogout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }

    private fun showToast(message: String, isError: Boolean = false) {
        localState.update { it.copy(toast = message, toastIsError = isError) }
        viewModelScope.launch {
            delay(TOAST_DURATION.inWholeMilliseconds)
            localState.update { if (it.toast == message) it.copy(toast = null, toastIsError = false) else it }
        }
    }

    /** Localizes a fetch failure for [profileRepository.profile] into a user-facing toast. */
    private fun ProfileException.toDisplayMessage(): String = when (this) {
        is ProfileException.Network -> "Нет подключения к интернету"
        is ProfileException.Unauthorized -> "Сессия истекла, войдите заново"
        is ProfileException.NotFound -> "Профиль не найден"
        is ProfileException.Conflict -> message ?: "Конфликт данных профиля"
        is ProfileException.InvalidRequest -> message ?: "Некорректные данные профиля"
        is ProfileException.Unknown -> message ?: "Не удалось загрузить профиль"
    }

    private companion object {
        val TOAST_DURATION = 2.seconds
    }
}
