package com.devsgroup.foodmanager.feature.dinner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodChipTone
import com.devsgroup.foodmanager.core.designsystem.component.FoodTagChip
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * The app's home: pick how tonight's meal happens. Only cooking is built; ordering and
 * eating out are shown as "скоро" so the shape of the product is visible from day one.
 * A top-level destination, so the shared top bar titles it — no header of its own.
 */
@Composable
internal fun WhatToEatScreen(
    onCookDinner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OptionCard(
            emoji = "🍳",
            title = "Приготовить ужин",
            subtitle = "Подберём блюдо, которое подойдёт всем за столом",
            onClick = onCookDinner,
        )
        OptionCard(
            emoji = "🛵",
            title = "Заказать еду",
            subtitle = "Доставка с учётом ваших вкусов",
            onClick = {},
            enabled = false,
        )
        OptionCard(
            emoji = "🍽️",
            title = "Поесть в городе",
            subtitle = "Кафе и рестораны поблизости",
            onClick = {},
            enabled = false,
        )
    }
}

/** A large tappable choice. Disabled options read as "скоро" rather than just greyed out. */
@Composable
internal fun OptionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!enabled) {
                FoodTagChip(label = "скоро", tone = FoodChipTone.Neutral)
            }
        }
    }
}

@Preview
@Composable
private fun WhatToEatPreview() {
    FoodManagerTheme {
        WhatToEatScreen(onCookDinner = {})
    }
}
