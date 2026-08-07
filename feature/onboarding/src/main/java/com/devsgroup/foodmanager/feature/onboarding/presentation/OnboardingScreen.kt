package com.devsgroup.foodmanager.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodToggleChip
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.Cuisine
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.ui.emoji
import com.devsgroup.foodmanager.core.ui.label

/**
 * The onboarding questions: three chip pickers that seed the taste profile the feed
 * ranks and filters on. Renders full-bleed (no shared top bar or bottom nav), like the
 * auth screens — see `FoodApp`'s chromeless-route list.
 */
@Composable
internal fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigation is driven by an event rather than by observing `onboardingComplete`:
    // the flag is what the app-level gate reads at launch, and reacting to it here too
    // would race that gate on the frame the flag flips.
    LaunchedEffect(viewModel) {
        viewModel.finishedEvents.collect { onFinished() }
    }

    OnboardingContent(
        uiState = uiState,
        onToggleCuisine = viewModel::toggleCuisine,
        onToggleDiet = viewModel::toggleDiet,
        onToggleAllergen = viewModel::toggleAllergen,
        onBack = viewModel::back,
        onNext = viewModel::next,
        onSkipAll = viewModel::skipAll,
    )
}

@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onToggleCuisine: (Cuisine) -> Unit,
    onToggleDiet: (DietaryTag) -> Unit,
    onToggleAllergen: (Allergen) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkipAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        StepIndicator(current = uiState.stepNumber, total = uiState.totalSteps)

        Spacer(Modifier.height(28.dp))

        // Only the question body animates; the indicator and the actions stay put, so
        // stepping through doesn't shift the buttons under the user's thumb.
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "OnboardingStep",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                when (step) {
                    OnboardingStep.CUISINES -> StepBody(
                        title = "Что вам нравится?",
                        subtitle = "Выберите кухни, которые любите — с них и начнём подбор.",
                    ) {
                        Cuisine.entries.forEach { cuisine ->
                            FoodToggleChip(
                                label = cuisine.label,
                                leadingEmoji = cuisine.emoji,
                                selected = cuisine in uiState.cuisines,
                                onClick = { onToggleCuisine(cuisine) },
                            )
                        }
                    }

                    OnboardingStep.DIETS -> StepBody(
                        title = "Есть диета?",
                        subtitle = "Блюда, которые ей не соответствуют, мы вам показывать не будем.",
                    ) {
                        DietaryTag.entries.forEach { diet ->
                            FoodToggleChip(
                                label = diet.label,
                                leadingEmoji = diet.emoji,
                                selected = diet in uiState.diets,
                                onClick = { onToggleDiet(diet) },
                            )
                        }
                    }

                    OnboardingStep.ALLERGIES -> StepBody(
                        title = "Аллергии?",
                        subtitle = "Отметьте всё, что вам нельзя — такие блюда исчезнут из подборки.",
                    ) {
                        Allergen.entries.forEach { allergen ->
                            FoodToggleChip(
                                label = allergen.label,
                                selected = allergen in uiState.allergens,
                                onClick = { onToggleAllergen(allergen) },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        FoodButton(
            // Every step is skippable, so the primary action reads as "move on" rather
            // than "confirm" when nothing is selected on this step.
            text = when {
                uiState.isSaving -> "Сохраняем…"
                uiState.isLastStep -> "Готово"
                else -> "Далее"
            },
            onClick = onNext,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.isFirstStep) {
                FoodTextButton(
                    text = "Пропустить всё",
                    onClick = onSkipAll,
                    enabled = !uiState.isSaving,
                    compact = true,
                )
            } else {
                FoodTextButton(
                    text = "Назад",
                    onClick = onBack,
                    enabled = !uiState.isSaving,
                    compact = true,
                )
            }
            Text(
                text = "Всё это можно менять в профиле",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Title + supporting line + the step's chip grid. */
@Composable
private fun StepBody(
    title: String,
    subtitle: String,
    chips: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    FlowRow(
        modifier = Modifier.padding(top = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = { chips() },
    )
}

/** A row of bars, one per step, filled up to the current one. */
@Composable
private fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                shape = CircleShape,
                color = if (index < current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {}
        }
    }
}

@Preview
@Composable
private fun OnboardingCuisinesPreview() {
    FoodManagerTheme {
        OnboardingContent(
            uiState = OnboardingUiState(cuisines = setOf(Cuisine.ITALIAN, Cuisine.KOREAN)),
            onToggleCuisine = {},
            onToggleDiet = {},
            onToggleAllergen = {},
            onBack = {},
            onNext = {},
            onSkipAll = {},
        )
    }
}

@Preview
@Composable
private fun OnboardingAllergiesPreview() {
    FoodManagerTheme {
        OnboardingContent(
            uiState = OnboardingUiState(
                step = OnboardingStep.ALLERGIES,
                allergens = setOf(Allergen.PEANUTS),
            ),
            onToggleCuisine = {},
            onToggleDiet = {},
            onToggleAllergen = {},
            onBack = {},
            onNext = {},
            onSkipAll = {},
        )
    }
}
