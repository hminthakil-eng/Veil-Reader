package com.veilreader.app.manga.reader.ui

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.HorizontalGesture
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderBoundary
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.MangaReaderController
import com.veilreader.app.manga.reader.MangaReaderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaReaderUiReducerTest {

    private val controller = MangaReaderController()
    private val reducer = MangaReaderUiReducer(controller)

    @Test
    fun centerTapOnlyTogglesControls() {
        val state = uiState()

        val reduction = reducer.reduce(
            state,
            MangaReaderUiIntent.Tap(0.5)
        )

        assertTrue(reduction.state.controlsVisible)
        assertEquals(0, reduction.state.reader.position.itemIndex)
        assertTrue(reduction.effects.isEmpty())
    }

    @Test
    fun rtlLeftEdgeAdvancesAndHidesControls() {
        val state = uiState(
            direction = MangaPageDirection.RIGHT_TO_LEFT,
            itemIndex = 0,
            controlsVisible = true
        )

        val reduction = reducer.reduce(
            state,
            MangaReaderUiIntent.Tap(0.05)
        )

        assertEquals(1, reduction.state.reader.position.itemIndex)
        assertEquals(false, reduction.state.controlsVisible)
        assertTrue(
            reduction.effects.any { it is MangaReaderUiEffect.SnapshotChanged }
        )
    }

    @Test
    fun zoomedSwipeDoesNotAccidentallyTurnPage() {
        var state = uiState(itemIndex = 2)
        state = state.copy(
            reader = controller.updateZoom(
                state.reader,
                scale = 2.5,
                centerXFraction = 0.5,
                centerYFraction = 0.5
            )
        )

        val reduction = reducer.reduce(
            state,
            MangaReaderUiIntent.Swipe(HorizontalGesture.SWIPE_RIGHT)
        )

        assertEquals(2, reduction.state.reader.position.itemIndex)
        assertTrue(reduction.effects.isEmpty())
    }

    @Test
    fun zoomedEdgeTapDoesNotTurnPage() {
        var state = uiState(itemIndex = 2)
        state = state.copy(
            reader = controller.updateZoom(
                state.reader,
                scale = 2.0,
                centerXFraction = 0.2,
                centerYFraction = 0.4
            )
        )

        val reduction = reducer.reduce(
            state,
            MangaReaderUiIntent.Tap(0.02)
        )

        assertEquals(2, reduction.state.reader.position.itemIndex)
        assertTrue(reduction.state.controlsVisible)
    }

    @Test
    fun doubleTapTogglesZoomAroundTapPoint() {
        val state = uiState()

        val zoomed = reducer.reduce(
            state,
            MangaReaderUiIntent.DoubleTap(0.2, 0.8)
        ).state

        assertEquals(2.5, zoomed.reader.zoom.scale, 0.0001)
        assertEquals(0.2, zoomed.reader.zoom.centerXFraction, 0.0001)
        assertEquals(0.8, zoomed.reader.zoom.centerYFraction, 0.0001)

        val reset = reducer.reduce(
            zoomed,
            MangaReaderUiIntent.DoubleTap(0.5, 0.5)
        ).state

        assertEquals(1.0, reset.reader.zoom.scale, 0.0001)
    }

    @Test
    fun chapterBoundaryEmitsEffectWithoutMutatingPosition() {
        val state = uiState(itemIndex = 2, pageCount = 3)

        val reduction = reducer.reduce(
            state,
            MangaReaderUiIntent.Swipe(HorizontalGesture.SWIPE_RIGHT)
        )

        assertEquals(2, reduction.state.reader.position.itemIndex)
        val boundary = reduction.effects
            .filterIsInstance<MangaReaderUiEffect.ChapterBoundaryRequested>()
            .single()
        assertEquals(MangaReaderBoundary.NEXT_CHAPTER, boundary.boundary)
    }

    @Test
    fun webtoonViewportIntentPersistsOffset() {
        val state = uiState(mode = MangaReaderMode.WEBTOON, pageCount = 20)

        val reduction = reducer.reduce(
            state,
            MangaReaderUiIntent.WebtoonPositionChanged(
                itemIndex = 7,
                offsetFraction = 0.65
            )
        )

        val position = reduction.state.reader.position as
            com.veilreader.app.manga.reader.MangaReaderPosition.Webtoon
        assertEquals(7, position.itemIndex)
        assertEquals(0.65, position.offsetFraction, 0.0001)
        assertTrue(
            reduction.effects.any { it is MangaReaderUiEffect.SnapshotChanged }
        )
    }

    private fun uiState(
        direction: MangaPageDirection = MangaPageDirection.RIGHT_TO_LEFT,
        mode: MangaReaderMode = MangaReaderMode.PAGED,
        itemIndex: Int = 0,
        pageCount: Int = 5,
        controlsVisible: Boolean = false
    ): MangaReaderUiState {
        val chapter = MangaReaderChapterRef(
            mangaId = CanonicalMangaId("ui-work"),
            anchor = MangaChapterAnchor(number = 1.0, languageTag = "en")
        )
        return MangaReaderUiState(
            reader = controller.initial(
                chapter = chapter,
                mode = mode,
                direction = direction,
                pageCount = pageCount,
                initialItemIndex = itemIndex
            ),
            controlsVisible = controlsVisible
        )
    }
}
