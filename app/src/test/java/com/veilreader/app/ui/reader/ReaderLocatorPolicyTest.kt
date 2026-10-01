package com.veilreader.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLocatorPolicyTest {

    @Test
    fun consecutiveCommittedDuplicate_isRejected_butRevisitAfterMovementIsAccepted() {
        val gate = ReaderLocatorDeduplicator()

        assertTrue(gate.acceptCommit("book:a"))
        assertFalse(gate.acceptCommit("book:a"))
        assertTrue(gate.acceptCommit("book:b"))
        assertTrue(gate.acceptCommit("book:a"))
    }

    @Test
    fun reset_allowsCurrentCommittedLocationAgainForNewReaderSession() {
        val gate = ReaderLocatorDeduplicator()

        assertTrue(gate.acceptCommit("book:a"))
        assertFalse(gate.acceptCommit("book:a"))
        gate.reset()
        assertTrue(gate.acceptCommit("book:a"))
    }

    @Test
    fun observationCannotConsumeLaterCommit() {
        val gate = ReaderLocatorDeduplicator()

        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.commitsLocator)
        // Observation never enters the commit gate.
        assertTrue(gate.acceptCommit("book:a"))
        assertFalse(gate.acceptCommit("book:a"))
    }

    @Test
    fun firstNavigatorPositionIsCheckpointWithoutPageReward() {
        val opening = navigatorLocatorEvent(
            isInitialEmission = true,
            isContinuousScroll = false,
            isPaperMode = false
        )
        assertTrue(opening.commitsLocator)
        assertFalse(opening.countsPageTurn)

        val turn = navigatorLocatorEvent(
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false
        )
        assertTrue(turn.countsPageTurn)

        val scroll = navigatorLocatorEvent(
            isInitialEmission = true,
            isContinuousScroll = true,
            isPaperMode = false
        )
        assertTrue(scroll.commitsLocator)
        assertFalse(scroll.countsPageTurn)

        val paperObservation = navigatorLocatorEvent(
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = true
        )
        assertFalse(paperObservation.commitsLocator)
    }

    @Test
    fun viewportRelayout_isCheckpointWithoutPageTurnCredit() {
        val relayout = readerObservedLocatorEvent(
            programmaticNavigationSettled = false,
            viewportRelayoutPending = true,
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false
        )
        assertTrue(relayout.commitsLocator)
        assertFalse(relayout.countsPageTurn)

        val userTurn = readerObservedLocatorEvent(
            programmaticNavigationSettled = false,
            viewportRelayoutPending = false,
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false
        )
        assertTrue(userTurn.countsPageTurn)

        val jumpWins = readerObservedLocatorEvent(
            programmaticNavigationSettled = true,
            viewportRelayoutPending = true,
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false
        )
        assertTrue(jumpWins.commitsLocator)
        assertFalse(jumpWins.countsPageTurn)
    }

    @Test
    fun locatorCommitAndPageTurnSemantics_areIndependent() {
        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.countsPageTurn)

        assertTrue(ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT.countsPageTurn)

        assertTrue(ReaderLocatorEvent.NAVIGATOR_PAGE_TURN.commitsLocator)
        assertTrue(ReaderLocatorEvent.NAVIGATOR_PAGE_TURN.countsPageTurn)

        assertTrue(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.countsPageTurn)

        assertTrue(ReaderLocatorEvent.PAPER_COMMIT.commitsLocator)
        assertTrue(ReaderLocatorEvent.PAPER_COMMIT.countsPageTurn)

        assertTrue(ReaderLocatorEvent.FINAL_SNAPSHOT.commitsLocator)
        assertFalse(ReaderLocatorEvent.FINAL_SNAPSHOT.countsPageTurn)
    }
}
