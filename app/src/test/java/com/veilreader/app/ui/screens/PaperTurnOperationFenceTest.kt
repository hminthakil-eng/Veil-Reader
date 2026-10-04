package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperTurnOperationFenceTest {

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
