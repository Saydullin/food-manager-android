package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.datastore.ProfilePreferenceList
import com.devsgroup.foodmanager.core.network.profile.ProfileApi
import com.devsgroup.foodmanager.core.network.profile.model.ProfileResponse
import com.devsgroup.foodmanager.core.network.profile.model.SetDietsRequest
import com.devsgroup.foodmanager.core.network.profile.model.SetFoodExceptionsRequest
import com.devsgroup.foodmanager.core.network.profile.model.SetFoodPreferencesRequest

/**
 * Routes one [ProfilePreferenceList] to its full-replace endpoint. Shared by
 * [DefaultProfileRepository] (the immediate push) and `ProfileSyncWorker` (the deferred
 * replay) so a given list is sent identically down both paths.
 */
internal suspend fun ProfileApi.setPreferenceList(
    list: ProfilePreferenceList,
    values: List<String>,
): ProfileResponse = when (list) {
    ProfilePreferenceList.FOOD_PREFERENCES -> setFoodPreferences(SetFoodPreferencesRequest(values))
    ProfilePreferenceList.FOOD_EXCEPTIONS -> setFoodExceptions(SetFoodExceptionsRequest(values))
    ProfilePreferenceList.DIETS -> setDiets(SetDietsRequest(values))
}
