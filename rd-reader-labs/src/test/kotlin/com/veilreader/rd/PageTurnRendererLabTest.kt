package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageTurnRendererLabTest {
    private val budget = PageTurnPerformanceBudget(
        captureP95Ms = 8.0,
        frameP95Ms = 16.7,
        memoryMiB = 16.0
    )

    @Test fun gateRejectsAnyBudgetRegression() {
        assertTrue(PageTurnGate.passes(PageTurnMeasurement(7.0, 16.0, 12.0), budget))
        assertFalse(PageTurnGate.passes(PageTurnMeasurement(9.0, 16.0, 12.0), budget))
    }
}
