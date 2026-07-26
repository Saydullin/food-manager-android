package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodAccentButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodChipTone
import com.devsgroup.foodmanager.core.designsystem.component.FoodTagChip
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.DietaryTag

/**
 * "Вкусовой профиль" — the learned/editable taste profile: liked tags, avoided tags,
 * and the selected diet. Each subsection follows the same shape (an uppercase label,
 * an inline add action, then wrapped chips), so [TasteSubsectionHeader] factors that out.
 */
@Composable
internal fun TasteProfileSection(
    favoriteTags: Set<String>,
    avoidTags: Set<String>,
    dietTags: Set<DietaryTag>,
    onAddFavoriteTagClick: () -> Unit,
    onRemoveFavoriteTag: (String) -> Unit,
    onAddAvoidTagClick: () -> Unit,
    onRemoveAvoidTag: (String) -> Unit,
    onDietClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Вкусовой профиль",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Учли по вашим оценкам блюд",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(16.dp))

        TasteSubsectionHeader(
            label = "ПРЕДПОЧТЕНИЯ",
            onAddClick = onAddFavoriteTagClick,
        )
        AnimatedItemList(
            items = favoriteTags.toList(),
            keySelector = { it },
            layout = { content ->
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp).animateContentSize(itemSpring()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = { content() },
                )
            },
        ) { tag ->
            FoodTagChip(label = tag, onRemove = { onRemoveFavoriteTag(tag) })
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
        )

        TasteSubsectionHeader(
            label = "ИСКЛЮЧЕНИЯ",
            onAddClick = onAddAvoidTagClick,
        )
        AnimatedItemList(
            items = avoidTags.toList(),
            keySelector = { it },
            layout = { content ->
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp).animateContentSize(itemSpring()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = { content() },
                )
            },
        ) { tag ->
            FoodTagChip(label = tag, tone = FoodChipTone.Destructive, onRemove = { onRemoveAvoidTag(tag) })
        }
        if (avoidTags.isEmpty()) {
            Text(
                text = "Пока ничего — свайпайте «Не то», и мы запомним.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
        )

        TasteSubsectionHeader(
            label = "ДИЕТА",
            onAddClick = onDietClick,
            buttonText = if (dietTags.isNotEmpty()) "Изменить" else "Добавить",
        )
        AnimatedItemList(
            items = dietTags.toList(),
            keySelector = { it },
            layout = { content ->
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp).animateContentSize(itemSpring()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = { content() },
                )
            },
        ) { tag ->
            FoodTagChip(label = tag.label, tone = FoodChipTone.Neutral, leadingEmoji = tag.emoji)
        }
        if (dietTags.isEmpty()) {
            Text(
                text = "Полная свобода, никаких диет",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TasteSubsectionHeader(
    label: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Добавить",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FoodAccentButton(
            text = buttonText,
            onClick = onAddClick,
            compact = true,
        )
    }
}

@Preview
@Composable
private fun TasteProfileSectionPreview() {
    FoodManagerTheme {
        Surface {
            TasteProfileSection(
                favoriteTags = setOf("Острое", "Итальянская", "Рыба"),
                avoidTags = emptySet(),
                dietTags = setOf(DietaryTag.VEGETARIAN),
                onAddFavoriteTagClick = {},
                onRemoveFavoriteTag = {},
                onAddAvoidTagClick = {},
                onRemoveAvoidTag = {},
                onDietClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
