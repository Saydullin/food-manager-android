package com.devsgroup.foodmanager.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopAppBar
import com.devsgroup.foodmanager.navigation.FoodNavHost
import com.devsgroup.foodmanager.navigation.TopLevelDestination

@Composable
fun FoodApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The active bottom-nav destination, or null on child screens (details, onboarding).
    // Its presence drives the shared header title AND whether the navigation bar shows.
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
            modifier = Modifier.padding(innerPadding),
        )
    }
}
