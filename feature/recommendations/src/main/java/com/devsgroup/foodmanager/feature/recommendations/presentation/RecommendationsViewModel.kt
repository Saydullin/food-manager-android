package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class RecommendationsUiState(val screenTitle: String = "For You")

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    // TODO: inject GetRecommendedFoodsUseCase for the personalized feed.
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()
}
