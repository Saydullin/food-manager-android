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

private val LightColorScheme = lightColorScheme(
    primary = Tomato,
    secondary = Basil,
    tertiary = Saffron,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = TomatoLight,
    secondary = BasilLight,
    tertiary = SaffronLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

/**
 * App theme built on Material 3 (Expressive). Dynamic color is used on Android 12+
 * and falls back to the brand palette on older devices.
 */
@Composable
fun FoodManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
