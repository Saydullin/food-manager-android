package com.devsgroup.foodmanager.feature.dinner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodModalBottomSheet
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField
import com.devsgroup.foodmanager.core.designsystem.component.FoodToggleChip
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.Cuisine
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.Sex
import com.devsgroup.foodmanager.core.ui.emoji
import com.devsgroup.foodmanager.core.ui.label

/**
 * Adds or edits one eater. Name, sex and age are always visible; everything else
 * (tastes, allergies, doctor's advice) sits behind "Ещё" — optional, per the notes.
 *
 * For the self eater the taste/allergy/diet sections are hidden: those live on the
 * account profile and are merged in from there, so editing them here would fork them.
 *
 * Saving is asynchronous (the self eater is saved to the server): the sheet stays open
 * with a spinner, closes itself once the draft reports [EaterDraft.saved], and shows
 * [EaterDraft.errorMessage] otherwise.
 */
@Composable
internal fun EaterEditorSheet(
    draft: EaterDraft,
    onChange: ((EaterDraft) -> EaterDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    FoodModalBottomSheet(onDismissRequest = onDismiss) { dismiss ->
        // Plays the sheet's own close animation rather than vanishing on the state change.
        LaunchedEffect(draft.saved) { if (draft.saved) dismiss() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = when {
                    draft.isSelf -> "О себе"
                    draft.isNew -> "Кто ещё ужинает?"
                    else -> draft.name
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (!draft.isSelf) {
                FoodTextField(
                    value = draft.name,
                    onValueChange = { name -> onChange { it.copy(name = name, nameError = false) } },
                    label = "ИМЯ",
                    placeholder = "например, Маша",
                    isError = draft.nameError,
                    errorMessage = if (draft.nameError) "Введите имя" else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FoodTextField(
                value = draft.ageText,
                onValueChange = { text ->
                    onChange { it.copy(ageText = text.filter(Char::isDigit).take(3), ageError = false) }
                },
                label = "ВОЗРАСТ",
                placeholder = "лет",
                isRequired = false,
                isError = draft.ageError,
                errorMessage = if (draft.ageError) "Возраст от 0 до 120" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )

            Section(title = "Пол") {
                Sex.entries.forEach { sex ->
                    FoodToggleChip(
                        label = sex.label,
                        selected = draft.sex == sex,
                        // Tapping the selected one clears it: the field is optional.
                        onClick = { onChange { it.copy(sex = if (it.sex == sex) null else sex) } },
                    )
                }
            }

            FoodTextButton(
                text = if (draft.showMore) "Скрыть подробности ▴" else "Ещё: вкусы, аллергии, здоровье ▾",
                onClick = { onChange { it.copy(showMore = !it.showMore) } },
                compact = true,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (draft.showMore) {
                if (draft.isSelf) {
                    Text(
                        text = "Любимые кухни, аллергии и диеты берутся из вашего профиля.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    TasteSections(draft = draft, onChange = onChange)
                }

                Section(title = "Рекомендации врача") {
                    MedicalRestriction.entries.forEach { restriction ->
                        FoodToggleChip(
                            label = restriction.label,
                            selected = restriction in draft.medical,
                            onClick = { onChange { it.copy(medical = it.medical.toggle(restriction)) } },
                        )
                    }
                }
            }

            draft.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            FoodButton(
                text = if (draft.isSaving) "Сохраняем…" else "Сохранить",
                onClick = onSave,
                enabled = !draft.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            if (!draft.isNew && !draft.isSelf) {
                FoodTextButton(
                    text = "Удалить",
                    onClick = {
                        onDelete()
                        dismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TasteSections(
    draft: EaterDraft,
    onChange: ((EaterDraft) -> EaterDraft) -> Unit,
) {
    Section(title = "Любит") {
        Cuisine.entries.forEach { cuisine ->
            FoodToggleChip(
                label = cuisine.label,
                leadingEmoji = cuisine.emoji,
                selected = cuisine.name in draft.likes,
                onClick = { onChange { it.copy(likes = it.likes.toggle(cuisine.name)) } },
            )
        }
    }
    Section(title = "Не ест") {
        FoodToggleChip(
            label = "Острое",
            leadingEmoji = "🌶️",
            selected = SPICY_CODE in draft.avoid,
            onClick = { onChange { it.copy(avoid = it.avoid.toggle(SPICY_CODE)) } },
        )
        Cuisine.entries.forEach { cuisine ->
            FoodToggleChip(
                label = cuisine.label,
                leadingEmoji = cuisine.emoji,
                selected = cuisine.name in draft.avoid,
                onClick = { onChange { it.copy(avoid = it.avoid.toggle(cuisine.name)) } },
            )
        }
    }
    Section(title = "Аллергии") {
        Allergen.entries.forEach { allergen ->
            FoodToggleChip(
                label = allergen.label,
                selected = allergen in draft.allergens,
                onClick = { onChange { it.copy(allergens = it.allergens.toggle(allergen)) } },
            )
        }
    }
    Section(title = "Диета") {
        DietaryTag.entries.forEach { diet ->
            FoodToggleChip(
                label = diet.label,
                leadingEmoji = diet.emoji,
                selected = diet in draft.diets,
                onClick = { onChange { it.copy(diets = it.diets.toggle(diet)) } },
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    hint: String? = null,
    chips: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { chips() },
    )
    hint?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
