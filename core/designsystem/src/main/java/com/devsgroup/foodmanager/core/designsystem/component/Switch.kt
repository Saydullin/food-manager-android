package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The brand on/off switch — a settings-row toggle (push notifications, dark theme).
 * A thin restyle of the M3 `Switch` so its thumb/track pick up the brand orange instead
 * of the default primary role wherever that ever diverges; kept as its own atom for the
 * same reason [FoodTextField] restyles `OutlinedTextField`.
 */
@Composable
fun FoodSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Preview
@Composable
private fun FoodSwitchPreview() {
    FoodManagerTheme {
        Surface {
            FoodSwitch(checked = true, onCheckedChange = {})
        }
    }
}
