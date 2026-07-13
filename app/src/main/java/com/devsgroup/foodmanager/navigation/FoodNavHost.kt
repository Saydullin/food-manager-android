package com.devsgroup.foodmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.devsgroup.foodmanager.feature.details.navigation.foodDetailsScreen
import com.devsgroup.foodmanager.feature.details.navigation.navigateToFoodDetails
import com.devsgroup.foodmanager.feature.favorites.navigation.favoritesScreen
import com.devsgroup.foodmanager.feature.onboarding.navigation.OnboardingRoute
import com.devsgroup.foodmanager.feature.onboarding.navigation.onboardingScreen
import com.devsgroup.foodmanager.feature.profile.navigation.profileScreen
import com.devsgroup.foodmanager.feature.recommendations.navigation.RecommendationsRoute
import com.devsgroup.foodmanager.feature.recommendations.navigation.recommendationsScreen

/**
 * Assembles every feature's navigation graph. This is the only place that depends
 * on all features at once — features never depend on each other.
 *
 * Starts on [RecommendationsRoute] for a runnable demo. In production, gate the
 * start destination on `TasteProfileRepository.isOnboardingComplete()` and route
 * new users through [OnboardingRoute] first.
 */
@Composable
fun FoodNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = RecommendationsRoute,
        modifier = modifier,
    ) {
        onboardingScreen(
            onFinished = {
                navController.navigate(RecommendationsRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                }
            },
        )
        recommendationsScreen(onFoodClick = { foodId -> navController.navigateToFoodDetails(foodId) })
        favoritesScreen(onFoodClick = { foodId -> navController.navigateToFoodDetails(foodId) })
        profileScreen()
        foodDetailsScreen(onBack = { navController.navigateUp() })
    }
}
