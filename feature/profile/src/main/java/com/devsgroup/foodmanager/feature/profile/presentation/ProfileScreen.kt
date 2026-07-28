package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.AppLanguage
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.UserGroup
import com.devsgroup.foodmanager.core.model.UserProfile

/**
 * "Профиль" — the top-level profile screen: identity header, editable taste profile,
 * groups, own recipes, liked dishes, and app settings. Rendered under the shared
 * top-level app bar/bottom nav (see `FoodApp`), so this composable owns no `Scaffold`
 * of its own — just the scrollable body and its overlay sheets/toast.
 */
@Composable
internal fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToGroupDetail: (groupId: String) -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onEditProfileClick = onNavigateToEditProfile,
        onAddFavoriteTagClick = viewModel::onAddFavoriteTagClick,
        onRemoveFavoriteTag = viewModel::onRemoveFavoriteTag,
        onAddAvoidTagClick = viewModel::onAddAvoidTagClick,
        onRemoveAvoidTag = viewModel::onRemoveAvoidTag,
        onDietClick = viewModel::onDietClick,
        onNewGroupClick = viewModel::onNewGroupClick,
        onGroupClick = { group -> onNavigateToGroupDetail(group.id) },
        onDeleteGroup = viewModel::onDeleteGroup,
        onCreateRecipeClick = viewModel::onCreateRecipeClick,
        onUnlikeFood = viewModel::onUnlikeFood,
        onToggleNotifications = viewModel::onToggleNotifications,
        onToggleDarkTheme = viewModel::onToggleDarkTheme,
        onLanguageClick = viewModel::onLanguageClick,
        onHelpClick = viewModel::onHelpClick,
        onLogoutClick = { viewModel.onLogout(onLoggedOut) },
        onSheetInputChange = viewModel::onSheetInputChange,
        onSubmitFavoriteTag = viewModel::onSubmitFavoriteTag,
        onSubmitAvoidTag = viewModel::onSubmitAvoidTag,
        onSubmitNewGroup = viewModel::onSubmitNewGroup,
        onToggleDietTag = viewModel::onToggleDietTag,
        onSelectLanguage = viewModel::onSelectLanguage,
        onDismissSheet = viewModel::onDismissSheet,
        modifier = modifier,
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onRefresh: () -> Unit,
    onEditProfileClick: () -> Unit,
    onAddFavoriteTagClick: () -> Unit,
    onRemoveFavoriteTag: (String) -> Unit,
    onAddAvoidTagClick: () -> Unit,
    onRemoveAvoidTag: (String) -> Unit,
    onDietClick: () -> Unit,
    onNewGroupClick: () -> Unit,
    onGroupClick: (UserGroup) -> Unit,
    onDeleteGroup: (UserGroup) -> Unit,
    onCreateRecipeClick: () -> Unit,
    onUnlikeFood: (Food) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSheetInputChange: (String) -> Unit,
    onSubmitFavoriteTag: () -> Unit,
    onSubmitAvoidTag: () -> Unit,
    onSubmitNewGroup: () -> Unit,
    onToggleDietTag: (DietaryTag) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                ProfileHeader(profile = uiState.profile, isBanned = uiState.isBanned, onEditClick = onEditProfileClick)
                PreferredTagsSection(
                    favoriteTags = uiState.favoriteTags,
                    onAddFavoriteTagClick = onAddFavoriteTagClick,
                    onRemoveFavoriteTag = onRemoveFavoriteTag,
                )
                AvoidedTagsSection(
                    avoidTags = uiState.avoidTags,
                    onAddAvoidTagClick = onAddAvoidTagClick,
                    onRemoveAvoidTag = onRemoveAvoidTag,
                )
                DietSection(
                    dietTags = uiState.dietTags,
                    onDietClick = onDietClick,
                )
                GroupsSection(
                    groups = uiState.groups,
                    onNewGroupClick = onNewGroupClick,
                    onGroupClick = onGroupClick,
                    onDeleteGroup = onDeleteGroup,
                )
                MyRecipesSection(
                    recipes = uiState.myRecipes,
                    onCreateClick = onCreateRecipeClick,
                )
                SettingsSection(
                    notificationsEnabled = uiState.notificationsEnabled,
                    darkThemeEnabled = uiState.darkThemeEnabled,
                    language = uiState.language,
                    onToggleNotifications = onToggleNotifications,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onLanguageClick = onLanguageClick,
                    onHelpClick = onHelpClick,
                    onLogoutClick = onLogoutClick,
                )
            }
        }

        ProfileToastHost(
            message = uiState.toast,
            isError = uiState.toastIsError,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }

    when (val sheet = uiState.sheet) {
        is ProfileSheet.AddFavoriteTag -> TextInputSheet(
            title = "Добавить предпочтения",
            helper = "Например: «Острое», «Итальянская», «Рыба»",
            fieldLabel = "ТЕГ",
            value = sheet.input,
            onValueChange = onSheetInputChange,
            placeholder = "Что вы любите",
            submitLabel = "Добавить",
            onSubmit = onSubmitFavoriteTag,
            onDismiss = onDismissSheet,
        )

        is ProfileSheet.AddAvoidTag -> TextInputSheet(
            title = "Добавить исключения",
            helper = "Аллергия или блюдо, которое не хочется видеть в подборке",
            fieldLabel = "ТЕГ",
            value = sheet.input,
            onValueChange = onSheetInputChange,
            placeholder = "Например: орехи",
            submitLabel = "Добавить",
            onSubmit = onSubmitAvoidTag,
            onDismiss = onDismissSheet,
        )

        is ProfileSheet.NewGroup -> TextInputSheet(
            title = "Новая группа",
            helper = "Название группы - потом позовёте туда близких",
            fieldLabel = "НАЗВАНИЕ",
            value = sheet.input,
            onValueChange = onSheetInputChange,
            placeholder = "Например: Семья",
            submitLabel = "Создать группу",
            onSubmit = onSubmitNewGroup,
            onDismiss = onDismissSheet,
        )

        ProfileSheet.Diet -> DietSheet(
            selected = uiState.dietTags,
            onToggle = onToggleDietTag,
            onDismiss = onDismissSheet,
        )

        ProfileSheet.Language -> LanguageSheet(
            selected = uiState.language,
            onSelect = onSelectLanguage,
            onDismiss = onDismissSheet,
        )

        null -> Unit
    }
}

/**
 * The bottom-anchored confirmation pill. Keeps the last non-null [message] (and its
 * [isError] style) around while [AnimatedVisibility] plays the exit transition, so
 * neither blanks out mid-fade once the view model clears the toast.
 */
@Composable
internal fun ProfileToastHost(message: String?, isError: Boolean = false, modifier: Modifier = Modifier) {
    var lastMessage by remember { mutableStateOf("") }
    var lastIsError by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        message?.let {
            lastMessage = it
            lastIsError = isError
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (lastIsError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.inverseSurface
            },
            contentColor = if (lastIsError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.inverseOnSurface
            },
            shadowElevation = 6.dp,
        ) {
            Text(
                text = lastMessage,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    FoodManagerTheme {
        Surface {
            ProfileContent(
                uiState = ProfileUiState(
                    profile = UserProfile(name = "Мария", username = "@chef_maria", age = 28),
                    favoriteTags = setOf("Острое", "Итальянская"),
                    avoidTags = emptySet(),
                    dietTags = setOf(DietaryTag.VEGETARIAN),
                    groups = listOf(UserGroup(id = "1", emoji = "👨‍👩‍👧", name = "Семья")),
                ),
                onRefresh = {},
                onEditProfileClick = {},
                onAddFavoriteTagClick = {},
                onRemoveFavoriteTag = {},
                onAddAvoidTagClick = {},
                onRemoveAvoidTag = {},
                onDietClick = {},
                onNewGroupClick = {},
                onGroupClick = {},
                onDeleteGroup = {},
                onCreateRecipeClick = {},
                onUnlikeFood = {},
                onToggleNotifications = {},
                onToggleDarkTheme = {},
                onLanguageClick = {},
                onHelpClick = {},
                onLogoutClick = {},
                onSheetInputChange = {},
                onSubmitFavoriteTag = {},
                onSubmitAvoidTag = {},
                onSubmitNewGroup = {},
                onToggleDietTag = {},
                onSelectLanguage = {},
                onDismissSheet = {},
            )
        }
    }
}
