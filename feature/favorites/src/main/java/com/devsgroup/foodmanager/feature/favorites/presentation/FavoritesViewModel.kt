package com.devsgroup.foodmanager.feature.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.model.Food
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class FavoritesUiState(
    val screenTitle: String = "Сохранённое",
    val foods: List<Food> = emptyList(),
    val isRefreshing: Boolean = false,
)

/** Refresh-on-launch/pull-to-refresh mirrors [ProfileViewModel]'s pattern. */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    /**
     * Foods removed locally but not yet confirmed by the server — filtered out of
     * [uiState] immediately so the list updates the instant the user taps delete,
     * same optimistic pattern as [RecommendationsViewModel.pendingRemovals]. Rolled
     * back if the call fails, since Room only drops the "liked" flag for real once
     * the delete is durably queued.
     */
    private val pendingRemovals = MutableStateFlow<Set<String>>(emptySet())
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<FavoritesUiState> = combine(
        foodRepository.likedFoods(),
        pendingRemovals,
        isRefreshing,
    ) { foods, pending, refreshing ->
        FavoritesUiState(
            foods = foods.filterNot { pending.contains(it.id) },
            isRefreshing = refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState(),
    )

    init {
        refresh()
    }

    /** Pull-to-refresh entry point from the screen. */
    fun onRefresh() = refresh()

    private fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            // likedFoods() is a live Room flow with no separate remote fetch to trigger
            // (see :core:network's FoodApi — no dedicated "list favorites" endpoint),
            // so this only drives the pull-to-refresh spinner's minimum visible duration.
            delay(REFRESH_SPINNER_DURATION)
            isRefreshing.value = false
        }
    }

    /** Removes a food from the list — same tap target as the Profile screen's "✕". */
    fun onDelete(food: Food) {
        pendingRemovals.update { it + food.id }
        viewModelScope.launch {
            runCatching { foodRepository.unlikeFood(food.id) }
                .onFailure { pendingRemovals.update { pending -> pending - food.id } }
        }
    }

    private companion object {
        val REFRESH_SPINNER_DURATION = 400.milliseconds
    }
}
