package com.devsgroup.foodmanager.feature.details.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodTopBarConfig
import com.devsgroup.foodmanager.core.designsystem.component.ReportFoodTopBar
import com.devsgroup.foodmanager.core.ui.PlaceholderScreen

@Composable
internal fun FoodDetailsScreen(
    foodId: String,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: FoodDetailsViewModel = hiltViewModel(),
) {
    // TODO: load and render the selected food; onBack pops the stack.
    ReportFoodTopBar(
        FoodTopBarConfig(
            title = "Food details: $foodId",
            navigationIcon = {
                FoodIconButton(
                    icon = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    onClick = onBack,
                )
            },
        ),
    )
    PlaceholderScreen(title = "Food details: $foodId")
}
