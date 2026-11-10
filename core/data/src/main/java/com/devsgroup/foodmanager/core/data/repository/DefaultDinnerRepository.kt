package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.data.mapper.toDomain
import com.devsgroup.foodmanager.core.data.mapper.toEntity
import com.devsgroup.foodmanager.core.database.dao.FoodDao
import com.devsgroup.foodmanager.core.domain.repository.DinnerRepository
import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.IngredientSource
import com.devsgroup.foodmanager.core.network.dinner.DinnerApi
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `GET /dinner/suggestions`, online-only — the answer depends on the whole catalog and
 * the server's rules, so there's no meaningful offline copy to serve.
 */
@Singleton
class DefaultDinnerRepository @Inject constructor(
    private val dinnerApi: DinnerApi,
    private val foodDao: FoodDao,
    private val json: Json,
) : DinnerRepository {

    override suspend fun suggestions(
        eaterIds: Set<String>,
        includeSelf: Boolean,
        source: IngredientSource,
        lang: String?,
    ): List<DinnerSuggestion> {
        val suggestions = accountApiCall(json, TAG) {
            dinnerApi.suggestions(
                eaterIds = eaterIds.sorted().joinToString(",").ifEmpty { null },
                includeSelf = includeSelf,
                source = source.name,
                lang = lang,
            ).suggestions
        }.map { it.toDomain() }
        cache(suggestions)
        return suggestions
    }

    /**
     * The details screen reads dishes from Room only, and a suggestion may be a dish the
     * feed never delivered. Cached without disturbing an existing row's feed slot or like.
     */
    private suspend fun cache(suggestions: List<DinnerSuggestion>) {
        if (suggestions.isEmpty()) return
        val foods = suggestions.map { it.food }
        val existing = foodDao.getByIds(foods.map { it.id }).associateBy { it.id }
        foodDao.upsertAll(
            foods.map { food ->
                val row = existing[food.id]
                food.toEntity(feedPosition = row?.feedPosition).copy(liked = row?.liked ?: false)
            },
        )
    }

    private companion object {
        const val TAG = "DefaultDinnerRepo"
    }
}
