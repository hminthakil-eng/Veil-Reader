package com.veilreader.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import com.veilreader.app.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Grayfog Design System v3.
 *
 * Typography uses licensed bundled fonts for deterministic offline rendering. The shell uses an
 * editorial/reading/utility hierarchy for Latin-script locales, while Persian/Arabic-script
 * locales deliberately avoid Latin-oriented negative tracking and monospace utility faces.
 */
enum class VeilScriptGroup {
    LATIN,
    PERSIAN_ARABIC
}

val LocalVeilScriptGroup = staticCompositionLocalOf { VeilScriptGroup.LATIN }

fun veilScriptGroupFor(language: String): VeilScriptGroup {
    val primary = language
        .trim()
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
    return when (primary) {
        "fa", "ar", "ur", "ps", "ckb" -> VeilScriptGroup.PERSIAN_ARABIC
        else -> VeilScriptGroup.LATIN
    }
}

fun usesArabicScript(text: String): Boolean =
    text.any { character ->
        val code = character.code
        code in 0x0600..0x06FF ||
            code in 0x0750..0x077F ||
            code in 0x0870..0x089F ||
            code in 0x08A0..0x08FF ||
            code in 0xFB50..0xFDFF ||
            code in 0xFE70..0xFEFF
    }


fun veilTrackingFor(
    text: String,
    latinTracking: TextUnit,
    scriptGroup: VeilScriptGroup
): TextUnit =
    if (
        scriptGroup == VeilScriptGroup.PERSIAN_ARABIC ||
        usesArabicScript(text)
    ) {
        0.sp
    } else {
        latinTracking
    }

/**
 * Local display tracking must never break connected Arabic-script glyphs.
 *
 * This helper protects both the active UI locale and mixed-script content. It should be used
 * whenever a screen intentionally overrides the typography token's letterSpacing.
 */
@Composable
fun TextStyle.withVeilTracking(
    text: String,
    latinTracking: TextUnit
): TextStyle =
    withVeilContentScript(text).copy(
        letterSpacing = veilTrackingFor(
            text = text,
            latinTracking = latinTracking,
            scriptGroup = LocalVeilScriptGroup.current
        )
    )

/** Stable optical measures shared across phone/tablet layouts. */
object VeilMeasure {
    val EditorialText = 680.dp
    val ReadingText = 720.dp
    val ArchiveContent = 840.dp
    val NarrowMetadata = 520.dp
}

/** Readable working width, after accessibility scaling, for architectural adjacency. */
object VeilComposition {
    const val ArtifactIdentityMinWidthDp = 600f
    const val ArchitecturalPairMinWidthDp = 640f
    const val ArchitecturalPairReadableWidthDp = 600f
    const val ResumeIdentityMinWidthDp = 150f
    const val ThresholdCoverMinObjectWidthDp = 84f
    const val FactPairMinWidthDp = 220f
    const val DossierTwoColumnsMinWidthDp = 240f
    const val DossierThreeColumnsMinWidthDp = 360f
    const val ArtifactCaptionMinWidthDp = 120f
    const val ArtifactCaptionMinHeightDp = 180f
    const val CompactArtifactCaptionWidthDp = 140f
    const val ChamberBridgeMinWidthDp = 440f
    const val ChamberCorridorMinWidthDp = 260f
    const val CastleRecordMinWidthDp = 280f
    const val FloorRegistrationWidthDp = 84f
    const val ChamberVaultRadiusDp = 56f
    const val ShellThemeChoiceReadableWidthDp = 88f
    const val ObservatoryVisibleConnections = 6
    const val ApproachCondenseFontScale = 1.3f
    const val ApproachShortHeightDp = 500
    // On a phone the active volume follows the doorway before a large empty foreground.
    // Empty Threshold and the wide architectural pair retain their full approach.
    const val ThresholdActiveApproachMaxHeightDp = 216f
    const val ControlCaptionCondenseFontScale = 1.3f
    const val GalleryLargeTextCoverMaxWidthDp = 168f
    const val InstrumentActionsReadableWidthDp = 300f
}

/** Missing-cover registration is subordinate to a readable identity field. */
object VeilArtifact {
    val RegistrationSize = 42.dp
}

/** Semantic shape families: archive plates stay sharp; architectural surfaces can breathe. */
object VeilShapeLanguage {
    // Plate: metadata, archive rows, precise utility surfaces.
    val Plate = ArenaGeometry.PlateRadius

    // Folio: paper/document surfaces. Still intentionally sharper than generic Material cards.
    val Folio = ArenaGeometry.FolioRadius

    // Architectural: structural shell surfaces and navigation.
    val Architectural = ArenaGeometry.ArchitectureRadius

    // Chamber: dialogs, book-detail/history regions and contained world surfaces.
    val Chamber = ArenaGeometry.ChamberRadius

    // Hero: rare large focal surfaces only.
    val Hero = ArenaGeometry.HeroRadius

    // Seal: radial/ritual controls. A very high radius keeps size, not radius, authoritative.
    val Seal = ArenaGeometry.SealRadius
}

/** Offline shell faces. Publication fonts remain exclusively renderer-owned. */
private object LatinFamilies {
    val Editorial = FontFamily(
        Font(R.font.veil_editorial_regular, FontWeight.Normal),
        Font(R.font.veil_editorial_semibold, FontWeight.SemiBold)
    )
    val Reading = FontFamily(
        Font(R.font.veil_literary_regular, FontWeight.Normal),
        Font(R.font.veil_literary_semibold, FontWeight.SemiBold)
    )
    val Utility = FontFamily(
        Font(R.font.veil_utility_regular, FontWeight.Normal),
        Font(R.font.veil_utility_semibold, FontWeight.SemiBold)
    )
}

/** Vazirmatn gives Persian/Arabic equal authored treatment, with connected-script metrics. */
private object RtlFamilies {
    val Editorial = FontFamily(
        Font(R.font.veil_persian_regular, FontWeight.Normal),
        Font(R.font.veil_persian_semibold, FontWeight.SemiBold)
    )
    val Reading = Editorial
    val Utility = Editorial
}

/** Imported metadata can use Arabic script even when the interface is English. */
fun TextStyle.withVeilContentScript(text: String): TextStyle {
    if (!usesArabicScript(text)) return this
    val scriptLineHeight = if (fontSize.isSp) {
        maxOf(if (lineHeight.isSp) lineHeight.value else 0f, fontSize.value * 1.5f).sp
    } else lineHeight
    return copy(fontFamily = RtlFamilies.Editorial, letterSpacing = 0.sp, lineHeight = scriptLineHeight)
}

val VeilLatinTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 51.sp,
        letterSpacing = (-0.82).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.48).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.22).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.18).sp
    ),
    titleMedium = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.06).sp
    ),
    titleSmall = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = LatinFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = LatinFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.02.sp
    ),
    bodySmall = TextStyle(
        fontFamily = LatinFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.04.sp
    ),
    labelLarge = TextStyle(
        fontFamily = LatinFamilies.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.32.sp
    ),
    labelMedium = TextStyle(
        fontFamily = LatinFamilies.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.45.sp
    ),
    labelSmall = TextStyle(
        fontFamily = LatinFamilies.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.65.sp
    )
)

val VeilPersianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 61.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RtlFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RtlFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RtlFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    )
)

fun veilTypographyFor(script: VeilScriptGroup): Typography =
    when (script) {
        VeilScriptGroup.LATIN -> VeilLatinTypography
        VeilScriptGroup.PERSIAN_ARABIC -> VeilPersianTypography
    }

/** Spatial proportions: world maps dominate; Threshold gives the current artifact more room. */
object VeilProportion {
    const val WorldPrimary = 0.60f
    const val CastleMapPrimary = 0.68f
    const val ThresholdPrimary = 0.44f
}

/** The selected relationship is an instrument reading; other links remain quiet context. */
object VeilObservation {
    const val SelectedEdgeAlpha = 0.36f
    const val SelectedStrengthAlpha = 0.018f
    const val ContextEdgeAlpha = 0.035f
    const val ContextStrengthAlpha = 0.0015f
    const val UnfocusedEdgeAlpha = 0.08f
    const val UnfocusedStrengthAlpha = 0.004f
    const val ContextStrokeDp = 0.45f
    const val SelectedStrokeDp = 1f
}
