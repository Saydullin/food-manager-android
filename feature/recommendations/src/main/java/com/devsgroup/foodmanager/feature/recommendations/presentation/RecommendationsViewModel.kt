package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * One dish in the recommendations deck, shaped for the [RecommendationCard] overlay.
 *
 * This is a *presentation* model, not the domain [com.devsgroup.foodmanager.core.model.Food]:
 * it carries exactly the display-ready strings the card paints (already-formatted
 * badges like "480 ккал", human categories, etc.). When the real feed arrives, the
 * server DTO → domain `Food` → this model mapping lands in the data/domain layers and
 * the UI below does not change.
 */
data class RecommendationUiModel(
    val id: String,
    val imageUrl: String,
    val title: String,
    val description: String,
    /** Short chips over the top-left corner, e.g. "480 ккал", "Вег". */
    val badges: List<String> = emptyList(),
    /** The eyebrow above the title, e.g. ["Острое", "Тайская"]. */
    val categories: List<String> = emptyList(),
    /** Optional personalization hint shown under the card ("Советуем: …"). */
    val tip: String? = null,
)

/**
 * The verdict that advanced the deck to the current card. Drives the *direction* of
 * the card transition so the animation reinforces what the tap meant: a save lifts
 * the old card up, a dislike flicks it left, a skip flicks it right.
 */
enum class RecommendationVerdict { SAVED, SKIPPED, DISLIKED }

data class RecommendationsUiState(
    val screenTitle: String = "For You",
    /** The recommendation deck, ordered by match score. */
    val cards: List<RecommendationUiModel> = emptyList(),
    /** Index of the card currently on top of the deck. */
    val currentIndex: Int = 0,
    /** How the deck last advanced — `null` before the first action. */
    val lastVerdict: RecommendationVerdict? = null,
    /** Whether the "Не люблю" reason sheet is currently shown. */
    val showDislikeSheet: Boolean = false,
) {
    /** The card on top of the deck, or `null` once every card has been reviewed. */
    val currentCard: RecommendationUiModel? get() = cards.getOrNull(currentIndex)
}

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    // TODO: inject GetRecommendedFoodsUseCase and map its FoodRecommendation stream
    //  into `cards` instead of the sample deck below.
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendationsUiState(cards = SampleDeck))
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    /** "Не люблю" — open the reason sheet so we can record a *precise* dislike. */
    fun onDislike() {
        _uiState.update { it.copy(showDislikeSheet = true) }
    }

    /** A reason was chosen in the sheet: record the negative signal, close it, advance. */
    fun onDislikeReasonSelected(reason: DislikeReason) {
        // TODO: record the dislike + reason via the personalization use-case.
        _uiState.update { it.copy(showDislikeSheet = false) }
        advance(RecommendationVerdict.DISLIKED)
    }

    /** Sheet dismissed without a choice (scrim tap, swipe-down, or back). */
    fun onDislikeSheetDismiss() {
        _uiState.update { it.copy(showDislikeSheet = false) }
    }

    /** "Пропустить" — skip the current card without recording a preference, then advance. */
    fun onReject() {
        // TODO: optionally record a "skip" signal before advancing.
        advance(RecommendationVerdict.SKIPPED)
    }

    /** "Сохранить" — save the current food to favorites, then advance. */
    fun onAddToCart() {
        // TODO: save the current food via the cart/favorites use-case.
        advance(RecommendationVerdict.SAVED)
    }

    /** Move to the next card in the deck, recording [verdict]. Clamped past the end. */
    private fun advance(verdict: RecommendationVerdict) {
        _uiState.update { state ->
            state.copy(
                currentIndex = (state.currentIndex + 1).coerceAtMost(state.cards.size),
                lastVerdict = verdict,
            )
        }
    }
}

/**
 * Stand-in deck used until the personalized feed is wired in. Photos are loaded from
 * the network by Coil, so the images match the categories/titles below.
 */
private val SampleDeck = listOf(
    RecommendationUiModel(
        id = "tom-yum",
        imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=900&q=80",
        title = "Том Ям с креветками",
        description = "Острый тайский суп на кокосовом молоке с креветками, грибами и лемонграссом.",
        badges = listOf("480 ккал", "Остро"),
        categories = listOf("Острое", "Тайская"),
        tip = "Советуем: вы часто лайкаете острое",
    ),
    RecommendationUiModel(
        id = "pepperoni",
        imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80",
        title = "Пицца Пепперони",
        description = "Тонкое тесто, томатный соус, моцарелла и острые колбаски пепперони.",
        badges = listOf("720 ккал"),
        categories = listOf("Классика", "Итальянская"),
    ),
    RecommendationUiModel(
        id = "ramen",
        imageUrl = "https://images.unsplash.com/photo-1557872943-16a5ac26437e?auto=format&fit=crop&w=900&q=80",
        title = "Рамен Тонкоцу",
        description = "Наваристый бульон, тонкая лапша, свинина чашу и маринованное яйцо.",
        badges = listOf("560 ккал"),
        categories = listOf("Сытное", "Японская"),
        tip = "Топ выбор на этой неделе",
    ),
    RecommendationUiModel(
        id = "cheeseburger",
        imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80",
        title = "Чизбургер Дабл",
        description = "Две говяжьи котлеты, чеддер, маринованные огурцы и фирменный соус.",
        badges = listOf("810 ккал"),
        categories = listOf("Стрит-фуд", "Американская"),
    ),
    RecommendationUiModel(
        id = "greek-salad",
        imageUrl = "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=80",
        title = "Греческий салат",
        description = "Свежие овощи, оливки и фета с оливковым маслом и орегано.",
        badges = listOf("320 ккал", "Вег"),
        categories = listOf("Лёгкое", "Средиземноморская"),
        tip = "Лёгкий вариант для обеда",
    ),
)
