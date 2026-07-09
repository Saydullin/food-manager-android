package com.devsgroup.foodmanager.core.domain.usecase

import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.model.SwipeAction
import javax.inject.Inject

/** Records a swipe so the recommender can refine future suggestions. */
class RecordSwipeUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
) {
    suspend operator fun invoke(foodId: String, action: SwipeAction) =
        foodRepository.recordSwipe(foodId, action)
}
