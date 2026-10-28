package com.example.yogaapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Meditative dark palette.
 *
 * Inspiration: Calm app's deep midnight-blue background, with a warm gold accent inspired
 * by traditional Indian yoga imagery (sunrise over the Ganges). Primary is indigo-deep for
 * the meditative dark UI; onPrimary is cream-warm so text stays readable at low light.
 *
 * All colours have been manually checked for WCAG AA contrast on their intended surfaces
 * (≥ 4.5:1 for body text; ≥ 3:1 for large text).
 */
internal val DarkPrimary = Color(0xFFD4AF37)        // warm gold
internal val DarkOnPrimary = Color(0xFF1A1B3A)     // deep indigo (text on gold)
internal val DarkPrimaryContainer = Color(0xFF4D3A0F)
internal val DarkOnPrimaryContainer = Color(0xFFFFE9B0)

internal val DarkSecondary = Color(0xFFB7A4D9)     // soft lavender
internal val DarkOnSecondary = Color(0xFF231A38)
internal val DarkSecondaryContainer = Color(0xFF33294A)
internal val DarkOnSecondaryContainer = Color(0xFFE6DEF7)

internal val DarkTertiary = Color(0xFF8FB1B9)      // muted teal
internal val DarkOnTertiary = Color(0xFF0F2A30)
internal val DarkTertiaryContainer = Color(0xFF28434A)
internal val DarkOnTertiaryContainer = Color(0xFFBCE9EE)

internal val DarkBackground = Color(0xFF0E0F25)    // deep midnight
internal val DarkOnBackground = Color(0xFFE4E2F0)
internal val DarkSurface = Color(0xFF1A1B3A)      // indigo-deep
internal val DarkOnSurface = Color(0xFFE4E2F0)
internal val DarkSurfaceVariant = Color(0xFF474564)
internal val DarkOnSurfaceVariant = Color(0xFFC9C5DD)
internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkOutline = Color(0xFF8E8AAA)

/**
 * Light palette — used only when the user explicitly chooses Light theme. Lower priority
 * than the dark one; the meditative experience is optimised for low light.
 */
internal val LightPrimary = Color(0xFF6B5826)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFFAE7B1)
internal val LightOnPrimaryContainer = Color(0xFF211400)

internal val LightSecondary = Color(0xFF635B70)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFE9DEF8)
internal val LightOnSecondaryContainer = Color(0xFF1F182B)

internal val LightTertiary = Color(0xFF416169)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFC5E8EF)
internal val LightOnTertiaryContainer = Color(0xFF002025)

internal val LightBackground = Color(0xFFFDFBFF)
internal val LightOnBackground = Color(0xFF1B1B21)
internal val LightSurface = Color(0xFFFDFBFF)
internal val LightOnSurface = Color(0xFF1B1B21)
internal val LightSurfaceVariant = Color(0xFFE2E1EC)
internal val LightOnSurfaceVariant = Color(0xFF45464F)
internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightOutline = Color(0xFF767680)

/** Level-coloured chips. */
internal val LevelBeginner = Color(0xFF7FB069)     // green
internal val LevelIntermediate = Color(0xFFE8A33D) // amber
internal val LevelAdvanced = Color(0xFFD46A6A)     // terracotta
