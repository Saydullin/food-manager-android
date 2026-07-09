package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.ui.PlaceholderScreen

@Composable
internal fun RecommendationsScreen(
    onFoodClick: (foodId: String) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlaceholderScreen(title = uiState.screenTitle)
}
