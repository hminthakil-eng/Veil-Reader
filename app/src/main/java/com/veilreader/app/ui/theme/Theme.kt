package com.veilreader.app.ui.theme

import android.animation.ValueAnimator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
    // W26: Arena is the canonical visual source of truth. Keep these names as compatibility aliases
    // so reliability work and screen-by-screen migration stay on one branch without a second theme.
    val Ink = ArenaPalette.Void
    val Archive = ArenaPalette.Archive
    val Iron = ArenaPalette.Iron
    val RaisedIron = ArenaPalette.RaisedArchive

    val Moon = ArenaPalette.Moon
    val Mist = ArenaPalette.Mist
    val BorderDark = ArenaPalette.Border
    val StrongBorderDark = ArenaPalette.StrongBorder

    val Brass = ArenaPalette.AntiqueGold
    val DeepBrass = ArenaPalette.DeepBrass
    val Spirit = Color(0xFF789CA5)
    val MoonCrimson = ArenaPalette.Oxblood

    val LightCanvas = ArenaPalette.Parchment
    val LightSurface = ArenaPalette.ParchmentLight
    val LightElevated = Color(0xFFE2D1AF)
    val LightInk = ArenaPalette.InkOnPaper
    val LightMist = Color(0xFF6D6357)
    val BorderLight = ArenaPalette.ParchmentShadow
    val StrongBorderLight = Color(0xFF806C4D)
    val LightBrass = Color(0xFF7B5727)
    val LightSpirit = Color(0xFF315F66)
    val LightCrimson = Color(0xFF70232D)

    // Reader material stays independent from shell dark/light mode.
    val ReaderPaper = ArenaPalette.Parchment

    // Compatibility aliases while legacy call-sites are rebuilt in place.
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
    // Motion grammar. Call sites should describe intent, not invent durations.
    const val MICRO_FAST_MS = 90
    const val TAP_MS = 110
    const val FUNCTIONAL_ENTER_MS = 140
    const val FUNCTIONAL_EXIT_MS = 110
    const val FUNCTIONAL_MS = 160
    const val SPATIAL_MS = 320
    const val RITUAL_MS = 900

    // Sanctuary-specific behavior. Spatial motion collapses to a short fade when reduced motion
    // is requested; the idle timeout itself remains unchanged because it is interaction policy.
    const val REDUCED_MOTION_FADE_MS = 70
    const val READER_SNACKBAR_SHIFT_MS = 160
    const val READER_AUTO_HIDE_MS = 3_600L

    // Physical paper timings are named centrally even though curl geometry remains physics-owned.
    const val PAPER_TAP_TURN_MS = 440
    const val PAPER_BOUNDARY_IN_MS = 82
    const val PAPER_BOUNDARY_OUT_MS = 108
    const val FRAME_SETTLE_MS = 18L
    const val PAGE_REVEAL_MS = 28L

    // Compatibility aliases while existing call sites migrate to semantic motion roles.
    const val QUICK_MS = TAP_MS
    const val STANDARD_MS = FUNCTIONAL_MS
    const val CEREMONIAL_MS = SPATIAL_MS
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

private val VeilHighContrastDarkColors = darkColorScheme(
    primary = Color(0xFFFFD98A),
    onPrimary = Color(0xFF0A0804),
    primaryContainer = Color(0xFF5E451C),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF9FE8F1),
    onSecondary = Color(0xFF001416),
    secondaryContainer = Color(0xFF153D42),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFB4B8),
    onTertiary = Color(0xFF220004),
    tertiaryContainer = Color(0xFF6A1F2A),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF090B0F),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF151A22),
    onSurfaceVariant = Color(0xFFE8E9EA),
    outline = Color(0xFFD9DDE3),
    outlineVariant = Color(0xFF8E97A4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF3B0000)
)

private val VeilHighContrastLightColors = lightColorScheme(
    primary = Color(0xFF654600),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE3A5),
    onPrimaryContainer = Color(0xFF1F1600),
    secondary = Color(0xFF004F56),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB8F0F5),
    onSecondaryContainer = Color(0xFF001416),
    tertiary = Color(0xFF7A0015),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9DE),
    onTertiaryContainer = Color(0xFF2A0006),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF202124),
    outline = Color(0xFF34383D),
    outlineVariant = Color(0xFF666B72),
    error = Color(0xFF8C0009),
    onError = Color(0xFFFFFFFF)
)

private val VeilShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(VeilShapeLanguage.Plate),
    small = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
)

val LocalVeilReducedMotion = staticCompositionLocalOf { false }
val LocalVeilHighContrast = staticCompositionLocalOf { false }

@Composable
fun VeilTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    highContrastEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colors = when {
        highContrastEnabled && useDarkTheme -> VeilHighContrastDarkColors
        highContrastEnabled -> VeilHighContrastLightColors
        useDarkTheme -> VeilDarkColors
        else -> VeilLightColors
    }
    val language = LocalConfiguration.current.locales[0].language
    val scriptGroup = veilScriptGroupFor(language)
    val typography = veilTypographyFor(scriptGroup)
    val reducedMotion = !ValueAnimator.areAnimatorsEnabled()

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = VeilShapes
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onBackground,
            LocalVeilScriptGroup provides scriptGroup,
            LocalVeilReducedMotion provides reducedMotion,
            LocalVeilHighContrast provides highContrastEnabled
        ) {
            content()
        }
    }
}

