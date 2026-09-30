package com.veilreader.app.manga.reader

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaReaderControllerTest {

    private val controller = MangaReaderController(minZoom = 1.0, maxZoom = 4.0)
    private val chapter = MangaReaderChapterRef(
        mangaId = CanonicalMangaId("reader-work"),
        anchor = MangaChapterAnchor(volume = 1.0, number = 3.0, languageTag = "ja")
    )

    @Test
    fun rtlAndLtrTapGestureMappingsAreDeterministic() {
        assertEquals(
            MangaReaderAdvance.FORWARD,
            controller.advanceForTap(MangaPageDirection.RIGHT_TO_LEFT, ReaderEdge.LEFT)
        )
        assertEquals(
            MangaReaderAdvance.BACKWARD,
            controller.advanceForTap(MangaPageDirection.RIGHT_TO_LEFT, ReaderEdge.RIGHT)
        )
        assertEquals(
            MangaReaderAdvance.FORWARD,
            controller.advanceForTap(MangaPageDirection.LEFT_TO_RIGHT, ReaderEdge.RIGHT)
        )

        assertEquals(
            MangaReaderAdvance.FORWARD,
            controller.advanceForGesture(
                MangaPageDirection.RIGHT_TO_LEFT,
                HorizontalGesture.SWIPE_RIGHT
            )
        )
        assertEquals(
            MangaReaderAdvance.FORWARD,
            controller.advanceForGesture(
                MangaPageDirection.LEFT_TO_RIGHT,
                HorizontalGesture.SWIPE_LEFT
            )
        )
    }

    @Test
    fun pagedAdvanceResetsZoomAndSignalsChapterBoundaries() {
        var state = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.PAGED,
            pageCount = 3,
            initialItemIndex = 1
        )
        state = controller.updateZoom(state, 3.0, 0.2, 0.8)

        val forward = controller.advance(state, MangaReaderAdvance.FORWARD)
        assertEquals(2, forward.state.position.itemIndex)
        assertEquals(1.0, forward.state.zoom.scale, 0.0001)
        assertEquals(MangaReaderBoundary.NONE, forward.boundary)

        val nextBoundary = controller.advance(forward.state, MangaReaderAdvance.FORWARD)
        assertEquals(MangaReaderBoundary.NEXT_CHAPTER, nextBoundary.boundary)
        assertEquals(2, nextBoundary.state.position.itemIndex)

        val first = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.PAGED,
            pageCount = 3
        )
        val previousBoundary = controller.advance(first, MangaReaderAdvance.BACKWARD)
        assertEquals(MangaReaderBoundary.PREVIOUS_CHAPTER, previousBoundary.boundary)
        assertEquals(0, previousBoundary.state.position.itemIndex)
    }

    @Test
    fun webtoonScrollPreservesZoomAndClampsPosition() {
        var state = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.WEBTOON,
            pageCount = 10,
            initialItemIndex = 2
        )
        state = controller.updateZoom(state, 2.5, 0.6, 0.7)
        state = controller.updateWebtoonPosition(
            state,
            itemIndex = 99,
            offsetFraction = 1.4
        )

        assertEquals(9, state.position.itemIndex)
        val position = state.position as MangaReaderPosition.Webtoon
        assertEquals(1.0, position.offsetFraction, 0.0001)
        assertEquals(2.5, state.zoom.scale, 0.0001)
    }

    @Test
    fun zoomInputIsAlwaysFiniteAndClamped() {
        val state = controller.initial(chapter, pageCount = 2)
        val high = controller.updateZoom(state, 99.0, -2.0, 5.0)

        assertEquals(4.0, high.zoom.scale, 0.0001)
        assertEquals(0.0, high.zoom.centerXFraction, 0.0001)
        assertEquals(1.0, high.zoom.centerYFraction, 0.0001)

        val nonFinite = controller.updateZoom(
            high,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        )

        assertEquals(1.0, nonFinite.zoom.scale, 0.0001)
        assertEquals(0.5, nonFinite.zoom.centerXFraction, 0.0001)
        assertEquals(0.5, nonFinite.zoom.centerYFraction, 0.0001)
    }

    @Test
    fun orientationPolicyChangeNeverMovesReadingPosition() {
        val state = controller.initial(
            chapter = chapter,
            pageCount = 20,
            initialItemIndex = 8
        )

        val locked = controller.setOrientationPolicy(
            state,
            MangaOrientationPolicy.LANDSCAPE
        )

        assertEquals(8, locked.position.itemIndex)
        assertEquals(MangaOrientationPolicy.LANDSCAPE, locked.orientationPolicy)
    }
}
