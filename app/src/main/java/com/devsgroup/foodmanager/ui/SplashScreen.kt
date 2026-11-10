package com.devsgroup.foodmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Shown while [AppViewModel.uiState] resolves to [AppUiState.Ready] — the persisted
 * session/settings are still being read, so the start destination isn't known yet.
 * [FoodApp] cross-fades from this into [FoodAppContent] once that resolves.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // TODO: replace with the splash animation.
        // Explicit color: outside a Surface, Text falls back to LocalContentColor (black),
        // which is unreadable on the dark background.
        Text(
            text = "Food Manager",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
