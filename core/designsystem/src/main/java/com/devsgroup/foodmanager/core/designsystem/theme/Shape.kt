package com.devsgroup.foodmanager.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Generous, soft-cornered shape scale for the "large blocks" language.
 *
 * The hero cards use [Shapes.extraLarge]; buttons/chips use [Shapes.large];
 * everything is rounder than the Material 3 baseline to match the design.
 */
internal val FoodShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
