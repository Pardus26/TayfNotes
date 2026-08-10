package com.eldora25.tayfnotes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class TayfTheme {
    MIDNIGHT, SUNSET, FOREST, OCEAN, LAVENDER, ROSE, SLATE, EMERALD, ROYAL, CRIMSON
}

/**
 * Extension to get a high-contrast color (Black or White) based on background luminance
 */
fun Color.contentColor(): Color {
    return if (this.luminance() > 0.45f) Color.Black else Color.White
}

/**
 * Neon Glow Capsule for important icons (Like the Color Palette)
 */
@Composable
fun NeonCapsule(
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    val contrastColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    val neonColor = if (backgroundColor.luminance() > 0.5f) Color.Blue else Color.Yellow
    
    Box(
        modifier = Modifier
            .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = neonColor, spotColor = neonColor)
            .background(backgroundColor, CircleShape)
            .border(2.dp, contrastColor.copy(alpha = 0.8f), CircleShape)
            .padding(10.dp)
    ) {
        content()
    }
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
