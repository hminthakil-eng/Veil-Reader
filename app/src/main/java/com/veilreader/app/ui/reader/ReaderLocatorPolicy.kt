package com.veilreader.app.ui.reader

/**
 * Why a locator reached ReaderViewModel.
 *
 * Observation and commitment are deliberately separate. A navigator can emit a position while an
 * animation/gesture is still settling; that observation must never consume the later real commit.
 */
internal enum class ReaderLocatorEvent(
    val commitsLocator: Boolean,
    val countsPageTurn: Boolean,
    /**
     * Skip the 250 ms progress coalescer for semantic commits that users perceive as complete.
     *
     * This only moves the write into the existing serialized Room queue immediately; it does not
     * claim a synchronous fsync or create a second progress writer.
     */
    val bypassProgressDebounce: Boolean
) {
    NAVIGATOR_POSITION(
        commitsLocator = false,
        countsPageTurn = false,
        bypassProgressDebounce = false
    ),
    NAVIGATOR_SCROLL_COMMIT(
        commitsLocator = true,
        countsPageTurn = false,
        bypassProgressDebounce = false
    ),
    RELAYOUT_CHECKPOINT(
        commitsLocator = true,
        countsPageTurn = false,
        bypassProgressDebounce = false
    ),
    NAVIGATOR_PAGE_TURN(
        commitsLocator = true,
        countsPageTurn = true,
        bypassProgressDebounce = true
    ),
    NAVIGATION_JUMP_COMMIT(
        commitsLocator = true,
        countsPageTurn = false,
        bypassProgressDebounce = true
    ),
    PAPER_COMMIT(
        commitsLocator = true,
        countsPageTurn = true,
        bypassProgressDebounce = true
    ),
    FINAL_SNAPSHOT(
        commitsLocator = true,
        countsPageTurn = false,
        bypassProgressDebounce = true
    )
}

/** The navigator's first position is an opening checkpoint, even if loading took a long time. */
internal fun navigatorLocatorEvent(
    isInitialEmission: Boolean,
    isContinuousScroll: Boolean,
    isPaperMode: Boolean
): ReaderLocatorEvent = when {
    isContinuousScroll -> ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT
    isInitialEmission -> ReaderLocatorEvent.FINAL_SNAPSHOT
    isPaperMode -> ReaderLocatorEvent.NAVIGATOR_POSITION
    else -> ReaderLocatorEvent.NAVIGATOR_PAGE_TURN
}

internal fun readerObservedLocatorEvent(
    programmaticNavigationSettled: Boolean,
    viewportRelayoutPending: Boolean,
    isInitialEmission: Boolean,
    isContinuousScroll: Boolean,
    isPaperMode: Boolean,
    isSlidePreviewActive: Boolean = false
): ReaderLocatorEvent =
    when {
        programmaticNavigationSettled -> ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT
        viewportRelayoutPending -> ReaderLocatorEvent.RELAYOUT_CHECKPOINT
        isSlidePreviewActive -> ReaderLocatorEvent.NAVIGATOR_POSITION
        else -> navigatorLocatorEvent(
            isInitialEmission = isInitialEmission,
            isContinuousScroll = isContinuousScroll,
            isPaperMode = isPaperMode
        )
    }

internal data class ReaderLocatorCommit(
    val sequence: Long,
    val locatorJson: String,
    val progression: Float
)

/**
 * Suppresses only consecutive duplicate committed locators.
 *
 * Observations never enter this gate, so an observation cannot swallow a later paper/page commit.
 * Revisiting an older location after moving elsewhere is accepted again.
 */
internal class ReaderLocatorDeduplicator {
    private var lastCommittedLocationKey: String? = null

    fun acceptCommit(locationKey: String): Boolean {
        if (locationKey == lastCommittedLocationKey) return false
        lastCommittedLocationKey = locationKey
        return true
    }

    fun reset() {
        lastCommittedLocationKey = null
    }
}
