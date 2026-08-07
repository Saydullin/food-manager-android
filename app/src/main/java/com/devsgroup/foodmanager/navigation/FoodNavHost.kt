package com.devsgroup.foodmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.devsgroup.foodmanager.feature.auth.navigation.RegistrationRoute
import com.devsgroup.foodmanager.feature.auth.navigation.authScreens
import com.devsgroup.foodmanager.feature.auth.navigation.navigateToEmailConfirmation
import com.devsgroup.foodmanager.feature.auth.navigation.navigateToLogin
import com.devsgroup.foodmanager.feature.auth.navigation.navigateToRegistration
import com.devsgroup.foodmanager.feature.details.navigation.foodDetailsScreen
import com.devsgroup.foodmanager.feature.details.navigation.navigateToFoodDetails
import com.devsgroup.foodmanager.feature.favorites.navigation.favoritesScreen
import com.devsgroup.foodmanager.feature.onboarding.navigation.OnboardingRoute
import com.devsgroup.foodmanager.feature.onboarding.navigation.onboardingScreen
import com.devsgroup.foodmanager.feature.profile.navigation.editProfileScreen
import com.devsgroup.foodmanager.feature.profile.navigation.groupDetailScreen
import com.devsgroup.foodmanager.feature.profile.navigation.navigateToEditProfile
import com.devsgroup.foodmanager.feature.profile.navigation.navigateToGroupDetail
import com.devsgroup.foodmanager.feature.profile.navigation.profileScreen
import com.devsgroup.foodmanager.feature.recommendations.navigation.RecommendationsRoute
import com.devsgroup.foodmanager.feature.recommendations.navigation.recommendationsScreen

/**
 * Assembles every feature's navigation graph. This is the only place that depends
 * on all features at once — features never depend on each other.
 *
 * [startDestination] is chosen by the app-level auth gate: signed-out users start in
 * the auth flow ([com.devsgroup.foodmanager.feature.auth.navigation.RegistrationRoute]),
 * signed-in users on [OnboardingRoute] or [RecommendationsRoute] depending on whether
 * they've answered the onboarding questions.
 *
 * [postAuthDestination] answers the same question at the moment the auth flow completes
 * — a lambda rather than a value because a just-registered account flips from
 * "unanswered" to "answered" while this graph stays composed.
 */
@Composable
fun FoodNavHost(
    navController: NavHostController,
    startDestination: Any,
    postAuthDestination: () -> Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        authScreens(
            onAuthenticated = {
                navController.navigate(postAuthDestination()) {
                    // Clear the auth flow so Back never returns to it.
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToLogin = {
                navController.navigateToLogin(navOptions { launchSingleTop = true })
            },
            onNavigateToRegistration = {
                navController.navigateToRegistration(navOptions { launchSingleTop = true })
            },
            onNavigateToEmailConfirmation = {
                navController.navigateToEmailConfirmation(navOptions { launchSingleTop = true })
            },
        )
        onboardingScreen(
            onFinished = {
                navController.navigate(RecommendationsRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                }
            },
        )
        recommendationsScreen(onFoodClick = { foodId -> navController.navigateToFoodDetails(foodId) })
        favoritesScreen(onFoodClick = { foodId -> navController.navigateToFoodDetails(foodId) })
        profileScreen(
            onNavigateToEditProfile = { navController.navigateToEditProfile() },
            onNavigateToGroupDetail = { groupId -> navController.navigateToGroupDetail(groupId) },
            onLoggedOut = {
                navController.navigate(RegistrationRoute) {
                    // Clear the whole back stack so Back never returns into the signed-out app.
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
        editProfileScreen(onBack = { navController.navigateUp() })
        groupDetailScreen(onBack = { navController.navigateUp() })
        foodDetailsScreen(onBack = { navController.navigateUp() })
    }
}
