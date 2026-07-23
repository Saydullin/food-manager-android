package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodAccentButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopBarConfig
import com.devsgroup.foodmanager.core.designsystem.component.LocalFoodTopBarState
import com.devsgroup.foodmanager.core.designsystem.component.ReportFoodTopBar
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.GroupMember

/** "Группа" — a group's member list, pushed from tapping a row in [GroupsSection]. */
@Composable
internal fun GroupDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupDetailContent(
        uiState = uiState,
        onBack = onBack,
        onInviteClick = viewModel::onOpenInvite,
        onOfferClick = viewModel::onOfferClick,
        onInviteInputChange = viewModel::onInviteInputChange,
        onSendInvite = viewModel::onSendInvite,
        onDismissInvite = viewModel::onDismissInvite,
        modifier = modifier,
    )
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailUiState,
    onBack: () -> Unit,
    onInviteClick: () -> Unit,
    onOfferClick: () -> Unit,
    onInviteInputChange: (String) -> Unit,
    onSendInvite: () -> Unit,
    onDismissInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportFoodTopBar(
        FoodTopBarConfig(
            title = uiState.groupName,
            navigationIcon = {
                FoodIconButton(
                    icon = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    onClick = onBack,
                )
            },
        ),
    )
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "УЧАСТНИКИ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FoodAccentButton(text = "＋ Пригласить", onClick = onInviteClick)
            }
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                uiState.members.forEach { member ->
                    MemberRow(member = member, onOfferClick = onOfferClick)
                }
            }
        }

        ProfileToastHost(
            message = uiState.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }

    if (uiState.inviteSheetOpen) {
        TextInputSheet(
            title = "Пригласить в группу",
            helper = "Отправим приглашение по имени — в демо просто добавим участника",
            fieldLabel = "ИМЯ",
            value = uiState.inviteInput,
            onValueChange = onInviteInputChange,
            placeholder = "Имя близкого",
            submitLabel = "Отправить",
            onSubmit = onSendInvite,
            onDismiss = onDismissInvite,
        )
    }
}

@Composable
private fun MemberRow(
    member: GroupMember,
    onOfferClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = member.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = member.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = member.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (member.isPending) {
                Text(
                    text = "Ожидает",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FoodAccentButton(text = "Предложить", onClick = onOfferClick)
            }
        }
    }
}

@Preview
@Composable
private fun GroupDetailScreenPreview() {
    FoodManagerTheme {
        Surface {
            CompositionLocalProvider(LocalFoodTopBarState provides remember { mutableStateOf(null) }) {
                GroupDetailContent(
                    uiState = GroupDetailUiState(
                        groupName = "Семья",
                        members = listOf(
                            GroupMember(id = "1", name = "Вы", role = "Организатор"),
                            GroupMember(id = "2", name = "Олег", role = "Приглашён(а)", isPending = true),
                        ),
                    ),
                    onBack = {},
                    onInviteClick = {},
                    onOfferClick = {},
                    onInviteInputChange = {},
                    onSendInvite = {},
                    onDismissInvite = {},
                )
            }
        }
    }
}
