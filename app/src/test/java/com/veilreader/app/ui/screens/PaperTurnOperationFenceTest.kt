package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperTurnOperationFenceTest {

    @Test
    fun `cancelled worker cannot clear or restore a newer turn even after it completes`() {
        assertTrue(readerTurnMayCleanCancelledOperation(12L, 13L, 0L))
        assertFalse(readerTurnMayCleanCancelledOperation(12L, 14L, 14L))
        assertFalse(readerTurnMayCleanCancelledOperation(12L, 14L, 0L))
        assertFalse(readerTurnMayCleanCancelledOperation(0L, 1L, 0L))
        assertTrue(readerTurnMayCleanCancelledOperation(Long.MAX_VALUE, 1L, 0L))
    }

    @Test
    fun `operation generation advances and survives rollover`() {
        assertEquals(8L, nextPaperTurnOperationGeneration(7L))
        assertEquals(1L, nextPaperTurnOperationGeneration(Long.MAX_VALUE))
    }

    @Test
    fun `only exact active operation token may commit`() {
        assertTrue(
            paperTurnOperationIsCurrent(
                operationToken = 12L,
                activeOperationToken = 12L
            )
        )
        assertFalse(
            paperTurnOperationIsCurrent(
                operationToken = 11L,
                activeOperationToken = 12L
            )
        )
        assertFalse(
            paperTurnOperationIsCurrent(
                operationToken = 12L,
                activeOperationToken = 0L
            )
        )
        assertFalse(
            paperTurnOperationIsCurrent(
                operationToken = 0L,
                activeOperationToken = 0L
            )
        )
    }
}
