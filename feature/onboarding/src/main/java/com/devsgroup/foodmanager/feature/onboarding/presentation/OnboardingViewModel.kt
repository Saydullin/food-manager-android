package com.devsgroup.foodmanager.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.Cuisine
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.ProfileException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The onboarding questions, in the order they're asked. */
enum class OnboardingStep {
    /** Favourite cuisines — the strongest positive signal the ranking has. */
    CUISINES,

    /** Diets, which the feed enforces as a hard filter. */
    DIETS,

    /** Allergies, likewise enforced — and the reason this flow exists at all. */
    ALLERGIES,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CUISINES,
    val cuisines: Set<Cuisine> = emptySet(),
    val diets: Set<DietaryTag> = emptySet(),
    val allergens: Set<Allergen> = emptySet(),
    /** True while the answers are being saved on the final step. */
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val isFirstStep: Boolean get() = step == OnboardingStep.CUISINES
    val isLastStep: Boolean get() = step == OnboardingStep.ALLERGIES

    /**
     * Every step is skippable. "No allergies" and "no particular cuisine" are real
     * answers, and gating the flow on a selection would push users into declaring
     * something untrue just to get past it — which is worse than no data, because the
     * feed acts on it.
     */
    val stepNumber: Int get() = step.ordinal + 1
    val totalSteps: Int get() = OnboardingStep.entries.size
}

/**
 * Collects the taste profile the feed's ranking needs, then writes it through
 * [ProfileRepository] — the same three full-replace endpoints the profile screen edits,
 * so onboarding needs no repository of its own and inherits that path's local-first,
 * offline-queued behaviour for free.
 *
 * Answers are held locally and saved once, on finish, rather than per step: three
 * separate list endpoints means saving as you go would leave a user who quits mid-flow
 * with a partly-answered profile that still counts as "not onboarded", and they'd be
 * asked everything again on next launch with their earlier answers already applied.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _finishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emitted once the answers are saved, so the screen can leave the flow. */
    val finishedEvents: SharedFlow<Unit> = _finishedEvents.asSharedFlow()

    /**
     * Entries already on the account that no picker can represent — free text the user
     * typed on the profile screen ("что-нибудь острое"), or a catalog code newer than this
     * app build. Held aside at prefill and merged back in [save], because the three
     * endpoints are whole-list replacements: writing only the picker's selection would
     * delete these.
     */
    private var unmappedPreferences: List<String> = emptyList()
    private var unmappedExceptions: List<String> = emptyList()
    private var unmappedDiets: List<String> = emptyList()

    init {
        prefillFromExistingProfile()
    }

    /**
     * Seeds the pickers from whatever the account already has.
     *
     * Matters for the returning user: login hands off the moment the session verifies,
     * before any profile has been fetched, so a fresh install of an existing account can
     * reach this screen with the completion flag still unset. Prefilling turns that from
     * a wrong-feeling re-interrogation into a confirmation of what they already chose —
     * and stops "Готово" from wiping server-side answers with empty lists.
     *
     * Only fills sets that are still empty, so it can never undo a selection the user has
     * already made. Reads the cached/persisted profile only — never blocks on the network,
     * and skips entirely when there's nothing cached (`profile()` never emits then).
     */
    private fun prefillFromExistingProfile() = viewModelScope.launch {
        if (!profileRepository.hasCachedProfile()) return@launch
        val profile = profileRepository.profile().first()

        val (cuisines, leftoverPreferences) = profile.foodPreferences.partitionByEnum<Cuisine>()
        val (allergens, leftoverExceptions) = profile.foodExceptions.partitionByEnum<Allergen>()
        val (diets, leftoverDiets) = profile.diets.partitionByEnum<DietaryTag>()
        unmappedPreferences = leftoverPreferences
        unmappedExceptions = leftoverExceptions
        unmappedDiets = leftoverDiets

        _uiState.update { state ->
            state.copy(
                cuisines = state.cuisines.ifEmpty { cuisines },
                allergens = state.allergens.ifEmpty { allergens },
                diets = state.diets.ifEmpty { diets },
            )
        }
    }

    /**
     * Splits stored taste strings into the ones this app's pickers understand (as enums)
     * and the ones they don't (kept verbatim). Matching is case-insensitive on the enum
     * name, since the name *is* the server's code.
     */
    private inline fun <reified T : Enum<T>> List<String>.partitionByEnum(): Pair<Set<T>, List<String>> {
        val matched = mutableSetOf<T>()
        val leftover = mutableListOf<String>()
        for (value in this) {
            val entry = enumValues<T>().firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
            if (entry != null) matched += entry else leftover += value
        }
        return matched to leftover
    }

    fun toggleCuisine(cuisine: Cuisine) = _uiState.update {
        it.copy(cuisines = it.cuisines.toggle(cuisine), errorMessage = null)
    }

    fun toggleDiet(diet: DietaryTag) = _uiState.update {
        it.copy(diets = it.diets.toggle(diet), errorMessage = null)
    }

    fun toggleAllergen(allergen: Allergen) = _uiState.update {
        it.copy(allergens = it.allergens.toggle(allergen), errorMessage = null)
    }

    fun back() = _uiState.update { state ->
        val previous = OnboardingStep.entries.getOrNull(state.step.ordinal - 1) ?: return@update state
        state.copy(step = previous, errorMessage = null)
    }

    /** Advances a step, or saves and finishes on the last one. */
    fun next() {
        val state = _uiState.value
        if (state.isSaving) return
        val following = OnboardingStep.entries.getOrNull(state.step.ordinal + 1)
        if (following != null) {
            _uiState.update { it.copy(step = following, errorMessage = null) }
        } else {
            save()
        }
    }

    /**
     * Leaves onboarding without answering anything, but still marks it done — a user who
     * declined once shouldn't be asked again on every launch. They can fill the same
     * fields in from the profile screen whenever they want.
     */
    fun skipAll() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            profileRepository.completeOnboarding()
            _finishedEvents.emit(Unit)
        }
    }

    private fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // Codes, not display labels: the feed matches preferences against cuisine
                // codes and exceptions against allergen codes, so a localized string here
                // would store fine and then quietly never match anything.
                //
                // Each list is sent as "what the pickers say" plus the entries they
                // couldn't represent — these endpoints replace the whole list, so omitting
                // the leftovers would delete the user's own free-text tags.
                profileRepository.updateFoodPreferences(unmappedPreferences + state.cuisines.map { it.name })
                profileRepository.updateFoodExceptions(unmappedExceptions + state.allergens.map { it.name })
                profileRepository.updateDiets(unmappedDiets + state.diets.map { it.name })
                // Last, so a failure above leaves the flow un-answered and re-asked rather
                // than marking a profile complete that never got written.
                profileRepository.completeOnboarding()
                _finishedEvents.emit(Unit)
            } catch (e: ProfileException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Не удалось сохранить. Попробуйте ещё раз.",
                    )
                }
            }
        }
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
}
