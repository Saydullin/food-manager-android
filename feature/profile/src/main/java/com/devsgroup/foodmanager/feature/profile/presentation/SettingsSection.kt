package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsgroup.foodmanager.core.designsystem.component.FoodSwitch
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.AppLanguage

/** "Настройки" — notifications, diet shortcut, theme, language, help, and logout. */
@Composable
internal fun SettingsSection(
    notificationsEnabled: Boolean,
    darkThemeEnabled: Boolean,
    language: AppLanguage,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(
            modifier = Modifier.padding(top = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SettingsCard {
                SettingsRowLabel(emoji = "🔔", label = "Push-уведомления")
                FoodSwitch(checked = notificationsEnabled, onCheckedChange = onToggleNotifications)
            }
            SettingsCard {
                SettingsRowLabel(emoji = "🌙", label = "Тёмная тема")
                FoodSwitch(checked = darkThemeEnabled, onCheckedChange = onToggleDarkTheme)
            }
            SettingsCard(onClick = onLanguageClick) {
                SettingsRowLabel(emoji = "🌐", label = "Язык")
                Text(
                    text = language.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChevronIcon()
            }
            SettingsCard(onClick = onHelpClick) {
                SettingsRowLabel(emoji = "💬", label = "Помощь и поддержка")
                ChevronIcon()
            }
            TextButton(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                contentPadding = PaddingValues(14.dp),
            ) {
                Text(text = "🚪", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Выйти из аккаунта",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The `#2E1C15`-style card shell every settings row (except logout) sits in. */
@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val color = MaterialTheme.colorScheme.surfaceContainer
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val rowModifier = Modifier.padding(14.dp)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border) {
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border) {
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun RowScope.SettingsRowLabel(emoji: String, label: String) {
    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun ChevronIcon() {
    Text(
        text = "›",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal val AppLanguage.label: String
    get() = when (this) {
        AppLanguage.RUSSIAN -> "Русский"
        AppLanguage.ENGLISH -> "English"
    }

@Preview
@Composable
private fun SettingsSectionPreview() {
    FoodManagerTheme {
        Surface {
            SettingsSection(
                notificationsEnabled = true,
                darkThemeEnabled = true,
                language = AppLanguage.RUSSIAN,
                onToggleNotifications = {},
                onToggleDarkTheme = {},
                onLanguageClick = {},
                onHelpClick = {},
                onLogoutClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
