package com.veilreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VeilDarkColors = darkColorScheme(
    primary = Color(0xFFC7A6FF),
    onPrimary = Color(0xFF24133F),
    secondary = Color(0xFFE8C976),
    tertiary = Color(0xFF7DD8C9),
    background = Color(0xFF0D0B10),
    onBackground = Color(0xFFF4EFF8),
    surface = Color(0xFF151119),
    onSurface = Color(0xFFF4EFF8),
    surfaceVariant = Color(0xFF211A26),
    onSurfaceVariant = Color(0xFFCFC3D7)
)

private val VeilLightColors = lightColorScheme(
    primary = Color(0xFF6941A5),
    secondary = Color(0xFF775A00),
    background = Color(0xFFF9F5FA),
    surface = Color(0xFFFFF9FF),
    onSurface = Color(0xFF211B23)
)

@Composable
fun VeilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) VeilDarkColors else VeilLightColors,
        typography = Typography(),
        content = content
    )
}
