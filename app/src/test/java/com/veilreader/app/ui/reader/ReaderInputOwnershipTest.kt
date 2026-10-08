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

    @Test fun navigatorCannotStartPaperPreview() {
        val navigator = ReaderInputOwnership().acquire(ReaderInputOwner.NAVIGATOR)!!
        assertNull(navigator.beginPreview())
        assertEquals(navigator, navigator.release(ReaderInputOwner.SLIDE))
    }
}
