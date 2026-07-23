package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField
import com.devsgroup.foodmanager.core.designsystem.component.FoodToggleChip
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopAppBar
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.CookingAttitude

/** "Редактировать профиль" — a full-screen push from the header's edit (pencil) button. */
@Composable
internal fun EditProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onAvatarChange(it.toString()) } }

    EditProfileContent(
        uiState = uiState,
        onBack = onBack,
        onAvatarClick = {
            pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onDeletePhotoClick = viewModel::onAvatarDelete,
        onStatusEmojiSelect = viewModel::onStatusEmojiSelect,
        onUsernameChange = viewModel::onUsernameChange,
        onNameChange = viewModel::onNameChange,
        onAgeChange = viewModel::onAgeChange,
        onCookingAttitudeSelect = viewModel::onCookingAttitudeSelect,
        onBioChange = viewModel::onBioChange,
        onSaveClick = { viewModel.onSave(onBack) },
        modifier = modifier,
    )
}

@Composable
private fun EditProfileContent(
    uiState: EditProfileUiState,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    onDeletePhotoClick: () -> Unit,
    onStatusEmojiSelect: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onCookingAttitudeSelect: (CookingAttitude) -> Unit,
    onBioChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FoodTopAppBar(
                title = "Редактировать профиль",
                navigationIcon = {
                    FoodIconButton(
                        icon = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Назад",
                        onClick = onBack,
                    )
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            EditProfileAvatarSection(
                avatarUrl = uiState.avatarUrl,
                displayName = uiState.name.ifBlank { uiState.username },
                statusEmoji = uiState.statusEmoji,
                onAvatarClick = onAvatarClick,
                onStatusEmojiSelect = onStatusEmojiSelect,
                onDeletePhotoClick = onDeletePhotoClick,
            )
            Spacer(Modifier.height(24.dp))
            FoodTextField(
                value = uiState.username,
                onValueChange = onUsernameChange,
                label = "НИКНЕЙМ",
                placeholder = "nickname",
                description = "Отображается с «@» перед именем".takeIf { uiState.usernameError == null },
                isError = uiState.usernameError != null,
                errorMessage = uiState.usernameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(18.dp))
            FoodTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = "ИМЯ",
                isRequired = false,
                placeholder = "Как вас называть",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(18.dp))
            FoodTextField(
                value = uiState.age,
                onValueChange = onAgeChange,
                label = "ВОЗРАСТ",
                isRequired = false,
                placeholder = "Например, 28",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "ОТНОШЕНИЕ К ГОТОВКЕ · необязательно",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                CookingAttitude.entries.forEach { attitude ->
                    FoodToggleChip(
                        label = attitude.label,
                        selected = uiState.cookingAttitude == attitude,
                        onClick = { onCookingAttitudeSelect(attitude) },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            FoodTextField(
                value = uiState.bio,
                onValueChange = onBioChange,
                label = "О СЕБЕ",
                isRequired = false,
                placeholder = "Пара слов о себе",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Spacer(Modifier.height(28.dp))
            FoodButton(
                text = if (uiState.isSaving) "Сохраняем…" else "Сохранить",
                onClick = onSaveClick,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal val CookingAttitude.label: String
    get() = when (this) {
        CookingAttitude.LOVE_COOKING -> "Люблю готовить"
        CookingAttitude.COOK_SOMETIMES -> "Готовлю иногда"
        CookingAttitude.PREFER_ORDERING -> "Предпочитаю заказывать"
        CookingAttitude.LEARNING -> "Учусь готовить"
    }

/**
 * Avatar + emoji-status editor — the block above "Никнейм" (see the profile-image-edit
 * mock): a tappable circular avatar carrying an optional emoji-status badge, a picker
 * grid to set that status, a hint line, and a destructive "Удалить фото" action that
 * only shows once a photo is set.
 */
@Composable
private fun EditProfileAvatarSection(
    avatarUrl: String?,
    displayName: String,
    statusEmoji: String?,
    onAvatarClick: () -> Unit,
    onStatusEmojiSelect: (String) -> Unit,
    onDeletePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(96.dp)) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable(onClick = onAvatarClick),
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onAvatarClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (statusEmoji != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = statusEmoji, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            STATUS_EMOJIS.forEach { emoji ->
                StatusEmojiOption(
                    emoji = emoji,
                    selected = emoji == statusEmoji,
                    onClick = { onStatusEmojiSelect(emoji) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Нажмите на аватар, чтобы выбрать новое фото",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (avatarUrl != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Удалить фото",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onDeletePhotoClick),
            )
        }
    }
}

/** One selectable circle in the emoji-status grid — an orange ring marks the current pick. */
@Composable
private fun StatusEmojiOption(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, accent) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Fun status emoji a user can pin to their avatar badge — decorative, not tied to any domain enum. */
private val STATUS_EMOJIS = listOf("😄", "🌱", "🔥", "🔍", "☕", "🍚", "🍰", "🎉")

@Preview
@Composable
private fun EditProfileScreenPreview() {
    FoodManagerTheme {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(username = "chef_maria", name = "Мария", statusEmoji = "🍰"),
                onBack = {},
                onAvatarClick = {},
                onDeletePhotoClick = {},
                onStatusEmojiSelect = {},
                onUsernameChange = {},
                onNameChange = {},
                onAgeChange = {},
                onCookingAttitudeSelect = {},
                onBioChange = {},
                onSaveClick = {},
            )
        }
    }
}
