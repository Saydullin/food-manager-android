package com.devsgroup.foodmanager.feature.dinner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsgroup.foodmanager.core.domain.repository.HouseholdRepository
import com.devsgroup.foodmanager.core.domain.repository.KitchenRepository
import com.devsgroup.foodmanager.core.domain.repository.PantryRepository
import com.devsgroup.foodmanager.core.domain.usecase.SuggestDinnerUseCase
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.Ingredient
import com.devsgroup.foodmanager.core.model.IngredientSource
import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.model.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * The wizard's steps. [APPLIANCES] is only shown until the user has answered it once;
 * [PANTRY] only on the "из того, что есть дома" path.
 */
enum class DinnerStep { EATERS, APPLIANCES, SOURCE, PANTRY, RESULTS }

/** The in-progress edit of one eater, shown in the editor sheet. */
data class EaterDraft(
    /** `null` for a new eater. */
    val id: String? = null,
    val isSelf: Boolean = false,
    val name: String = "",
    val sex: Sex? = null,
    val ageText: String = "",
    val likes: Set<String> = emptySet(),
    val avoid: Set<String> = emptySet(),
    val allergens: Set<Allergen> = emptySet(),
    val diets: Set<DietaryTag> = emptySet(),
    val medical: Set<MedicalRestriction> = emptySet(),
    /** Whether the optional "ещё" section is expanded. */
    val showMore: Boolean = false,
    val nameError: Boolean = false,
    val ageError: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Set once the save went through, so the sheet plays its close animation. */
    val saved: Boolean = false,
) {
    val isNew: Boolean get() = id == null
}

data class CookDinnerUiState(
    /** Steps visited in this session, so "Назад" retraces the actual path. Never empty. */
    val backStack: List<DinnerStep> = listOf(DinnerStep.EATERS),
    val eaters: List<Eater> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    /** The saved answer — `null` until the equipment step has been answered or skipped once. */
    val savedAppliances: Set<KitchenAppliance>? = null,
    /** The equipment step's working selection. */
    val applianceDraft: Set<KitchenAppliance> = emptySet(),
    val pantry: List<Ingredient> = emptyList(),
    val pantryQuery: String = "",
    val pantrySearchResults: List<Ingredient> = emptyList(),
    val isSearchingPantry: Boolean = false,
    val pantrySearchError: String? = null,
    val source: IngredientSource = IngredientSource.ANY,
    val suggestions: List<DinnerSuggestion> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val suggestionsError: String? = null,
    val editor: EaterDraft? = null,
) {
    val step: DinnerStep get() = backStack.last()
    val selectedEaters: List<Eater> get() = eaters.filter { it.id in selectedIds }
}

/**
 * Drives the whole "Приготовить ужин" flow from one destination, like onboarding does:
 * the steps share one request being assembled, and a single ViewModel is simpler than
 * scoping one to a nested nav graph.
 *
 * Household, kitchen and pantry are local-first (they work offline and sync in the
 * background); the suggestions themselves come from the server and need a connection.
 */
@HiltViewModel
class CookDinnerViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val kitchenRepository: KitchenRepository,
    private val pantryRepository: PantryRepository,
    private val suggestDinner: SuggestDinnerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookDinnerUiState())
    val uiState: StateFlow<CookDinnerUiState> = _uiState.asStateFlow()

    private var suggestionsJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            householdRepository.eaters().collect { eaters ->
                _uiState.update { state ->
                    val ids = eaters.mapTo(HashSet()) { it.id }
                    // Cooking for yourself is the default on first load; after that only
                    // drop selections of eaters that were deleted.
                    val selected = if (state.eaters.isEmpty()) setOf(Eater.SELF_ID) else state.selectedIds intersect ids
                    state.copy(eaters = eaters, selectedIds = selected)
                }
            }
        }
        viewModelScope.launch {
            kitchenRepository.appliances().collect { saved -> _uiState.update { it.copy(savedAppliances = saved) } }
        }
        viewModelScope.launch {
            pantryRepository.items().collect { items -> _uiState.update { it.copy(pantry = items) } }
        }
        // Best-effort pulls of what other devices may have changed; the local copies
        // already render, so a failure (offline) needs no UI.
        viewModelScope.launch { runCatching { householdRepository.refresh() } }
        viewModelScope.launch { runCatching { kitchenRepository.refresh() } }
        viewModelScope.launch { runCatching { pantryRepository.refresh() } }
    }

    // region Navigation between steps

    fun toggleEater(eaterId: String) = _uiState.update { state ->
        val selected = if (eaterId in state.selectedIds) state.selectedIds - eaterId else state.selectedIds + eaterId
        state.copy(selectedIds = selected)
    }

    /** From "для кого": ask about equipment only the first time, then go straight to ingredients. */
    fun confirmEaters() = viewModelScope.launch {
        if (_uiState.value.selectedIds.isEmpty()) return@launch
        // Read rather than trust the state field: the DataStore read may not have landed yet.
        val saved = kitchenRepository.appliances().first()
        if (saved == null) openAppliances() else push(DinnerStep.SOURCE)
    }

    /** Also the "изменить" link on the ingredients step. */
    fun openAppliances() {
        _uiState.update { it.copy(applianceDraft = it.savedAppliances.orEmpty()) }
        push(DinnerStep.APPLIANCES)
    }

    fun toggleAppliance(appliance: KitchenAppliance) = _uiState.update { state ->
        val draft = state.applianceDraft
        state.copy(applianceDraft = if (appliance in draft) draft - appliance else draft + appliance)
    }

    /**
     * Saves even when skipped (as whatever is selected, possibly nothing): "можно скипнуть"
     * means "don't make me answer", and asking again on every dinner would defeat that.
     */
    fun confirmAppliances() = viewModelScope.launch {
        // Navigate first: the save is local-first, the push behind it may take a moment.
        if (_uiState.value.backStack.dropLast(1).lastOrNull() == DinnerStep.SOURCE) pop() else push(DinnerStep.SOURCE)
        kitchenRepository.setAppliances(_uiState.value.applianceDraft)
    }

    fun skipAppliances() = viewModelScope.launch {
        _uiState.update { it.copy(applianceDraft = it.savedAppliances.orEmpty()) }
        confirmAppliances().join()
    }

    fun chooseAnyIngredients() = showResults(IngredientSource.ANY)

    fun chooseAtHome() {
        push(DinnerStep.PANTRY)
        // Offer something to tap right away instead of an empty search.
        if (_uiState.value.pantrySearchResults.isEmpty()) search("")
    }

    fun confirmPantry() = showResults(IngredientSource.AT_HOME)

    fun retrySuggestions() = loadSuggestions()

    /** Returns false when already on the first step, so the screen should leave the flow. */
    fun back(): Boolean {
        if (_uiState.value.editor != null) {
            closeEditor()
            return true
        }
        if (_uiState.value.backStack.size <= 1) return false
        pop()
        return true
    }

    private fun push(step: DinnerStep) = _uiState.update { it.copy(backStack = it.backStack + step) }

    private fun pop() {
        val leaving = _uiState.value.step
        _uiState.update { it.copy(backStack = it.backStack.dropLast(1)) }
        if (leaving == DinnerStep.RESULTS) suggestionsJob?.cancel()
    }

    // endregion

    // region Suggestions

    private fun showResults(source: IngredientSource) {
        _uiState.update { it.copy(source = source) }
        push(DinnerStep.RESULTS)
        loadSuggestions()
    }

    private fun loadSuggestions() {
        suggestionsJob?.cancel()
        val state = _uiState.value
        _uiState.update { it.copy(suggestions = emptyList(), suggestionsError = null, isLoadingSuggestions = true) }
        suggestionsJob = viewModelScope.launch {
            try {
                val suggestions = suggestDinner(state.selectedIds, state.source)
                _uiState.update { it.copy(suggestions = suggestions, isLoadingSuggestions = false) }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(suggestionsError = e.toUserMessage(), isLoadingSuggestions = false) }
            }
        }
    }

    // endregion

    // region Pantry

    fun onPantryQueryChange(query: String) {
        _uiState.update { it.copy(pantryQuery = query) }
        search(query, debounce = true)
    }

    private fun search(query: String, debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchingPantry = true, pantrySearchError = null) }
            try {
                val results = pantryRepository.search(query)
                _uiState.update { it.copy(pantrySearchResults = results, isSearchingPantry = false) }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(pantrySearchError = e.toUserMessage(), isSearchingPantry = false) }
            }
        }
    }

    fun addToPantry(ingredient: Ingredient) = updatePantry { it + ingredient }

    fun removeFromPantry(ingredientId: String) = updatePantry { items -> items.filterNot { it.id == ingredientId } }

    private fun updatePantry(transform: (List<Ingredient>) -> List<Ingredient>) = viewModelScope.launch {
        pantryRepository.setItems(transform(_uiState.value.pantry).distinctBy { it.id })
    }

    // endregion

    // region Eater editor

    fun addEater() = _uiState.update { it.copy(editor = EaterDraft()) }

    fun editEater(eaterId: String) = _uiState.update { state ->
        val eater = state.eaters.firstOrNull { it.id == eaterId } ?: return@update state
        state.copy(
            editor = EaterDraft(
                id = eater.id,
                isSelf = eater.isSelf,
                name = eater.name,
                sex = eater.sex,
                ageText = eater.age?.toString().orEmpty(),
                likes = eater.likes,
                avoid = eater.avoid,
                allergens = eater.allergens,
                diets = eater.diets,
                medical = eater.medical,
                // Open straight onto the details if there are any — that's what's being edited.
                showMore = eater.medical.isNotEmpty() || (!eater.isSelf && hasTasteDetails(eater)),
            ),
        )
    }

    private fun hasTasteDetails(eater: Eater) =
        eater.allergens.isNotEmpty() || eater.diets.isNotEmpty() || eater.avoid.isNotEmpty() || eater.likes.isNotEmpty()

    fun updateEditor(transform: (EaterDraft) -> EaterDraft) = _uiState.update { state ->
        state.copy(editor = state.editor?.let { transform(it).copy(errorMessage = null) })
    }

    fun closeEditor() = _uiState.update { it.copy(editor = null) }

    /**
     * Validates and saves. On success the draft is flagged [EaterDraft.saved] (the sheet
     * then animates itself closed); on failure the sheet stays open with the error.
     */
    fun saveEditor() {
        val draft = _uiState.value.editor ?: return
        if (draft.isSaving) return
        val name = draft.name.trim()
        val age = draft.ageText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val nameError = !draft.isSelf && name.isEmpty()
        val ageError = draft.ageText.isNotBlank() && (age == null || age !in 0..MAX_AGE)
        if (nameError || ageError) {
            _uiState.update { it.copy(editor = draft.copy(nameError = nameError, ageError = ageError)) }
            return
        }

        val eater = Eater(
            id = draft.id ?: UUID.randomUUID().toString(),
            name = name,
            isSelf = draft.isSelf,
            sex = draft.sex,
            age = age,
            likes = draft.likes,
            avoid = draft.avoid,
            allergens = draft.allergens,
            diets = draft.diets,
            medical = draft.medical,
        )
        _uiState.update { it.copy(editor = draft.copy(isSaving = true, errorMessage = null)) }
        viewModelScope.launch {
            try {
                householdRepository.upsert(eater)
                _uiState.update { state ->
                    state.copy(
                        // Someone just added is someone you're cooking for.
                        selectedIds = if (draft.isNew) state.selectedIds + eater.id else state.selectedIds,
                        editor = state.editor?.copy(isSaving = false, saved = true),
                    )
                }
            } catch (e: ProfileException) {
                _uiState.update { it.copy(editor = it.editor?.copy(isSaving = false, errorMessage = e.toUserMessage())) }
            }
        }
    }

    /** Local-first and never fails visibly, so the sheet can close right away. */
    fun deleteEditedEater() {
        val id = _uiState.value.editor?.id ?: return
        viewModelScope.launch { householdRepository.delete(id) }
    }

    // endregion

    private fun ProfileException.toUserMessage(): String = when (this) {
        is ProfileException.Network -> "Нет подключения к интернету. Попробуйте ещё раз."
        is ProfileException.InvalidRequest -> message ?: "Сервер не принял данные."
        else -> "Что-то пошло не так. Попробуйте ещё раз."
    }

    private companion object {
        const val MAX_AGE = 120
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
