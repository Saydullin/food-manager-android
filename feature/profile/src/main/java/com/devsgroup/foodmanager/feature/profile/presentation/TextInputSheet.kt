package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodButton
import com.devsgroup.foodmanager.core.designsystem.component.FoodModalBottomSheet
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextField

/**
 * The shared shape behind every "type a name, submit" sheet in this feature — add a
 * favorite tag, add an avoid tag, name a new group, invite a member by name. All four
 * are the same molecule with different copy, so they share this one implementation
 * instead of four near-identical composables.
 */
@Composable
internal fun TextInputSheet(
    title: String,
    helper: String,
    fieldLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    submitLabel: String,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FoodModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp, bottom = 16.dp),
        )
        FoodTextField(
            value = value,
            onValueChange = onValueChange,
            label = fieldLabel,
            placeholder = placeholder,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        Spacer(Modifier.height(20.dp))
        FoodButton(
            text = submitLabel,
            onClick = onSubmit,
            enabled = value.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
