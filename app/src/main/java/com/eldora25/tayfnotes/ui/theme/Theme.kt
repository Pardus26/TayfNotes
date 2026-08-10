package com.eldora25.tayfnotes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

enum class TayfTheme {
    MIDNIGHT, SUNSET, FOREST, OCEAN, LAVENDER, ROSE, SLATE, EMERALD, ROYAL, CRIMSON
}

/**
 * Extension to get a high-contrast color (Black or White) based on background luminance
 */
fun Color.contentColor(): Color {
    return if (this.luminance() > 0.45f) Color.Black else Color.White
}

private fun getDarkColorScheme(theme: TayfTheme) = darkColorScheme(
    primary = when(theme) {
        TayfTheme.MIDNIGHT -> PremiumGold
        TayfTheme.SUNSET -> SunsetPrimary
        TayfTheme.FOREST -> ForestPrimary
        TayfTheme.OCEAN -> OceanPrimary
        TayfTheme.LAVENDER -> LavenderPrimary
        TayfTheme.ROSE -> RosePrimary
        TayfTheme.SLATE -> SlatePrimary
        TayfTheme.EMERALD -> EmeraldPrimary
        TayfTheme.ROYAL -> RoyalPrimary
        TayfTheme.CRIMSON -> CrimsonPrimary
    },
    secondary = MidnightLight,
    background = MidnightBlue,
    surface = MidnightBlue,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = Color.White.copy(alpha = 0.5f)
)

private fun getLightColorScheme(theme: TayfTheme) = lightColorScheme(
    primary = when(theme) {
        TayfTheme.MIDNIGHT -> MidnightBlue
        TayfTheme.SUNSET -> SunsetPrimary
        TayfTheme.FOREST -> ForestPrimary
        TayfTheme.OCEAN -> OceanPrimary
        TayfTheme.LAVENDER -> LavenderPrimary
        TayfTheme.ROSE -> RosePrimary
        TayfTheme.SLATE -> SlatePrimary
        TayfTheme.EMERALD -> EmeraldPrimary
        TayfTheme.ROYAL -> RoyalPrimary
        TayfTheme.CRIMSON -> CrimsonPrimary
    },
    secondary = MidnightLight,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = MidnightBlue,
    onSurface = MidnightBlue,
    onSurfaceVariant = Color.DarkGray,
    outline = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun TayfNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    currentTheme: TayfTheme = TayfTheme.MIDNIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(currentTheme)
        else -> getLightColorScheme(currentTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
