package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTransitionAvailabilityTest {
    @Test
    fun disabledPaper_explainsRemap_withoutPretendingSlideIsPaper() {
        val requested = ReaderAppearance().withNavigationMode(ReaderNavigationMode.PAPER_CURL)
        assertEquals(
            ReaderTransitionUnavailableReason.PAPER_ROLLOUT_DISABLED,
            readerTransitionUnavailableReason(requested, BookFormat.EPUB, false, false)
        )
        val effective = applyMaterialPageRolloutToAppearance(requested, BookFormat.EPUB, false, false)
        assertEquals(ReaderNavigationMode.PAGED, effective.navigationMode)
        assertEquals(ReaderNavigationMode.PAPER_CURL, requested.navigationMode)
    }

    @Test
    fun fixedLayout_explainsEveryUnsupportedRemap_beforeRolloutReason() {
        ReaderNavigationMode.entries.forEach { mode ->
            val requested = ReaderAppearance().withNavigationMode(mode)
            assertEquals(
                if (mode == ReaderNavigationMode.PAGED) null
                else ReaderTransitionUnavailableReason.FIXED_LAYOUT_LEAF_UNSUPPORTED,
                readerTransitionUnavailableReason(requested, BookFormat.EPUB, true, false)
            )
            assertEquals(
                ReaderNavigationMode.PAGED,
                effectiveReaderAppearanceForPublication(requested, true).navigationMode
            )
        }
    }

    @Test
    fun availableModes_haveNoFalseUnavailabilityNotice() {
        ReaderNavigationMode.entries.forEach { mode ->
            val requested = ReaderAppearance().withNavigationMode(mode)
            assertNull(readerTransitionUnavailableReason(requested, BookFormat.EPUB, false, true))
            if (mode != ReaderNavigationMode.PAPER_CURL) {
                assertNull(readerTransitionUnavailableReason(requested, BookFormat.EPUB, false, false))
            }
            assertNull(readerTransitionUnavailableReason(requested, BookFormat.PDF, false, false))
        }
    }
}
