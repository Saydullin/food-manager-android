package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodTags

/** "Понравившееся" — dishes liked from "Для вас", with a quick "✕" to unlike. */
@Composable
internal fun LikedDishesSection(
    likedFoods: List<Food>,
    onUnlike: (Food) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Понравившееся",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (likedFoods.isEmpty()) {
            Text(
                text = "Пока пусто — лайкайте блюда в «Для вас», и они появятся здесь.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 13.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 13.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                likedFoods.forEach { food -> LikedDishRow(food = food, onUnlike = { onUnlike(food) }) }
            }
        }
    }
}

@Composable
private fun LikedDishRow(food: Food, onUnlike: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoTile(imageUrl = food.images.firstOrNull().orEmpty(), modifier = Modifier.size(56.dp))
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
                contentDescription = "Убрать «${food.name}» из понравившегося",
                onClick = onUnlike,
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
private fun LikedDishesSectionPreview() {
    FoodManagerTheme {
        Surface {
            LikedDishesSection(
                likedFoods = listOf(
                    Food(
                        id = "1",
                        name = "Том Ям",
                        description = "",
                        content = null,
                        cuisine = "THAI",
                        images = emptyList(),
                        nutrition = null,
                        tags = FoodTags(),
                        createdAt = "",
                        updatedAt = "",
                    ),
                ),
                onUnlike = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
