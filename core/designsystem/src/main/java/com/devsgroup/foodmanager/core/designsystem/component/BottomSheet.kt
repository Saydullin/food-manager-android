package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The brand modal bottom sheet: a scrim-backed panel that slides up from the
 * bottom for a focused sub-task (pick a reason, confirm an action, quick edit)
 * without leaving the current screen.
 *
 * It is a design-system atom — the sheet sibling to the [FoodButton] family and
 * [FoodTopAppBar] — for the same reason those are: sheets should look and inset
 * the same everywhere, and "how our sheets behave" should be a single-file edit.
 * As with [FoodTopAppBar], the experimental Material 3 `@OptIn` is contained here
 * so feature callers never touch the experimental API themselves.
 *
 * The atom owns only the *frame*: the sheet container, the drag handle, and a
 * consistent content [Column] (side padding, a little top breathing room, and a
 * `navigationBarsPadding()` so the last control always clears the gesture bar).
 * The [content] is whatever the caller stacks inside that column — this component
 * is UI-only and holds no state or domain model.
 *
 * Visibility is driven by the caller (unidirectional data flow): render this only
 * while your state says the sheet is open, and flip that state back off from
 * [onDismissRequest] (scrim tap, swipe-down, or system back). Keeping the
 * experimental `SheetState` out of the signature is deliberate — it is what lets
 * callers stay free of the experimental `@OptIn`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        FoodSheetLayout(content = content)
    }
}

/**
 * Shared content frame for every sheet: full width, brand side padding, a touch
 * of top space under the drag handle, and a navigation-bar inset at the bottom.
 * Factored out so the [Preview] renders the exact layout the real sheet uses
 * (a `ModalBottomSheet` shows in its own window and won't render in a preview).
 */
@Composable
private fun FoodSheetLayout(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        content = content,
    )
}

@Preview
@Composable
private fun FoodModalBottomSheetPreview() {
    FoodManagerTheme {
        // Preview the content frame on a sheet-shaped surface — the real
        // ModalBottomSheet renders in its own window and can't show here.
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 1.dp,
        ) {
            FoodSheetLayout {
                Text(
                    text = "Заголовок",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Пример содержимого нижнего листа.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
