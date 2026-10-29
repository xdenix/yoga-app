package com.example.yogaapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.yogaapp.data.datastore.ThemeMode

/**
 * App theme — Meditative Dark by default.
 *
 * The dark palette is the canonical experience (yoga apps are used in low-light settings);
 * light is provided as an opt-in. Material You dynamic colours are OFF by default — we want
 * our hand-tuned indigo+gold palette to remain consistent across devices.
 */
@Composable
fun YogaAppTheme(
    themeMode: ThemeMode = ThemeMode.Dark,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light, ThemeMode.DustyRose -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            themeMode != ThemeMode.DustyRose ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        themeMode == ThemeMode.DustyRose -> lightColorScheme(
            primary = RosePrimary,
            onPrimary = RoseOnPrimary,
            primaryContainer = RosePrimaryContainer,
            onPrimaryContainer = RoseOnPrimaryContainer,
            secondary = RoseSecondary,
            onSecondary = RoseOnSecondary,
            secondaryContainer = RoseSecondaryContainer,
            onSecondaryContainer = RoseOnSecondaryContainer,
            tertiary = RoseTertiary,
            onTertiary = RoseOnTertiary,
            tertiaryContainer = RoseTertiaryContainer,
            onTertiaryContainer = RoseOnTertiaryContainer,
            background = RoseBackground,
            onBackground = RoseOnBackground,
            surface = RoseSurface,
            onSurface = RoseOnSurface,
            surfaceVariant = RoseSurfaceVariant,
            onSurfaceVariant = RoseOnSurfaceVariant,
            error = RoseError,
            onError = RoseOnError,
            outline = RoseOutline,
        )
        isDark -> darkColorScheme(
            primary = DarkPrimary,
            onPrimary = DarkOnPrimary,
            primaryContainer = DarkPrimaryContainer,
            onPrimaryContainer = DarkOnPrimaryContainer,
            secondary = DarkSecondary,
            onSecondary = DarkOnSecondary,
            secondaryContainer = DarkSecondaryContainer,
            onSecondaryContainer = DarkOnSecondaryContainer,
            tertiary = DarkTertiary,
            onTertiary = DarkOnTertiary,
            tertiaryContainer = DarkTertiaryContainer,
            onTertiaryContainer = DarkOnTertiaryContainer,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            error = DarkError,
            onError = DarkOnError,
            outline = DarkOutline,
        )
        else -> lightColorScheme(
            primary = LightPrimary,
            onPrimary = LightOnPrimary,
            primaryContainer = LightPrimaryContainer,
            onPrimaryContainer = LightOnPrimaryContainer,
            secondary = LightSecondary,
            onSecondary = LightOnSecondary,
            secondaryContainer = LightSecondaryContainer,
            onSecondaryContainer = LightOnSecondaryContainer,
            tertiary = LightTertiary,
            onTertiary = LightOnTertiary,
            tertiaryContainer = LightTertiaryContainer,
            onTertiaryContainer = LightOnTertiaryContainer,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            error = LightError,
            onError = LightOnError,
            outline = LightOutline,
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = YogaAppTypography,
        content = content,
    )
}
