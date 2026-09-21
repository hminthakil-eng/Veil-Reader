package com.veilreader.app.manga.reader.ui

import com.veilreader.app.manga.reader.HorizontalGesture
import com.veilreader.app.manga.reader.MangaOrientationPolicy
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderBoundary
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.MangaReaderSnapshot
import com.veilreader.app.manga.reader.MangaReaderState

data class MangaReaderUiState(
    val reader: MangaReaderState,
    val controlsVisible: Boolean = false
)

sealed interface MangaReaderUiIntent {
    data class Tap(val xFraction: Double) : MangaReaderUiIntent
    data class DoubleTap(
        val xFraction: Double,
        val yFraction: Double
    ) : MangaReaderUiIntent

    data class Swipe(val gesture: HorizontalGesture) : MangaReaderUiIntent

    data class TransformZoom(
        val scale: Double,
        val centerXFraction: Double,
        val centerYFraction: Double
    ) : MangaReaderUiIntent

    data class WebtoonPositionChanged(
        val itemIndex: Int,
        val offsetFraction: Double
    ) : MangaReaderUiIntent

    data class PageCountResolved(val pageCount: Int) : MangaReaderUiIntent
    data class SetMode(val mode: MangaReaderMode) : MangaReaderUiIntent
    data class SetDirection(val direction: MangaPageDirection) : MangaReaderUiIntent
    data class SetOrientationPolicy(val policy: MangaOrientationPolicy) : MangaReaderUiIntent

    data object ToggleControls : MangaReaderUiIntent
    data object HideControls : MangaReaderUiIntent
    data object ResetZoom : MangaReaderUiIntent
}

sealed interface MangaReaderUiEffect {
    data class ChapterBoundaryRequested(
        val boundary: MangaReaderBoundary
    ) : MangaReaderUiEffect

    data class SnapshotChanged(
        val snapshot: MangaReaderSnapshot
    ) : MangaReaderUiEffect
}

data class MangaReaderUiReduction(
    val state: MangaReaderUiState,
    val effects: List<MangaReaderUiEffect> = emptyList()
)

enum class MangaTapZone {
    LEFT_EDGE,
    CENTER,
    RIGHT_EDGE
}

object MangaTapZoneClassifier {
    fun classify(
        xFraction: Double,
        edgeFraction: Double = 0.28
    ): MangaTapZone {
        require(edgeFraction.isFinite() && edgeFraction in 0.05..0.45)
        val x = xFraction.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.5
        return when {
            x < edgeFraction -> MangaTapZone.LEFT_EDGE
            x > 1.0 - edgeFraction -> MangaTapZone.RIGHT_EDGE
            else -> MangaTapZone.CENTER
        }
    }
}
