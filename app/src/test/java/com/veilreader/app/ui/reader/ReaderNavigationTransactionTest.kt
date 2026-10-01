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
    fun sameLocation_doesNotStartProgrammaticTransaction() {
        assertFalse(
            shouldStartReaderLocationJump(
                originLocatorJson = "same",
                targetLocatorJson = "same"
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
