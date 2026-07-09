package com.devsgroup.foodmanager.feature.details.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.devsgroup.foodmanager.feature.details.presentation.FoodDetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data class FoodDetailsRoute(val foodId: String)

fun NavController.navigateToFoodDetails(foodId: String, navOptions: NavOptions? = null) =
    navigate(FoodDetailsRoute(foodId), navOptions)

fun NavGraphBuilder.foodDetailsScreen(onBack: () -> Unit) {
    composable<FoodDetailsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<FoodDetailsRoute>()
        FoodDetailsScreen(foodId = route.foodId, onBack = onBack)
    }
}
