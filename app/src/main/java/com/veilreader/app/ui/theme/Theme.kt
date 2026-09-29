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
import com.veilreader.app.domain.AppThemeMode

/** Veil Reader visual system: quiet reading utility wrapped in a mysterious world. */
object VeilPalette {
    val Ink = Color(0xFF0B0A0F)
    val Obsidian = Color(0xFF121117)
    val Slate = Color(0xFF1B1921)
    val RaisedSlate = Color(0xFF24212B)
    val Moon = Color(0xFFF5F0F7)
    val Mist = Color(0xFFCFC6D3)
    val Amethyst = Color(0xFFD5BCFF)
    val DeepAmethyst = Color(0xFF4C2D6D)
    val OldGold = Color(0xFFE5C97B)
    val Jade = Color(0xFF8EDBC7)
    val AshLine = Color(0xFF514A58)
    val Parchment = Color(0xFFF6F1EA)
    val WarmPaper = Color(0xFFFFFBF6)
    val InkOnPaper = Color(0xFF252128)
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
    const val QUICK_MS = 150
    const val STANDARD_MS = 250
    const val CEREMONIAL_MS = 480
}

private val VeilDarkColors = darkColorScheme(
    primary = VeilPalette.Amethyst,
    onPrimary = Color(0xFF251538),
    primaryContainer = Color(0xFF372349),
    onPrimaryContainer = Color(0xFFF0E2FF),
    secondary = VeilPalette.OldGold,
    onSecondary = Color(0xFF342906),
    secondaryContainer = Color(0xFF443712),
    onSecondaryContainer = Color(0xFFFFEBB2),
    tertiary = VeilPalette.Jade,
    onTertiary = Color(0xFF07342A),
    background = VeilPalette.Ink,
    onBackground = VeilPalette.Moon,
    surface = VeilPalette.Obsidian,
    onSurface = VeilPalette.Moon,
    surfaceVariant = VeilPalette.Slate,
    onSurfaceVariant = VeilPalette.Mist,
    outline = VeilPalette.AshLine,
    outlineVariant = Color(0xFF332E39),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val VeilLightColors = lightColorScheme(
    primary = Color(0xFF68438D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBD9FF),
    onPrimaryContainer = Color(0xFF28113F),
    secondary = Color(0xFF755D12),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8A7),
    onSecondaryContainer = Color(0xFF251C00),
    tertiary = Color(0xFF276D5E),
    onTertiary = Color.White,
    background = VeilPalette.Parchment,
    onBackground = VeilPalette.InkOnPaper,
    surface = VeilPalette.WarmPaper,
    onSurface = VeilPalette.InkOnPaper,
    surfaceVariant = Color(0xFFECE6ED),
    onSurfaceVariant = Color(0xFF514A54),
    outline = Color(0xFF807781),
    outlineVariant = Color(0xFFD3CAD5)
)

private val VeilTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.65.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.35.sp
    )
)

private val VeilShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Composable
fun VeilTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) VeilDarkColors else VeilLightColors,
        typography = VeilTypography,
        shapes = VeilShapes,
        content = content
    )
}
