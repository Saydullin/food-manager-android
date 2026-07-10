package com.devsgroup.foodmanager.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette for the "bold / dark surface / large blocks" identity.
 *
 * The three signature accents descend in energy — vivid [BrandOrange] → earthy
 * [BrandTerracotta] → muted [BrandRose] — and map onto Material 3's
 * primary / secondary / tertiary *container* roles so the three hero cards read
 * as one deliberate system. Dark text rides the bright orange; warm cream rides
 * the two deeper accents.
 */

// --- Signature accents (card fills) ---------------------------------------
internal val BrandOrange = Color(0xFFE8823C)      // card 01
internal val BrandTerracotta = Color(0xFFC0503A)  // card 02
internal val BrandRose = Color(0xFF8F5D57)        // card 03

// Accent variants used for on-surface text/icons (the "Добрый вечер" greeting, etc.)
internal val BrandOrangeBright = Color(0xFFEF9A54)
internal val BrandTerracottaBright = Color(0xFFE08267)
internal val BrandRoseBright = Color(0xFFCE9F98)

// Ink that sits on top of each accent.
internal val OnOrangeInk = Color(0xFF3A1C08)      // near-black brown on the bright orange
internal val OnAccentCream = Color(0xFFFBEAE3)    // warm cream on the deeper accents

// --- Dark surfaces (the hero look) ----------------------------------------
internal val DarkBackground = Color(0xFF1E1512)   // warm near-black brown
internal val DarkOnBackground = Color(0xFFF2EBE4) // warm off-white
internal val DarkSurfaceVariant = Color(0xFF4B3A33)
internal val DarkOnSurfaceVariant = Color(0xFFD8C6BC)
internal val DarkOutline = Color(0xFFA08D83)
internal val DarkOutlineVariant = Color(0xFF52403A)

internal val DarkSurfaceContainerLowest = Color(0xFF17100D)
internal val DarkSurfaceContainerLow = Color(0xFF261C18)
internal val DarkSurfaceContainer = Color(0xFF2A1F1B)
internal val DarkSurfaceContainerHigh = Color(0xFF352822)
internal val DarkSurfaceContainerHighest = Color(0xFF41322C)

// --- Light surfaces (complete counterpart; accents stay vivid) ------------
internal val LightBackground = Color(0xFFFCF4ED)
internal val LightOnBackground = Color(0xFF221710)
internal val LightSurfaceVariant = Color(0xFFF1E1D6)
internal val LightOnSurfaceVariant = Color(0xFF53443B)
internal val LightOutline = Color(0xFF867369)
internal val LightOutlineVariant = Color(0xFFD9C5B9)

internal val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val LightSurfaceContainerLow = Color(0xFFF7EEE6)
internal val LightSurfaceContainer = Color(0xFFF1E8DF)
internal val LightSurfaceContainerHigh = Color(0xFFECE2D9)
internal val LightSurfaceContainerHighest = Color(0xFFE6DCD2)

// Deeper accent shades for on-light "primary/secondary/tertiary" (non-container) roles.
internal val OrangeDeep = Color(0xFFB0530E)
internal val TerracottaDeep = Color(0xFFA6432D)
internal val RoseDeep = Color(0xFF7A4A44)

// --- Error (warm red, tuned to the palette) -------------------------------
internal val DarkError = Color(0xFFFFB4A6)
internal val DarkOnError = Color(0xFF690003)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD4)

internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)
