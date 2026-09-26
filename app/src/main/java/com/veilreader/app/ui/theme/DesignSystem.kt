package com.veilreader.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val Plate = 2.dp
    val Architectural = 4.dp
    val Chamber = 8.dp
    val Hero = 12.dp
}

/**
 * Latin shell typography. Generic families are intentional temporary stand-ins until the
 * approved bundled editorial font pack is committed; roles and metrics are already locked.
 */
private object LatinFamilies {
    val Editorial = FontFamily.Serif
    val Reading = FontFamily.Serif
    val Utility = FontFamily.Monospace
}

/**
 * Persian/Arabic shell typography. Sans-serif is safer than forcing the Latin editorial serif
 * onto Arabic shaping. Tracking is kept at zero and vertical metrics are more generous.
 */
private object RtlFamilies {
    val Editorial = FontFamily.SansSerif
    val Reading = FontFamily.SansSerif
    val Utility = FontFamily.SansSerif
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
        fontSize = 11.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RtlFamilies.Utility,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

fun veilTypographyFor(script: VeilScriptGroup): Typography =
    when (script) {
        VeilScriptGroup.LATIN -> VeilLatinTypography
        VeilScriptGroup.PERSIAN_ARABIC -> VeilPersianTypography
    }
