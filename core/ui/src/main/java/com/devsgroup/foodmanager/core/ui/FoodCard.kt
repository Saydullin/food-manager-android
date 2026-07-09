package com.devsgroup.foodmanager.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.devsgroup.foodmanager.core.model.Food

/**
 * Domain-aware card used across features (the swipe deck, favorites, recommendations).
 * Lives in :core:ui because it depends on the [Food] model, unlike pure design-system
 * primitives.
 */
@Composable
fun FoodCard(
    food: Food,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            model = food.imageUrl,
            contentDescription = food.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = food.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = food.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
