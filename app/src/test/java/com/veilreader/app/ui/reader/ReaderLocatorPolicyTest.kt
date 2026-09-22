package com.veilreader.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLocatorPolicyTest {

    @Test
    fun consecutiveDuplicate_isRejected_butRevisitAfterMovementIsAccepted() {
        val gate = ReaderLocatorDeduplicator()

        assertTrue(gate.accept("book:a"))
        assertFalse(gate.accept("book:a"))
        assertTrue(gate.accept("book:b"))
        assertTrue(gate.accept("book:a"))
    }

    @Test
    fun reset_allowsCurrentLocationAgainForNewReaderSession() {
        val gate = ReaderLocatorDeduplicator()

        assertTrue(gate.accept("book:a"))
        assertFalse(gate.accept("book:a"))
        gate.reset()
        assertTrue(gate.accept("book:a"))
    }

    @Test
    fun onlyCommittedPageEventsCountAsPageTurns() {
        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.countsPageTurn)
        assertTrue(ReaderLocatorEvent.NAVIGATOR_PAGE_TURN.countsPageTurn)
        assertTrue(ReaderLocatorEvent.PAPER_COMMIT.countsPageTurn)
        assertFalse(ReaderLocatorEvent.FINAL_SNAPSHOT.countsPageTurn)
    }
}
