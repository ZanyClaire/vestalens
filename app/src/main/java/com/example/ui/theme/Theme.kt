package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VistaLensDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = OnAmberGold,
    primaryContainer = AmberGoldContainer,
    onPrimaryContainer = AmberGoldLight,
    secondary = ElectricCyan,
    onSecondary = OnElectricCyan,
    secondaryContainer = ElectricCyanContainer,
    onSecondaryContainer = ElectricCyanLight,
    tertiary = EmeraldVerified,
    onTertiary = Color(0xFF032218),
    background = ObsidianNight,
    onBackground = IvoryTextPrimary,
    surface = MidnightSurface,
    onSurface = IvoryTextPrimary,
    surfaceVariant = SlateCardSurface,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainerHigh = SlateElevatedSurface,
    outline = SlateBorderSubtle,
    error = CoralAlert
)

private val VistaLensLightColorScheme = lightColorScheme(
    primary = BronzeAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = DeepTealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = WarmIvoryBackground,
    onBackground = DeepNavyInk,
    surface = WarmPaperSurface,
    onSurface = DeepNavyInk,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFE11D48)
)

val VistaLensShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VistaLensDarkColorScheme else VistaLensLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VistaLensShapes,
        content = content
    )
}
