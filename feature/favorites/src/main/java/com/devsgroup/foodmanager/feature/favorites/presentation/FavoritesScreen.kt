package com.devsgroup.foodmanager.feature.favorites.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodTags

@Composable
internal fun FavoritesScreen(
    onFoodClick: (foodId: String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesContent(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onFoodClick = { food -> onFoodClick(food.id) },
        onDelete = viewModel::onDelete,
    )
}

@Composable
private fun FavoritesContent(
    uiState: FavoritesUiState,
    onRefresh: () -> Unit,
    onFoodClick: (Food) -> Unit,
    onDelete: (Food) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        if (uiState.foods.isEmpty()) {
            EmptyFavorites()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                items(uiState.foods, key = { it.id }) { food ->
                    FavoriteFoodRow(
                        food = food,
                        onClick = { onFoodClick(food) },
                        onDelete = { onDelete(food) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavorites(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Пока пусто — лайкайте блюда в «Для вас», и они появятся здесь.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun FavoriteFoodRow(
    food: Food,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = food.images.firstOrNull(),
                contentDescription = food.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = food.cuisine?.let(::cuisineLabel) ?: "Разное",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            FoodIconButton(
                icon = painterResource(R.drawable.ic_close),
                contentDescription = "Убрать «${food.name}» из сохранённого",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
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
private fun FavoritesScreenPreview() {
    FoodManagerTheme {
        Surface {
            FavoritesContent(
                uiState = FavoritesUiState(
                    foods = listOf(
                        Food(
                            id = "1",
                            name = "Том Ям",
                            description = "",
                            cuisine = "THAI",
                            images = emptyList(),
                            nutrition = null,
                            tags = FoodTags(),
                            createdAt = "",
                            updatedAt = "",
                        ),
                    ),
                ),
                onRefresh = {},
                onFoodClick = {},
                onDelete = {},
            )
        }
    }
}
