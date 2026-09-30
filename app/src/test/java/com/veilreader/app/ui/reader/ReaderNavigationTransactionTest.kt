package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNavigationTransactionTest {

    @Test
    fun firstSettledLocator_consumesProgrammaticTransactionOnce() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin(originLocatorJson = "origin")

        val settled = gate.consumeSettled()

        assertEquals(started.token, settled?.token)
        assertEquals("origin", settled?.originLocatorJson)
        assertNull(gate.consumeSettled())
    }

    @Test
    fun staleCancel_cannotCancelNewerJump() {
        val gate = ReaderNavigationTransactionGate()
        val first = gate.begin("a")
        val second = gate.begin("b")

        gate.cancel(first.token)

        assertEquals(second.token, gate.consumeSettled()?.token)
    }

    @Test
    fun currentCancel_removesPendingJump() {
        val gate = ReaderNavigationTransactionGate()
        val current = gate.begin("a")

        gate.cancel(current.token)

        assertNull(gate.consumeSettled())
    }

    @Test
    fun jumpCommit_persistsWithoutPageTurnCredit() {
        assertTrue(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.countsPageTurn)
    }
}
