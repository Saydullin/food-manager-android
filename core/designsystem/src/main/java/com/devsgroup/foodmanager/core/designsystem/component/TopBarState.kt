package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the host app's single, persistent [FoodTopAppBar] should currently show.
 *
 * Pushed (child) screens report this instead of hosting their own `Scaffold` +
 * [FoodTopAppBar], so exactly one bar instance stays alive across a push/pop —
 * only its title/navigation icon swap, its height never toggles mid-transition.
 */
data class FoodTopBarConfig(
    val title: String,
    val navigationIcon: @Composable (() -> Unit)? = null,
    val actions: @Composable RowScope.() -> Unit = {},
)

val LocalFoodTopBarState = staticCompositionLocalOf<MutableState<FoodTopBarConfig?>> {
    error(
        "LocalFoodTopBarState not provided — wrap the app content in " +
            "CompositionLocalProvider(LocalFoodTopBarState provides ...)",
    )
}

/** Publishes [config] as the host's current top bar for as long as this call site is composed. */
@Composable
fun ReportFoodTopBar(config: FoodTopBarConfig) {
    val state = LocalFoodTopBarState.current
    SideEffect { state.value = config }
}
