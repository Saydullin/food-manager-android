package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextButton
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.Recipe

/**
 * "Мои рецепты" — the user's own recipes. Creating one isn't built yet, so
 * [onCreateClick] is a stub action (shows a "coming soon" toast) rather than real
 * navigation, matching the design's stub button.
 */
@Composable
internal fun MyRecipesSection(
    recipes: List<Recipe>,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Мои рецепты",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            FoodTextButton(
                text = "Создать",
                onClick = onCreateClick,
                compact = true,
            )
        }

        if (recipes.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 13.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                recipes.forEach { recipe -> RecipeRow(recipe) }
            }
        }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, modifier: Modifier = Modifier) {
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
            PhotoTile(imageUrl = recipe.imageUrl, modifier = Modifier.size(56.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recipe.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = recipe.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = "СВОЙ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** A recipe/dish photo tile, or a neutral placeholder while there's no image yet. */
@Composable
internal fun PhotoTile(imageUrl: String?, modifier: Modifier = Modifier) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(MaterialTheme.shapes.small),
        )
    } else {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🍽️", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview
@Composable
private fun MyRecipesSectionPreview() {
    FoodManagerTheme {
        Surface {
            MyRecipesSection(
                recipes = listOf(Recipe(id = "1", name = "Дедушкин борщ", meta = "45 мин · 6 порций")),
                onCreateClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
