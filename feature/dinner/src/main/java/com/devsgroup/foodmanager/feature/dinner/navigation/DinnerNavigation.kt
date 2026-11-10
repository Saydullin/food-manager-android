package com.devsgroup.foodmanager.feature.dinner.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.devsgroup.foodmanager.feature.dinner.presentation.CookDinnerScreen
import com.devsgroup.foodmanager.feature.dinner.presentation.WhatToEatScreen
import kotlinx.serialization.Serializable

/** The app's home: "what are we eating?" — cook / order / eat out. */
@Serializable
data object WhatToEatRoute

/** The "cook dinner" wizard: who eats → equipment → ingredients → dishes. */
@Serializable
data object CookDinnerRoute

fun NavController.navigateToCookDinner(navOptions: NavOptions? = null) =
    navigate(CookDinnerRoute, navOptions)

fun NavGraphBuilder.dinnerScreens(
    onCookDinner: () -> Unit,
    onBack: () -> Unit,
    onFoodClick: (String) -> Unit,
) {
    composable<WhatToEatRoute> {
        WhatToEatScreen(onCookDinner = onCookDinner)
    }
    composable<CookDinnerRoute> {
        CookDinnerScreen(onBack = onBack, onFoodClick = onFoodClick)
    }
}
