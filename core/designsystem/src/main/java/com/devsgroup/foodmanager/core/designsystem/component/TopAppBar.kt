package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
 * The brand screen header: a bold, left-aligned title (Nunito) with optional
 * leading navigation and trailing [actions] — e.g. the cart badge from the mock.
 *
 * Every screen wears the same header, so it is styled once here as a design-system
 * atom — the top-bar sibling to the [FoodButton] family. `@OptIn` is contained
 * here, so callers never touch the experimental Material 3 API.
 *
 * Exactly one instance of this lives in the whole app, hosted by `FoodApp`'s
 * Scaffold — top-level screens and pushed child screens alike just report what it
 * should show (see `ReportFoodTopBar`) rather than each mounting their own bar.
 * That keeps this composable's own lifetime — and so its height — stable across
 * every push/pop; only [title] crosses over, which is why it is cross-faded here
 * instead of swapped instantly.
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
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                },
                label = "FoodTopAppBarTitle",
            ) { animatedTitle ->
                Text(text = animatedTitle, style = MaterialTheme.typography.headlineSmall)
            }
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
