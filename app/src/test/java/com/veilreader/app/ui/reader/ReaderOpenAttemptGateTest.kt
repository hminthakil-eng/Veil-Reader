package com.veilreader.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderOpenAttemptGateTest {

    @Test
    fun newerAttemptSupersedesOlderSuspendCompletion() {
        val gate = ReaderOpenAttemptGate()
        val older = gate.begin("book-a", "session-a")
        val newer = gate.begin("book-b", "session-b")

        assertFalse(gate.isCurrent(older))
        assertTrue(gate.isCurrent(newer))
        assertFalse(gate.complete(older))
        assertTrue(gate.complete(newer))
    }

    @Test
    fun staleCancellationCannotCancelNewerAttempt() {
        val gate = ReaderOpenAttemptGate()
        val older = gate.begin("book", "session-a")
        val newer = gate.begin("book", "session-b")

        assertFalse(gate.cancel(older))
        assertTrue(gate.isCurrent(newer))
    }

    @Test
    fun cancelSessionOnlyCancelsMatchingOwner() {
        val gate = ReaderOpenAttemptGate()
        gate.begin("book", "session-b")

        assertNull(gate.cancelSession("session-a"))
        assertTrue(gate.cancelSession("session-b") != null)
    }
}
