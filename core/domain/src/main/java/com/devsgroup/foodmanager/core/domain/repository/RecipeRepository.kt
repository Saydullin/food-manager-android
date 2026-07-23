package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.Recipe
import kotlinx.coroutines.flow.Flow

/** Contract for recipes the user has created themselves. */
interface RecipeRepository {

    fun myRecipes(): Flow<List<Recipe>>
}
