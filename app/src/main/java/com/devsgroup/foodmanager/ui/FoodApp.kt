package com.devsgroup.foodmanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.SharedFlow
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopAppBar
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopBarConfig
import com.devsgroup.foodmanager.core.designsystem.component.LocalFoodTopBarState
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.feature.auth.navigation.EmailConfirmationRoute
import com.devsgroup.foodmanager.feature.auth.navigation.LoginRoute
import com.devsgroup.foodmanager.feature.auth.navigation.RegistrationRoute
import com.devsgroup.foodmanager.feature.onboarding.navigation.OnboardingRoute
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
                // Signed-out users land in the auth flow; signed-in-but-unverified users
                // land on the email-confirmation gate; only a verified session skips both.
                startDestination = when {
                    !state.isLoggedIn -> RegistrationRoute
                    !state.emailVerified -> EmailConfirmationRoute
                    else -> RecommendationsRoute
                },
                forcedLogoutEvents = viewModel.forcedLogoutEvents,
            )
        }
    }
}

@Composable
private fun FoodAppContent(startDestination: Any, forcedLogoutEvents: SharedFlow<Unit>) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // A silent token refresh failed outright (refresh token itself rejected) — the stored
    // session is already gone by the time this fires, so just clear the back stack and
    // drop the user back into the auth flow, mirroring the profile screen's own logout.
    LaunchedEffect(navController) {
        forcedLogoutEvents.collect {
            navController.navigate(RegistrationRoute) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // The active bottom-nav destination, or null on child screens (details, group/edit
    // profile) and the full-bleed auth/onboarding screens. Its presence drives the default
    // header title AND whether the navigation bar shows.
    val currentTopLevelDestination = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }

    // Chromeless screens own their whole frame (no shared header, no bottom nav) — auth and
    // onboarding. Every other destination (top-level or a child push) shares the one header
    // below, so pushing/popping between them never toggles the bar's existence, only its
    // content — nothing about the shared layout size changes for those transitions.
    val isChromelessRoute = currentDestination?.let { destination ->
        destination.hasRoute(RegistrationRoute::class) ||
            destination.hasRoute(LoginRoute::class) ||
            destination.hasRoute(EmailConfirmationRoute::class) ||
            destination.hasRoute(OnboardingRoute::class)
    } == true

    // Child screens (edit profile, group detail) report their own title/back-icon here
    // instead of hosting a Scaffold of their own — see ReportFoodTopBar. Top-level screens
    // need no such wiring: their title is derived below straight from the destination.
    val reportedTopBar = remember { mutableStateOf<FoodTopBarConfig?>(null) }
    val topBarConfig = currentTopLevelDestination
        ?.let { FoodTopBarConfig(title = it.label) }
        ?: reportedTopBar.value

    // Retained across the push into a chromeless screen: currentDestination flips the instant
    // navigate() is called, well before the exit animation finishes, which would otherwise
    // blank the still-fading-out bar's title instead of fading out what was already showing.
    var lastTopBarConfig by remember { mutableStateOf(topBarConfig) }
    if (topBarConfig != null) {
        lastTopBarConfig = topBarConfig
    }

    CompositionLocalProvider(LocalFoodTopBarState provides reportedTopBar) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AnimatedVisibility(
                    visible = !isChromelessRoute,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    lastTopBarConfig?.let { config ->
                        FoodTopAppBar(
                            title = config.title,
                            navigationIcon = config.navigationIcon ?: {},
                            actions = config.actions,
                        )
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentTopLevelDestination != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
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
