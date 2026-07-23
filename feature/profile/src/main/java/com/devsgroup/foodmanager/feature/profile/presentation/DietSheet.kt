package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodModalBottomSheet
import com.devsgroup.foodmanager.core.designsystem.component.FoodToggleChip
import com.devsgroup.foodmanager.core.model.DietaryTag

/** "Диета и аллергии" — a multi-select of every [DietaryTag], committed as each is tapped. */
@Composable
internal fun DietSheet(
    selected: Set<DietaryTag>,
    onToggle: (DietaryTag) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FoodModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Text(text = "Диета и аллергии", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Учтём при подборе блюд",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp, bottom = 16.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            DietaryTag.entries.forEach { tag ->
                FoodToggleChip(
                    label = tag.label,
                    leadingEmoji = tag.emoji,
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        FoodButton(text = "Готово", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}
