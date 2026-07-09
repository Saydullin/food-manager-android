package com.devsgroup.foodmanager.feature.details.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.devsgroup.foodmanager.core.ui.PlaceholderScreen

@Composable
internal fun FoodDetailsScreen(
    foodId: String,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: FoodDetailsViewModel = hiltViewModel(),
) {
    // TODO: load and render the selected food; onBack pops the stack.
    PlaceholderScreen(title = "Food details: $foodId")
}
