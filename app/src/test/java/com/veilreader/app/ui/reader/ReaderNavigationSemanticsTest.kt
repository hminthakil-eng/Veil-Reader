package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderNavigationSemanticsTest {

    @Test
    fun explorationReasons_preserveDurableReadingAnchor() {
        val explorationReasons = listOf(
            ReaderNavigationReason.SEARCH_RESULT,
            ReaderNavigationReason.TABLE_OF_CONTENTS,
            ReaderNavigationReason.FOOTNOTE,
            ReaderNavigationReason.BOOKMARK,
            ReaderNavigationReason.HIGHLIGHT,
            ReaderNavigationReason.PAGE_PREVIEW,
            ReaderNavigationReason.REFERENCE,
            ReaderNavigationReason.INTERNAL_LINK,
            ReaderNavigationReason.PDF_INTERNAL_LINK,
            ReaderNavigationReason.LISTENING_POSITION
        )

        explorationReasons.forEach { reason ->
            assertEquals(
                reason.name,
                ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR,
                readerNavigationIntentFor(reason).commitPolicy
            )
        }
    }

    @Test
    fun explicitRestoreAndReturn_canCommitOnSettlement() {
        listOf(
            ReaderNavigationReason.RETURN_PREVIOUS,
            ReaderNavigationReason.RESTORE
        ).forEach { reason ->
            assertEquals(
                ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT,
                readerNavigationIntentFor(reason).commitPolicy
            )
        }
    }

    @Test
    fun unknownRemainsCompatibilityCommit_untilCallSiteIsClassified() {
        assertEquals(
            ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT,
            readerNavigationIntentFor(ReaderNavigationReason.UNKNOWN).commitPolicy
        )
    }
}
