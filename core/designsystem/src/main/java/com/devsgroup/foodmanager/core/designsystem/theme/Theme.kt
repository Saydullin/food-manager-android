package com.devsgroup.foodmanager.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// The hero look from the design: warm near-black surface, three descending accents.
private val DarkColorScheme = darkColorScheme(
    primary = BrandOrangeBright,
    onPrimary = OnOrangeInk,
    primaryContainer = BrandOrange,
    onPrimaryContainer = OnOrangeInk,

    secondary = BrandTerracottaBright,
    onSecondary = OnAccentCream,
    secondaryContainer = BrandTerracotta,
    onSecondaryContainer = OnAccentCream,

    tertiary = BrandRoseBright,
    onTertiary = OnAccentCream,
    tertiaryContainer = BrandRose,
    onTertiaryContainer = OnAccentCream,

    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = BrandOrangeBright,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,

    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

// Faithful light counterpart — accents stay vivid so card identity survives.
private val LightColorScheme = lightColorScheme(
    primary = OrangeDeep,
    onPrimary = OnAccentCream,
    primaryContainer = BrandOrange,
    onPrimaryContainer = OnOrangeInk,

    secondary = TerracottaDeep,
    onSecondary = OnAccentCream,
    secondaryContainer = BrandTerracotta,
    onSecondaryContainer = OnAccentCream,

    tertiary = RoseDeep,
    onTertiary = OnAccentCream,
    tertiaryContainer = BrandRose,
    onTertiaryContainer = OnAccentCream,

    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = OrangeDeep,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,

    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

/**
 * App theme built on Material 3 (Expressive).
 *
 * Dynamic color defaults to **off** — this is a strongly branded, bold identity and
 * deferring to wallpaper-derived colors would dissolve it. Callers can opt back in.
 */
@Composable
fun FoodManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FoodTypography,
        shapes = FoodShapes,
        content = content,
    )
}
