package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun RecommendationsScreen(
    onFoodClick: (foodId: String) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // The deck advances by *replacing* the top card, so the swap is animated: the
        // old card flies off in the direction of the verdict while the next one settles
        // up from below with a soft spring. Keyed on the card itself, so every switch —
        // and the final hand-off to the empty state — flows.
        AnimatedContent(
            targetState = uiState.currentCard,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = { cardFlow(uiState.lastVerdict) },
            label = "recommendationCard",
        ) { card ->
            if (card != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // The hero photo card fills the space above the tip; tapping it opens
                    // the dish. Clip first so the tap ripple honors the rounded corners.
                    RecommendationCard(
                        card = card,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .clickable { onFoodClick(card.id) },
                    )
                    card.tip?.let { tip ->
                        RecommendationTip(
                            text = tip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            } else {
                // The deck is exhausted — every card has been reviewed.
                EmptyDeck(modifier = Modifier.fillMaxSize())
            }
        }

        // The footer molecule; events flow up to the ViewModel (unidirectional data flow).
        RecommendationActionsBar(
            onDislike = viewModel::onDislike,
            onReject = viewModel::onReject,
            onAddToCart = viewModel::onAddToCart,
        )
    }

    // "Не люблю" sheet — rendered only while the ViewModel says it's open; the
    // same UDF loop closes it on a reason pick or a dismiss.
    if (uiState.showDislikeSheet) {
        DislikeReasonSheet(
            onReasonSelected = viewModel::onDislikeReasonSelected,
            onDismiss = viewModel::onDislikeSheetDismiss,
        )
    }
}

/**
 * The card-swap transition. The incoming card always **rises up from just below and
 * springs into place** (a gentle overshoot), so a new dish reads as "dealt" onto the
 * deck. The outgoing card leaves in the direction that matches the [verdict], turning
 * the animation into feedback for the tap:
 *  - [SAVED][RecommendationVerdict.SAVED] — lifts up and grows as it fades (accepted).
 *  - [DISLIKED][RecommendationVerdict.DISLIKED] — flicks left and shrinks (rejected).
 *  - [SKIPPED][RecommendationVerdict.SKIPPED] — flicks right and shrinks (set aside).
 *
 * The enter uses a low-stiffness spring for an organic settle; the exit is a short
 * eased tween so it clears quickly and doesn't fight the incoming card.
 */
private fun cardFlow(
    verdict: RecommendationVerdict?,
): ContentTransform {
    val enter = slideInVertically(
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
    ) { fullHeight -> fullHeight / 6 } +
        fadeIn(animationSpec = tween(durationMillis = 250)) +
        scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            initialScale = 0.90f,
        )

    val exit = when (verdict) {
        RecommendationVerdict.SAVED ->
            slideOutVertically(tween(durationMillis = 320, easing = EaseIn)) { fullHeight -> -fullHeight } +
                fadeOut(tween(durationMillis = 220)) +
                scaleOut(tween(durationMillis = 320), targetScale = 1.06f)

        RecommendationVerdict.DISLIKED ->
            slideOutHorizontally(tween(durationMillis = 320, easing = EaseIn)) { fullWidth -> -fullWidth } +
                fadeOut(tween(durationMillis = 220)) +
                scaleOut(tween(durationMillis = 320), targetScale = 0.90f)

        RecommendationVerdict.SKIPPED ->
            slideOutHorizontally(tween(durationMillis = 320, easing = EaseIn)) { fullWidth -> fullWidth } +
                fadeOut(tween(durationMillis = 220)) +
                scaleOut(tween(durationMillis = 320), targetScale = 0.90f)

        null -> fadeOut(tween(durationMillis = 220))
    }

    return enter togetherWith exit
}

/**
 * The personalization hint under the card ("Советуем: вы часто лайкаете острое") —
 * a soft container that explains *why* this dish surfaced.
 */
@Composable
private fun RecommendationTip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "✨", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shown once the user has reviewed every recommendation in the deck. */
@Composable
private fun EmptyDeck(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Пока всё!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Вы просмотрели все рекомендации. Загляните позже за новыми блюдами.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
