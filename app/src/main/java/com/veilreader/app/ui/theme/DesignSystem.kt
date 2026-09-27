package com.veilreader.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R

/**
 * Grayfog Design System v3.
 *
 * Typography is script-aware even before bundled font binaries land. The shell uses an
 * editorial/reading/utility hierarchy for Latin-script locales, while Persian/Arabic-script
 * locales deliberately avoid Latin-oriented negative tracking and monospace utility faces.
 */
enum class VeilScriptGroup {
    LATIN,
    PERSIAN_ARABIC
}

val LocalVeilScriptGroup = staticCompositionLocalOf { VeilScriptGroup.LATIN }

fun veilScriptGroupFor(language: String): VeilScriptGroup =
    when (language.lowercase()) {
        "fa", "ar", "ur", "ps", "ckb" -> VeilScriptGroup.PERSIAN_ARABIC
        else -> VeilScriptGroup.LATIN
    }

fun usesArabicScript(text: String): Boolean =
    text.any { character ->
        val code = character.code
        code in 0x0600..0x06FF ||
            code in 0x0750..0x077F ||
            code in 0x08A0..0x08FF ||
            code in 0xFB50..0xFDFF ||
            code in 0xFE70..0xFEFF
    }

/** Stable optical measures shared across phone/tablet layouts. */
object VeilMeasure {
    val EditorialText = 680.dp
    val ReadingText = 720.dp
    val ArchiveContent = 840.dp
    val NarrowMetadata = 520.dp
}

/** Semantic shape families: archive plates stay sharp; architectural surfaces can breathe. */
object VeilShapeLanguage {
    // Plate: metadata, archive rows, precise utility surfaces.
    val Plate = 0.dp

    // Folio: paper/document surfaces. Still intentionally sharper than generic Material cards.
    val Folio = 1.dp

    // Architectural: structural shell surfaces and navigation.
    val Architectural = 2.dp

    // Chamber: dialogs, book-detail/history regions and contained world surfaces.
    val Chamber = 4.dp

    // Hero: rare large focal surfaces only.
    val Hero = 7.dp

    // Seal: radial/ritual controls. A very high radius keeps size, not radius, authoritative.
    val Seal = 999.dp
}

/**
 * Deterministic bundled typography.
 *
 * These variable fonts were previously validated for Veil Reader in the P7 typography work.
 * They contain Latin plus Persian/Arabic coverage and keep app-shell metrics independent from
 * Samsung/OEM custom system fonts. Publication typography inside Reader remains Readium-owned.
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

private val VeilUiSansFamily = FontFamily(
    veilVariableFont(R.font.veil_ui_sans, FontWeight.Normal),
    veilVariableFont(R.font.veil_ui_sans, FontWeight.Medium),
    veilVariableFont(R.font.veil_ui_sans, FontWeight.SemiBold),
    veilVariableFont(R.font.veil_ui_sans, FontWeight.Bold)
)

private val VeilDisplayFamily = FontFamily(
    veilVariableFont(R.font.veil_display_serif, FontWeight.Normal),
    veilVariableFont(R.font.veil_display_serif, FontWeight.Medium),
    veilVariableFont(R.font.veil_display_serif, FontWeight.SemiBold),
    veilVariableFont(R.font.veil_display_serif, FontWeight.Bold)
)

private val VazirmatnUiFamily = FontFamily(
    Font(R.font.vazirmatn_ui_regular, weight = FontWeight.Normal),
    Font(R.font.vazirmatn_ui_medium, weight = FontWeight.Medium),
    Font(R.font.vazirmatn_ui_semibold, weight = FontWeight.SemiBold),
    Font(R.font.vazirmatn_ui_bold, weight = FontWeight.Bold)
)

private object LatinFamilies {
    val Editorial = VeilDisplayFamily
    val Reading = VeilUiSansFamily
    val Utility = VeilUiSansFamily
}

private object RtlFamilies {
    val Editorial = VazirmatnUiFamily
    val Reading = VazirmatnUiFamily
    val Utility = VazirmatnUiFamily
}

val VeilLatinTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 54.sp,
        lineHeight = 57.sp,
        letterSpacing = (-1.05).sp
    ),
    displayMedium = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 49.sp,
        letterSpacing = (-0.82).sp
    ),
    displaySmall = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 43.sp,
        letterSpacing = (-0.66).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.62).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = LatinFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.28).sp
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
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.02.sp
    ),
    bodySmall = TextStyle(
        fontFamily = LatinFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
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
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.72.sp
    ),
    labelSmall = TextStyle(
        fontFamily = LatinFamilies.Utility,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.05.sp
    )
)

val VeilPersianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 61.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    displayMedium = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 55.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    displaySmall = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Bold,
        fontSize = 35.sp,
        lineHeight = 49.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    headlineLarge = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    headlineMedium = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    headlineSmall = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    titleLarge = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    titleMedium = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    titleSmall = TextStyle(
        fontFamily = RtlFamilies.Editorial,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    bodyLarge = TextStyle(
        fontFamily = RtlFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    bodyMedium = TextStyle(
        fontFamily = RtlFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    bodySmall = TextStyle(
        fontFamily = RtlFamilies.Reading,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    labelLarge = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    labelMedium = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    ),
    labelSmall = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.ContentOrRtl
    )
)

fun veilTypographyFor(script: VeilScriptGroup): Typography =
    when (script) {
        VeilScriptGroup.LATIN -> VeilLatinTypography
        VeilScriptGroup.PERSIAN_ARABIC -> VeilPersianTypography
    }
