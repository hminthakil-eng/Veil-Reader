package com.veilreader.app.manga.reader

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor

enum class MangaReaderMode {
    PAGED,
    WEBTOON
}

enum class MangaPageDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT
}

enum class MangaOrientationPolicy {
    FOLLOW_SYSTEM,
    PORTRAIT,
    LANDSCAPE
}

data class MangaReaderChapterRef(
    val mangaId: CanonicalMangaId,
    val anchor: MangaChapterAnchor
)

sealed interface MangaReaderPosition {
    val itemIndex: Int

    data class Paged(
        override val itemIndex: Int
    ) : MangaReaderPosition {
        init {
            require(itemIndex >= 0) { "Paged item index cannot be negative" }
        }
    }

    data class Webtoon(
        override val itemIndex: Int,
        val offsetFraction: Double
    ) : MangaReaderPosition {
        init {
            require(itemIndex >= 0) { "Webtoon item index cannot be negative" }
            require(offsetFraction.isFinite() && offsetFraction in 0.0..1.0) {
                "Webtoon offset must be between 0 and 1"
            }
        }
    }
}

data class MangaZoomState(
    val scale: Double = 1.0,
    val centerXFraction: Double = 0.5,
    val centerYFraction: Double = 0.5
) {
    init {
        require(scale.isFinite() && scale > 0.0) { "Zoom scale must be finite and positive" }
        require(centerXFraction.isFinite() && centerXFraction in 0.0..1.0)
        require(centerYFraction.isFinite() && centerYFraction in 0.0..1.0)
    }
}

data class MangaReaderState(
    val chapter: MangaReaderChapterRef,
    val mode: MangaReaderMode,
    val direction: MangaPageDirection,
    val orientationPolicy: MangaOrientationPolicy,
    val pageCount: Int?,
    val position: MangaReaderPosition,
    val zoom: MangaZoomState = MangaZoomState()
) {
    init {
        require(pageCount == null || pageCount > 0) { "Page count must be positive when known" }
        require(pageCount == null || position.itemIndex < pageCount) {
            "Reader position is outside page count"
        }
        require(
            (mode == MangaReaderMode.PAGED && position is MangaReaderPosition.Paged) ||
                (mode == MangaReaderMode.WEBTOON && position is MangaReaderPosition.Webtoon)
        ) { "Reader position must match reader mode" }
    }
}

data class MangaReaderSnapshot(
    val version: Int = CURRENT_VERSION,
    val chapter: MangaReaderChapterRef,
    val mode: MangaReaderMode,
    val direction: MangaPageDirection,
    val orientationPolicy: MangaOrientationPolicy,
    val itemIndex: Int,
    val webtoonOffsetFraction: Double,
    val zoom: MangaZoomState
) {
    init {
        require(version > 0)
        require(itemIndex >= 0)
        require(webtoonOffsetFraction.isFinite() && webtoonOffsetFraction in 0.0..1.0)
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

enum class MangaReaderAdvance {
    FORWARD,
    BACKWARD
}

enum class MangaReaderBoundary {
    NONE,
    PREVIOUS_CHAPTER,
    NEXT_CHAPTER
}

data class MangaReaderTransition(
    val state: MangaReaderState,
    val boundary: MangaReaderBoundary = MangaReaderBoundary.NONE
)

enum class HorizontalGesture {
    SWIPE_LEFT,
    SWIPE_RIGHT
}

enum class ReaderEdge {
    LEFT,
    RIGHT
}
