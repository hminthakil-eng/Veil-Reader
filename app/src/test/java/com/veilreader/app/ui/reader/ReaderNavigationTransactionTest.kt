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
        val origin = """{"href":"chapter-01.xhtml","locations":{"position":1}}"""
        val target = """{"href":"chapter-04.xhtml","locations":{"position":12},"title":"Saved"}"""
        val started = gate.begin(
            originLocatorJson = origin,
            nowElapsedMs = 100L,
            targetLocatorJson = target
        )

        assertNull(
            gate.consumeSettled(
                observedLocatorJson =
                    """{"href":"chapter-02.xhtml","locations":{"position":6}}""",
                nowElapsedMs = 300L
            )
        )
        assertTrue(gate.isActive(nowElapsedMs = 350L))

        val settled = gate.consumeSettled(
            observedLocatorJson =
                """{"href":"chapter-04.xhtml","locations":{"position":12},"title":"Renderer title"}""",
            nowElapsedMs = 600L
        )
        assertEquals(started.token, settled?.token)
    }

    @Test
    fun passageRevisit_metadata_survivesOnlyUntilTheTargetSettles() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 2_000L)
        val origin = """{"href":"chapter-01.xhtml","locations":{"position":1}}"""
        val target = """{"href":"chapter-08.xhtml","locations":{"position":22}}"""
        gate.begin(
            originLocatorJson = origin,
            nowElapsedMs = 100L,
            targetLocatorJson = target,
            passageVisitLocatorJson = target
        )

        assertNull(
            gate.consumeSettled(
                observedLocatorJson =
                    """{"href":"chapter-05.xhtml","locations":{"position":12}}""",
                nowElapsedMs = 300L
            )
        )

        val settled = requireNotNull(
            gate.consumeSettled(
                observedLocatorJson =
                    """{"href":"chapter-08.xhtml","locations":{"position":22},"title":"Landed"}""",
                nowElapsedMs = 500L
            )
        )
        assertEquals(target, settled.passageVisitLocatorJson)
        assertNull(
            gate.consumeSettled(
                observedLocatorJson =
                    """{"href":"chapter-09.xhtml","locations":{"position":24}}""",
                nowElapsedMs = 700L
            )
        )
    }

    @Test
    fun locatorTarget_matchesStableProgression_whenPresentationMetadataChanges() {
        val target =
            """{"href":"chapter.xhtml","locations":{"totalProgression":0.421},"text":{"highlight":"old"}}"""
        val observed =
            """{"href":"chapter.xhtml","locations":{"totalProgression":0.422},"title":"Current"}"""

        assertTrue(
            readerLocatorMatchesTarget(
                observedLocatorJson = observed,
                targetLocatorJson = target
            )
        )
        assertFalse(
            readerLocatorMatchesTarget(
                observedLocatorJson =
                    """{"href":"chapter.xhtml","locations":{"totalProgression":0.44}}""",
                targetLocatorJson = target
            )
        )
    }

    @Test
    fun linkTarget_waitsForDestinationResource_andAllowsFragmentTarget() {
        val gate = ReaderNavigationTransactionGate(timeoutMs = 2_000L)
        val origin =
            """{"href":"chapter-01.xhtml","locations":{"position":1}}"""
        val started = gate.begin(
            originLocatorJson = origin,
            nowElapsedMs = 100L,
            targetHref = "chapter-04.xhtml#scene-2"
        )

        assertNull(
            gate.consumeSettled(
                observedLocatorJson =
                    """{"href":"chapter-02.xhtml","locations":{"position":5}}""",
                nowElapsedMs = 300L
            )
        )

        assertEquals(
            started.token,
            gate.consumeSettled(
                observedLocatorJson =
                    """{"href":"chapter-04.xhtml","locations":{"position":12}}""",
                nowElapsedMs = 500L
            )?.token
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
