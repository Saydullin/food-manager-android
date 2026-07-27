package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodModalBottomSheet
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField
import com.devsgroup.foodmanager.core.designsystem.component.FoodToggleChip

/**
 * A reason a user can give for disliking a recommended dish. The choice is a
 * personalization signal — it tells the recommender *why* the card missed, so the
 * next ones land better.
 *
 * Unlike a fixed enum, the list of reasons offered is built per-card from that dish's
 * own tags/cuisine (see [buildDislikeReasons]) — a "Не нравятся орехи" option only
 * makes sense to offer for a dish that's actually tagged with nuts. [isOther] marks
 * the one reason that isn't tag-derived: it always comes last and needs [detail]
 * filled in by the user before it can be submitted.
 *
 * It lives in `presentation` for now because the domain layer for recommendations
 * is still a placeholder. Promote it to the feature's `domain` package (or
 * `:core:domain`) once the "record dislike" use-case exists — the sheet and the
 * [RecommendationsViewModel] already speak in terms of this type, so the move is
 * mechanical.
 */
data class DislikeReason(
    /** The server's `reason` code, e.g. `"DISLIKE_TAG"`, `"WRONG_CUISINE"`, `"ALREADY_ATE"`, `"OTHER"`. */
    val code: String,
    /** The server's `reasonDetail` code (a tag/cuisine code) — `null` when the reason needs none. */
    val detail: String?,
    val label: String,
    val isOther: Boolean = false,
)

/** Builds the dish-specific dislike reasons for [card]: one per tag it carries, then cuisine, then "Другое" last. */
internal fun buildDislikeReasons(card: RecommendationUiModel): List<DislikeReason> {
    val tagReasons = card.tagCodes.map { code ->
        DislikeReason(code = "DISLIKE_TAG", detail = code, label = tagLabel(code).replaceFirstChar { it.uppercase() })
    }
    val cuisineReason = card.cuisineCode?.let { code ->
        DislikeReason(code = "WRONG_CUISINE", detail = code, label = "Не та кухня")
    }
    val other = DislikeReason(code = "OTHER", detail = null, label = "Другое", isOther = true)
    return tagReasons + listOfNotNull(cuisineReason) + other
}

/** Localizes a server tag code (diet/allergen/feature/intolerance); unknown codes fall back to the raw code. */
private fun tagLabel(code: String): String = when (code) {
    "VEGETARIAN" -> "вегетарианское"
    "VEGAN" -> "веганское"
    "GLUTEN_FREE" -> "без глютена"
    "DAIRY_FREE" -> "без молока"
    "HALAL" -> "халяль"
    "KOSHER" -> "кошер"
    "LOW_CARB" -> "мало углеводов"
    "SPICY" -> "острое"
    "NUTS" -> "орехи"
    "PEANUTS" -> "арахис"
    "MILK" -> "молоко"
    "EGGS" -> "яйца"
    "FISH" -> "рыба"
    "SHELLFISH" -> "моллюски"
    "SOY" -> "соя"
    "GLUTEN" -> "глютен"
    "SESAME" -> "кунжут"
    else -> code
}

/**
 * The "Не люблю" bottom sheet — a feature *molecule* that asks *why* the user
 * disliked the current card so the answer can sharpen future recommendations.
 * The reasons are a multi-choice grid — a dish can miss for more than one reason
 * at once (e.g. both "острое" and "не та кухня") — submitted together via
 * [onReasonsSelected] once the user taps "Отправить".
 *
 * It lives in the feature (like [RecommendationActionsBar]) because [reasons] are
 * specific to the recommendations flow, and it is built only from design-system
 * atoms ([FoodModalBottomSheet], [FoodToggleChip], [FoodTextField], [FoodButton]).
 * It stays UI-only: it emits the chosen reasons and a dismiss event and holds no
 * state itself beyond the in-progress selection — visibility and recording live
 * in [RecommendationsViewModel].
 */
@Composable
internal fun DislikeReasonSheet(
    reasons: List<DislikeReason>,
    onReasonsSelected: (List<DislikeReason>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var otherDetail by rememberSaveable { mutableStateOf<String?>(null) }

    val other = reasons.lastOrNull { it.isOther }
    val gridReasons = reasons.filterNot { it.isOther }
    val hasSelection = selected.isNotEmpty() || (other != null && !otherDetail.orEmpty().isBlank())

    FoodModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = "Почему не нравится?",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Выберите одну или несколько причин — рекомендации станут точнее.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Column(
            modifier = Modifier
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                items(gridReasons, key = { it.detail ?: it.code }) { reason ->
                    val key = reason.detail ?: reason.code
                    FoodToggleChip(
                        modifier = Modifier.fillMaxWidth(),
                        label = reason.label,
                        selected = key in selected,
                        onClick = {
                            selected = if (key in selected) selected - key else selected + key
                        },
                    )
                }
            }

            if (other != null) {
                FoodTextField(
                    value = otherDetail.orEmpty(),
                    onValueChange = { otherDetail = it },
                    label = other.label,
                    placeholder = "Опишите причину (необязательно)",
                )
            }

            FoodButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Отправить",
                enabled = hasSelection,
                onClick = {
                    val chosen = gridReasons.filter { (it.detail ?: it.code) in selected } +
                        listOfNotNull(other?.takeIf { !otherDetail.orEmpty().isBlank() }?.copy(detail = otherDetail))
                    onReasonsSelected(chosen)
                },
            )
        }
    }
}
