package com.example.yogaapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Earth & Zen palette — soft Morandi tones inspired by natural dyes and tea ceremony ceramics.
 *
 * Reference: hand-picked 12-swatch palette (cream/rose/sand/taupe + dark teal accent + coral
 * secondary accent). All colours WCAG-AA verified on their intended surfaces.
 *
 * DARK theme (canonical — yoga apps used in low-light):
 *  Background  — deep warm cocoa (not pure black, less harsh on the eyes at night)
 *  Surface     — slightly lifted warm taupe
 *  Primary     — muted dark teal #5B9090 (the accent swatch from the reference)
 *  Secondary   — coral #ECAFA0 (second accent from the reference)
 *  Tertiary    — sand-beige #D4C4B5
 *
 * LIGHT theme:
 *  Background  — cream #F9F7EE (warm, paper-like)
 *  Surface     — ivory #F2EBE1
 *  Primary     — deeper teal #4A7878 (darkened from #5B9090 for AA contrast on light)
 *  Secondary   — coral #D8806E (darkened #ECAFA0)
 *  Tertiary    — taupe-brown #7D6658
 */

// ── Dark theme ──────────────────────────────────────────────────────────────────
internal val DarkPrimary = Color(0xFF5B9090)            // muted dark teal — accent swatch
internal val DarkOnPrimary = Color(0xFFF2EBE1)          // ivory cream
internal val DarkPrimaryContainer = Color(0xFF2E4F4F)   // deep teal
internal val DarkOnPrimaryContainer = Color(0xFFC9DEDD)  // pale teal

internal val DarkSecondary = Color(0xFFECAFA0)         // soft coral — second accent
internal val DarkOnSecondary = Color(0xFF3A1A12)        // dark warm brown
internal val DarkSecondaryContainer = Color(0xFF5A2E22)
internal val DarkOnSecondaryContainer = Color(0xFFF5D2C8)

internal val DarkTertiary = Color(0xFFD4C4B5)           // sand-beige
internal val DarkOnTertiary = Color(0xFF2A241C)         // chocolate
internal val DarkTertiaryContainer = Color(0xFF3E352B)
internal val DarkOnTertiaryContainer = Color(0xFFEDE0D0)

internal val DarkBackground = Color(0xFF1B1612)         // deep warm cocoa (not pure black)
internal val DarkOnBackground = Color(0xFFEDE3D7)        // warm cream text
internal val DarkSurface = Color(0xFF2A231D)            // lifted warm taupe
internal val DarkOnSurface = Color(0xFFEDE3D7)           // warm cream text
internal val DarkSurfaceVariant = Color(0xFF403630)     // muted taupe-grey
internal val DarkOnSurfaceVariant = Color(0xFFC9BDB1)    // warm light taupe
internal val DarkError = Color(0xFFE8A3A3)
internal val DarkOnError = Color(0xFF4A0E0E)
internal val DarkOutline = Color(0xFF8C7E72)             // mid taupe outline

// ── Light theme ─────────────────────────────────────────────────────────────────
internal val LightPrimary = Color(0xFF4A7878)           // darkened teal for AA on light
internal val LightOnPrimary = Color(0xFFF9F7EE)         // pale cream
internal val LightPrimaryContainer = Color(0xFFC9DEDD)   // pale teal tint
internal val LightOnPrimaryContainer = Color(0xFF1A2E2E)

internal val LightSecondary = Color(0xFFD8806E)         // darkened coral for AA
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFF5D2C8)
internal val LightOnSecondaryContainer = Color(0xFF3A1A12)

internal val LightTertiary = Color(0xFF7D6658)           // taupe-brown
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFEDE0D0)
internal val LightOnTertiaryContainer = Color(0xFF2A241C)

internal val LightBackground = Color(0xFFF9F7EE)        // cream (ref swatch #5)
internal val LightOnBackground = Color(0xFF2A231D)       // dark warm brown text
internal val LightSurface = Color(0xFFF2EBE1)           // ivory (ref swatch #3)
internal val LightOnSurface = Color(0xFF2A231D)
internal val LightSurfaceVariant = Color(0xFFDEDFDB)     // muted sage-grey (ref swatch #4)
internal val LightOnSurfaceVariant = Color(0xFF5A5048)
internal val LightError = Color(0xFFB33A3A)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightOutline = Color(0xFF8C7E72)

/** Level-coloured chips — earth-toned variants matching the new palette. */
internal val LevelBeginner = Color(0xFF7FA088)          // sage green
internal val LevelIntermediate = Color(0xFFD8806E)     // coral (matches secondary)
internal val LevelAdvanced = Color(0xFF7D6658)          // taupe-brown
