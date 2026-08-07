package com.devsgroup.foodmanager.feature.onboarding.presentation

import app.cash.turbine.test
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.Cuisine
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * A stand-in for the real profile repository: records what onboarding writes, and lets a
 * test seed an already-populated account. Hand-written rather than mocked so the
 * assertions read as "what reached the server, in what order".
 */
private class FakeProfileRepository(
    private var cached: UserProfile? = null,
) : ProfileRepository {

    var savedPreferences: List<String>? = null
    var savedExceptions: List<String>? = null
    var savedDiets: List<String>? = null
    var completed = false
    /** Names of the calls, in order — the finish path's ordering is load-bearing. */
    val calls = mutableListOf<String>()
    var failWith: ProfileException? = null

    private val profileFlow = MutableStateFlow(cached)

    override val emailVerified: Flow<Boolean> = MutableStateFlow(true)
    override val isBanned: Flow<Boolean> = MutableStateFlow(false)
    override val onboardingComplete: Flow<Boolean> = MutableStateFlow(false)

    override fun profile(): Flow<UserProfile> = profileFlow.filterNotNull()
    override suspend fun hasCachedProfile(): Boolean = cached != null
    override suspend fun refresh() = Unit
    override suspend fun updateProfile(profile: UserProfile) = Unit
    override fun invalidateCache() = Unit
    override suspend fun uploadAvatarImage(bytes: ByteArray, fileName: String, mimeType: String): String = ""

    override suspend fun updateFoodPreferences(preferences: List<String>) {
        failWith?.let { throw it }
        calls += "preferences"
        savedPreferences = preferences
    }

    override suspend fun updateFoodExceptions(exceptions: List<String>) {
        failWith?.let { throw it }
        calls += "exceptions"
        savedExceptions = exceptions
    }

    override suspend fun updateDiets(diets: List<String>) {
        failWith?.let { throw it }
        calls += "diets"
        savedDiets = diets
    }

    override suspend fun completeOnboarding() {
        calls += "complete"
        completed = true
    }
}

private fun profileWith(
    preferences: List<String> = emptyList(),
    exceptions: List<String> = emptyList(),
    diets: List<String> = emptyList(),
) = UserProfile(
    name = "Саша",
    username = "sasha",
    foodPreferences = preferences,
    foodExceptions = exceptions,
    diets = diets,
)

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repository: FakeProfileRepository) = OnboardingViewModel(repository)

    // --- Stepping -----------------------------------------------------------

    @Test
    fun `starts on the cuisine step`() = runTest {
        val state = viewModel(FakeProfileRepository()).uiState.value
        assertEquals(OnboardingStep.CUISINES, state.step)
        assertTrue(state.isFirstStep)
        assertFalse(state.isLastStep)
    }

    @Test
    fun `next walks forward through every step then saves`() = runTest {
        val repository = FakeProfileRepository()
        val vm = viewModel(repository)

        vm.next()
        assertEquals(OnboardingStep.DIETS, vm.uiState.value.step)
        vm.next()
        assertEquals(OnboardingStep.ALLERGIES, vm.uiState.value.step)
        assertTrue(vm.uiState.value.isLastStep)

        // The last step's "next" is the save, not another step.
        vm.next()
        assertTrue(repository.completed)
    }

    @Test
    fun `back returns to the previous step and stops at the first`() = runTest {
        val vm = viewModel(FakeProfileRepository())
        vm.next()
        vm.back()
        assertEquals(OnboardingStep.CUISINES, vm.uiState.value.step)
        vm.back()
        assertEquals(OnboardingStep.CUISINES, vm.uiState.value.step)
    }

    // --- Selection ----------------------------------------------------------

    @Test
    fun `toggling a chip twice clears it`() = runTest {
        val vm = viewModel(FakeProfileRepository())
        vm.toggleCuisine(Cuisine.ITALIAN)
        assertEquals(setOf(Cuisine.ITALIAN), vm.uiState.value.cuisines)
        vm.toggleCuisine(Cuisine.ITALIAN)
        assertTrue(vm.uiState.value.cuisines.isEmpty())
    }

    // --- Saving -------------------------------------------------------------

    @Test
    fun `saves the enum names, which are the server's codes`() = runTest {
        val repository = FakeProfileRepository()
        val vm = viewModel(repository)

        vm.toggleCuisine(Cuisine.ITALIAN)
        vm.toggleCuisine(Cuisine.KOREAN)
        vm.toggleDiet(DietaryTag.VEGAN)
        vm.toggleAllergen(Allergen.TREE_NUTS)
        vm.next(); vm.next(); vm.next()

        // Codes, not labels — the feed's ranking matches on exactly these strings.
        assertEquals(listOf("ITALIAN", "KOREAN"), repository.savedPreferences)
        assertEquals(listOf("TREE_NUTS"), repository.savedExceptions)
        assertEquals(listOf("VEGAN"), repository.savedDiets)
    }

    @Test
    fun `an empty answer is saved as an empty list, not skipped`() = runTest {
        val repository = FakeProfileRepository()
        val vm = viewModel(repository)
        vm.next(); vm.next(); vm.next()

        assertEquals(emptyList<String>(), repository.savedPreferences)
        assertEquals(emptyList<String>(), repository.savedExceptions)
        assertEquals(emptyList<String>(), repository.savedDiets)
        assertTrue(repository.completed)
    }

    @Test
    fun `marks onboarding complete only after the answers are written`() = runTest {
        val repository = FakeProfileRepository()
        val vm = viewModel(repository)
        vm.next(); vm.next(); vm.next()

        // If "complete" landed first, a failure mid-save would strand the user with a
        // profile flagged done but never filled in.
        assertEquals(listOf("preferences", "exceptions", "diets", "complete"), repository.calls)
    }

    @Test
    fun `a failed save surfaces an error and does not mark onboarding complete`() = runTest {
        val repository = FakeProfileRepository().apply { failWith = ProfileException.Network(null) }
        val vm = viewModel(repository)
        vm.next(); vm.next(); vm.next()

        assertFalse(repository.completed)
        assertFalse(vm.uiState.value.isSaving)
        assertEquals("Network error", vm.uiState.value.errorMessage)
    }

    @Test
    fun `emits finished once saved, so the screen can leave the flow`() = runTest {
        val repository = FakeProfileRepository()
        val vm = viewModel(repository)
        vm.finishedEvents.test {
            vm.next(); vm.next(); vm.next()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not emit finished when the save failed`() = runTest {
        val repository = FakeProfileRepository().apply { failWith = ProfileException.Network(null) }
        val vm = viewModel(repository)
        vm.finishedEvents.test {
            vm.next(); vm.next(); vm.next()
            expectNoEvents()
        }
    }

    // --- Skipping -----------------------------------------------------------

    @Test
    fun `skipping marks onboarding complete without writing any answers`() = runTest {
        val repository = FakeProfileRepository()
        viewModel(repository).skipAll()

        assertTrue(repository.completed)
        // Nothing written — a declined flow must not overwrite whatever the account has.
        assertNull(repository.savedPreferences)
        assertNull(repository.savedExceptions)
        assertNull(repository.savedDiets)
    }

    // --- Prefill ------------------------------------------------------------

    @Test
    fun `prefills the pickers from an account that already has answers`() = runTest {
        val repository = FakeProfileRepository(
            profileWith(
                preferences = listOf("ITALIAN"),
                exceptions = listOf("PEANUTS"),
                diets = listOf("VEGAN"),
            ),
        )
        val state = viewModel(repository).uiState.first { it.cuisines.isNotEmpty() }

        assertEquals(setOf(Cuisine.ITALIAN), state.cuisines)
        assertEquals(setOf(Allergen.PEANUTS), state.allergens)
        assertEquals(setOf(DietaryTag.VEGAN), state.diets)
    }

    @Test
    fun `prefill matches a stored code case-insensitively`() = runTest {
        val repository = FakeProfileRepository(profileWith(preferences = listOf("italian")))
        val state = viewModel(repository).uiState.first { it.cuisines.isNotEmpty() }
        assertEquals(setOf(Cuisine.ITALIAN), state.cuisines)
    }

    @Test
    fun `keeps free-text tags the pickers cannot represent`() = runTest {
        // The profile screen lets users type their own tags. These endpoints replace the
        // whole list, so finishing onboarding must not delete them.
        val repository = FakeProfileRepository(
            profileWith(
                preferences = listOf("что-нибудь острое", "ITALIAN"),
                exceptions = listOf("грибы"),
            ),
        )
        val vm = viewModel(repository)
        vm.uiState.first { it.cuisines.isNotEmpty() }

        vm.toggleCuisine(Cuisine.KOREAN)
        vm.next(); vm.next(); vm.next()

        assertEquals(listOf("что-нибудь острое", "ITALIAN", "KOREAN"), repository.savedPreferences)
        assertEquals(listOf("грибы"), repository.savedExceptions)
    }

    @Test
    fun `deselecting a prefilled chip removes it on save`() = runTest {
        val repository = FakeProfileRepository(profileWith(preferences = listOf("ITALIAN", "KOREAN")))
        val vm = viewModel(repository)
        vm.uiState.first { it.cuisines.isNotEmpty() }

        vm.toggleCuisine(Cuisine.ITALIAN)
        vm.next(); vm.next(); vm.next()

        assertEquals(listOf("KOREAN"), repository.savedPreferences)
    }

    @Test
    fun `no prefill when the account has nothing cached yet`() = runTest {
        val vm = viewModel(FakeProfileRepository(cached = null))
        val state = vm.uiState.value
        assertTrue(state.cuisines.isEmpty())
        assertTrue(state.allergens.isEmpty())
        assertTrue(state.diets.isEmpty())
    }
}
