package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The brand screen header: a bold, left-aligned title (Unbounded) with optional
 * leading navigation and trailing [actions] — e.g. the cart badge from the mock.
 *
 * Every screen wears the same header, so it is styled once here as a design-system
 * atom — the top-bar sibling to the [FoodButton] family. `@OptIn` is contained
 * here, so callers never touch the experimental Material 3 API.
 *
 * Placement (see FoodApp / feature screens):
 *  - Top-level screens ("Для вас", "Обзор", …) share one instance hosted at the app
 *    level, with the title coming from the active bottom-nav destination.
 *  - Child screens (food details) render their own header with a back
 *    [navigationIcon] inside the feature's own Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

@Preview
@Composable
private fun FoodTopAppBarPreview() {
    FoodManagerTheme {
        Surface {
            FoodTopAppBar(title = "Для вас")
        }
    }
}
