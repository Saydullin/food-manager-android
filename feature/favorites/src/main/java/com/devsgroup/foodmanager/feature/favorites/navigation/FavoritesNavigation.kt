package com.devsgroup.foodmanager.feature.favorites.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.favorites.presentation.FavoritesScreen
import kotlinx.serialization.Serializable

@Serializable
data object FavoritesRoute

fun NavController.navigateToFavorites(navOptions: NavOptions? = null) =
    navigate(FavoritesRoute, navOptions)

fun NavGraphBuilder.favoritesScreen(onFoodClick: (foodId: String) -> Unit) {
    composable<FavoritesRoute> {
        FavoritesScreen(onFoodClick = onFoodClick)
    }
}
