package com.devsgroup.foodmanager.feature.details.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.feature.details.navigation.FoodDetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FoodDetailsUiState(
    val isLoading: Boolean = true,
    val food: Food? = null,
)

@HiltViewModel
class FoodDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    foodRepository: FoodRepository,
) : ViewModel() {

    private val foodId = savedStateHandle.toRoute<FoodDetailsRoute>().foodId

    val uiState: StateFlow<FoodDetailsUiState> = foodRepository.food(foodId)
        .map { food -> FoodDetailsUiState(isLoading = false, food = food) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodDetailsUiState(),
        )
}
