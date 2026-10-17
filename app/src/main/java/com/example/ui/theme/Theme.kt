package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.NeonBlue,
    onPrimary = Color.Black,
    primaryContainer = AppColors.SurfaceSoft,
    onPrimaryContainer = AppColors.NeonBlue,
    secondary = AppColors.Coral,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.SurfaceSoft,
    onSecondaryContainer = AppColors.Coral,
    tertiary = AppColors.NeonPurple,
    onTertiary = Color.White,
    background = AppColors.Bg,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceSoft,
    onSurfaceVariant = AppColors.TextSoft,
    error = AppColors.NeonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // roket_ro is designed as an immersive dark futuristic neon theme
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
