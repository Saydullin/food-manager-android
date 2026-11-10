package com.devsgroup.foodmanager.feature.dinner.presentation

import com.devsgroup.foodmanager.core.domain.repository.HouseholdRepository
import com.devsgroup.foodmanager.core.domain.repository.KitchenRepository
import com.devsgroup.foodmanager.core.domain.repository.PantryRepository
import com.devsgroup.foodmanager.core.domain.usecase.SuggestDinnerUseCase
import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.Food
import com.devsgroup.foodmanager.core.model.FoodTags
import com.devsgroup.foodmanager.core.model.Ingredient
import com.devsgroup.foodmanager.core.model.IngredientSource
import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.model.ProfileException
import com.devsgroup.foodmanager.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeHouseholdRepository : HouseholdRepository {
    val state = MutableStateFlow(listOf(Eater(id = Eater.SELF_ID, name = "Я", isSelf = true)))
    var failWith: ProfileException? = null
    var refreshed = false

    override fun eaters(): Flow<List<Eater>> = state
    override suspend fun refresh() {
        refreshed = true
    }

    override suspend fun upsert(eater: Eater) {
        failWith?.let { throw it }
        state.update { list -> list.filterNot { it.id == eater.id } + eater }
    }

    override suspend fun delete(eaterId: String) = state.update { list -> list.filterNot { it.id == eaterId } }
}

private class FakeKitchenRepository(initial: Set<KitchenAppliance>? = null) : KitchenRepository {
    val state = MutableStateFlow(initial)
    override fun appliances(): Flow<Set<KitchenAppliance>?> = state
    override suspend fun setAppliances(appliances: Set<KitchenAppliance>) {
        state.value = appliances
    }

    override suspend fun refresh() = Unit
}

private class FakePantryRepository : PantryRepository {
    val state = MutableStateFlow<List<Ingredient>>(emptyList())
    val catalog = listOf(Ingredient("egg", "Яйца"), Ingredient("milk", "Молоко"))
    override fun items(): Flow<List<Ingredient>> = state
    override suspend fun setItems(items: List<Ingredient>) {
        state.value = items
    }

    override suspend fun search(query: String): List<Ingredient> = catalog.filter { query in it.name }
    override suspend fun refresh() = Unit
}

class CookDinnerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val household = FakeHouseholdRepository()
    private val pantry = FakePantryRepository()
    private val pasta = DinnerSuggestion(
        food = Food(
            id = "pasta", name = "Паста", description = null, content = null, cuisine = "ITALIAN",
            images = emptyList(), nutrition = null, tags = FoodTags(), createdAt = "", updatedAt = "",
        ),
        missingIngredients = emptyList(),
    )
    private val suggestDinner = mockk<SuggestDinnerUseCase> {
        coEvery { this@mockk.invoke(any(), any()) } returns listOf(pasta)
    }

    private fun viewModel(kitchen: KitchenRepository = FakeKitchenRepository()) =
        CookDinnerViewModel(household, kitchen, pantry, suggestDinner)

    @Test
    fun `self is selected by default and data is refreshed on open`() {
        val vm = viewModel()
        assertEquals(setOf(Eater.SELF_ID), vm.uiState.value.selectedIds)
        assertTrue(household.refreshed)
    }

    @Test
    fun `equipment is asked the first time and saved`() = runTest {
        val kitchen = FakeKitchenRepository(initial = null)
        val vm = viewModel(kitchen)

        vm.confirmEaters()
        assertEquals(DinnerStep.APPLIANCES, vm.uiState.value.step)

        vm.toggleAppliance(KitchenAppliance.OVEN)
        vm.confirmAppliances()

        assertEquals(setOf(KitchenAppliance.OVEN), kitchen.state.value)
        assertEquals(DinnerStep.SOURCE, vm.uiState.value.step)
    }

    @Test
    fun `equipment is skipped once answered`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = setOf(KitchenAppliance.STOVE)))
        vm.confirmEaters()
        assertEquals(DinnerStep.SOURCE, vm.uiState.value.step)
    }

    @Test
    fun `skipping equipment still records an answer`() = runTest {
        val kitchen = FakeKitchenRepository(initial = null)
        val vm = viewModel(kitchen)

        vm.confirmEaters()
        vm.toggleAppliance(KitchenAppliance.GRILL)
        vm.skipAppliances()

        assertEquals(emptySet<KitchenAppliance>(), kitchen.state.value)
        assertEquals(DinnerStep.SOURCE, vm.uiState.value.step)
    }

    @Test
    fun `changing equipment from the ingredients step returns to it`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.confirmEaters()

        vm.openAppliances()
        vm.confirmAppliances()

        assertEquals(listOf(DinnerStep.EATERS, DinnerStep.SOURCE), vm.uiState.value.backStack)
    }

    @Test
    fun `back retraces steps and reports leaving on the first one`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.confirmEaters()

        assertTrue(vm.back())
        assertEquals(DinnerStep.EATERS, vm.uiState.value.step)
        assertFalse(vm.back())
    }

    @Test
    fun `can't continue with nobody selected`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.toggleEater(Eater.SELF_ID)

        vm.confirmEaters()

        assertEquals(DinnerStep.EATERS, vm.uiState.value.step)
    }

    @Test
    fun `a new eater needs a name and is selected once saved`() = runTest {
        val vm = viewModel()
        vm.addEater()

        vm.saveEditor()
        assertTrue(vm.uiState.value.editor!!.nameError)

        vm.updateEditor { it.copy(name = "Маша", ageText = "6") }
        vm.saveEditor()

        val masha = household.state.value.single { it.name == "Маша" }
        assertEquals(6, masha.age)
        assertTrue(masha.id in vm.uiState.value.selectedIds)
        assertTrue(vm.uiState.value.editor!!.saved)
    }

    @Test
    fun `the self eater saves without a name`() = runTest {
        val vm = viewModel()
        vm.editEater(Eater.SELF_ID)
        vm.updateEditor { it.copy(ageText = "30") }

        vm.saveEditor()

        assertTrue(vm.uiState.value.editor!!.saved)
        assertEquals(30, household.state.value.single { it.isSelf }.age)
    }

    @Test
    fun `a rejected save keeps the sheet open with the error`() = runTest {
        household.failWith = ProfileException.Network(null)
        val vm = viewModel()
        vm.addEater()
        vm.updateEditor { it.copy(name = "Дед") }

        vm.saveEditor()

        val editor = vm.uiState.value.editor!!
        assertFalse(editor.saved)
        assertFalse(editor.isSaving)
        assertTrue(editor.errorMessage!!.isNotBlank())
    }

    @Test
    fun `an out of range age is rejected`() {
        val vm = viewModel()
        vm.addEater()
        vm.updateEditor { it.copy(name = "Дед", ageText = "200") }

        vm.saveEditor()

        assertTrue(vm.uiState.value.editor!!.ageError)
    }

    @Test
    fun `back closes the editor before leaving the step`() {
        val vm = viewModel()
        vm.addEater()

        assertTrue(vm.back())
        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun `choosing any ingredients asks the server for the selected table`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.confirmEaters()

        vm.chooseAnyIngredients()

        val state = vm.uiState.value
        assertEquals(DinnerStep.RESULTS, state.step)
        assertEquals(listOf(pasta), state.suggestions)
        assertFalse(state.isLoadingSuggestions)
        assertNull(state.suggestionsError)
        coVerify { suggestDinner(setOf(Eater.SELF_ID), IngredientSource.ANY) }
    }

    @Test
    fun `an offline suggestion request shows an error and can be retried`() = runTest {
        coEvery { suggestDinner(any(), any()) } throws ProfileException.Network(null)
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.confirmEaters()
        vm.chooseAnyIngredients()
        assertTrue(vm.uiState.value.suggestionsError != null)

        coEvery { suggestDinner(any(), any()) } returns listOf(pasta)
        vm.retrySuggestions()

        assertNull(vm.uiState.value.suggestionsError)
        assertEquals(listOf(pasta), vm.uiState.value.suggestions)
    }

    @Test
    fun `cooking from home goes through the pantry and asks for AT_HOME`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.confirmEaters()

        vm.chooseAtHome()
        assertEquals(DinnerStep.PANTRY, vm.uiState.value.step)
        assertEquals(pantry.catalog, vm.uiState.value.pantrySearchResults)

        vm.addToPantry(pantry.catalog.first())
        vm.addToPantry(pantry.catalog.first())
        assertEquals(listOf(pantry.catalog.first()), pantry.state.value)

        vm.confirmPantry()
        assertEquals(DinnerStep.RESULTS, vm.uiState.value.step)
        coVerify { suggestDinner(any(), IngredientSource.AT_HOME) }
    }

    @Test
    fun `pantry search is debounced`() = runTest {
        val vm = viewModel(FakeKitchenRepository(initial = emptySet()))
        vm.onPantryQueryChange("Мол")
        assertEquals("Мол", vm.uiState.value.pantryQuery)

        advanceUntilIdle()

        assertEquals(listOf(pantry.catalog[1]), vm.uiState.value.pantrySearchResults)
    }
}
