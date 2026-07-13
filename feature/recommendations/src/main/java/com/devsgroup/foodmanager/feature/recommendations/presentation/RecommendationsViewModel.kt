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

    /** "Позже" — dismiss the current card without a verdict and advance the deck. */
    fun onLater() {
        // TODO: advance to the next recommendation without recording a preference.
    }

    /** "Не то" — record a dislike so future recommendations improve, then advance. */
    fun onReject() {
        // TODO: record a negative signal via the personalization use-case.
    }

    /** "В корзину" — add the current food to the cart. */
    fun onAddToCart() {
        // TODO: add the current food to the cart via the cart use-case.
    }
}
