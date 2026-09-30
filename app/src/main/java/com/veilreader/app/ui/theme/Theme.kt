package com.veilreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Veil Reader visual system.
 *
 * The shell is an archival instrument: blue-black structure, aged brass detail and editorial type.
 * The reader remains materially quiet and paper-led. Amethyst is reserved for rare/mystical states,
 * not used as the default "premium" paint.
 */
object VeilPalette {
    val Ink = Color(0xFF07090C)
    val Obsidian = Color(0xFF0B0F14)
    val ArchiveBlue = Color(0xFF101722)
    val Slate = Color(0xFF171E28)
    val RaisedSlate = Color(0xFF202936)

    val Moon = Color(0xFFF1EEE7)
    val Mist = Color(0xFFB7BDC5)
    val Fog = Color(0xFF8793A2)

    val OldGold = Color(0xFFC5A15B)
    val Brass = Color(0xFF9C7A3D)
    val PaleGold = Color(0xFFE4CD96)
    val Amethyst = Color(0xFF9A86B8)
    val DeepAmethyst = Color(0xFF4A3A5D)
    val Jade = Color(0xFF79B5A4)
    val AshLine = Color(0xFF3B4653)

    val Parchment = Color(0xFFEDE3CF)
    val WarmPaper = Color(0xFFF7EEDC)
    val AgedPaper = Color(0xFFE2D2B5)
    val InkOnPaper = Color(0xFF282118)
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
    const val STANDARD_MS = 230
    const val CEREMONIAL_MS = 520
}

private val VeilDarkColors = darkColorScheme(
    primary = VeilPalette.OldGold,
    onPrimary = VeilPalette.Ink,
    primaryContainer = Color(0xFF2B2518),
    onPrimaryContainer = VeilPalette.PaleGold,
    secondary = VeilPalette.Mist,
    onSecondary = VeilPalette.Ink,
    secondaryContainer = Color(0xFF222B35),
    onSecondaryContainer = Color(0xFFE0E6EC),
    tertiary = VeilPalette.Amethyst,
    onTertiary = Color(0xFF17101F),
    tertiaryContainer = VeilPalette.DeepAmethyst,
    onTertiaryContainer = Color(0xFFEADFFF),
    background = VeilPalette.Ink,
    onBackground = VeilPalette.Moon,
    surface = VeilPalette.Obsidian,
    onSurface = VeilPalette.Moon,
    surfaceVariant = VeilPalette.ArchiveBlue,
    onSurfaceVariant = VeilPalette.Mist,
    outline = Color(0xFF6E6147),
    outlineVariant = VeilPalette.AshLine,
    error = Color(0xFFE8A29B),
    onError = Color(0xFF35100D)
)

private val VeilLightColors = lightColorScheme(
    primary = Color(0xFF775B27),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBD9AE),
    onPrimaryContainer = Color(0xFF281D08),
    secondary = Color(0xFF4F5E6A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE5EB),
    onSecondaryContainer = Color(0xFF132029),
    tertiary = Color(0xFF665675),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFA),
    onTertiaryContainer = Color(0xFF21172A),
    background = VeilPalette.Parchment,
    onBackground = VeilPalette.InkOnPaper,
    surface = VeilPalette.WarmPaper,
    onSurface = VeilPalette.InkOnPaper,
    surfaceVariant = VeilPalette.AgedPaper,
    onSurfaceVariant = Color(0xFF564C3F),
    outline = Color(0xFF88785E),
    outlineVariant = Color(0xFFC8B99E)
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
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
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
        letterSpacing = 0.55.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.25.sp
    )
)

/**
 * Architectural rather than pill-like. Large radii are deliberately capped so screens read like
 * bound plates and archive furniture, not a stack of generic rounded cards.
 */
private val VeilShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
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
