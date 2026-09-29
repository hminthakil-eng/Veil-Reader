package com.veilreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GameCompletionEvidenceTest {
    @Test
    fun `finished library is a completion evidence floor for legacy data`() {
        assertEquals(7, completionEvidenceFloor(finishedBooks = 7, sealedCycles = 0))
    }

    @Test
    fun `sealed reread cycles preserve more history than current finished shelf`() {
        assertEquals(12, completionEvidenceFloor(finishedBooks = 5, sealedCycles = 12))
    }

    @Test
    fun `negative imported counters never reduce durable history`() {
        assertEquals(0, completionEvidenceFloor(finishedBooks = -3, sealedCycles = -1))
    }
}
