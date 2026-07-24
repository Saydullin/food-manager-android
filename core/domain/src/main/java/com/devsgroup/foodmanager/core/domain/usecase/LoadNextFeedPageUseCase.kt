package com.devsgroup.foodmanager.core.domain.usecase

import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import javax.inject.Inject

/** Fetches and caches the next page of the dish feed. Returns whether more pages remain. */
class LoadNextFeedPageUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
) {
    suspend operator fun invoke(): Boolean = foodRepository.loadNextFeedPage()
}
