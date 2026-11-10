package com.devsgroup.foodmanager.core.data.mapper

import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.DietaryTag
import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.Sex
import com.devsgroup.foodmanager.core.network.dinner.model.DinnerSuggestionsResponseDto
import com.devsgroup.foodmanager.core.network.household.model.EaterDto
import com.devsgroup.foodmanager.core.network.household.model.KitchenDto
import com.devsgroup.foodmanager.core.network.household.model.PantryItemDto
import com.devsgroup.foodmanager.core.network.profile.model.EditDinnerSelfRequest
import com.devsgroup.foodmanager.core.network.profile.model.ProfileUserDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the lenient decoding the cook-dinner mappers promise: unknown codes are dropped, never thrown. */
class CookDinnerMappersTest {

    /** Same configuration as `NetworkModule.providesJson`. */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `eater drops unknown enum codes instead of throwing`() {
        val eater = EaterDto(
            id = "e1",
            name = "Masha",
            sex = "NON_BINARY",
            allergens = listOf("PEANUTS", "KRYPTONITE"),
            diets = listOf("VEGAN", "PALEO_PLUS"),
            medicalRestrictions = listOf("LOW_SALT", "NO_FUN"),
        ).toDomain()

        assertNull(eater.sex)
        assertEquals(setOf(Allergen.PEANUTS), eater.allergens)
        assertEquals(setOf(DietaryTag.VEGAN), eater.diets)
        assertEquals(setOf(MedicalRestriction.LOW_SALT), eater.medical)
        assertFalse(eater.isSelf)
    }

    @Test
    fun `eater round-trips through the put request`() {
        val eater = EaterDto(
            id = "e1",
            name = "Masha",
            sex = "FEMALE",
            age = 6,
            likes = listOf("ITALIAN"),
            avoid = listOf("SPICY", "кинза"),
            allergens = listOf("PEANUTS"),
            medicalRestrictions = listOf("NO_SPICY"),
        ).toDomain()

        val request = eater.toPutRequest()

        assertEquals("Masha", request.name)
        assertEquals("FEMALE", request.sex)
        assertEquals(6, request.age)
        assertEquals(listOf("SPICY", "кинза"), request.avoid)
        assertEquals(listOf("PEANUTS"), request.allergens)
        assertEquals(listOf("NO_SPICY"), request.medicalRestrictions)
    }

    @Test
    fun `kitchen keeps null distinct from empty and drops unknown appliances`() {
        assertNull(KitchenDto(appliances = null).toDomain())
        assertEquals(emptySet<KitchenAppliance>(), KitchenDto(appliances = emptyList()).toDomain())
        assertEquals(
            setOf(KitchenAppliance.OVEN),
            KitchenDto(appliances = listOf("OVEN", "TOASTER")).toDomain(),
        )
        assertEquals(
            listOf("STOVE"),
            setOf(KitchenAppliance.STOVE).toPutKitchenRequest().appliances,
        )
    }

    @Test
    fun `pantry item maps ingredientId to id`() {
        val ingredient = PantryItemDto(ingredientId = "i1", name = "Tuna").toDomain()

        assertEquals("i1", ingredient.id)
        assertEquals("Tuna", ingredient.name)
    }

    @Test
    fun `profile maps sex and medical restrictions with safe lookup`() {
        val profile = ProfileUserDto(
            id = "u1",
            username = "tester",
            sex = "MALE",
            medicalRestrictions = listOf("LOW_SALT", "FUTURE_CODE"),
        ).toDomain()

        assertEquals(Sex.MALE, profile.sex)
        assertEquals(setOf(MedicalRestriction.LOW_SALT), profile.medicalRestrictions)
    }

    @Test
    fun `edit dinner self always serializes sex and age, as null to clear them`() {
        val cleared = json.encodeToString(
            EditDinnerSelfRequest(sex = null, age = null, medicalRestrictions = emptyList()),
        )
        val set = json.encodeToString(
            EditDinnerSelfRequest(sex = "MALE", age = 34, medicalRestrictions = listOf("LOW_SALT")),
        )

        assertEquals("""{"sex":null,"age":null,"medicalRestrictions":[]}""", cleared)
        assertEquals("""{"sex":"MALE","age":34,"medicalRestrictions":["LOW_SALT"]}""", set)
    }

    @Test
    fun `suggestion decodes float macros, ingredients and appliances`() {
        val body = """
            {"suggestions":[{
              "food":{
                "id":"f1","name":"Tuna Roll","createdAt":"c","updatedAt":"u",
                "nutrition":{"calories":320,"servings":1,"protein":24.5,"fat":9,"carbs":37.4},
                "tags":{"appliances":["STOVE"]},
                "ingredients":[{"id":"l1","ingredientId":"i1","name":"Tuna","amount":150,"unit":"g"}]
              },
              "missingIngredients":[{"ingredientId":"i1","name":"Tuna","amount":0.5,"unit":"kg"}]
            }]}
        """.trimIndent()

        val suggestion = json.decodeFromString<DinnerSuggestionsResponseDto>(body)
            .suggestions.single().toDomain()

        assertEquals(25, suggestion.food.nutrition?.protein)
        assertEquals(9, suggestion.food.nutrition?.fat)
        assertEquals(37, suggestion.food.nutrition?.carbs)
        assertEquals(setOf("STOVE"), suggestion.food.tags.appliances)
        assertEquals("i1", suggestion.food.ingredients.single().ingredientId)
        assertEquals(150.0, suggestion.food.ingredients.single().amount, 0.0)
        assertEquals(0.5, suggestion.missingIngredients.single().amount, 0.0)
    }
}
