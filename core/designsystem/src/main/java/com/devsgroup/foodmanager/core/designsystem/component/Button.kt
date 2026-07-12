package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The app's button family — the design-system atoms for calls-to-action.
 *
 * All variants live in this one file on purpose: they are the same atom at different
 * emphasis levels, share one signature shape `(text, onClick, modifier, enabled)`, and
 * changing "how our buttons look" should be a single-file edit. New button *variants*
 * belong here as siblings; only a genuinely different component (e.g. a text field)
 * earns its own file in `component/`.
 *
 * Emphasis ladder, high → low. Reach for the lowest emphasis that still reads:
 *  - [FoodButton]         filled, brand orange — the one primary action on a screen.
 *  - [FoodTonalButton]    softer fill — a secondary action sitting next to the primary.
 *  - [FoodElevatedButton] filled + shadow — when a button floats over busy content.
 *  - [FoodOutlinedButton] outline only — secondary action on a plain surface.
 *  - [FoodTextButton]     no container — lowest emphasis (dialogs, "Skip", inline links).
 */

// --- High emphasis --------------------------------------------------------

/** Primary, filled call-to-action. One per screen. */
@Composable
fun FoodButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

// --- Medium emphasis ------------------------------------------------------

/** Secondary action with a soft tonal fill — pairs beside a [FoodButton]. */
@Composable
fun FoodTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

/** Filled action with a shadow — use when the button floats over busy content. */
@Composable
fun FoodElevatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

/** Secondary action drawn as an outline — good on plain surfaces. */
@Composable
fun FoodOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

// --- Low emphasis ---------------------------------------------------------

/** Lowest-emphasis action with no container — dialogs, "Skip", inline links. */
@Composable
fun FoodTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

@Preview
@Composable
private fun FoodButtonsPreview() {
    FoodManagerTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FoodButton(text = "Начать", onClick = {})
                FoodTonalButton(text = "Позже", onClick = {})
                FoodElevatedButton(text = "Поделиться", onClick = {})
                FoodOutlinedButton(text = "Отмена", onClick = {})
                FoodTextButton(text = "Пропустить", onClick = {})
            }
        }
    }
}
