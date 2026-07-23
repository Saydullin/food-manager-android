package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.RecipeRepository
import com.devsgroup.foodmanager.core.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Placeholder implementation — creating recipes isn't built yet (the profile screen only
 * shows a stub "＋ Создать" action), so this always reports an empty list.
 */
class DefaultRecipeRepository @Inject constructor() : RecipeRepository {

    override fun myRecipes(): Flow<List<Recipe>> = flowOf(emptyList())
}
