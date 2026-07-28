package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The tone that drives a [FoodTagChip]'s color — which set it belongs to on the taste
 * profile ("Предпочтения" vs "Исключения") rather than anything about the chip mechanics.
 */
enum class FoodChipTone { Accent, Destructive, Neutral }

/**
 * A tag chip — a "Предпочтения"/"Исключения" tag the user picked, or a static "Диета"
 * chip when [onRemove] is left null. The sibling to [FoodToggleChip]: this one *names*
 * a choice already made (with an optional trailing ✕ to take it back); that one *makes*
 * the choice from a picker list.
 *
 * @param tone which palette the chip reads in — [FoodChipTone.Accent] (orange tint,
 * liked), [FoodChipTone.Destructive] (outlined red, avoided) or [FoodChipTone.Neutral]
 * (dark fill, diet — pair with [leadingEmoji]).
 * @param onRemove shows a trailing ✕ and makes the whole chip tappable; omit for a
 * static, non-removable chip.
 */
@Composable
fun FoodTagChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: FoodChipTone = FoodChipTone.Accent,
    leadingEmoji: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    val colors = tone.colors()
    if (onRemove != null) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Surface(
                onClick = onRemove,
                modifier = modifier,
                shape = CircleShape,
                color = colors.container,
                contentColor = colors.content,
                border = colors.border,
            ) {
                ChipContent(label = label, leadingEmoji = leadingEmoji, showRemove = true)
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = colors.container,
            contentColor = colors.content,
            border = colors.border,
        ) {
            ChipContent(label = label, leadingEmoji = leadingEmoji, showRemove = false)
        }
    }
}

/**
 * A selectable option inside a picker sheet (the "Диета" bottom sheet's toggle list) —
 * the sibling to [FoodTagChip] that *makes* the selection instead of just naming one
 * already made. Tapping calls [onClick]; the caller owns [selected] (unidirectional
 * data flow) and flips it in response.
 */
@Composable
fun FoodToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val container = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = CircleShape,
            color = container,
            contentColor = content,
        ) {
            ChipContent(label = label, leadingEmoji = leadingEmoji, showRemove = false)
        }
    }
}

private data class ChipColors(val container: Color, val content: Color, val border: BorderStroke?)

@Composable
private fun FoodChipTone.colors(): ChipColors = when (this) {
    FoodChipTone.Accent -> ChipColors(
        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        content = MaterialTheme.colorScheme.primary,
        border = null,
    )

    FoodChipTone.Destructive -> ChipColors(
        container = Color.Transparent,
        content = MaterialTheme.colorScheme.error,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
    )

    FoodChipTone.Neutral -> ChipColors(
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
        border = null,
    )
}

/** Shared label row: an optional leading emoji, the label, and an optional trailing ✕. */
@Composable
private fun ChipContent(label: String, leadingEmoji: String?, showRemove: Boolean) {
    Row(
        modifier = Modifier.padding(
            start = 16.dp,
            end = if (showRemove) 12.dp else 16.dp,
            top = 10.dp,
            bottom = 10.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        if (showRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview
@Composable
private fun FoodChipsPreview() {
    FoodManagerTheme {
        Surface {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FoodTagChip(label = "Острое", onRemove = {})
                FoodTagChip(label = "Орехи", tone = FoodChipTone.Destructive, onRemove = {})
                FoodTagChip(label = "Веган", tone = FoodChipTone.Neutral, leadingEmoji = "🥦")
                FoodToggleChip(label = "Вегетарианец", leadingEmoji = "🥕", selected = true, onClick = {})
                FoodToggleChip(label = "Без глютена", leadingEmoji = "🌾", selected = false, onClick = {})
            }
        }
    }
}
