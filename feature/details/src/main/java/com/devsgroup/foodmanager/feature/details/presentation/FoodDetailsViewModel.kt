package com.devsgroup.foodmanager.feature.details.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FoodDetailsUiState(val foodId: String = "")

@HiltViewModel
class FoodDetailsViewModel @Inject constructor(
    // TODO: read the foodId from SavedStateHandle.toRoute<FoodDetailsRoute>()
    //       and stream FoodRepository.food(id).
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodDetailsUiState())
    val uiState: StateFlow<FoodDetailsUiState> = _uiState.asStateFlow()
}
