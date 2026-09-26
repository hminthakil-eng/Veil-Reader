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
    // Grayfog reference-match tokens. Keep semantic names stable so screens can migrate incrementally.
    val Ink = Color(0xFF06090C)
    val Obsidian = Color(0xFF0B1015)
    val Slate = Color(0xFF111A22)
    val RaisedSlate = Color(0xFF18232D)
    val Moon = Color(0xFFF1E7D6)
    val Mist = Color(0xFFC9BCA9)
    val Amethyst = Color(0xFFC9A96B) // legacy primary alias -> aged brass
    val DeepAmethyst = Color(0xFF4B3824)
    val OldGold = Color(0xFFC9A96B)
    val BrightGold = Color(0xFFE5C88B)
    val AntiqueBrass = Color(0xFF9C7547)
    val TarnishedBrass = Color(0xFF705336)
    val Jade = Color(0xFF8FAEA5)
    val AshLine = Color(0xFF4A4035)
    val Parchment = Color(0xFFE8DCC0)
    val WarmPaper = Color(0xFFF1E5C9)
    val AgedPaper = Color(0xFFD5C29D)
    val InkOnPaper = Color(0xFF292117)
    val BloodRed = Color(0xFF6F2527)
    val GrayfogBlue = Color(0xFF17232D)
    val MidnightBlue = Color(0xFF223440)
    val VeilBlack = Color(0xFF030608)
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
    onPrimary = VeilPalette.Ink,
    primaryContainer = VeilPalette.DeepAmethyst,
    onPrimaryContainer = VeilPalette.Moon,
    secondary = VeilPalette.OldGold,
    onSecondary = VeilPalette.Ink,
    secondaryContainer = VeilPalette.TarnishedBrass,
    onSecondaryContainer = VeilPalette.Moon,
    tertiary = VeilPalette.Jade,
    onTertiary = Color(0xFF07342A),
    background = VeilPalette.Ink,
    onBackground = VeilPalette.Moon,
    surface = VeilPalette.Obsidian,
    onSurface = VeilPalette.Moon,
    surfaceVariant = VeilPalette.Slate,
    onSurfaceVariant = VeilPalette.Mist,
    outline = VeilPalette.AshLine,
    outlineVariant = VeilPalette.TarnishedBrass,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val VeilLightColors = lightColorScheme(
    primary = Color(0xFF765A34),
    onPrimary = Color.White,
    primaryContainer = VeilPalette.AgedPaper,
    onPrimaryContainer = VeilPalette.InkOnPaper,
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
    surfaceVariant = Color(0xFFE0D1B5),
    onSurfaceVariant = Color(0xFF625440),
    outline = Color(0xFF897252),
    outlineVariant = Color(0xFFC5B18E)
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
        fontWeight = FontWeight.Bold,
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
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

