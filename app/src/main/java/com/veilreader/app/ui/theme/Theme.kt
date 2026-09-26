package com.veilreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    val Ink = Color(0xFF0B0D12)
    val Archive = Color(0xFF111821)
    val Iron = Color(0xFF1B2230)
    val RaisedIron = Color(0xFF263247)

    val Moon = Color(0xFFEEE9DE)
    val Mist = Color(0xFFA7A9AA)
    val BorderDark = Color(0xFF2F3947)
    val StrongBorderDark = Color(0xFF5C6672)

    val Brass = Color(0xFFC9A96B)
    val DeepBrass = Color(0xFF5A4526)
    val Spirit = Color(0xFF7BA8B1)
    val MoonCrimson = Color(0xFF7A2E2E)

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
    val ReaderPaper = Color(0xFFE8DCC0)

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
    val xxxl = 40.dp
}

object VeilMotion {
    const val QUICK_MS = 120
    const val STANDARD_MS = 180
    const val CEREMONIAL_MS = 320
}

object VeilStroke {
    val Hairline = 1.dp
    val Emphasis = 1.5.dp
}

object VeilOpacity {
    const val Secondary = 0.78f
    const val Muted = 0.62f
    const val Hairline = 0.34f
    const val Glow = 0.10f
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

private object VeilType {
    // Generic sans-serif can inherit a user-selected system font on some Android skins.
    // Keep the Grayfog shell deterministic with serif reading/editorial faces and a
    // restrained monospace utility face until bundled font resources land.
    val Editorial = FontFamily.Serif
    val Reading = FontFamily.Serif
    val Utility = FontFamily.Monospace
}

private val VeilTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 51.sp,
        letterSpacing = (-0.82).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.48).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.22).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.18).sp
    ),
    titleMedium = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.06).sp
    ),
    titleSmall = TextStyle(
        fontFamily = VeilType.Editorial,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VeilType.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VeilType.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.02.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VeilType.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.04.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VeilType.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.32.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VeilType.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.72.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VeilType.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.05.sp
    )
)

private val VeilShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
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

    val colors = if (useDarkTheme) VeilDarkColors else VeilLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = VeilTypography,
        shapes = VeilShapes
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground
        ) {
            content()
        }
    }
}

