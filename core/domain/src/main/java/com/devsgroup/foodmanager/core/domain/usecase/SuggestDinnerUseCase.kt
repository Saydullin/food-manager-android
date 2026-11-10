package com.devsgroup.foodmanager.core.domain.usecase

import com.devsgroup.foodmanager.core.domain.repository.DinnerRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.model.DinnerSuggestion
import com.devsgroup.foodmanager.core.model.Eater
import com.devsgroup.foodmanager.core.model.IngredientSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Dishes that suit everyone selected at the table.
 *
 * Translates the UI's selection (which treats "Я" as just another eater) into the
 * server's shape (the account owner is `includeSelf`, not an eater id) and adds the
 * user's content language — the same way [LoadNextFeedPageUseCase] does for the feed.
 */
class SuggestDinnerUseCase @Inject constructor(
    private val dinnerRepository: DinnerRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(
        selectedEaterIds: Set<String>,
        source: IngredientSource,
    ): List<DinnerSuggestion> {
        val lang = settingsRepository.language().first().tag
        return dinnerRepository.suggestions(
            eaterIds = selectedEaterIds - Eater.SELF_ID,
            includeSelf = Eater.SELF_ID in selectedEaterIds,
            source = source,
            lang = lang,
        )
    }
}
