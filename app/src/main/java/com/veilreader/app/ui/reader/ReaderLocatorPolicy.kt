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
     * Session-owned saves must first succeed in the existing atomic crash checkpoint, then enter
     * the serialized Room queue immediately. This does not add another long-term progress store.
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
    OPENING_CHECKPOINT(
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
    isInitialEmission -> ReaderLocatorEvent.OPENING_CHECKPOINT
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
    private var previousCommittedLocationKey: String? = null
    private var lastCommitRequiresDurability = false
    private var previousCommitRequiresDurability = false

    /** Persisting an unchanged location more strongly is not another page turn. */
    fun countsPageTurnFor(locationKey: String, event: ReaderLocatorEvent): Boolean =
        event.countsPageTurn && locationKey != lastCommittedLocationKey

    fun acceptCommit(
        locationKey: String,
        retryDurability: Boolean = false,
        requireDurability: Boolean = false
    ): Boolean {
        if (locationKey == lastCommittedLocationKey) {
            // A scroll/opening checkpoint may still be coalesced. Its final snapshot must
            // reach the crash journal even when the semantic location has not changed.
            if (!retryDurability && !(requireDurability && !lastCommitRequiresDurability)) {
                return false
            }
            lastCommitRequiresDurability = lastCommitRequiresDurability || requireDurability
            return true
        }
        previousCommittedLocationKey = lastCommittedLocationKey
        previousCommitRequiresDurability = lastCommitRequiresDurability
        lastCommittedLocationKey = locationKey
        lastCommitRequiresDurability = requireDurability
        return true
    }

    /** Roll back only the pending owner, allowing retry without forgetting the last saved place. */
    fun rejectCommit(locationKey: String) {
        if (lastCommittedLocationKey == locationKey) {
            lastCommittedLocationKey = previousCommittedLocationKey
            lastCommitRequiresDurability = previousCommitRequiresDurability
            previousCommittedLocationKey = null
            previousCommitRequiresDurability = false
        }
    }

    fun reset() {
        lastCommittedLocationKey = null
        previousCommittedLocationKey = null
        lastCommitRequiresDurability = false
        previousCommitRequiresDurability = false
    }
}
