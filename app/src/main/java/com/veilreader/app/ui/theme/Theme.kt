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

/**
 * Veil Reader visual system.
 *
 * Gray Fog Archive v1.1 keeps the reading surface calm and legible while the app shell
 * carries restrained archival mystery. Publication/Reader themes remain independent.
 */
object VeilPalette {
    // Gray Fog semantic foundation.
    val Ink = Color(0xFF0B0E12)
    val Archive = Color(0xFF12171E)
    val Iron = Color(0xFF1B222B)
    val RaisedIron = Color(0xFF242C36)

    val Moon = Color(0xFFF2EFE7)
    val Mist = Color(0xFFAEB4B8)
    val BorderDark = Color(0xFF313A45)
    val StrongBorderDark = Color(0xFF596572)

    val Brass = Color(0xFFC4A260)
    val DeepBrass = Color(0xFF56472E)
    val Spirit = Color(0xFF74B6B8)
    val MoonCrimson = Color(0xFFC25C6C)

    val LightCanvas = Color(0xFFF2EFE7)
    val LightSurface = Color(0xFFFBF8F1)
    val LightElevated = Color(0xFFEEE8DD)
    val LightInk = Color(0xFF15191F)
    val LightMist = Color(0xFF67675F)
    val BorderLight = Color(0xFFC9C1B4)
    val StrongBorderLight = Color(0xFF81786A)
    val LightBrass = Color(0xFF8A6A35)
    val LightSpirit = Color(0xFF2C6E73)
    val LightCrimson = Color(0xFF7B2838)

    // Reader paper stays separate from the app-shell surfaces.
    val ReaderPaper = Color(0xFFF7F1E3)

    // Compatibility aliases: existing screens can migrate incrementally without a parallel theme.
    val Obsidian = Archive
    val Slate = Iron
    val RaisedSlate = RaisedIron
    val Amethyst = Brass
    val DeepAmethyst = DeepBrass
    val OldGold = Brass
    val Jade = Spirit
    val AshLine = BorderDark
    val Parchment = LightCanvas
    val WarmPaper = ReaderPaper
    val InkOnPaper = LightInk
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
    primary = VeilPalette.Brass,
    onPrimary = Color(0xFF16120C),
    primaryContainer = VeilPalette.DeepBrass,
    onPrimaryContainer = Color(0xFFF4E4BE),
    secondary = VeilPalette.Spirit,
    onSecondary = Color(0xFF082E31),
    secondaryContainer = Color(0xFF17383B),
    onSecondaryContainer = Color(0xFFD4EEEE),
    tertiary = VeilPalette.MoonCrimson,
    onTertiary = VeilPalette.Ink,
    tertiaryContainer = Color(0xFF55212B),
    onTertiaryContainer = Color(0xFFF8DDE2),
    background = VeilPalette.Ink,
    onBackground = VeilPalette.Moon,
    surface = VeilPalette.Archive,
    onSurface = VeilPalette.Moon,
    surfaceVariant = VeilPalette.Iron,
    onSurfaceVariant = VeilPalette.Mist,
    outline = VeilPalette.StrongBorderDark,
    outlineVariant = VeilPalette.BorderDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val VeilLightColors = lightColorScheme(
    primary = VeilPalette.LightBrass,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D9B8),
    onPrimaryContainer = Color(0xFF2B2113),
    secondary = VeilPalette.LightSpirit,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6ECEC),
    onSecondaryContainer = Color(0xFF103235),
    tertiary = VeilPalette.LightCrimson,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5DCE1),
    onTertiaryContainer = Color(0xFF48111D),
    background = VeilPalette.LightCanvas,
    onBackground = VeilPalette.LightInk,
    surface = VeilPalette.LightSurface,
    onSurface = VeilPalette.LightInk,
    surfaceVariant = VeilPalette.LightElevated,
    onSurfaceVariant = VeilPalette.LightMist,
    outline = VeilPalette.StrongBorderLight,
    outlineVariant = VeilPalette.BorderLight
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
