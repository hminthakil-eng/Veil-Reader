package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderInputOwnershipTest {
    @Test fun pendingPaperPreviewBlocksSlideAndProgrammaticNavigation() {
        val paper = ReaderInputOwnership().acquire(ReaderInputOwner.PAPER)!!
            .beginPreview()!!
        assertFalse(paper.canAcquire(ReaderInputOwner.SLIDE))
        assertNull(paper.acquire(ReaderInputOwner.PROGRAMMATIC))
        assertEquals(paper, paper.release(ReaderInputOwner.PAPER))
    }

    @Test fun ownershipTransfersOnlyAfterPreviewSettles() {
        val paper = ReaderInputOwnership().acquire(ReaderInputOwner.PAPER)!!
            .beginPreview()!!
        val settled = paper.finishPreview().release(ReaderInputOwner.PAPER)
        assertTrue(settled.canAcquire(ReaderInputOwner.SLIDE))
        assertEquals(ReaderInputOwner.SLIDE, settled.acquire(ReaderInputOwner.SLIDE)?.owner)
    }

    @Test fun competingOwnerCannotStealIdleNavigator() {
        val navigator = ReaderInputOwnership().acquire(ReaderInputOwner.NAVIGATOR)!!
        assertFalse(navigator.canAcquire(ReaderInputOwner.PAPER))
        assertFalse(navigator.canAcquire(ReaderInputOwner.PROGRAMMATIC))
        assertNull(navigator.acquire(ReaderInputOwner.SLIDE))
        assertEquals(ReaderInputOwner.NAVIGATOR, navigator.owner)
    }

    @Test fun sameOwnerCannotBypassPendingPreview() {
        val paper = ReaderInputOwnership().acquire(ReaderInputOwner.PAPER)!!
            .beginPreview()!!
        assertFalse(paper.canAcquire(ReaderInputOwner.PAPER))
        assertNull(paper.acquire(ReaderInputOwner.PAPER))
    }

    @Test fun duplicatePreviewStartIsRejectedUntilFinish() {
        val paper = ReaderInputOwnership().acquire(ReaderInputOwner.PAPER)!!
        val preview = paper.beginPreview()!!
        assertNull(preview.beginPreview())
        assertEquals(preview, preview.release(ReaderInputOwner.PAPER))
        val finished = preview.finishPreview()
        assertEquals(true, finished.beginPreview()?.previewPending)
    }

    @Test fun slidePreviewHasSameReentryProtection() {
        val slide = ReaderInputOwnership().acquire(ReaderInputOwner.SLIDE)!!
            .beginPreview()!!
        assertNull(slide.beginPreview())
        assertFalse(slide.canAcquire(ReaderInputOwner.NAVIGATOR))
    }

    @Test fun explicitReleaseAllowsHandoff() {
        val navigator = ReaderInputOwnership().acquire(ReaderInputOwner.NAVIGATOR)!!
        val released = navigator.release(ReaderInputOwner.NAVIGATOR)
        assertEquals(ReaderInputOwner.NONE, released.owner)
        assertEquals(ReaderInputOwner.PROGRAMMATIC,
            released.acquire(ReaderInputOwner.PROGRAMMATIC)?.owner)
    }

    @Test fun navigatorCannotStartPaperPreview() {
        val navigator = ReaderInputOwnership().acquire(ReaderInputOwner.NAVIGATOR)!!
        assertNull(navigator.beginPreview())
        assertEquals(navigator, navigator.release(ReaderInputOwner.SLIDE))
    }
}
