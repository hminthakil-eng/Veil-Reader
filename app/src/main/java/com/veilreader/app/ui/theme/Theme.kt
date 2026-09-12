package com.veilreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Veil Reader 0.9 visual foundation.
 *
 * The palette is intentionally restrained: ink, parchment, moonlight, amethyst, old gold and jade.
 * It should feel atmospheric without turning the reader into a game UI. Reader-specific page themes
 * continue to live in the reader subsystem; this theme owns the surrounding app world.
 */
object VeilPalette {
    val Ink = Color(0xFF09090D)
    val Obsidian = Color(0xFF111016)
    val Slate = Color(0xFF1A1720)
    val RaisedSlate = Color(0xFF211D27)
    val Moon = Color(0xFFF2EDF4)
    val Mist = Color(0xFFCAC0CF)
    val Amethyst = Color(0xFFD2B7FF)
    val DeepAmethyst = Color(0xFF4E2C76)
    val OldGold = Color(0xFFE1C479)
    val Jade = Color(0xFF88D4C1)
    val AshLine = Color(0xFF4D4555)
    val Parchment = Color(0xFFF5F0E8)
    val WarmPaper = Color(0xFFFFFAF3)
    val InkOnPaper = Color(0xFF252129)
}

object VeilSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object VeilMotion {
    const val QUICK_MS = 140
    const val STANDARD_MS = 220
    const val CEREMONIAL_MS = 520
}

private val VeilDarkColors = darkColorScheme(
    primary = VeilPalette.Amethyst,
    onPrimary = Color(0xFF28113E),
    primaryContainer = VeilPalette.DeepAmethyst,
    onPrimaryContainer = Color(0xFFEEDCFF),
    secondary = VeilPalette.OldGold,
    onSecondary = Color(0xFF302605),
    secondaryContainer = Color(0xFF4A3B12),
    onSecondaryContainer = Color(0xFFFFEAB0),
    tertiary = VeilPalette.Jade,
    onTertiary = Color(0xFF082B24),
    background = VeilPalette.Ink,
    onBackground = VeilPalette.Moon,
    surface = VeilPalette.Obsidian,
    onSurface = VeilPalette.Moon,
    surfaceVariant = VeilPalette.Slate,
    onSurfaceVariant = VeilPalette.Mist,
    outline = VeilPalette.AshLine,
    outlineVariant = Color(0xFF302B36),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val VeilLightColors = lightColorScheme(
    primary = Color(0xFF65408C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD8FF),
    onPrimaryContainer = Color(0xFF25103B),
    secondary = Color(0xFF725C13),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7A3),
    onSecondaryContainer = Color(0xFF241B00),
    tertiary = Color(0xFF286B5D),
    onTertiary = Color.White,
    background = VeilPalette.Parchment,
    onBackground = VeilPalette.InkOnPaper,
    surface = VeilPalette.WarmPaper,
    onSurface = VeilPalette.InkOnPaper,
    surfaceVariant = Color(0xFFECE5EE),
    onSurfaceVariant = Color(0xFF4A434D),
    outline = Color(0xFF7B727E),
    outlineVariant = Color(0xFFD0C7D2)
)

private val VeilTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.9.sp
    )
)

private val VeilShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
)

@Composable
fun VeilTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) VeilDarkColors else VeilLightColors,
        typography = VeilTypography,
        shapes = VeilShapes,
        content = content
    )
}
