package com.devsgroup.foodmanager.feature.recommendations.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.recommendations.presentation.RecommendationsScreen
import kotlinx.serialization.Serializable

@Serializable
data object RecommendationsRoute

fun NavController.navigateToRecommendations(navOptions: NavOptions? = null) =
    navigate(RecommendationsRoute, navOptions)

fun NavGraphBuilder.recommendationsScreen(onFoodClick: (foodId: String) -> Unit) {
    composable<RecommendationsRoute> {
        RecommendationsScreen(onFoodClick = onFoodClick)
    }
}
