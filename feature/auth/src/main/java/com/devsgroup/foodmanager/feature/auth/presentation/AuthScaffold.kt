package com.devsgroup.foodmanager.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodTextButton

/**
 * Shared frame for the auth screens (registration, login): the brand mark, a bold
 * [title] and a muted [subtitle], then whatever fields/actions the caller stacks in
 * [content]. Keeping the chrome here means the two screens differ only by their copy
 * and form, and "how the auth flow looks" stays a single-file edit — the same reasoning
 * the design-system atoms follow.
 *
 * The screens render full-bleed (no shared top bar / bottom nav), so the column is
 * vertically scrollable to survive small screens and the IME.
 *
 * @param title the headline, in the design's voice ("Создать аккаунт").
 * @param subtitle the supporting line under the title.
 * @param titleStyle the headline type; defaults to the two-line display size, pass a
 *   smaller style for short single-line titles.
 * @param content the form body — fields, primary action, and footer link.
 */
@Composable
internal fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.displayMedium,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
//        BrandMark()

        Spacer(Modifier.height(40.dp))

        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        content()
    }
}

/**
 * The switch-flow line under the primary action — a muted [prompt] followed by the
 * orange [action] link (e.g. "Уже есть аккаунт? Войти"). Shared so registration and
 * login stay mirror images of each other.
 */
@Composable
internal fun AuthFooter(
    prompt: String,
    action: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FoodTextButton(
            text = action,
            onClick = onActionClick,
            compact = true
        )
    }
}

/**
 * The "НА УЖИН" wordmark: the app initial in an orange tile beside the tracked-out
 * name. Local to the auth feature for now — promote to `:core:designsystem` if a
 * second surface needs it.
 */
@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Н",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = "НА УЖИН",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
        )
    }
}
