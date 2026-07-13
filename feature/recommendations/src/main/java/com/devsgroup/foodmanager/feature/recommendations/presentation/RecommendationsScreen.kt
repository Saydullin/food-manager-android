package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun RecommendationsScreen(
    onFoodClick: (foodId: String) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel(),
) {

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {

        }

        // The footer molecule; events flow up to the ViewModel (unidirectional data flow).
        RecommendationActionsBar(
            onLater = viewModel::onLater,
            onReject = viewModel::onReject,
            onAddToCart = viewModel::onAddToCart,
        )
    }

}
