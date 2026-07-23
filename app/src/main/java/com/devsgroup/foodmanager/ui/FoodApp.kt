package com.devsgroup.foodmanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopAppBar
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.feature.auth.navigation.RegistrationRoute
import com.devsgroup.foodmanager.feature.recommendations.navigation.RecommendationsRoute
import com.devsgroup.foodmanager.navigation.FoodNavHost
import com.devsgroup.foodmanager.navigation.TopLevelDestination

/**
 * Theming lives here (not [com.devsgroup.foodmanager.MainActivity]) because the dark/light
 * choice is a user preference read from [AppViewModel] — the profile screen's "Тёмная
 * тема" toggle needs to switch the whole app live, not just at process start.
 */
@Composable
fun FoodApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        // Hold a neutral surface while the persisted session is read, so the start
        // destination is decided exactly once. The theme preference isn't loaded yet
        // either, so this brief window follows the system theme.
        AppUiState.Loading -> FoodManagerTheme { LoadingScreen() }
        is AppUiState.Ready -> FoodManagerTheme(darkTheme = state.darkThemeEnabled) {
            FoodAppContent(
                // Signed-out users land in the auth flow; signed-in users skip it.
                startDestination = if (state.isLoggedIn) RecommendationsRoute else RegistrationRoute,
            )
        }
    }
}

@Composable
private fun FoodAppContent(startDestination: Any) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The active bottom-nav destination, or null on child screens (details, onboarding,
    // and the full-bleed auth screens). Its presence drives the shared header title AND
    // whether the navigation bar shows.
    val currentTopLevelDestination = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }

    // Retained across the push into a child screen (edit profile, group/food details): the
    // back-stack entry — and so currentTopLevelDestination — flips to null the instant
    // navigate() is called, well before the exit animation below finishes. Without this, the
    // header/nav bar would swap to blank content mid-fade instead of fading out what was
    // already showing.
    var lastTopLevelDestination by remember { mutableStateOf(currentTopLevelDestination) }
    if (currentTopLevelDestination != null) {
        lastTopLevelDestination = currentTopLevelDestination
    }
    val chromeExitSpec = tween<Float>(durationMillis = 400)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Scaffold falls back to reserving its own inset (status/navigation-bar height) for
        // content whenever topBar/bottomBar are empty — which is exactly the case on child
        // screens. Each child screen already handles that inset itself via its own Scaffold +
        // FoodTopAppBar, so leaving this at the default double-reserves the space, producing
        // the oversized gap at the top of e.g. EditProfileScreen.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Top-level screens share one header; child screens supply their own. Faded
            // (not just conditionally composed) so the header's height stays reserved for the
            // whole exit animation instead of collapsing to zero the instant the destination
            // changes — that instant collapse is what yanked the still-visible outgoing
            // screen's content upward.
            AnimatedVisibility(
                visible = currentTopLevelDestination != null,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = chromeExitSpec),
            ) {
                FoodTopAppBar(title = lastTopLevelDestination?.label.orEmpty())
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = currentTopLevelDestination != null,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = chromeExitSpec),
            ) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected =
                            currentDestination?.hasRoute(destination.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(destination.emoji) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        FoodNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
