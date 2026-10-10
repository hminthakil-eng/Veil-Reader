package com.veilreader.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLocatorPolicyTest {

    @Test
    fun durabilityUpgradeDoesNotAwardAnotherPageTurn() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("opening"))
        assertFalse(gate.countsPageTurnFor("opening", ReaderLocatorEvent.NAVIGATOR_PAGE_TURN))
        assertTrue(gate.acceptCommit("opening", requireDurability = true))
        assertFalse(gate.countsPageTurnFor("opening", ReaderLocatorEvent.PAPER_COMMIT))
        assertTrue(gate.countsPageTurnFor("next", ReaderLocatorEvent.NAVIGATOR_PAGE_TURN))
        assertTrue(gate.countsPageTurnFor("previous", ReaderLocatorEvent.PAPER_COMMIT))
        assertFalse(gate.countsPageTurnFor("jump", ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT))
    }

    @Test
    fun rejectedDestinationRestoresDurabilityStrengthOfPreviousPosition() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("saved", requireDurability = true))
        assertTrue(gate.acceptCommit("failed"))
        gate.rejectCommit("failed")
        assertFalse(gate.acceptCommit("saved", requireDurability = true))
        assertTrue(gate.acceptCommit("failed", requireDurability = true))
    }

    @Test
    fun coalescedDuplicateDoesNotDowngradeDurablePosition() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("saved", requireDurability = true))
        assertFalse(gate.acceptCommit("saved"))
        assertFalse(gate.acceptCommit("saved", requireDurability = true))
    }

    @Test
    fun finalSnapshotPromotesCoalescedPositionToCrashDurableCommit() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("scroll"))
        assertFalse(gate.acceptCommit("scroll"))
        assertTrue(gate.acceptCommit("scroll", requireDurability = true))
        assertFalse(gate.acceptCommit("scroll", requireDurability = true))
    }

    @Test
    fun failedDurabilityPromotionRemainsRetryable() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("scroll"))
        assertTrue(gate.acceptCommit("scroll", requireDurability = true))
        gate.rejectCommit("scroll")
        assertTrue(gate.acceptCommit("scroll", requireDurability = true))
        assertFalse(gate.acceptCommit("scroll", requireDurability = true))
    }

    @Test
    fun movingAfterDurableCommitRequiresNewCheckpointAndResetClearsStrength() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("a", requireDurability = true))
        assertTrue(gate.acceptCommit("b"))
        assertTrue(gate.acceptCommit("b", requireDurability = true))
        gate.reset()
        assertTrue(gate.acceptCommit("b", requireDurability = true))
    }

    @Test
    fun durabilityRetryCanRecommitAnObservedDuplicateWithoutLosingSavedOrigin() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("saved"))
        assertTrue(gate.acceptCommit("observed"))
        assertFalse(gate.acceptCommit("observed"))
        assertTrue(gate.acceptCommit("observed", retryDurability = true))
        gate.rejectCommit("observed")
        assertFalse(gate.acceptCommit("saved"))
        assertTrue(gate.acceptCommit("observed"))
    }

    @Test
    fun rejectedSaveAllowsRetryAndPreservesPreviousCommittedDuplicate() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("saved"))
        assertTrue(gate.acceptCommit("failed"))
        gate.rejectCommit("failed")
        assertFalse(gate.acceptCommit("saved"))
        assertTrue(gate.acceptCommit("failed"))
    }

    @Test
    fun staleRejectionDoesNotReleaseNewerCommit() {
        val gate = ReaderLocatorDeduplicator()
        assertTrue(gate.acceptCommit("old"))
        assertTrue(gate.acceptCommit("new"))
        gate.rejectCommit("old")
        assertFalse(gate.acceptCommit("new"))
    }

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
        assertFalse(opening.bypassProgressDebounce)

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
    fun slidePreviewIsObservationUntilSettledCommitOwnsTheTurn() {
        val preview = readerObservedLocatorEvent(
            programmaticNavigationSettled = false,
            viewportRelayoutPending = false,
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false,
            isSlidePreviewActive = true
        )
        assertFalse(preview.commitsLocator)
        assertFalse(preview.countsPageTurn)

        val settled = readerObservedLocatorEvent(
            programmaticNavigationSettled = false,
            viewportRelayoutPending = false,
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false,
            isSlidePreviewActive = false
        )
        assertTrue(settled.commitsLocator)
        assertTrue(settled.countsPageTurn)
    }

    @Test
    fun programmaticSettlementStillOutranksSlidePreviewClassification() {
        val jump = readerObservedLocatorEvent(
            programmaticNavigationSettled = true,
            viewportRelayoutPending = false,
            isInitialEmission = false,
            isContinuousScroll = false,
            isPaperMode = false,
            isSlidePreviewActive = true
        )
        assertTrue(jump.commitsLocator)
        assertFalse(jump.countsPageTurn)
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
        assertFalse(relayout.bypassProgressDebounce)

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
    fun semanticCommitsBypassProgressDebounce_butScrollAndObservationsRemainCoalesced() {
        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.bypassProgressDebounce)
        assertFalse(ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT.bypassProgressDebounce)
        assertFalse(ReaderLocatorEvent.OPENING_CHECKPOINT.bypassProgressDebounce)
        assertFalse(ReaderLocatorEvent.RELAYOUT_CHECKPOINT.bypassProgressDebounce)

        assertTrue(ReaderLocatorEvent.NAVIGATOR_PAGE_TURN.bypassProgressDebounce)
        assertTrue(ReaderLocatorEvent.PAPER_COMMIT.bypassProgressDebounce)
        assertTrue(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.bypassProgressDebounce)
        assertTrue(ReaderLocatorEvent.FINAL_SNAPSHOT.bypassProgressDebounce)
    }

    @Test
    fun classifierNeverPromotesContinuousScrollIntoDebounceBypass() {
        val initialScroll = navigatorLocatorEvent(
            isInitialEmission = true,
            isContinuousScroll = true,
            isPaperMode = false
        )
        val laterScroll = navigatorLocatorEvent(
            isInitialEmission = false,
            isContinuousScroll = true,
            isPaperMode = false
        )
        assertTrue(initialScroll.commitsLocator)
        assertTrue(laterScroll.commitsLocator)
        assertFalse(initialScroll.bypassProgressDebounce)
        assertFalse(laterScroll.bypassProgressDebounce)
    }

    @Test
    fun programmaticAndFinalSettlementsAreCrashRecoveryClassWithoutPageTurnCredit() {
        val jump = ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT
        val finalSnapshot = ReaderLocatorEvent.FINAL_SNAPSHOT

        assertTrue(jump.commitsLocator)
        assertTrue(jump.bypassProgressDebounce)
        assertFalse(jump.countsPageTurn)

        assertTrue(finalSnapshot.commitsLocator)
        assertTrue(finalSnapshot.bypassProgressDebounce)
        assertFalse(finalSnapshot.countsPageTurn)
    }

    @Test
    fun locatorCommitAndPageTurnSemantics_areIndependent() {
        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATOR_POSITION.countsPageTurn)

        assertTrue(ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT.countsPageTurn)

        assertTrue(ReaderLocatorEvent.OPENING_CHECKPOINT.commitsLocator)
        assertFalse(ReaderLocatorEvent.OPENING_CHECKPOINT.countsPageTurn)
        assertFalse(ReaderLocatorEvent.OPENING_CHECKPOINT.bypassProgressDebounce)

        assertTrue(ReaderLocatorEvent.RELAYOUT_CHECKPOINT.commitsLocator)
        assertFalse(ReaderLocatorEvent.RELAYOUT_CHECKPOINT.countsPageTurn)
        assertFalse(ReaderLocatorEvent.RELAYOUT_CHECKPOINT.bypassProgressDebounce)

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
