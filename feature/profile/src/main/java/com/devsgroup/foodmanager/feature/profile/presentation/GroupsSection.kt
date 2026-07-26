package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodAccentButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.GroupMember
import com.devsgroup.foodmanager.core.model.UserGroup

/** "Группы" — the household/friend groups list, with create/open/delete actions. */
@Composable
internal fun GroupsSection(
    groups: List<UserGroup>,
    onNewGroupClick: () -> Unit,
    onGroupClick: (UserGroup) -> Unit,
    onDeleteGroup: (UserGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Группы",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            FoodAccentButton(
                text = "Создать",
                onClick = onNewGroupClick,
                compact = true,
            )
        }

        if (groups.isEmpty()) {
            Text(
                text = "Пока нет групп — создайте, чтобы делить блюда с близкими.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 13.dp),
            )
        } else {
            AnimatedItemList(
                items = groups,
                keySelector = { it.id },
                layout = { content ->
                    Column(
                        modifier = Modifier.padding(top = 13.dp).animateContentSize(itemSpring()),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                        content = { content() },
                    )
                },
            ) { group ->
                GroupRow(
                    group = group,
                    onClick = { onGroupClick(group) },
                    onDelete = { onDeleteGroup(group) },
                )
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: UserGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp, horizontal = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmojiTile(emoji = group.emoji)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = group.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = pluralize(group.members.size, one = "участник", few = "участника", many = "участников"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FoodIconButton(
                icon = painterResource(R.drawable.ic_delete),
                contentDescription = "Удалить группу «${group.name}»",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun EmojiTile(emoji: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview
@Composable
private fun GroupsSectionPreview() {
    FoodManagerTheme {
        Surface {
            GroupsSection(
                groups = listOf(
                    UserGroup(
                        id = "1",
                        emoji = "👨‍👩‍👧",
                        name = "Семья",
                        members = listOf(
                            GroupMember(id = "m1", name = "Вы", role = "Организатор"),
                            GroupMember(id = "m2", name = "Олег", role = "Участник", isPending = true),
                        ),
                    ),
                ),
                onNewGroupClick = {},
                onGroupClick = {},
                onDeleteGroup = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
