package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNavigationTransactionTest {

    @Test
    fun firstDifferentLocator_consumesProgrammaticTransactionOnce() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin(originLocatorJson = "origin", nowElapsedMs = 100L)

        val settled = gate.consumeSettled(
            observedLocatorJson = "destination",
            nowElapsedMs = 600L
        )

        assertEquals(started.token, settled?.token)
        assertEquals("origin", settled?.originLocatorJson)
        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "later",
                nowElapsedMs = 700L
            )
        )
    }

    @Test
    fun originReEmission_doesNotPrematurelySettleJump() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 1_000L)
        val started = gate.begin(originLocatorJson = "origin", nowElapsedMs = 100L)

        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "origin",
                nowElapsedMs = 400L
            )
        )
        assertTrue(gate.isActive(nowElapsedMs = 450L))

        val settled = gate.consumeSettled(
            observedLocatorJson = "destination",
            nowElapsedMs = 600L
        )
        assertEquals(started.token, settled?.token)
        assertFalse(gate.isActive(nowElapsedMs = 650L))
    }

    @Test
    fun missingOrigin_allowsFirstObservedLocatorToSettle() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin(originLocatorJson = null, nowElapsedMs = 100L)

        assertEquals(
            started.token,
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 300L
            )?.token
        )
    }

    @Test
    fun staleCancel_cannotCancelNewerJump() {
        val gate = ReaderNavigationTransactionGate()
        val first = gate.begin("a", nowElapsedMs = 100L)
        val second = gate.begin("b", nowElapsedMs = 200L)

        gate.cancel(first.token)

        assertEquals(
            second.token,
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 500L
            )?.token
        )
    }

    @Test
    fun currentCancel_removesPendingJump() {
        val gate = ReaderNavigationTransactionGate()
        val current = gate.begin("a", nowElapsedMs = 100L)

        gate.cancel(current.token)

        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 500L
            )
        )
    }

    @Test
    fun activeTransaction_canBeObservedWithoutConsumingIt() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 1_000L)
        val transaction = gate.begin("origin", nowElapsedMs = 100L)

        assertTrue(gate.isActive(nowElapsedMs = 500L))
        assertEquals(
            transaction.token,
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 600L
            )?.token
        )
    }

    @Test
    fun closeCanCancelFreshTransactionWithoutSettlingIt() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 1_000L)
        val transaction = gate.begin("origin", nowElapsedMs = 100L)

        assertEquals(transaction.token, gate.cancelActive(nowElapsedMs = 500L)?.token)
        assertFalse(gate.isActive(nowElapsedMs = 600L))
    }

    @Test
    fun expiredTransaction_doesNotCaptureLaterUserTurn() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 1_000L)
        gate.begin(originLocatorJson = "origin", nowElapsedMs = 100L)

        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 1_101L
            )
        )
    }

    @Test
    fun sameLocation_doesNotStartProgrammaticTransaction() {
        assertFalse(
            shouldStartReaderLocationJump(
                originLocatorJson = "same",
                targetLocatorJson = "same"
            )
        )
    }

    @Test
    fun missingOrigin_stillAllowsKnownDestinationJump() {
        assertTrue(
            shouldStartReaderLocationJump(
                originLocatorJson = null,
                targetLocatorJson = "destination"
            )
        )
        assertFalse(
            shouldStartReaderLocationJump(
                originLocatorJson = "origin",
                targetLocatorJson = null
            )
        )
        assertFalse(
            shouldStartReaderLocationJump(
                originLocatorJson = "origin",
                targetLocatorJson = ""
            )
        )
    }

    @Test
    fun jumpCommit_persistsWithoutPageTurnCredit() {
        assertTrue(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.countsPageTurn)
    }
}
