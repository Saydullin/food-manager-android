package com.devsgroup.foodmanager.feature.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodChipTone
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTagChip
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopBarConfig
import com.devsgroup.foodmanager.core.designsystem.component.HtmlText
import com.devsgroup.foodmanager.core.designsystem.component.ReportFoodTopBar
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodTags
import com.devsgroup.foodmanager.core.model.Nutrition

@Composable
internal fun FoodDetailsScreen(
    foodId: String,
    onBack: () -> Unit,
    viewModel: FoodDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReportFoodTopBar(
        FoodTopBarConfig(
            title = uiState.food?.name.orEmpty(),
            navigationIcon = {
                FoodIconButton(
                    icon = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    onClick = onBack,
                )
            },
        ),
    )
    FoodDetailsContent(uiState = uiState)
}

@Composable
private fun FoodDetailsContent(
    uiState: FoodDetailsUiState,
    modifier: Modifier = Modifier,
) {
    val food = uiState.food
    when {
        uiState.isLoading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        food == null -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Блюдо не найдено",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> FoodDetails(food = food, modifier = modifier)
    }
}

@Composable
private fun FoodDetails(
    food: Food,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (food.images.isNotEmpty()) {
            AsyncImage(
                model = food.images.first(),
                contentDescription = food.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = food.name, style = MaterialTheme.typography.headlineSmall)

            val cuisine = food.cuisine
            val nutritionLine = food.nutrition?.toLine()
            if (cuisine != null || nutritionLine != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cuisine != null) {
                        FoodTagChip(label = cuisineLabel(cuisine), tone = FoodChipTone.Neutral)
                    }
                    if (nutritionLine != null) {
                        Text(
                            text = nutritionLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            val description = food.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val content = food.content
            if (!content.isNullOrBlank()) {
                Text(text = "Рецепт", style = MaterialTheme.typography.titleMedium)
                HtmlText(
                    html = content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** "480 ккал • 18 г белк. • 22 г жир. • 40 г угл.", same wording as the recommendations card. */
private fun Nutrition.toLine(): String? {
    val parts = listOfNotNull(
        calories?.let { "$it ккал" },
        protein?.let { "$it г белк." },
        fat?.let { "$it г жир." },
        carbs?.let { "$it г угл." },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  •  ")
}

/** Localizes a server cuisine code; unknown/new codes fall back to the raw code. */
private fun cuisineLabel(code: String): String = when (code) {
    "ITALIAN" -> "Итальянская"
    "JAPANESE" -> "Японская"
    "MEXICAN" -> "Мексиканская"
    "INDIAN" -> "Индийская"
    "CHINESE" -> "Китайская"
    "MEDITERRANEAN" -> "Средиземноморская"
    "AMERICAN" -> "Американская"
    "THAI" -> "Тайская"
    "FRENCH" -> "Французская"
    else -> code
}

@Preview
@Composable
private fun FoodDetailsScreenPreview() {
    FoodManagerTheme {
        FoodDetailsContent(
            uiState = FoodDetailsUiState(
                isLoading = false,
                food = Food(
                    id = "1",
                    name = "Том Ям с креветками",
                    description = "Острый тайский суп на кокосовом молоке с креветками и грибами.",
                    content = "<p>Разогрейте бульон, добавьте пасту том-ям и варите 5 минут.</p><p>Добавьте креветки и грибы, готовьте ещё 5 минут.</p>",
                    cuisine = "THAI",
                    images = emptyList(),
                    nutrition = Nutrition(calories = 480, servings = 2, protein = 18, fat = 22, carbs = 40),
                    tags = FoodTags(),
                    createdAt = "",
                    updatedAt = "",
                ),
            ),
        )
    }
}
