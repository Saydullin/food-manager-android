package com.devsgroup.foodmanager.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsgroup.foodmanager.core.ui.PlaceholderScreen

@Composable
internal fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // TODO: capture cuisine/dietary interests, then call onFinished().
    PlaceholderScreen(title = uiState.screenTitle)
}
