package com.devsgroup.foodmanager.core.domain.usecase

import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.model.FoodRecommendation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the personalized food deck for the discovery / recommendations screens. */
class GetRecommendedFoodsUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
) {
    operator fun invoke(): Flow<List<FoodRecommendation>> = foodRepository.recommendedFoods()
}
