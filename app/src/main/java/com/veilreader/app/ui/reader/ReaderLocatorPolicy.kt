package com.veilreader.app.ui.reader

/**
 * Why a locator reached ReaderViewModel.
 *
 * Observation and commitment are deliberately separate. A navigator can emit a position while an
 * animation/gesture is still settling; that observation must never consume the later real commit.
 */
internal enum class ReaderLocatorEvent(
    val commitsLocator: Boolean,
    val countsPageTurn: Boolean
) {
    NAVIGATOR_POSITION(commitsLocator = false, countsPageTurn = false),
    NAVIGATOR_SCROLL_COMMIT(commitsLocator = true, countsPageTurn = false),
    NAVIGATOR_PAGE_TURN(commitsLocator = true, countsPageTurn = true),
    PAPER_COMMIT(commitsLocator = true, countsPageTurn = true),
    FINAL_SNAPSHOT(commitsLocator = true, countsPageTurn = false)
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
