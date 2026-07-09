package com.devsgroup.foodmanager.feature.favorites.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FavoritesUiState(val screenTitle: String = "Favorites")

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    // TODO: inject FoodRepository.likedFoods() source.
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
}
