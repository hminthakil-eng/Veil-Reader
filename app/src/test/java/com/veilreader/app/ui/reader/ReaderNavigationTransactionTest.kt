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
        val started = gate.begin(originLocatorJson = "origin", nowElapsedMs = 100L)

        val settled = gate.consumeSettled(nowElapsedMs = 600L)

        assertEquals(started.token, settled?.token)
        assertEquals("origin", settled?.originLocatorJson)
        assertNull(gate.consumeSettled(nowElapsedMs = 500L))
    }

    @Test
    fun staleCancel_cannotCancelNewerJump() {
        val gate = ReaderNavigationTransactionGate()
        val first = gate.begin("a", nowElapsedMs = 100L)
        val second = gate.begin("b", nowElapsedMs = 200L)

        gate.cancel(first.token)

        assertEquals(second.token, gate.consumeSettled(nowElapsedMs = 500L)?.token)
    }

    @Test
    fun currentCancel_removesPendingJump() {
        val gate = ReaderNavigationTransactionGate()
        val current = gate.begin("a", nowElapsedMs = 100L)

        gate.cancel(current.token)

        assertNull(gate.consumeSettled(nowElapsedMs = 500L))
    }

    @Test
    fun activeTransaction_canBeObservedWithoutConsumingIt() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 1_000L)
        val transaction = gate.begin("origin", nowElapsedMs = 100L)

        assertTrue(gate.isActive(nowElapsedMs = 500L))
        assertEquals(transaction.token, gate.consumeSettled(nowElapsedMs = 600L)?.token)
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

        assertNull(gate.consumeSettled(nowElapsedMs = 1_101L))
    }

    @Test
    fun pdfSourceEmission_cannotSettleDestinationOrOverwriteReturnHistory() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin("page1", 100L, expectedPdfPage = 3)
        assertNull(gate.consumeSettled(120L, observedPdfPage = 1))
        assertTrue(gate.isActive(121L))
        assertEquals(started.token, gate.consumeSettled(150L, observedPdfPage = 3)?.token)
        assertNull(gate.consumeSettled(600L, observedPdfPage = 3))
    }

    @Test
    fun fastPdfBackground_canFlushReachedDestinationWithoutWaitingForDebounce() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin("page1", 100L, expectedPdfPage = 3)
        assertNull(gate.consumeReachedPdfDestination(110L, observedPdfPage = 1))
        assertEquals(started.token, gate.consumeReachedPdfDestination(130L, observedPdfPage = 3)?.token)
        assertNull(gate.cancelActive(140L))
        assertNull(gate.consumeSettled(600L, observedPdfPage = 3))
    }

    @Test
    fun nativeSwipeImmediatelyAfterPdfLink_cannotMakeFastCloseDiscardTheActualPage() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin("page1", 100L, expectedPdfPage = 3, originPdfPage = 1)
        assertNull(gate.consumeReachedPdfDestination(110L, observedPdfPage = 1))
        // StateFlow may conflate page 3 with a fast native swipe to page 4 before UI debounce.
        assertEquals(started.token, gate.consumeReachedPdfDestination(130L, observedPdfPage = 4)?.token)
        assertFalse(gate.isActive(140L))
        assertNull(gate.cancelActive(150L))
    }

    @Test
    fun lifecyclePdfFlush_cannotSettleAnOrdinaryOrExpiredTransaction() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 100L)
        gate.begin("epub", 100L)
        assertNull(gate.consumeReachedPdfDestination(120L, observedPdfPage = 3))
        assertTrue(gate.isActive(125L))
        gate.begin("pdf", 130L, expectedPdfPage = 3)
        assertNull(gate.consumeReachedPdfDestination(231L, observedPdfPage = 3))
        assertFalse(gate.isActive(232L))
    }

    @Test
    fun jumpCommit_persistsWithoutPageTurnCredit() {
        assertTrue(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.countsPageTurn)
    }
}
