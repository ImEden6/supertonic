package com.supertone.supertonic.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemePalette {
    PURPLE, TEAL, ORANGE, PINK, DYNAMIC
}

// Purple Color Schemes
private val PurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    secondary = PurpleSecondary,
    onSecondary = PurpleOnSecondary,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    tertiary = PurpleTertiary,
    onTertiary = PurpleOnTertiary,
    tertiaryContainer = PurpleTertiaryContainer,
    onTertiaryContainer = PurpleOnTertiaryContainer,
    background = PurpleBackground,
    onBackground = PurpleOnBackground,
    surface = PurpleSurface,
    onSurface = PurpleOnSurface,
    surfaceVariant = PurpleSurfaceVariant,
    onSurfaceVariant = PurpleOnSurfaceVariant,
    outline = PurpleOutline
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = PurplePrimaryDark,
    onPrimary = PurpleOnPrimaryDark,
    primaryContainer = PurplePrimaryContainerDark,
    onPrimaryContainer = PurpleOnPrimaryContainerDark,
    secondary = PurpleSecondaryDark,
    onSecondary = PurpleOnSecondaryDark,
    secondaryContainer = PurpleSecondaryContainerDark,
    onSecondaryContainer = PurpleOnSecondaryContainerDark,
    tertiary = PurpleTertiaryDark,
    onTertiary = PurpleOnTertiaryDark,
    tertiaryContainer = PurpleTertiaryContainerDark,
    onTertiaryContainer = PurpleOnTertiaryContainerDark,
    background = PurpleBackgroundDark,
    onBackground = PurpleOnBackgroundDark,
    surface = PurpleSurfaceDark,
    onSurface = PurpleOnSurfaceDark,
    surfaceVariant = PurpleSurfaceVariantDark,
    onSurfaceVariant = PurpleOnSurfaceVariantDark,
    outline = PurpleOutlineDark
)

// Teal Color Schemes
private val TealLightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    background = TealBackground,
    onBackground = TealOnBackground,
    surface = TealSurface,
    onSurface = TealOnSurface,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = TealOnSurfaceVariant,
    outline = TealOutline
)

private val TealDarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = TealOnSecondaryContainerDark,
    background = TealBackgroundDark,
    onBackground = TealOnBackgroundDark,
    surface = TealSurfaceDark,
    onSurface = TealOnSurfaceDark,
    surfaceVariant = TealSurfaceVariantDark,
    onSurfaceVariant = TealOnSurfaceVariantDark,
    outline = TealOutlineDark
)

// Orange Color Schemes
private val OrangeLightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = OrangeOnSecondary,
    secondaryContainer = OrangeSecondaryContainer,
    onSecondaryContainer = OrangeOnSecondaryContainer,
    background = OrangeBackground,
    onBackground = OrangeOnBackground,
    surface = OrangeSurface,
    onSurface = OrangeOnSurface,
    surfaceVariant = OrangeSurfaceVariant,
    onSurfaceVariant = OrangeOnSurfaceVariant,
    outline = OrangeOutline
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = OrangeOnPrimaryDark,
    primaryContainer = OrangePrimaryContainerDark,
    onPrimaryContainer = OrangeOnPrimaryContainerDark,
    secondary = OrangeSecondaryDark,
    onSecondary = OrangeOnSecondaryDark,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OrangeOnSecondaryContainerDark,
    background = OrangeBackgroundDark,
    onBackground = OrangeOnBackgroundDark,
    surface = OrangeSurfaceDark,
    onSurface = OrangeOnSurfaceDark,
    surfaceVariant = OrangeSurfaceVariantDark,
    onSurfaceVariant = OrangeOnSurfaceVariantDark,
    outline = OrangeOutlineDark
)

// Pink Color Schemes
private val PinkLightColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = PinkOnPrimary,
    primaryContainer = PinkPrimaryContainer,
    onPrimaryContainer = PinkOnPrimaryContainer,
    secondary = PinkSecondary,
    onSecondary = PinkOnSecondary,
    secondaryContainer = PinkSecondaryContainer,
    onSecondaryContainer = PinkOnSecondaryContainer,
    background = PinkBackground,
    onBackground = PinkOnBackground,
    surface = PinkSurface,
    onSurface = PinkOnSurface,
    surfaceVariant = PinkSurfaceVariant,
    onSurfaceVariant = PinkOnSurfaceVariant,
    outline = PinkOutline
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkPrimaryDark,
    onPrimary = PinkOnPrimaryDark,
    primaryContainer = PinkPrimaryContainerDark,
    onPrimaryContainer = PinkOnPrimaryContainerDark,
    secondary = PinkSecondaryDark,
    onSecondary = PinkOnSecondaryDark,
    secondaryContainer = PinkSecondaryContainerDark,
    onSecondaryContainer = PinkOnSecondaryContainerDark,
    background = PinkBackgroundDark,
    onBackground = PinkOnBackgroundDark,
    surface = PinkSurfaceDark,
    onSurface = PinkOnSurfaceDark,
    surfaceVariant = PinkSurfaceVariantDark,
    onSurfaceVariant = PinkOnSurfaceVariantDark,
    outline = PinkOutlineDark
)

@Composable
fun SupertonicTheme(
    palette: ThemePalette = ThemePalette.PURPLE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        palette == ThemePalette.DYNAMIC && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == ThemePalette.PURPLE || (palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) -> {
            if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        }
        palette == ThemePalette.TEAL -> {
            if (darkTheme) TealDarkColorScheme else TealLightColorScheme
        }
        palette == ThemePalette.ORANGE -> {
            if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        }
        palette == ThemePalette.PINK -> {
            if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
        }
        else -> {
            if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
