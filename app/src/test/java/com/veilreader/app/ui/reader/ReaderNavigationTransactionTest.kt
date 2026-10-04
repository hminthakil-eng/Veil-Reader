package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNavigationTransactionTest {

    @Test
    fun navigationTransactionToken_neverUsesZeroOrNegativeSentinel() {
        assertEquals(1L, nextReaderNavigationTransactionToken(0L))
        assertEquals(8L, nextReaderNavigationTransactionToken(7L))
        assertEquals(1L, nextReaderNavigationTransactionToken(Long.MAX_VALUE))
    }

    @Test
    fun fastPdfFlush_preservesOnlyTruthfulPassageVisits() {
        val gate = ReaderNavigationTransactionGate()
        gate.begin("page1", 10L, expectedPdfPage = 3, originPdfPage = 1, passageVisitLocatorJson = "page3")
        val fastSwipe = requireNotNull(gate.consumeReachedPdfDestination(20L, 4))
        assertNull(fastSwipe.passageVisitAfterSettlement(4))
        assertEquals("page3", fastSwipe.passageVisitAfterSettlement(3))
        gate.begin("page1", 30L, expectedPdfPage = 3, passageVisitLocatorJson = "page3")
        val reached = requireNotNull(gate.consumeReachedPdfDestination(40L, 3))
        assertEquals("page3", reached.passageVisitAfterSettlement(3))
    }

    @Test
    fun convergedPdfJump_requiresBothDocumentPageAndStableTarget() {
        val gate = ReaderNavigationTransactionGate()
        val target = ReaderNavigationIdentity("book.pdf", 3, null, null)
        val transaction = gate.begin(
            originLocatorJson = "source",
            nowElapsedMs = 10L,
            targetIdentity = target,
            expectedPdfPage = 3,
            originPdfPage = 1,
            passageVisitLocatorJson = "saved-passage"
        )
        assertNull(gate.consumeSettled("intermediate", 20L, target.copy(position = 2), 3))
        assertNull(gate.consumeSettled("wrong-page", 30L, target, 2))
        val settled = gate.consumeSettled("destination", 40L, target, 3)
        assertEquals(transaction.token, settled?.token)
        assertEquals("saved-passage", settled?.passageVisitLocatorJson)
        assertFalse(gate.isActive(50L))
    }

    @Test
    fun pdfLifecycleSettlement_doesNotConsumeEpubTargetTransaction() {
        val gate = ReaderNavigationTransactionGate()
        val target = ReaderNavigationIdentity("chapter.xhtml", 8, null, null)
        gate.begin("source", 10L, targetIdentity = target)
        assertNull(gate.consumeReachedPdfDestination(20L, 3))
        assertTrue(gate.isActive(30L))
        assertNull(gate.consumeSettled("intermediate", 40L, target.copy(position = 5)))
        assertEquals("source", gate.consumeSettled("destination", 50L, target)?.originLocatorJson)
    }

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
    fun visualDeparture_detectsAdjacentViewportEvenWhenReadiumPositionChunkIsUnchanged() {
        val origin = ReaderNavigationIdentity(
            href = "chapter.xhtml",
            position = 12,
            cssSelector = null,
            totalProgression = 0.42,
            progression = 0.20
        )
        val nextViewport = origin.copy(progression = 0.25)

        assertTrue(
            readerNavigationIdentityMatchesTarget(
                observed = nextViewport,
                target = origin
            )
        )
        assertTrue(
            readerNavigationIdentityHasVisuallyDeparted(
                origin = origin,
                observed = nextViewport
            )
        )
    }

    @Test
    fun visualDeparture_ignoresTinyProgressionNoiseButDetectsResourceChange() {
        val origin = ReaderNavigationIdentity(
            href = "text/./chapter.xhtml",
            position = 12,
            cssSelector = null,
            totalProgression = 0.42,
            progression = 0.20
        )

        assertFalse(
            readerNavigationIdentityHasVisuallyDeparted(
                origin = origin,
                observed = origin.copy(
                    href = "text/chapter.xhtml",
                    progression = 0.20005
                )
            )
        )
        assertTrue(
            readerNavigationIdentityHasVisuallyDeparted(
                origin = origin,
                observed = origin.copy(
                    href = "text/chapter-02.xhtml",
                    progression = 0.20
                )
            )
        )
    }

    @Test
    fun targetLocator_ignoresIntermediateResource_untilTargetPositionArrives() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 2_000L)
        val origin = "origin"
        val target = ReaderNavigationIdentity(
            href = "chapter-04.xhtml",
            position = 12,
            cssSelector = null,
            totalProgression = 0.42
        )
        val started = gate.begin(
            originLocatorJson = origin,
            nowElapsedMs = 100L,
            targetIdentity = target
        )

        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "intermediate",
                nowElapsedMs = 300L,
                observedIdentity = ReaderNavigationIdentity(
                    href = "chapter-02.xhtml",
                    position = 6,
                    cssSelector = null,
                    totalProgression = 0.20
                )
            )
        )
        assertTrue(gate.isActive(nowElapsedMs = 350L))

        val settled = gate.consumeSettled(
            observedLocatorJson = "destination-with-renderer-metadata",
            nowElapsedMs = 600L,
            observedIdentity = target.copy(totalProgression = 0.421)
        )
        assertEquals(started.token, settled?.token)
    }

    @Test
    fun locatorTarget_matchesStableProgression_whenPresentationMetadataChanges() {
        val target = ReaderNavigationIdentity(
            href = "chapter.xhtml",
            position = null,
            cssSelector = null,
            totalProgression = 0.421
        )

        assertTrue(
            readerNavigationIdentityMatchesTarget(
                observed = target.copy(totalProgression = 0.422),
                target = target
            )
        )
        assertFalse(
            readerNavigationIdentityMatchesTarget(
                observed = target.copy(totalProgression = 0.44),
                target = target
            )
        )
    }

    @Test
    fun linkTarget_waitsForDestinationResource_andAllowsFragmentTarget() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 2_000L)
        val started = gate.begin(
            originLocatorJson = "origin",
            nowElapsedMs = 100L,
            targetHref = "chapter-04.xhtml#scene-2"
        )

        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "intermediate",
                nowElapsedMs = 300L,
                observedIdentity = ReaderNavigationIdentity(
                    href = "chapter-02.xhtml",
                    position = 5,
                    cssSelector = null,
                    totalProgression = 0.20
                )
            )
        )

        assertEquals(
            started.token,
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 500L,
                observedIdentity = ReaderNavigationIdentity(
                    href = "chapter-04.xhtml",
                    position = 12,
                    cssSelector = null,
                    totalProgression = 0.42
                )
            )?.token
        )
    }

    @Test
    fun passageRevisit_metadata_survivesOnlyUntilTheTargetSettles() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 2_000L)
        val targetJson = """{"href":"chapter-08.xhtml","locations":{"position":22}}"""
        val target = ReaderNavigationIdentity(
            href = "chapter-08.xhtml",
            position = 22,
            cssSelector = null,
            totalProgression = 0.80
        )
        gate.begin(
            originLocatorJson = "origin",
            nowElapsedMs = 100L,
            targetIdentity = target,
            passageVisitLocatorJson = targetJson
        )

        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "intermediate",
                nowElapsedMs = 300L,
                observedIdentity = ReaderNavigationIdentity(
                    href = "chapter-05.xhtml",
                    position = 12,
                    cssSelector = null,
                    totalProgression = 0.50
                )
            )
        )

        val settled = requireNotNull(
            gate.consumeSettled(
                observedLocatorJson = "destination",
                nowElapsedMs = 500L,
                observedIdentity = target
            )
        )
        assertEquals(targetJson, settled.passageVisitLocatorJson)
        assertNull(
            gate.consumeSettled(
                observedLocatorJson = "later",
                nowElapsedMs = 700L,
                observedIdentity = target.copy(position = 24)
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
    fun sameStableLocation_doesNotStartProgrammaticTransaction_whenMetadataDiffers() {
        val target = ReaderNavigationIdentity(
            href = "chapter.xhtml",
            position = 12,
            cssSelector = null,
            totalProgression = 0.42
        )
        assertFalse(
            shouldStartReaderIdentityJump(
                origin = target.copy(totalProgression = 0.421),
                target = target
            )
        )
        assertTrue(
            shouldStartReaderIdentityJump(
                origin = target.copy(position = 11),
                target = target
            )
        )
    }

    @Test
    fun fragmentOnlyTarget_resolvesAgainstCurrentResourceForTracking() {
        assertEquals(
            "text/chapter-04.xhtml#scene-2",
            readerEffectiveTargetHref(
                currentHref = "text/chapter-04.xhtml",
                targetHref = "#scene-2"
            )
        )
        assertEquals(
            "text/chapter-05.xhtml#scene-2",
            readerEffectiveTargetHref(
                currentHref = "text/chapter-04.xhtml",
                targetHref = "text/chapter-05.xhtml#scene-2"
            )
        )
        assertNull(
            readerEffectiveTargetHref(
                currentHref = "text/chapter-04.xhtml",
                targetHref = " "
            )
        )
        assertNull(
            readerEffectiveTargetHref(
                currentHref = null,
                targetHref = "#scene-2"
            )
        )
    }

    @Test
    fun sameChapterHref_doesNotStartNoOpLinkTransaction() {
        assertFalse(
            shouldStartReaderLinkJump(
                currentHref = "text/chapter-04.xhtml",
                targetHref = "text/chapter-04.xhtml"
            )
        )
        assertFalse(
            shouldStartReaderLinkJump(
                currentHref = "text/chapter-04.xhtml",
                targetHref = "text/./chapter-04.xhtml"
            )
        )
        assertTrue(
            shouldStartReaderLinkJump(
                currentHref = "text/chapter-04.xhtml",
                targetHref = "text/chapter-04.xhtml#scene-2"
            )
        )
        assertTrue(
            shouldStartReaderLinkJump(
                currentHref = null,
                targetHref = "text/chapter-04.xhtml"
            )
        )
        assertFalse(
            shouldStartReaderLinkJump(
                currentHref = "text/chapter-04.xhtml",
                targetHref = " "
            )
        )
    }

    @Test
    fun missingOrigin_stillAllowsKnownDestinationJump() {
        val target = ReaderNavigationIdentity(
            href = "destination.xhtml",
            position = 4,
            cssSelector = null,
            totalProgression = 0.4
        )
        assertTrue(
            shouldStartReaderIdentityJump(
                origin = null,
                target = target
            )
        )
        assertFalse(
            shouldStartReaderIdentityJump(
                origin = target,
                target = null
            )
        )
    }

    @Test
    fun pdfSourceEmission_cannotSettleDestinationOrOverwriteReturnHistory() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin("page1", 100L, expectedPdfPage = 3)
        assertNull(gate.consumeSettled(nowElapsedMs = 120L, observedPdfPage = 1))
        assertTrue(gate.isActive(121L))
        assertEquals(started.token, gate.consumeSettled(nowElapsedMs = 150L, observedPdfPage = 3)?.token)
        assertNull(gate.consumeSettled(nowElapsedMs = 600L, observedPdfPage = 3))
    }

    @Test
    fun fastPdfBackground_canFlushReachedDestinationWithoutWaitingForDebounce() {
        val gate = ReaderNavigationTransactionGate()
        val started = gate.begin("page1", 100L, expectedPdfPage = 3)
        assertNull(gate.consumeReachedPdfDestination(110L, observedPdfPage = 1))
        assertEquals(started.token, gate.consumeReachedPdfDestination(130L, observedPdfPage = 3)?.token)
        assertNull(gate.cancelActive(140L))
        assertNull(gate.consumeSettled(nowElapsedMs = 600L, observedPdfPage = 3))
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
    fun closeClassification_acceptsReachedDestination_butRejectsOriginAndIntermediate() {
        val target = ReaderNavigationIdentity(
            href = "chapter-04.xhtml",
            position = 12,
            cssSelector = null,
            totalProgression = 0.42
        )
        val transaction = ReaderNavigationTransaction(
            token = 1L,
            originLocatorJson = "origin",
            targetIdentity = target,
            targetHref = null,
            passageVisitLocatorJson = null,
            startedAtElapsedMs = 100L
        )

        assertTrue(
            transaction.hasReachedObservedDestination(
                observedLocatorJson = "destination",
                observedIdentity = target
            )
        )
        assertFalse(
            transaction.hasReachedObservedDestination(
                observedLocatorJson = "origin",
                observedIdentity = target.copy(position = 4)
            )
        )
        assertFalse(
            transaction.hasReachedObservedDestination(
                observedLocatorJson = "intermediate",
                observedIdentity = target.copy(position = 8)
            )
        )
    }

    @Test
    fun closeClassification_requiresExpectedPdfPage() {
        val target = ReaderNavigationIdentity("book.pdf", 3, null, null)
        val transaction = ReaderNavigationTransaction(
            token = 2L,
            originLocatorJson = "page1",
            targetIdentity = target,
            targetHref = null,
            passageVisitLocatorJson = null,
            startedAtElapsedMs = 100L,
            expectedPdfPage = 3,
            originPdfPage = 1
        )

        assertTrue(
            transaction.hasReachedObservedDestination(
                observedLocatorJson = "page3",
                observedIdentity = target,
                observedPdfPage = 3
            )
        )
        assertFalse(
            transaction.hasReachedObservedDestination(
                observedLocatorJson = "page2",
                observedIdentity = target,
                observedPdfPage = 2
            )
        )
    }

    @Test
    fun jumpCommit_persistsWithoutPageTurnCredit() {
        assertTrue(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.commitsLocator)
        assertFalse(ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT.countsPageTurn)
    }
}
