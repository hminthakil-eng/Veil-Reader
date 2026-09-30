package com.veilreader.app.manga.reader.ui

import com.veilreader.app.manga.reader.MangaReaderAdvance
import com.veilreader.app.manga.reader.MangaReaderBoundary
import com.veilreader.app.manga.reader.MangaReaderController
import com.veilreader.app.manga.reader.ReaderEdge

class MangaReaderUiReducer(
    private val controller: MangaReaderController = MangaReaderController(),
    private val navigationZoomThreshold: Double = 1.01,
    private val doubleTapZoom: Double = 2.5
) {
    init {
        require(navigationZoomThreshold >= 1.0)
        require(doubleTapZoom > 1.0)
    }

    fun reduce(
        current: MangaReaderUiState,
        intent: MangaReaderUiIntent
    ): MangaReaderUiReduction = when (intent) {
        is MangaReaderUiIntent.Tap -> onTap(current, intent.xFraction)

        is MangaReaderUiIntent.DoubleTap -> {
            val zoomed = current.reader.zoom.scale > navigationZoomThreshold
            val next = if (zoomed) {
                controller.resetZoom(current.reader)
            } else {
                controller.updateZoom(
                    current.reader,
                    scale = doubleTapZoom,
                    centerXFraction = intent.xFraction,
                    centerYFraction = intent.yFraction
                )
            }
            persisted(current.copy(reader = next))
        }

        is MangaReaderUiIntent.Swipe -> {
            if (current.reader.zoom.scale > navigationZoomThreshold) {
                MangaReaderUiReduction(current)
            } else {
                advance(
                    current,
                    controller.advanceForGesture(current.reader.direction, intent.gesture)
                )
            }
        }

        is MangaReaderUiIntent.TransformZoom -> {
            val next = controller.updateZoom(
                current.reader,
                scale = intent.scale,
                centerXFraction = intent.centerXFraction,
                centerYFraction = intent.centerYFraction
            )
            persisted(current.copy(reader = next))
        }

        is MangaReaderUiIntent.WebtoonPositionChanged -> {
            val next = controller.updateWebtoonPosition(
                current.reader,
                itemIndex = intent.itemIndex,
                offsetFraction = intent.offsetFraction
            )
            persisted(current.copy(reader = next))
        }

        is MangaReaderUiIntent.PageCountResolved -> {
            val next = controller.withPageCount(current.reader, intent.pageCount)
            persisted(current.copy(reader = next))
        }

        is MangaReaderUiIntent.SetMode -> {
            val next = controller.setMode(current.reader, intent.mode)
            persisted(current.copy(reader = next))
        }

        is MangaReaderUiIntent.SetDirection -> {
            val next = controller.setDirection(current.reader, intent.direction)
            persisted(current.copy(reader = next))
        }

        is MangaReaderUiIntent.SetOrientationPolicy -> {
            val next = controller.setOrientationPolicy(current.reader, intent.policy)
            persisted(current.copy(reader = next))
        }

        MangaReaderUiIntent.ToggleControls ->
            MangaReaderUiReduction(current.copy(controlsVisible = !current.controlsVisible))

        MangaReaderUiIntent.HideControls ->
            MangaReaderUiReduction(current.copy(controlsVisible = false))

        MangaReaderUiIntent.ResetZoom -> {
            val next = controller.resetZoom(current.reader)
            persisted(current.copy(reader = next))
        }
    }

    private fun onTap(
        current: MangaReaderUiState,
        xFraction: Double
    ): MangaReaderUiReduction {
        if (current.reader.zoom.scale > navigationZoomThreshold) {
            return MangaReaderUiReduction(
                current.copy(controlsVisible = !current.controlsVisible)
            )
        }

        return when (MangaTapZoneClassifier.classify(xFraction)) {
            MangaTapZone.CENTER ->
                MangaReaderUiReduction(
                    current.copy(controlsVisible = !current.controlsVisible)
                )

            MangaTapZone.LEFT_EDGE ->
                advance(
                    current,
                    controller.advanceForTap(current.reader.direction, ReaderEdge.LEFT)
                )

            MangaTapZone.RIGHT_EDGE ->
                advance(
                    current,
                    controller.advanceForTap(current.reader.direction, ReaderEdge.RIGHT)
                )
        }
    }

    private fun advance(
        current: MangaReaderUiState,
        advance: MangaReaderAdvance
    ): MangaReaderUiReduction {
        val transition = controller.advance(current.reader, advance)
        if (transition.boundary != MangaReaderBoundary.NONE) {
            return MangaReaderUiReduction(
                state = current,
                effects = listOf(
                    MangaReaderUiEffect.ChapterBoundaryRequested(transition.boundary)
                )
            )
        }

        return persisted(
            current.copy(
                reader = transition.state,
                controlsVisible = false
            )
        )
    }

    private fun persisted(state: MangaReaderUiState): MangaReaderUiReduction =
        MangaReaderUiReduction(
            state = state,
            effects = listOf(
                MangaReaderUiEffect.SnapshotChanged(controller.snapshot(state.reader))
            )
        )
}
