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
 * Compatibility aliases for older feature code.
 *
 * New UI should prefer MaterialTheme semantic roles and VeilIdentity foundations over direct
 * palette reads.
 */
object VeilPalette {
    val Ink = VeilIdentityColor.Obsidian
    val Obsidian = VeilIdentityColor.Ink
    val Slate = VeilIdentityColor.Slate
    val RaisedSlate = VeilIdentityColor.RaisedSlate
    val Moon = VeilIdentityColor.Ivory
    val Mist = VeilIdentityColor.MistOnDark
    val Amethyst = VeilIdentityColor.Moonlight
    val DeepAmethyst = VeilIdentityColor.Ink
    val OldGold = VeilIdentityColor.AgedBrass
    val Jade = VeilIdentityColor.Moonlight
    val AshLine = VeilIdentityColor.RuleDark
    val Parchment = VeilIdentityColor.Ivory
    val WarmPaper = VeilIdentityColor.WarmPaper
    val InkOnPaper = VeilIdentityColor.InkOnPaper
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

/** Compatibility motion names; new interaction work should use VeilMotionGrammar directly. */
object VeilMotion {
    const val QUICK_MS = VeilMotionGrammar.RESPONSE_MS
    const val STANDARD_MS = VeilMotionGrammar.CONTINUITY_MS
    const val CEREMONIAL_MS = VeilMotionGrammar.THRESHOLD_MS
}

private val VeilDarkColors = darkColorScheme(
    // Moonlight is used as the digital/futuristic focus signal.
    primary = VeilIdentityColor.Moonlight,
    onPrimary = VeilIdentityColor.Obsidian,
    primaryContainer = VeilIdentityColor.Slate,
    onPrimaryContainer = VeilIdentityColor.Moonlight,

    // Brass is historical/material emphasis, not a blanket "luxury" color.
    secondary = VeilIdentityColor.AgedBrass,
    onSecondary = VeilIdentityColor.Obsidian,
    secondaryContainer = VeilIdentityColor.RaisedSlate,
    onSecondaryContainer = VeilIdentityColor.Ivory,

    tertiary = VeilIdentityColor.MistOnDark,
    onTertiary = VeilIdentityColor.Obsidian,

    background = VeilIdentityColor.Obsidian,
    onBackground = VeilIdentityColor.Ivory,
    surface = VeilIdentityColor.Ink,
    onSurface = VeilIdentityColor.Ivory,
    surfaceVariant = VeilIdentityColor.Slate,
    onSurfaceVariant = VeilIdentityColor.MistOnDark,
    outline = VeilIdentityColor.RuleDark,
    outlineVariant = Color(0xFF2C313A),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val VeilLightColors = lightColorScheme(
    // Light Veil is editorial, not simply the dark palette inverted.
    primary = Color(0xFF333944),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EAF0),
    onPrimaryContainer = Color(0xFF181C22),

    secondary = VeilIdentityColor.DeepBrass,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DFC8),
    onSecondaryContainer = Color(0xFF2A2114),

    tertiary = Color(0xFF536979),
    onTertiary = Color.White,

    background = VeilIdentityColor.Ivory,
    onBackground = VeilIdentityColor.InkOnPaper,
    surface = VeilIdentityColor.WarmPaper,
    onSurface = VeilIdentityColor.InkOnPaper,
    surfaceVariant = Color(0xFFEDE8DE),
    onSurfaceVariant = VeilIdentityColor.MistOnLight,
    outline = Color(0xFF857C70),
    outlineVariant = VeilIdentityColor.RuleLight,

    error = Color(0xFFBA1A1A),
    onError = Color.White
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

/**
 * Geometry intentionally avoids "card soup". Larger rounding remains available only through
 * explicit feature components rather than becoming the universal default.
 */
private val VeilShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(VeilRadius.Precision),
    small = androidx.compose.foundation.shape.RoundedCornerShape(VeilRadius.Control),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(VeilRadius.Panel),
    large = androidx.compose.foundation.shape.RoundedCornerShape(VeilRadius.Feature),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
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
