package com.devsgroup.foodmanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopAppBar
import com.devsgroup.foodmanager.feature.auth.navigation.RegistrationRoute
import com.devsgroup.foodmanager.feature.recommendations.navigation.RecommendationsRoute
import com.devsgroup.foodmanager.navigation.FoodNavHost
import com.devsgroup.foodmanager.navigation.TopLevelDestination

@Composable
fun FoodApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        // Hold a neutral surface while the persisted session is read, so the start
        // destination is decided exactly once.
        AppUiState.Loading -> LoadingScreen()
        is AppUiState.Ready -> FoodAppContent(
            // Signed-out users land in the auth flow; signed-in users skip it.
            startDestination = if (state.isLoggedIn) RecommendationsRoute else RegistrationRoute,
        )
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Top-level screens share one header; child screens supply their own.
            if (currentTopLevelDestination != null) {
                FoodTopAppBar(title = currentTopLevelDestination.label)
            }
        },
        bottomBar = {
            if (currentTopLevelDestination != null) {
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
