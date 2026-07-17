package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodOutlinedButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTonalButton
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * Recommendation footer — a *molecule* that arranges the design-system button
 * *atoms* into the three actions from the "Для вас" mock: "Сохранить" (save to
 * favorites), "Не люблю" (dislike — opens the [DislikeReasonSheet]), and
 * "Пропустить" (skip without a verdict).
 *
 * It lives in the feature — not in `:core:designsystem` — because these three
 * actions are specific to the recommendations flow, and not in `:core:ui`
 * because it references no domain model. It stays UI-only: one callback per
 * action, no `Food`, no ViewModel. That keeps it trivial to preview and easy to
 * promote up to `:core:ui`/`:core:designsystem` later, if a second feature (e.g.
 * the discover deck) ever needs the same bar.
 *
 * Leading icons are vector drawables from `:core:designsystem` passed as a
 * [androidx.compose.ui.graphics.painter.Painter].
 */
@Composable
internal fun RecommendationActionsBar(
    onDislike: () -> Unit,
    onReject: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The one primary action on the screen — widest, brand-orange fill.
        FoodButton(
            modifier = Modifier
                .fillMaxWidth(),
            text = "Сохранить",
            onClick = onAddToCart,
            leadingIcon = painterResource(R.drawable.ic_favorite),
        )
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A soft "no" — opens the reason sheet to capture *why* it missed.
            FoodTonalButton(
                modifier = Modifier
                    .weight(.4f),
                onClick = onDislike,
                text = "Не люблю",
                leadingIcon = painterResource(R.drawable.ic_close),
            )
            // Skip this card without recording a verdict.
            FoodOutlinedButton(
                modifier = Modifier.weight(.5f),
                text = "Пропустить",
                onClick = onReject,
                leadingIcon = painterResource(R.drawable.ic_skip),
            )
        }
    }
}

@Preview
@Composable
private fun RecommendationActionsBarPreview() {
    FoodManagerTheme {
        Surface {
            RecommendationActionsBar(
                onDislike = {},
                onReject = {},
                onAddToCart = {},
            )
        }
    }
}
