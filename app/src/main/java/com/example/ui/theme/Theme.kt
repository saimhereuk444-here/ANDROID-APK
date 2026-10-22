package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberGreenColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF02200D),
    primaryContainer = Color(0xFF0C4D26),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = EmeraldGreen,
    onSecondary = Color(0xFF012B16),
    secondaryContainer = Color(0xFF085E38),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = MintGlow,
    onTertiary = Color(0xFF032814),
    tertiaryContainer = Color(0xFF10683A),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    outlineVariant = Color(0xFF16522B),
    error = AccentRed,
    onError = Color.White
)

@Composable
fun SHSkinToolTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberGreenColorScheme,
        typography = Typography,
        content = content
    )
}
