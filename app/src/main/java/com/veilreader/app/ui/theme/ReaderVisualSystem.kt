package com.veilreader.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.ReaderTheme

/**
 * Veil Reader visual contract.
 *
 * This is a presentation-only layer for the Sanctuary. It deliberately owns no navigator,
 * persistence, publication, TTS or page-engine behavior. The shell keeps the Grayfog / archival
 * identity while the reading surface stays calm, typographic and minimally decorated.
 */
object ReaderVisualPalette {
    val Paper = Color(0xFFF4EFDF)
    val Ivory = Color(0xFFF8F5EA)
    val Sepia = Color(0xFFE9DDC3)
    val Charcoal = Color(0xFF242628)
    val Night = Color(0xFF141517)
    val Oled = Color(0xFF000000)

    val TextOnPaper = Color(0xFF29271F)
    val TextOnIvory = Color(0xFF262521)
    val TextOnSepia = Color(0xFF372F24)
    val TextOnDark = Color(0xFFE3E0D7)
    val TextOnNight = Color(0xFFD8D6D0)
    val TextOnOled = Color(0xFFD5D5D2)

    val GoldHighlight = Color(0xFFE6C56F)
    val RoseHighlight = Color(0xFFE58A93)
    val MintHighlight = Color(0xFF8CCEB7)
    val BlueHighlight = Color(0xFF8EB8E8)
    val VioletHighlight = Color(0xFFB29AE0)

    val SearchWash = ArenaPalette.AntiqueGold.copy(alpha = 0.18f)
    val TtsWash = ArenaPalette.Moon.copy(alpha = 0.07f)
    val SelectionEdge = ArenaPalette.AntiqueGold.copy(alpha = 0.82f)
}


data class ReaderVisualThemeArgb(
    val background: Int,
    val text: Int
)

internal fun readerVisualThemeArgb(theme: ReaderTheme): ReaderVisualThemeArgb =
    when (theme) {
        ReaderTheme.PAPER -> ReaderVisualThemeArgb(
            background = 0xFFF4EFDF.toInt(),
            text = 0xFF29271F.toInt()
        )
        ReaderTheme.SEPIA -> ReaderVisualThemeArgb(
            background = 0xFFE9DDC3.toInt(),
            text = 0xFF372F24.toInt()
        )
        ReaderTheme.DUSK -> ReaderVisualThemeArgb(
            background = 0xFF141517.toInt(),
            text = 0xFFD8D6D0.toInt()
        )
        ReaderTheme.OLED -> ReaderVisualThemeArgb(
            background = 0xFF000000.toInt(),
            text = 0xFFD5D5D2.toInt()
        )
    }

object ReaderVisualGeometry {
    val TouchTarget = 48.dp
    val ChromeIcon = 21.dp
    val AppearanceGlyph = 24.dp
    val StandardToolIcon = 18.dp
    val ReaderControlMinHeight = 56.dp

    val CompactControlRadius = 10.dp
    val CardRadius = 14.dp
    val SheetTopRadius = 24.dp

    val MobileHorizontalMargin = 22.dp
    val MobileHorizontalMarginMin = 14.dp
    val MobileHorizontalMarginMax = 40.dp

    val ChapterTopBreathingRoom = 28.dp
    val ReaderBottomBreathingRoom = 32.dp
}

object ReaderVisualOpacity {
    const val EnabledSecondary = 0.78f
    const val Disabled = 0.28f
    const val InactiveSurface = 0.46f
    const val InactiveArchiveSurface = 0.66f
    const val SelectedBorder = 0.82f
    const val QuietBorder = 0.22f
}

/**
 * Motion tokens for Reader chrome and tools only.
 *
 * Page-curl physics remain MaterialPageEngine-owned and are intentionally not represented here.
 */
object ReaderVisualMotion {
    const val InstantMs = 100
    const val HudFadeMs = 180
    const val SheetMs = 240
    const val SpatialMs = 300
    const val SignatureMs = 360
    const val NarrativeMaxMs = 450
}
