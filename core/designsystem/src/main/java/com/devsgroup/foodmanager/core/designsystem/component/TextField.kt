package com.devsgroup.foodmanager.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * Async, server-side check on the field's value — e.g. "is this nickname taken" —
 * shown as a trailing spinner/checkmark plus an accent-tinted helper line (see the
 * registration mocks). Distinct from [FoodTextField]'s `isError`/`errorMessage`: those
 * cover synchronous local validation (too short, bad format) and always take visual
 * priority, since a local error blocks submission regardless of what the server check
 * last reported.
 */
sealed interface FoodTextFieldCheckStatus {
    data class Checking(val message: String) : FoodTextFieldCheckStatus
    data class Available(val message: String) : FoodTextFieldCheckStatus
}

/**
 * The brand text field: a persistent eyebrow [label], a rounded filled input, and an
 * optional [description] / [errorMessage] line beneath it — the form-input sibling to
 * the [FoodButton] family, [FoodTopAppBar] and [FoodModalBottomSheet].
 *
 * It lives in its own file for the reason the [FoodButton] doc spells out: a genuinely
 * different component earns one. Every text entry in the app wears this same frame, so
 * "how our inputs look" stays a single-file edit.
 *
 * Design choices (see the registration mock):
 *  - The [label] is drawn **above** the field as a standalone tracked-out eyebrow
 *    (`labelLarge`), not the Material floating label — the design keeps the field name
 *    visible even while typing. Pass it in the design's uppercase voice ("НИКНЕЙМ").
 *  - When [isRequired] is `false` the label gains a muted "· необязательно" marker, so
 *    optional fields read as optional without extra caller wiring. Required is the
 *    default because most fields are.
 *  - The input is an M3 `OutlinedTextField` restyled to the brand: a warm filled
 *    container, a soft [outlineVariant] resting border that lifts to the orange
 *    [primary] on focus, and brand rounding ([Shapes.large]).
 *
 * State is fully hoisted (unidirectional data flow): the caller owns [value] and updates
 * it from [onValueChange]. This atom is UI-only — it holds no state and knows no domain.
 *
 * The helper line below the field shows [errorMessage] tinted with the error color while
 * [isError] is set, and otherwise falls back to [description]; pass validation copy
 * through [errorMessage] and keep [description] for the steady-state hint.
 *
 * @param value current text, owned by the caller.
 * @param onValueChange invoked with the edited text on every keystroke.
 * @param label the field name, shown as the eyebrow above the input.
 * @param placeholder faint sample text shown while [value] is empty (e.g. "chef_maria").
 * @param description steady-state helper text under the field.
 * @param isRequired when `false`, appends the optional marker to [label].
 * @param isError draws the field and helper line in the error color.
 * @param errorMessage shown in place of [description] while [isError] is set.
 * @param checkStatus an in-flight/resolved async check (e.g. nickname availability),
 * shown as a trailing icon plus accent helper text. Ignored while [isError] is set.
 * @param enabled when `false`, dims the field and blocks input.
 * @param singleLine keeps the input on one line (the default for form fields).
 * @param keyboardOptions IME type/capitalisation, e.g. [KeyboardType.Email] for a mail field.
 * @param keyboardActions handlers for IME actions (Done/Next).
 * @param visualTransformation e.g. a password mask; [VisualTransformation.None] by default.
 */
@Composable
fun FoodTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    description: String? = null,
    isRequired: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    checkStatus: FoodTextFieldCheckStatus? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FieldLabel(label = label, isRequired = isRequired)

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = isError,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = placeholder?.let {
                { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            trailingIcon = if (!isError && checkStatus != null) {
                { CheckStatusIcon(checkStatus) }
            } else {
                null
            },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        val checkMessage = checkStatus?.message.takeIf { !isError }
        val helper = errorMessage.takeIf { isError } ?: checkMessage ?: description
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    checkMessage != null -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** The trailing spinner ([FoodTextFieldCheckStatus.Checking]) or checkmark ([FoodTextFieldCheckStatus.Available]). */
@Composable
private fun CheckStatusIcon(status: FoodTextFieldCheckStatus) {
    when (status) {
        is FoodTextFieldCheckStatus.Checking -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )

        is FoodTextFieldCheckStatus.Available -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private val FoodTextFieldCheckStatus.message: String
    get() = when (this) {
        is FoodTextFieldCheckStatus.Checking -> message
        is FoodTextFieldCheckStatus.Available -> message
    }

@Composable
private fun FieldLabel(label: String, isRequired: Boolean) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            append(label)
            if (isRequired) {
                withStyle(SpanStyle(color = onSurfaceVariant)) {
                    append(REQUIRED_MARKER)
                }
            }
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

private const val REQUIRED_MARKER = "*"

@Preview
@Composable
private fun FoodTextFieldPreview() {
    FoodManagerTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FoodTextField(
                    value = "",
                    onValueChange = {},
                    label = "НИКНЕЙМ",
                    placeholder = "например, chef_maria",
                    description = "Придумайте уникальный никнейм",
                )
                FoodTextField(
                    value = "you@example.com",
                    onValueChange = {},
                    label = "ПОЧТА",
                    isRequired = false,
                    placeholder = "you@example.com",
                    description = "Понадобится, чтобы войти с нового устройства.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                FoodTextField(
                    value = "chef",
                    onValueChange = {},
                    label = "НИКНЕЙМ",
                    isError = true,
                    errorMessage = "Никнейм уже занят",
                )
                FoodTextField(
                    value = "ada",
                    onValueChange = {},
                    label = "НИКНЕЙМ",
                    checkStatus = FoodTextFieldCheckStatus.Checking("Проверяем доступность..."),
                )
                FoodTextField(
                    value = "ada",
                    onValueChange = {},
                    label = "НИКНЕЙМ",
                    checkStatus = FoodTextFieldCheckStatus.Available("Свободен — можно занять"),
                )
            }
        }
    }
}
