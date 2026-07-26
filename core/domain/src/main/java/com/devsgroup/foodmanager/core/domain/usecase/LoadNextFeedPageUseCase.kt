package com.devsgroup.foodmanager.core.domain.usecase

import com.devsgroup.foodmanager.core.domain.repository.FoodRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Fetches and caches the next page of the dish feed. Returns whether more pages remain. */
class LoadNextFeedPageUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): Boolean {
        val lang = settingsRepository.language().first().tag
        return foodRepository.loadNextFeedPage(lang)
    }
}
