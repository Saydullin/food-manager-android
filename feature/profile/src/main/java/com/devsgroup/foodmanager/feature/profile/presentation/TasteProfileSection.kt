package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * "Предпочтения" — liked tags learned from the user's swipes.
 */
@Composable
internal fun PreferredTagsSection(
    favoriteTags: Set<String>,
    onAddFavoriteTagClick: () -> Unit,
    onRemoveFavoriteTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TasteSectionHeader(
            label = "Предпочтения",
            onAddClick = onAddFavoriteTagClick,
        )
        Text(
            text = "Учли по вашим оценкам блюд",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        AnimatedItemList(
            items = favoriteTags.toList(),
            keySelector = { it },
            layout = { sizeAnimationModifier, content ->
                FlowRow(
                    modifier = Modifier.padding(top = 13.dp).then(sizeAnimationModifier),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = { content() },
                )
            },
        ) { tag ->
            FoodTagChip(label = tag, onRemove = { onRemoveFavoriteTag(tag) })
        }
    }
}

/**
 * "Исключения" — avoided tags learned from the user's swipes.
 */
@Composable
internal fun AvoidedTagsSection(
    avoidTags: Set<String>,
    onAddAvoidTagClick: () -> Unit,
    onRemoveAvoidTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TasteSectionHeader(
            label = "Исключения",
            onAddClick = onAddAvoidTagClick,
        )
        if (avoidTags.isEmpty()) {
            Text(
                text = "Пока ничего — свайпайте «Не то», и мы запомним.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 13.dp),
            )
        } else {
            AnimatedItemList(
                items = avoidTags.toList(),
                keySelector = { it },
                layout = { sizeAnimationModifier, content ->
                    FlowRow(
                        modifier = Modifier.padding(top = 13.dp).then(sizeAnimationModifier),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = { content() },
                    )
                },
            ) { tag ->
                FoodTagChip(label = tag, tone = FoodChipTone.Destructive, onRemove = { onRemoveAvoidTag(tag) })
            }
        }
    }
}

/**
 * "Диета" — the selected diet/allergy tags.
 */
@Composable
internal fun DietSection(
    dietTags: Set<DietaryTag>,
    onDietClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TasteSectionHeader(
            label = "Диета",
            onAddClick = onDietClick,
            buttonText = if (dietTags.isNotEmpty()) "Изменить" else "Добавить",
        )
        if (dietTags.isEmpty()) {
            Text(
                text = "Полная свобода, никаких диет",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 13.dp),
            )
        } else {
            AnimatedItemList(
                items = dietTags.toList(),
                keySelector = { it },
                layout = { sizeAnimationModifier, content ->
                    FlowRow(
                        modifier = Modifier.padding(top = 13.dp).then(sizeAnimationModifier),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = { content() },
                    )
                },
            ) { tag ->
                FoodTagChip(label = tag.label, tone = FoodChipTone.Neutral, leadingEmoji = tag.emoji)
            }
        }
    }
}

@Composable
private fun TasteSectionHeader(
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
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
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
private fun PreferredTagsSectionPreview() {
    FoodManagerTheme {
        Surface {
            PreferredTagsSection(
                favoriteTags = setOf("Острое", "Итальянская", "Рыба"),
                onAddFavoriteTagClick = {},
                onRemoveFavoriteTag = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun AvoidedTagsSectionPreview() {
    FoodManagerTheme {
        Surface {
            AvoidedTagsSection(
                avoidTags = emptySet(),
                onAddAvoidTagClick = {},
                onRemoveAvoidTag = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun DietSectionPreview() {
    FoodManagerTheme {
        Surface {
            DietSection(
                dietTags = setOf(DietaryTag.VEGETARIAN),
                onDietClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
