package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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
 * [onDismissRequest] (scrim tap, swipe-down, or system back) — those gestures are
 * handled by Material 3 internally, which animates the sheet away *before* calling
 * [onDismissRequest], so removing the composable on that callback is invisible.
 *
 * A "confirm"/"done" button inside [content] has no such built-in animation — it's
 * just a click handler — so [content] is handed a `dismiss` callback that plays the
 * exact same hide animation before calling [onDismissRequest] itself. Buttons that
 * close the sheet must call that `dismiss`, not [onDismissRequest]/the caller's own
 * state setter directly, or the sheet will vanish instead of sliding away. Keeping
 * the experimental `SheetState` itself out of the signature is deliberate — it is
 * what lets callers stay free of the experimental `@OptIn`.
 *
 * If [content] holds a focused text field, the keyboard must not linger after the
 * sheet is gone (or worse, hang around fighting the slide-down animation): the moment
 * the sheet's target state flips to [SheetValue.Hidden] — by swipe, scrim tap, back,
 * or [content]'s own `dismiss` call — focus is cleared and the IME is told to hide
 * in step with the close animation, not after it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue }
            .filter { it == SheetValue.Hidden }
            .collect {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
    }

    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissRequest()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        FoodSheetLayout { content(dismiss) }
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
