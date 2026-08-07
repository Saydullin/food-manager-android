package com.devsgroup.foodmanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    // Cross-fades from the splash into the real app once the session/settings resolve,
    // instead of the two swapping in on the same frame. Keyed only on Loading-vs-Ready
    // (not the whole uiState): keying on every field meant a fresh login flipping
    // isLoggedIn true mid-session produced a *new* Ready target for Crossfade, which
    // recomposed FoodAppContent into a brand new instance — a fresh rememberNavController()
    // and a start destination recomputed from whatever emailVerified/isLoggedIn happened to
    // read at that exact moment (often still stale, e.g. right after DefaultAuthRepository.verify()
    // flips isLoggedIn before any profile refetch lands), discarding the in-flight
    // onAuthenticated() navigation and dropping the user on EmailConfirmationScreen instead.
    Crossfade(targetState = uiState is AppUiState.Loading, label = "AppState") { isLoading ->
        if (isLoading) {
            // Hold the splash while the persisted session is read, so the start
            // destination is decided exactly once. The theme preference isn't loaded yet
            // either, so this brief window follows the system theme.
            FoodManagerTheme { SplashScreen() }
        } else {
            val state = uiState as AppUiState.Ready
            // Computed once, the first time this branch composes — later isLoggedIn/
            // emailVerified changes (a fresh login, a forced logout) must NOT recompute
            // this, or FoodAppContent's NavController gets torn down mid-navigation (see
            // above). Post-launch auth transitions are instead surfaced as explicit events
            // (e.g. forcedLogoutEvents) that navigate through the existing NavController.
            val startDestination = remember {
                // Signed-out users land in the auth flow; signed-in-but-unverified users
                // land on the email-confirmation gate; a verified session that hasn't
                // answered the onboarding questions gets asked them before the feed, since
                // the feed's ranking has nothing to work with until it does.
                when {
                    !state.isLoggedIn -> RegistrationRoute
                    !state.emailVerified -> EmailConfirmationRoute
                    !state.onboardingComplete -> OnboardingRoute
                    else -> RecommendationsRoute
                }
            }
            FoodManagerTheme(darkTheme = state.darkThemeEnabled) {
                FoodAppContent(
                    startDestination = startDestination,
                    // Read at hand-off time, not captured now: this composes before the
                    // user has even registered, so the value that matters is whatever the
                    // flag says once the auth flow actually completes.
                    postAuthDestination = {
                        val current = viewModel.uiState.value
                        if (current is AppUiState.Ready && !current.onboardingComplete) {
                            OnboardingRoute
                        } else {
                            RecommendationsRoute
                        }
                    },
                    forcedLogoutEvents = viewModel.forcedLogoutEvents,
                )
            }
        }
    }
}

@Composable
private fun FoodAppContent(
    startDestination: Any,
    postAuthDestination: () -> Any,
    forcedLogoutEvents: SharedFlow<Unit>,
) {
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
    //
    // currentDestination is null for one composition (NavHost hasn't attached its graph to
    // navController yet, since it's composed later, inside Scaffold's content slot) — falling
    // back to matching startDestination itself means that first frame already reflects the
    // real destination instead of a "nothing selected" guess, so the top/bottom bars never
    // have to snap into place a frame later.
    val currentTopLevelDestination = TopLevelDestination.entries.firstOrNull { destination ->
        if (currentDestination != null) {
            currentDestination.hasRoute(destination.route::class)
        } else {
            startDestination == destination.route
        }
    }

    // Chromeless screens own their whole frame (no shared header, no bottom nav) — auth and
    // onboarding. Every other destination (top-level or a child push) shares the one header
    // below, so pushing/popping between them never toggles the bar's existence, only its
    // content — nothing about the shared layout size changes for those transitions.
    val isChromelessRoute = if (currentDestination != null) {
        currentDestination.hasRoute(RegistrationRoute::class) ||
            currentDestination.hasRoute(LoginRoute::class) ||
            currentDestination.hasRoute(EmailConfirmationRoute::class) ||
            currentDestination.hasRoute(OnboardingRoute::class)
    } else {
        startDestination == RegistrationRoute ||
            startDestination == LoginRoute ||
            startDestination == EmailConfirmationRoute ||
            startDestination == OnboardingRoute
    }

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
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
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
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
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
                postAuthDestination = postAuthDestination,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
