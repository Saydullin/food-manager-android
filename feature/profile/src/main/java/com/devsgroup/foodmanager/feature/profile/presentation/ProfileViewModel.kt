package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.domain.repository.GroupRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.domain.repository.RecipeRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.model.AppLanguage
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.model.Recipe
import com.devsgroup.foodmanager.core.model.UserGroup
import com.devsgroup.foodmanager.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val isBanned: Boolean = false,
    val isRefreshing: Boolean = false,
)

/** Ephemeral UI-only state that no repository owns: which sheet is open, the toast. */
private data class LocalUiState(
    val sheet: ProfileSheet? = null,
    val toast: String? = null,
    val toastIsError: Boolean = false,
    val isRefreshing: Boolean = false,
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
                // A connectivity failure while just loading the screen shouldn't
                // interrupt the user with a toast — only surface it when they
                // actively try to save/change something (see EditProfileViewModel).
                if (e !is ProfileException.Network) {
                    showToast(e.toDisplayMessage(), isError = true)
                }
                emit(UserProfile(name = "", username = ""))
            },
        groupRepository.groups(),
        recipeRepository.myRecipes(),
        foodRepository.likedFoods(),
    ) { profile, groups, recipes, likedFoods ->
        RepositoryState(
            profile = profile,
            favoriteTags = profile.foodPreferences.toSet(),
            avoidTags = profile.foodExceptions.toSet(),
            dietTags = profile.diets.mapNotNull { runCatching { DietaryTag.valueOf(it) }.getOrNull() }.toSet(),
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
        profileRepository.isBanned,
    ) { repo, settings, local, isBanned ->
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
            isBanned = isBanned,
            isRefreshing = local.isRefreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    init {
        // Refresh-on-launch: the profile repository's cache is process-lifetime, so
        // without this a returning user would see whatever was fetched last time
        // instead of picking up server-side changes (e.g. edits from another device).
        viewModelScope.launch { refreshProfile(showErrorToast = false) }
    }

    /** Pull-to-refresh entry point from the screen. */
    fun onRefresh() {
        viewModelScope.launch { refreshProfile(showErrorToast = true) }
    }

    private suspend fun refreshProfile(showErrorToast: Boolean) {
        localState.update { it.copy(isRefreshing = true) }
        try {
            profileRepository.refresh()
        } catch (e: ProfileException) {
            if (showErrorToast) showToast(e.toDisplayMessage(), isError = true)
        } finally {
            localState.update { it.copy(isRefreshing = false) }
        }
    }

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
            runCatching {
                val current = profileRepository.profile().first().foodPreferences
                profileRepository.updateFoodPreferences(current + tag)
            }.onFailure(::onMutationFailure)
        }
    }

    fun onSubmitAvoidTag() {
        val tag = (localState.value.sheet as? ProfileSheet.AddAvoidTag)?.input?.trim()
        if (tag.isNullOrBlank()) return
        viewModelScope.launch {
            runCatching {
                val current = profileRepository.profile().first().foodExceptions
                profileRepository.updateFoodExceptions(current + tag)
            }.onFailure(::onMutationFailure)
        }
    }

    fun onRemoveFavoriteTag(tag: String) {
        viewModelScope.launch {
            runCatching {
                val current = profileRepository.profile().first().foodPreferences
                profileRepository.updateFoodPreferences(current - tag)
            }.onFailure(::onMutationFailure)
        }
    }

    fun onRemoveAvoidTag(tag: String) {
        viewModelScope.launch {
            runCatching {
                val current = profileRepository.profile().first().foodExceptions
                profileRepository.updateFoodExceptions(current - tag)
            }.onFailure(::onMutationFailure)
        }
    }

    fun onToggleDietTag(tag: DietaryTag) {
        viewModelScope.launch {
            runCatching {
                val current = profileRepository.profile().first().diets
                val updated = if (tag.name in current) current - tag.name else current + tag.name
                profileRepository.updateDiets(updated)
            }.onFailure(::onMutationFailure)
        }
    }

    /** Surfaces a failed taste-profile edit as a toast instead of crashing; rethrows anything unexpected. */
    private fun onMutationFailure(e: Throwable) {
        if (e is CancellationException) throw e
        if (e !is ProfileException) throw e
        showToast(e.toDisplayMessage(), isError = true)
    }

    // --- Groups ---------------------------------------------------------

    fun onSubmitNewGroup() {
        val name = (localState.value.sheet as? ProfileSheet.NewGroup)?.input?.trim()
        if (name.isNullOrBlank()) return
        viewModelScope.launch {
            groupRepository.createGroup(name)
            showToast("Группа «$name» создана")
        }
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
