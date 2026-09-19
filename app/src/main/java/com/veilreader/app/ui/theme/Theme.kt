package com.veilreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R

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

/**
 * App chrome is deliberately independent from OEM/system font overrides.
 *
 * Both bundled variable fonts contain Latin plus Persian/Arabic glyphs. Veil Reader's minSdk is 26,
 * which is Android O, the minimum Android version with variable-font support.
 */
@OptIn(ExperimentalTextApi::class)
private fun veilVariableFont(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        weight = weight,
        style = FontStyle.Normal
    )
)

private val VeilSansFamily = FontFamily(
    veilVariableFont(R.font.veil_ui_sans, FontWeight.Normal),
    veilVariableFont(R.font.veil_ui_sans, FontWeight.Medium),
    veilVariableFont(R.font.veil_ui_sans, FontWeight.SemiBold),
    veilVariableFont(R.font.veil_ui_sans, FontWeight.Bold)
)

private val VeilSerifFamily = FontFamily(
    veilVariableFont(R.font.veil_display_serif, FontWeight.Normal),
    veilVariableFont(R.font.veil_display_serif, FontWeight.Medium),
    veilVariableFont(R.font.veil_display_serif, FontWeight.SemiBold),
    veilVariableFont(R.font.veil_display_serif, FontWeight.Bold)
)

private val VeilTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VeilSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = VeilSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = VeilSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VeilSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.65.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VeilSansFamily,
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VeilDarkColors else VeilLightColors,
        typography = VeilTypography,
        shapes = VeilShapes
    ) {
        // MaterialTheme does not establish a global LocalTextStyle for arbitrary Text() calls.
        // Providing the bundled body style here prevents OEM/system font overrides from leaking
        // into app-shell text that does not explicitly choose a typography token.
        ProvideTextStyle(value = VeilTypography.bodyLarge) {
            content()
        }
    }
}
