package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodModalBottomSheet
import com.devsgroup.foodmanager.core.designsystem.component.FoodOutlinedButton

/**
 * The reasons a user can give for disliking a recommended dish. The choice is a
 * personalization signal — it tells the recommender *why* the card missed, so the
 * next ones land better.
 *
 * It lives in `presentation` for now because the domain layer for recommendations
 * is still a placeholder. Promote it to the feature's `domain` package (or
 * `:core:domain`) once the "record dislike" use-case exists — the sheet and the
 * [RecommendationsViewModel] already speak in terms of this type, so the move is
 * mechanical.
 */
enum class DislikeReason(val label: String) {
    Ingredients("Не нравятся ингредиенты"),
    TooSpicy("Слишком остро"),
    Cuisine("Не та кухня"),
    AlreadyTried("Уже пробовал(а)"),
    Other("Другое"),
}

/**
 * The "Не люблю" bottom sheet — a feature *molecule* that asks *why* the user
 * disliked the current card so the answer can sharpen future recommendations.
 *
 * It lives in the feature (like [RecommendationActionsBar]) because the question
 * and its [DislikeReason]s are specific to the recommendations flow, and it is
 * built only from design-system atoms ([FoodModalBottomSheet] + [FoodOutlinedButton]).
 * It stays UI-only: it emits the chosen reason and a dismiss event and holds no
 * state itself — visibility and recording live in [RecommendationsViewModel].
 */
@Composable
internal fun DislikeReasonSheet(
    onReasonSelected: (DislikeReason) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FoodModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = "Почему не нравится?",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Выберите причину — рекомендации станут точнее.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DislikeReason.entries.forEach { reason ->
                FoodOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = reason.label,
                    onClick = { onReasonSelected(reason) },
                )
            }
        }
    }
}
