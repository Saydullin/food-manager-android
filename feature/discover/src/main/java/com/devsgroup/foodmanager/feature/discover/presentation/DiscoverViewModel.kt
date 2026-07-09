package com.devsgroup.foodmanager.feature.discover.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DiscoverUiState(val screenTitle: String = "Discover")

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    // TODO: inject GetRecommendedFoodsUseCase + RecordSwipeUseCase for the swipe deck.
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
}
