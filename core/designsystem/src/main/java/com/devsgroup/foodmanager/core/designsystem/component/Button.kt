package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The app's button family — the design-system atoms for calls-to-action.
 *
 * All variants live in this one file on purpose: they are the same atom at different
 * emphasis levels, share one signature `(text, onClick, modifier, enabled, leadingIcon)`,
 * and changing "how our buttons look" should be a single-file edit. New button *variants*
 * belong here as siblings; only a genuinely different component (e.g. a text field)
 * earns its own file in `component/`.
 *
 * [leadingIcon] is an optional [Painter] drawn before the label — typically a
 * vector drawable via `painterResource(R.drawable.ic_…)`. It is decorative here
 * (the visible text carries the meaning), so it is rendered with a null content
 * description and tinted to match the button's content color.
 *
 * Emphasis ladder, high → low. Reach for the lowest emphasis that still reads:
 *  - [FoodButton]         filled, brand orange — the one primary action on a screen.
 *  - [FoodTonalButton]    softer fill — a secondary action sitting next to the primary.
 *  - [FoodElevatedButton] filled + shadow — when a button floats over busy content.
 *  - [FoodOutlinedButton] outline only — secondary action on a plain surface.
 *  - [FoodTextButton]     no container — lowest emphasis (dialogs, "Skip", inline links).
 *
 * [FoodIconButton] is the label-less sibling: the icon *is* the button (app-bar
 * actions, a corner "✕"), so it takes a required content description instead of a text.
 */

// --- High emphasis --------------------------------------------------------

/** Primary, filled call-to-action. One per screen. */
@Composable
fun FoodButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: Painter? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
    ) {
        ButtonContent(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            leadingIcon = leadingIcon,
        )
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
    leadingIcon: Painter? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
    ) {
        ButtonContent(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            leadingIcon = leadingIcon,
        )
    }
}

/** Filled action with a shadow — use when the button floats over busy content. */
@Composable
fun FoodElevatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: Painter? = null,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
    ) {
        ButtonContent(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            leadingIcon = leadingIcon,
        )
    }
}

/** Secondary action drawn as an outline — good on plain surfaces. */
@Composable
fun FoodOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: Painter? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
    ) {
        ButtonContent(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            leadingIcon = leadingIcon,
        )
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
    leadingIcon: Painter? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
    ) {
        ButtonContent(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            leadingIcon = leadingIcon,
        )
    }
}

// --- Icon only ------------------------------------------------------------

/**
 * Icon-only action with no label — the icon *is* the button (app-bar actions, a
 * corner "✕", compact inline controls). No container, so it is the icon-side
 * counterpart to [FoodTextButton]; the M3 `IconButton` still gives it a ripple and
 * the 48dp minimum touch target.
 *
 * Unlike the decorative [leadingIcon] on the labelled buttons, [contentDescription]
 * is **required**: with no visible text it is the only thing a screen reader can
 * announce for this control. Pass a short verb phrase (e.g. "Закрыть").
 */
@Composable
fun FoodIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Shared label layout for every button variant: an optional leading icon,
 * a standard gap, then the text.
 *
 * The label is capped at a single line and ellipsized when it overflows, so a
 * text too long for the button's width truncates ("В корзи…") rather than
 * wrapping or pushing the button taller. When the width is constrained (a
 * `Modifier.weight` sibling in a bar, `fillMaxWidth`, a large font scale), the
 * `Text` yields room for the icon and truncates itself; an intrinsically-sized
 * button that has room stays on one line exactly as before.
 */
@Composable
private fun ButtonContent(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    leadingIcon: Painter?,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(
            ButtonDefaults.IconSpacing,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        }
        Text(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                FoodTonalButton(
                    modifier = Modifier
                        .width(100.dp),
                    text = "Позже",
                    onClick = {},
                    leadingIcon = painterResource(R.drawable.ic_skip),
                )
                FoodTonalButton(
                    text = "Позже",
                    onClick = {},
                    leadingIcon = painterResource(R.drawable.ic_skip),
                )
                FoodButton(
                    text = "В корзину",
                    onClick = {},
                    leadingIcon = painterResource(R.drawable.ic_favorite),
                )
                FoodElevatedButton(text = "Поделиться", onClick = {})
                FoodOutlinedButton(text = "Не то", onClick = {})
                FoodTextButton(text = "Пропустить", onClick = {})
                FoodIconButton(
                    icon = painterResource(R.drawable.ic_close),
                    contentDescription = "Закрыть",
                    onClick = {},
                )
            }
        }
    }
}
