package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    val Bg = Color(0xFF0B0F1A)
    val BgElevated = Color(0xFF111726)
    val Surface = Color(0xFF161D2E)
    val SurfaceSoft = Color(0xFF1D2537)
    val SurfaceTranslucent = Color(0x8C161D2E)

    // Neon Accents
    val NeonBlue = Color(0xFF5BC8FF)
    val NeonCyan = Color(0xFF4FE3C1)
    val Coral = Color(0xFFFF8A65)
    val NeonOrange = Color(0xFFFF8A65)
    val NeonRed = Color(0xFFFF6B8A)
    val NeonPurple = Color(0xFF9C7DFF)
    val Gold = Color(0xFFFFD166)
    val NeonGold = Color(0xFFFFD166)
    val NeonGreen = Color(0xFF4ADE80)

    // Text Colors
    val TextPrimary = Color(0xFFE9ECF5)
    val TextSoft = Color(0xFFB8C1D9)
    val TextMuted = Color(0xFF7A849C)

    // Status Colors
    val Success = Color(0xFF4ADE80)
    val Warning = Color(0xFFFBBF24)
    val Danger = Color(0xFFEF4444)

    // Glass & Card Borders
    val GlassBorder = Color(0x24FFFFFF)
    val GlassBorderActive = Color(0x665BC8FF)

    // Gradients
    val NeonGradient = Brush.linearGradient(
        colors = listOf(NeonBlue, NeonCyan, Coral)
    )

    val RocketGradient = Brush.verticalGradient(
        colors = listOf(Coral, NeonPurple, NeonBlue)
    )

    val FlameGradient = Brush.verticalGradient(
        colors = listOf(Color.White, Coral, NeonRed, Color.Transparent)
    )

    val CardGlassGradient = Brush.linearGradient(
        colors = listOf(Color(0x18FFFFFF), Color(0x06FFFFFF))
    )

    val DarkRadialGlow = Brush.radialGradient(
        colors = listOf(Color(0xFF1A2645), Bg)
    )
}
