package com.devsgroup.foodmanager.feature.discover.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.discover.presentation.DiscoverScreen
import kotlinx.serialization.Serializable

@Serializable
data object DiscoverRoute

fun NavController.navigateToDiscover(navOptions: NavOptions? = null) =
    navigate(DiscoverRoute, navOptions)

fun NavGraphBuilder.discoverScreen(onFoodClick: (foodId: String) -> Unit) {
    composable<DiscoverRoute> {
        DiscoverScreen(onFoodClick = onFoodClick)
    }
}
