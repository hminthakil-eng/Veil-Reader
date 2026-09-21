package com.veilreader.app.manga.reader

class MangaReaderController(
    private val minZoom: Double = 1.0,
    private val maxZoom: Double = 5.0
) {
    init {
        require(minZoom.isFinite() && minZoom > 0.0)
        require(maxZoom.isFinite() && maxZoom >= minZoom)
    }

    fun initial(
        chapter: MangaReaderChapterRef,
        mode: MangaReaderMode = MangaReaderMode.PAGED,
        direction: MangaPageDirection = MangaPageDirection.RIGHT_TO_LEFT,
        orientationPolicy: MangaOrientationPolicy = MangaOrientationPolicy.FOLLOW_SYSTEM,
        pageCount: Int? = null,
        initialItemIndex: Int = 0
    ): MangaReaderState {
        val index = clampIndex(initialItemIndex, pageCount)
        return MangaReaderState(
            chapter = chapter,
            mode = mode,
            direction = direction,
            orientationPolicy = orientationPolicy,
            pageCount = pageCount,
            position = positionFor(mode, index, 0.0),
            zoom = defaultZoom()
        )
    }

    fun restore(
        snapshot: MangaReaderSnapshot,
        currentChapter: MangaReaderChapterRef,
        currentPageCount: Int?
    ): MangaReaderState {
        if (
            snapshot.version != MangaReaderSnapshot.CURRENT_VERSION ||
            !snapshot.chapter.sameLogicalChapter(currentChapter)
        ) {
            return initial(
                chapter = currentChapter,
                mode = snapshot.mode,
                direction = snapshot.direction,
                orientationPolicy = snapshot.orientationPolicy,
                pageCount = currentPageCount
            )
        }

        val index = clampIndex(snapshot.itemIndex, currentPageCount)
        return MangaReaderState(
            chapter = currentChapter,
            mode = snapshot.mode,
            direction = snapshot.direction,
            orientationPolicy = snapshot.orientationPolicy,
            pageCount = currentPageCount,
            position = positionFor(snapshot.mode, index, snapshot.webtoonOffsetFraction),
            zoom = sanitizeZoom(snapshot.zoom)
        )
    }

    fun snapshot(state: MangaReaderState): MangaReaderSnapshot =
        MangaReaderSnapshot(
            chapter = state.chapter,
            mode = state.mode,
            direction = state.direction,
            orientationPolicy = state.orientationPolicy,
            itemIndex = state.position.itemIndex,
            webtoonOffsetFraction = (state.position as? MangaReaderPosition.Webtoon)
                ?.offsetFraction
                ?: 0.0,
            zoom = sanitizeZoom(state.zoom)
        )

    fun withPageCount(
        state: MangaReaderState,
        pageCount: Int
    ): MangaReaderState {
        require(pageCount > 0)
        val index = clampIndex(state.position.itemIndex, pageCount)
        val offset = (state.position as? MangaReaderPosition.Webtoon)?.offsetFraction ?: 0.0
        return state.copy(
            pageCount = pageCount,
            position = positionFor(state.mode, index, offset)
        )
    }

    fun setMode(
        state: MangaReaderState,
        mode: MangaReaderMode
    ): MangaReaderState {
        if (mode == state.mode) return state
        val index = state.position.itemIndex
        val offset = (state.position as? MangaReaderPosition.Webtoon)?.offsetFraction ?: 0.0
        return state.copy(
            mode = mode,
            position = positionFor(mode, index, offset),
            zoom = defaultZoom()
        )
    }

    fun setDirection(
        state: MangaReaderState,
        direction: MangaPageDirection
    ): MangaReaderState = state.copy(direction = direction)

    fun setOrientationPolicy(
        state: MangaReaderState,
        policy: MangaOrientationPolicy
    ): MangaReaderState = state.copy(orientationPolicy = policy)

    fun moveToItem(
        state: MangaReaderState,
        itemIndex: Int,
        webtoonOffsetFraction: Double = 0.0
    ): MangaReaderState {
        val index = clampIndex(itemIndex, state.pageCount)
        val position = positionFor(
            mode = state.mode,
            itemIndex = index,
            webtoonOffset = webtoonOffsetFraction
        )
        return state.copy(
            position = position,
            zoom = defaultZoom()
        )
    }

    fun updateWebtoonPosition(
        state: MangaReaderState,
        itemIndex: Int,
        offsetFraction: Double
    ): MangaReaderState {
        require(state.mode == MangaReaderMode.WEBTOON) {
            "Webtoon position can only update in WEBTOON mode"
        }
        val index = clampIndex(itemIndex, state.pageCount)
        val safeOffset = offsetFraction.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.0
        return state.copy(
            position = MangaReaderPosition.Webtoon(index, safeOffset)
        )
    }

    fun updateZoom(
        state: MangaReaderState,
        scale: Double,
        centerXFraction: Double,
        centerYFraction: Double
    ): MangaReaderState {
        val requested = MangaZoomState(
            scale = scale.takeIf(Double::isFinite)?.coerceIn(minZoom, maxZoom) ?: minZoom,
            centerXFraction = centerXFraction.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.5,
            centerYFraction = centerYFraction.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.5
        )
        return state.copy(zoom = requested)
    }

    fun resetZoom(state: MangaReaderState): MangaReaderState =
        state.copy(zoom = defaultZoom())

    fun advance(
        state: MangaReaderState,
        advance: MangaReaderAdvance
    ): MangaReaderTransition {
        val pageCount = state.pageCount
        val current = state.position.itemIndex
        val delta = if (advance == MangaReaderAdvance.FORWARD) 1 else -1
        val target = current + delta

        if (target < 0) {
            return MangaReaderTransition(state, MangaReaderBoundary.PREVIOUS_CHAPTER)
        }
        if (pageCount != null && target >= pageCount) {
            return MangaReaderTransition(state, MangaReaderBoundary.NEXT_CHAPTER)
        }

        val nextPosition = when (state.mode) {
            MangaReaderMode.PAGED -> MangaReaderPosition.Paged(target)
            MangaReaderMode.WEBTOON -> MangaReaderPosition.Webtoon(target, 0.0)
        }
        val nextZoom = if (state.mode == MangaReaderMode.PAGED) defaultZoom() else state.zoom
        return MangaReaderTransition(
            state = state.copy(position = nextPosition, zoom = nextZoom)
        )
    }

    fun advanceForTap(
        direction: MangaPageDirection,
        edge: ReaderEdge
    ): MangaReaderAdvance = when (direction) {
        MangaPageDirection.LEFT_TO_RIGHT ->
            if (edge == ReaderEdge.RIGHT) MangaReaderAdvance.FORWARD else MangaReaderAdvance.BACKWARD

        MangaPageDirection.RIGHT_TO_LEFT ->
            if (edge == ReaderEdge.LEFT) MangaReaderAdvance.FORWARD else MangaReaderAdvance.BACKWARD
    }

    fun advanceForGesture(
        direction: MangaPageDirection,
        gesture: HorizontalGesture
    ): MangaReaderAdvance = when (direction) {
        MangaPageDirection.LEFT_TO_RIGHT ->
            if (gesture == HorizontalGesture.SWIPE_LEFT) {
                MangaReaderAdvance.FORWARD
            } else {
                MangaReaderAdvance.BACKWARD
            }

        MangaPageDirection.RIGHT_TO_LEFT ->
            if (gesture == HorizontalGesture.SWIPE_RIGHT) {
                MangaReaderAdvance.FORWARD
            } else {
                MangaReaderAdvance.BACKWARD
            }
    }

    private fun positionFor(
        mode: MangaReaderMode,
        itemIndex: Int,
        webtoonOffset: Double
    ): MangaReaderPosition = when (mode) {
        MangaReaderMode.PAGED -> MangaReaderPosition.Paged(itemIndex)
        MangaReaderMode.WEBTOON -> MangaReaderPosition.Webtoon(
            itemIndex = itemIndex,
            offsetFraction = webtoonOffset.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.0
        )
    }

    private fun clampIndex(index: Int, pageCount: Int?): Int {
        require(pageCount == null || pageCount > 0) { "Page count must be positive when known" }
        val nonNegative = index.coerceAtLeast(0)
        return if (pageCount == null) nonNegative else nonNegative.coerceAtMost(pageCount - 1)
    }

    private fun sanitizeZoom(zoom: MangaZoomState): MangaZoomState =
        MangaZoomState(
            scale = zoom.scale.coerceIn(minZoom, maxZoom),
            centerXFraction = zoom.centerXFraction.coerceIn(0.0, 1.0),
            centerYFraction = zoom.centerYFraction.coerceIn(0.0, 1.0)
        )

    private fun defaultZoom(): MangaZoomState = MangaZoomState(scale = minZoom)
}
