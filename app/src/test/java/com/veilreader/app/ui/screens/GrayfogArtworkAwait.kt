package com.veilreader.app.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/** Compose idle excludes IO; keep the native sandbox alive through actual cover decode. */
internal fun ComposeContentTestRule.awaitGrayfogArtwork() {
    fun awaitDecodedCovers() {
        waitForIdle()
        waitUntil(timeoutMillis = 20_000) {
            onAllNodes(SemanticsMatcher.keyIsDefined(BookCoverArtworkReady), useUnmergedTree = true)
                .fetchSemanticsNodes().all { it.config[BookCoverArtworkReady] }
        }
    }
    awaitDecodedCovers()
    mainClock.advanceTimeBy(200)
    // Fade/layout advancement may compose another cover. Settle it as well.
    awaitDecodedCovers()
    waitForIdle()
}
