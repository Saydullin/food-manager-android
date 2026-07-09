package com.devsgroup.foodmanager.feature.discover.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.ui.PlaceholderScreen

@Composable
internal fun DiscoverScreen(
    onFoodClick: (foodId: String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // TODO: render the swipe deck; onFoodClick opens the details screen.
    PlaceholderScreen(title = uiState.screenTitle)
}
