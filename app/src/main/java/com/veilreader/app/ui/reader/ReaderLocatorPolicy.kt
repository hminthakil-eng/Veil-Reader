package com.veilreader.app.ui.reader

/**
 * Why a locator reached ReaderViewModel.
 *
 * Keeping this semantic prevents a generic locator callback from silently deciding
 * whether a movement should count as a paced page turn.
 */
internal enum class ReaderLocatorEvent(val countsPageTurn: Boolean) {
    NAVIGATOR_POSITION(false),
    NAVIGATOR_PAGE_TURN(true),
    PAPER_COMMIT(true),
    FINAL_SNAPSHOT(false)
}

/**
 * Suppresses only consecutive duplicate locator events.
 *
 * Revisiting an older location after moving elsewhere is accepted again.
 */
internal class ReaderLocatorDeduplicator {
    private var lastLocationKey: String? = null

    fun accept(locationKey: String): Boolean {
        if (locationKey == lastLocationKey) return false
        lastLocationKey = locationKey
        return true
    }

    fun reset() {
        lastLocationKey = null
    }
}
