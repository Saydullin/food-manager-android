package com.devsgroup.foodmanager.feature.recommendations.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.usecase.GetRecommendedFoodsUseCase
import com.devsgroup.foodmanager.core.domain.usecase.LoadNextFeedPageUseCase
import com.devsgroup.foodmanager.core.domain.usecase.RecordSwipeUseCase
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.SwipeAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One dish in the recommendations deck, shaped for the [RecommendationCard] overlay.
 *
 * This is a *presentation* model, not the domain [Food]: it carries exactly the
 * display-ready strings the card paints (already-formatted badges like "480 ккал",
 * human categories, etc.). [cuisineCode] is the one exception — carried un-displayed
 * so a "wrong cuisine" dislike can report the dish's own cuisine back to the server.
 */
data class RecommendationUiModel(
    val id: String,
    val imageUrl: String,
    val title: String,
    val description: String,
    /** Nutrition chips over the top-left corner, e.g. "2 порц.", "480 ккал", "40 г угл.". */
    val nutritionBadges: List<String> = emptyList(),
    /** Diet chips on their own row under the nutrition badges, e.g. "Веган". */
    val dietBadges: List<String> = emptyList(),
    /** Allergen chips on their own row under the diet badges, e.g. "Орехи". */
    val allergyBadges: List<String> = emptyList(),
    /** The eyebrow above the title, e.g. ["Острое", "Тайская"]. */
    val categories: List<String> = emptyList(),
    /** Optional personalization hint shown under the card ("Советуем: …"). */
    val tip: String? = null,
    /** The raw server cuisine code (e.g. `"THAI"`), for the "wrong cuisine" dislike reason. */
    val cuisineCode: String? = null,
    /** Every raw tag/allergen/diet code this dish carries, for building per-dish dislike reasons. */
    val tagCodes: List<String> = emptyList(),
)

/**
 * The verdict that advanced the deck to the current card. Drives the *direction* of
 * the card transition so the animation reinforces what the tap meant: a save lifts
 * the old card up, a dislike flicks it left, a skip flicks it right.
 */
enum class RecommendationVerdict { SAVED, SKIPPED, DISLIKED }

data class RecommendationsUiState(
    val screenTitle: String = "For You",
    /** The cached feed, newest-first — shrinks as each swipe is confirmed by the server. */
    val cards: List<RecommendationUiModel> = emptyList(),
    /** Whether another page can still be fetched once [cards] runs low. */
    val hasMore: Boolean = true,
    /** How the deck last advanced — `null` before the first action. */
    val lastVerdict: RecommendationVerdict? = null,
    /** Whether the "Не люблю" reason sheet is currently shown. */
    val showDislikeSheet: Boolean = false,
) {
    /** The card on top of the deck, or `null` while empty (still loading, or truly exhausted). */
    val currentCard: RecommendationUiModel? get() = cards.firstOrNull()

    /** True once every dish has been swiped and the server has no more pages left. */
    val isExhausted: Boolean get() = cards.isEmpty() && !hasMore
}

/** Once the deck drops below this many cards, fetch another page to keep it stocked. */
private const val PrefetchThreshold = 5

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val getRecommendedFoodsUseCase: GetRecommendedFoodsUseCase,
    private val loadNextFeedPageUseCase: LoadNextFeedPageUseCase,
    private val recordSwipeUseCase: RecordSwipeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    /**
     * Foods swiped locally but not yet confirmed by the server. Filtered out of [cards]
     * immediately so the deck advances the instant the user taps — [recordSwipe] rolls
     * a food back out of this set (making it reappear) if the API call fails, since Room
     * only drops it for real once the server confirms.
     */
    private val pendingRemovals = MutableStateFlow<Set<String>>(emptySet())

    private var isLoadingPage = false

    init {
        viewModelScope.launch {
            combine(getRecommendedFoodsUseCase(), pendingRemovals) { foods, pending ->
                foods.filterNot { pending.contains(it.id) }.map { it.toUiModel() }
            }.collect { cards ->
                _uiState.update { it.copy(cards = cards) }
                maybeLoadMore(cards.size)
            }
        }
    }

    /** "Не люблю" — open the reason sheet so we can record a *precise* dislike. */
    fun onDislike() {
        _uiState.update { it.copy(showDislikeSheet = true) }
    }

    /** Reasons were chosen in the sheet: record one negative signal per reason, close it, advance. */
    fun onDislikeReasonsSelected(reasons: List<DislikeReason>) {
        val card = uiState.value.currentCard ?: return
        _uiState.update { it.copy(showDislikeSheet = false, lastVerdict = RecommendationVerdict.DISLIKED) }
        reasons.forEach { reason -> recordSwipe(card.id, SwipeAction.DISLIKE, reason.code, reason.detail) }
    }

    /** Sheet dismissed without a choice (scrim tap, swipe-down, or back). */
    fun onDislikeSheetDismiss() {
        _uiState.update { it.copy(showDislikeSheet = false) }
    }

    /** "Пропустить" — skip the current card without recording a preference, then advance. */
    fun onReject() {
        val card = uiState.value.currentCard ?: return
        _uiState.update { it.copy(lastVerdict = RecommendationVerdict.SKIPPED) }
        recordSwipe(card.id, SwipeAction.SKIP)
    }

    /** "Сохранить" — save the current food to favorites, then advance. */
    fun onAddToCart() {
        val card = uiState.value.currentCard ?: return
        _uiState.update { it.copy(lastVerdict = RecommendationVerdict.SAVED) }
        recordSwipe(card.id, SwipeAction.LIKE)
    }

    /** "Попробовать еще" — the deck was exhausted; ask the server for a fresh page. */
    fun onRetry() {
        if (isLoadingPage) return
        isLoadingPage = true
        viewModelScope.launch {
            val hasMore = runCatching { loadNextFeedPageUseCase() }
                .onFailure { Log.e(TAG, "Failed to retry loading feed page", it) }
                .getOrDefault(true)
            _uiState.update { it.copy(hasMore = hasMore) }
            isLoadingPage = false
        }
    }

    /**
     * Optimistically advances the deck — [foodId] is hidden via [pendingRemovals] before
     * the network call even starts, so the next card shows up instantly instead of
     * waiting on the round trip. The repository only clears the dish from Room once the
     * server confirms the swipe, so if the call fails, [foodId] is un-hidden and the
     * card reappears at the top of the deck.
     */
    private fun recordSwipe(foodId: String, action: SwipeAction, reason: String? = null, reasonDetail: String? = null) {
        pendingRemovals.update { it + foodId }
        viewModelScope.launch {
            runCatching { recordSwipeUseCase(foodId, action, reason, reasonDetail) }
                .onFailure {
                    Log.e(TAG, "Failed to record swipe for $foodId", it)
                    pendingRemovals.update { pending -> pending - foodId }
                }
        }
    }

    /** Keeps the deck stocked by fetching another page once it runs low. */
    private fun maybeLoadMore(currentSize: Int) {
        val state = uiState.value
        if (isLoadingPage || !state.hasMore || currentSize >= PrefetchThreshold) return
        isLoadingPage = true
        viewModelScope.launch {
            val hasMore = runCatching { loadNextFeedPageUseCase() }
                .onFailure { Log.e(TAG, "Failed to load next feed page", it) }
                .getOrDefault(state.hasMore)
            _uiState.update { it.copy(hasMore = hasMore) }
            isLoadingPage = false
        }
    }

    private companion object {
        const val TAG = "RecommendationsViewModel"
    }
}

private fun Food.toUiModel(): RecommendationUiModel = RecommendationUiModel(
    id = id,
    imageUrl = images.firstOrNull().orEmpty(),
    title = name,
    description = description.orEmpty(),
    nutritionBadges = listOfNotNull(
        nutrition?.servings?.let { "$it порц." },
        nutrition?.calories?.let { "$it ккал" },
        nutrition?.protein?.let { "$it г белк." },
        nutrition?.fat?.let { "$it г жир." },
        nutrition?.carbs?.let { "$it г угл." },
    ),
    dietBadges = tags.diets.map(::dietLabel),
    allergyBadges = tags.allergens.map(::allergenLabel),
    categories = listOfNotNull(cuisine?.let(::cuisineLabel)),
    cuisineCode = cuisine,
    tagCodes = (tags.diets + tags.allergens + tags.dietaryRestrictions + tags.intolerances + tags.features)
        .distinct(),
)

/** Localizes a server cuisine code; unknown/new codes fall back to the raw code. */
private fun cuisineLabel(code: String): String = when (code) {
    "ITALIAN" -> "Итальянская"
    "JAPANESE" -> "Японская"
    "MEXICAN" -> "Мексиканская"
    "INDIAN" -> "Индийская"
    "CHINESE" -> "Китайская"
    "MEDITERRANEAN" -> "Средиземноморская"
    "AMERICAN" -> "Американская"
    "THAI" -> "Тайская"
    "FRENCH" -> "Французская"
    else -> code
}

/** Localizes a server diet code; unknown/new codes fall back to the raw code. */
private fun dietLabel(code: String): String = when (code) {
    "VEGETARIAN" -> "Вег."
    "VEGAN" -> "Веган"
    "GLUTEN_FREE" -> "Без глютена"
    "DAIRY_FREE" -> "Без молока"
    "HALAL" -> "Халяль"
    "KOSHER" -> "Кошер"
    "LOW_CARB" -> "Мало углеводов"
    else -> code
}

/** Localizes a server allergen code; unknown/new codes fall back to the raw code. */
private fun allergenLabel(code: String): String = when (code) {
    "NUTS" -> "Орехи"
    "PEANUTS" -> "Арахис"
    "MILK" -> "Молоко"
    "EGGS" -> "Яйца"
    "FISH" -> "Рыба"
    "SHELLFISH" -> "Моллюски"
    "SOY" -> "Соя"
    "GLUTEN" -> "Глютен"
    "SESAME" -> "Кунжут"
    else -> code
}
