package com.devsgroup.foodmanager.feature.dinner.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodOutlinedButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTagChip
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField
import com.devsgroup.foodmanager.core.designsystem.component.FoodToggleChip
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopBarConfig
import com.devsgroup.foodmanager.core.designsystem.component.ReportFoodTopBar
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.Ingredient
import com.devsgroup.foodmanager.core.model.IngredientSource
import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.ui.label

/**
 * "Приготовить ужин": who's eating → kitchen equipment (first time only) → where the
 * ingredients come from → dishes that suit everyone. One destination with step state,
 * so the system back gesture walks back through the steps before leaving the flow.
 */
@Composable
internal fun CookDinnerScreen(
    onBack: () -> Unit,
    onFoodClick: (String) -> Unit,
    viewModel: CookDinnerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val handleBack = { if (!viewModel.back()) onBack() }

    BackHandler(onBack = handleBack)
    ReportFoodTopBar(
        FoodTopBarConfig(
            title = "Приготовить ужин",
            navigationIcon = {
                FoodIconButton(
                    icon = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    onClick = handleBack,
                )
            },
        ),
    )

    CookDinnerContent(
        uiState = uiState,
        onToggleEater = viewModel::toggleEater,
        onEditEater = viewModel::editEater,
        onAddEater = viewModel::addEater,
        onConfirmEaters = { viewModel.confirmEaters() },
        onToggleAppliance = viewModel::toggleAppliance,
        onConfirmAppliances = { viewModel.confirmAppliances() },
        onSkipAppliances = { viewModel.skipAppliances() },
        onChangeAppliances = viewModel::openAppliances,
        onChooseAny = viewModel::chooseAnyIngredients,
        onChooseAtHome = viewModel::chooseAtHome,
        onPantryQueryChange = viewModel::onPantryQueryChange,
        onAddToPantry = viewModel::addToPantry,
        onRemoveFromPantry = viewModel::removeFromPantry,
        onConfirmPantry = viewModel::confirmPantry,
        onRetrySuggestions = viewModel::retrySuggestions,
        onFoodClick = onFoodClick,
    )

    uiState.editor?.let { draft ->
        EaterEditorSheet(
            draft = draft,
            onChange = viewModel::updateEditor,
            onSave = viewModel::saveEditor,
            onDelete = viewModel::deleteEditedEater,
            onDismiss = viewModel::closeEditor,
        )
    }
}

@Composable
private fun CookDinnerContent(
    uiState: CookDinnerUiState,
    onToggleEater: (String) -> Unit,
    onEditEater: (String) -> Unit,
    onAddEater: () -> Unit,
    onConfirmEaters: () -> Unit,
    onToggleAppliance: (KitchenAppliance) -> Unit,
    onConfirmAppliances: () -> Unit,
    onSkipAppliances: () -> Unit,
    onChangeAppliances: () -> Unit,
    onChooseAny: () -> Unit,
    onChooseAtHome: () -> Unit,
    onPantryQueryChange: (String) -> Unit,
    onAddToPantry: (Ingredient) -> Unit,
    onRemoveFromPantry: (String) -> Unit,
    onConfirmPantry: () -> Unit,
    onRetrySuggestions: () -> Unit,
    onFoodClick: (String) -> Unit,
) {
    AnimatedContent(
        targetState = uiState.step,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "CookDinnerStep",
        modifier = Modifier.fillMaxSize(),
    ) { step ->
        when (step) {
            DinnerStep.EATERS -> EatersStep(
                eaters = uiState.eaters,
                selectedIds = uiState.selectedIds,
                onToggle = onToggleEater,
                onEdit = onEditEater,
                onAdd = onAddEater,
                onConfirm = onConfirmEaters,
            )

            DinnerStep.APPLIANCES -> AppliancesStep(
                selected = uiState.applianceDraft,
                onToggle = onToggleAppliance,
                onConfirm = onConfirmAppliances,
                onSkip = onSkipAppliances,
            )

            DinnerStep.SOURCE -> SourceStep(
                appliances = uiState.savedAppliances.orEmpty(),
                pantryCount = uiState.pantry.size,
                onChangeAppliances = onChangeAppliances,
                onChooseAny = onChooseAny,
                onChooseAtHome = onChooseAtHome,
            )

            DinnerStep.PANTRY -> PantryStep(
                pantry = uiState.pantry,
                query = uiState.pantryQuery,
                results = uiState.pantrySearchResults,
                isSearching = uiState.isSearchingPantry,
                searchError = uiState.pantrySearchError,
                onQueryChange = onPantryQueryChange,
                onAdd = onAddToPantry,
                onRemove = onRemoveFromPantry,
                onConfirm = onConfirmPantry,
            )

            DinnerStep.RESULTS -> ResultsStep(
                eaters = uiState.selectedEaters,
                fromPantry = uiState.source == IngredientSource.AT_HOME,
                suggestions = uiState.suggestions,
                isLoading = uiState.isLoadingSuggestions,
                error = uiState.suggestionsError,
                onRetry = onRetrySuggestions,
                onFoodClick = onFoodClick,
            )
        }
    }
}

// region Steps

/** Scrollable body with a heading on top and the step's actions pinned to the bottom. */
@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            content()
        }
        Spacer(Modifier.height(12.dp))
        actions()
    }
}

@Composable
private fun EatersStep(
    eaters: List<Eater>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
    onConfirm: () -> Unit,
) {
    StepScaffold(
        title = "Для кого готовим?",
        subtitle = "Отметьте всех, кто будет ужинать. Подберём блюдо, которое подойдёт каждому.",
        actions = {
            FoodButton(
                text = "Дальше",
                onClick = onConfirm,
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            eaters.forEach { eater ->
                EaterRow(
                    eater = eater,
                    selected = eater.id in selectedIds,
                    onToggle = { onToggle(eater.id) },
                    onEdit = { onEdit(eater.id) },
                )
            }
            FoodOutlinedButton(
                text = "+ Добавить",
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EaterRow(
    eater: Eater,
    selected: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (selected) "✓" else "○",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 14.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = eater.name, style = MaterialTheme.typography.titleMedium)
                eater.summary()?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FoodIconButton(
                icon = painterResource(R.drawable.ic_edit),
                contentDescription = "Изменить",
                onClick = onEdit,
            )
        }
    }
}

/** One meta line under an eater's name: age, then anything that restricts the menu. */
private fun Eater.summary(): String? {
    val parts = buildList {
        age?.let { add(ageLabel(it)) }
        if (allergens.isNotEmpty()) add("аллергия: " + allergens.joinToString { it.label.lowercase() })
        if (diets.isNotEmpty()) add(diets.joinToString { it.label.lowercase() })
        if (medical.isNotEmpty()) add(medical.joinToString { it.label.lowercase() })
    }
    return parts.joinToString(" · ").ifEmpty { if (isSelf) "Аллергии и диеты — из профиля" else null }
}

private fun ageLabel(age: Int): String {
    val word = when {
        age % 100 in 11..14 -> "лет"
        age % 10 == 1 -> "год"
        age % 10 in 2..4 -> "года"
        else -> "лет"
    }
    return "$age $word"
}

@Composable
private fun AppliancesStep(
    selected: Set<KitchenAppliance>,
    onToggle: (KitchenAppliance) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
) {
    StepScaffold(
        title = "Что есть на кухне?",
        subtitle = "Спросим один раз — потом это можно поменять.",
        actions = {
            FoodButton(text = "Дальше", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
            FoodTextButton(
                text = "Пропустить",
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            KitchenAppliance.entries.forEach { appliance ->
                FoodToggleChip(
                    label = appliance.label,
                    leadingEmoji = appliance.emoji,
                    selected = appliance in selected,
                    onClick = { onToggle(appliance) },
                )
            }
        }
    }
}

@Composable
private fun SourceStep(
    appliances: Set<KitchenAppliance>,
    pantryCount: Int,
    onChangeAppliances: () -> Unit,
    onChooseAny: () -> Unit,
    onChooseAtHome: () -> Unit,
) {
    StepScaffold(
        title = "Из чего готовим?",
        subtitle = "Только из того, что уже есть, или можно что-то докупить.",
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (appliances.isEmpty()) {
                        "Техника не указана"
                    } else {
                        "Техника: " + appliances.sortedBy { it.ordinal }.joinToString { it.label.lowercase() }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                FoodTextButton(text = "Изменить", onClick = onChangeAppliances, compact = true)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OptionCard(
                emoji = "🧺",
                title = "Из того, что есть дома",
                subtitle = if (pantryCount == 0) {
                    "Отметите продукты — подберём блюдо из них"
                } else {
                    "Дома отмечено продуктов: $pantryCount"
                },
                onClick = onChooseAtHome,
            )
            OptionCard(
                emoji = "🛒",
                title = "Любое",
                subtitle = "Недостающее докупим",
                onClick = onChooseAny,
            )
        }
    }
}

@Composable
private fun PantryStep(
    pantry: List<Ingredient>,
    query: String,
    results: List<Ingredient>,
    isSearching: Boolean,
    searchError: String?,
    onQueryChange: (String) -> Unit,
    onAdd: (Ingredient) -> Unit,
    onRemove: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val pantryIds = pantry.mapTo(HashSet()) { it.id }
    StepScaffold(
        title = "Что есть дома?",
        subtitle = "Отметьте продукты — подберём блюда, для которых почти ничего не надо докупать.",
        actions = {
            FoodButton(
                text = "Подобрать блюда",
                onClick = onConfirm,
                enabled = pantry.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        if (pantry.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                pantry.forEach { item ->
                    FoodTagChip(label = item.name, onRemove = { onRemove(item.id) })
                }
            }
        }
        FoodTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "ПОИСК ПРОДУКТОВ",
            placeholder = "например, курица",
            isRequired = false,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            isSearching -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            searchError != null -> HintText(searchError)

            results.none { it.id !in pantryIds } ->
                HintText("Ничего не нашли. Продукты появляются здесь из рецептов.")

            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                results.filterNot { it.id in pantryIds }.forEach { ingredient ->
                    FoodToggleChip(label = ingredient.name, selected = false, onClick = { onAdd(ingredient) })
                }
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun ResultsStep(
    eaters: List<Eater>,
    fromPantry: Boolean,
    suggestions: List<DinnerSuggestion>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onFoodClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Подойдёт всем",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "За столом: " + eaters.joinToString { it.name } +
                    ". Учитываем аллергии, диеты, рекомендации врача и технику на кухне.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
        items(suggestions, key = { it.food.id }) { suggestion ->
            SuggestionRow(suggestion = suggestion, onClick = { onFoodClick(suggestion.food.id) })
        }
        when {
            isLoading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> item {
                Column {
                    HintText(error)
                    FoodOutlinedButton(
                        text = "Повторить",
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            suggestions.isEmpty() -> item {
                HintText(
                    if (fromPantry) {
                        "Из того, что есть дома, ничего не получается. Отметьте ещё продукты или выберите «Любое»."
                    } else {
                        "Не нашли блюд, которые подходят всем. Попробуйте убрать кого-то из списка или ослабить ограничения."
                    },
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: DinnerSuggestion, onClick: () -> Unit) {
    val food = suggestion.food
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = food.images.firstOrNull(),
                contentDescription = food.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                food.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                val appliances = food.tags.appliances
                    .mapNotNull { code -> KitchenAppliance.entries.firstOrNull { it.name == code } }
                val meta = listOfNotNull(
                    food.nutrition?.calories?.let { "$it ккал" },
                    appliances.takeIf { it.isNotEmpty() }?.joinToString(" ") { it.emoji },
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (suggestion.missingIngredients.isNotEmpty()) {
                    Text(
                        text = "🛒 Докупить: " + suggestion.missingIngredients.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// endregion

@Preview
@Composable
private fun EatersStepPreview() {
    FoodManagerTheme {
        CookDinnerContent(
            uiState = CookDinnerUiState(
                eaters = listOf(
                    Eater(id = "self", name = "Я", isSelf = true),
                    Eater(id = "kid", name = "Маша", age = 6),
                ),
                selectedIds = setOf("self", "kid"),
            ),
            onToggleEater = {},
            onEditEater = {},
            onAddEater = {},
            onConfirmEaters = {},
            onToggleAppliance = {},
            onConfirmAppliances = {},
            onSkipAppliances = {},
            onChangeAppliances = {},
            onChooseAny = {},
            onChooseAtHome = {},
            onPantryQueryChange = {},
            onAddToPantry = {},
            onRemoveFromPantry = {},
            onConfirmPantry = {},
            onRetrySuggestions = {},
            onFoodClick = {},
        )
    }
}
