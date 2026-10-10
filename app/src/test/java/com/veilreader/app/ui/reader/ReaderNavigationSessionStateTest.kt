package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderNavigationSessionStateTest {

    @Test
    fun repeatedScrollDestinationDoesNotPromoteExplorationWithoutMovement() {
        val machine = ReaderNavigationSessionStateMachine("reading-anchor")
        machine.onProgrammaticSettlement(
            ReaderNavigationTransaction(
                token = 7L,
                originLocatorJson = "reading-anchor",
                targetIdentity = null,
                targetHref = "chapter.xhtml#result",
                passageVisitLocatorJson = null,
                startedAtElapsedMs = 7L,
                reason = ReaderNavigationReason.SEARCH_RESULT,
                commitPolicy = ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR
            ),
            "search-result"
        )

        assertFalse(machine.mayCommitObservedEvent(
            ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT, "search-result"
        ))
        assertEquals("reading-anchor", machine.locatorForDurabilityFallback("search-result"))
        assertTrue(machine.mayCommitObservedEvent(
            ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT, "next-reading-position"
        ))
        machine.onDurableReadingCommitAccepted("next-reading-position", accepted = false)
        assertTrue(machine.state.isExploring)
        machine.onDurableReadingCommitAccepted("next-reading-position", accepted = true)
        assertFalse(machine.state.isExploring)
        assertEquals("next-reading-position", machine.state.readingAnchorJson)
    }

    @Test
    fun explorationSettlement_preservesReadingAnchor() {
        val machine = ReaderNavigationSessionStateMachine("anchor")
        val transaction = ReaderNavigationTransaction(
            token = 1L,
            originLocatorJson = "anchor",
            targetIdentity = null,
            targetHref = "chapter.xhtml#result",
            passageVisitLocatorJson = null,
            startedAtElapsedMs = 1L,
            reason = ReaderNavigationReason.SEARCH_RESULT,
            commitPolicy = ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR
        )

        machine.onProgrammaticSettlement(transaction, "result")

        assertEquals("anchor", machine.state.readingAnchorJson)
        assertEquals("result", machine.state.explorationLocatorJson)
        assertTrue(machine.state.isExploring)
        assertEquals("anchor", machine.locatorForDurabilityFallback("result"))
    }

    @Test
    fun closeDuringExploration_fallsBackToDurableAnchor() {
        val machine = ReaderNavigationSessionStateMachine("chapter-3")
        machine.onProgrammaticSettlement(
            ReaderNavigationTransaction(
                token = 2L,
                originLocatorJson = "chapter-3",
                targetIdentity = null,
                targetHref = "notes.xhtml#7",
                passageVisitLocatorJson = null,
                startedAtElapsedMs = 2L,
                reason = ReaderNavigationReason.SAVED_PASSAGE,
                commitPolicy = ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR
            ),
            "note-7"
        )

        assertEquals("chapter-3", machine.locatorForDurabilityFallback("note-7"))
    }

    @Test
    fun realReadingCommit_promotesExplorationAndClearsTemporaryState() {
        val machine = ReaderNavigationSessionStateMachine("anchor")
        machine.onProgrammaticSettlement(
            ReaderNavigationTransaction(
                token = 3L,
                originLocatorJson = "anchor",
                targetIdentity = null,
                targetHref = "toc.xhtml#chapter-8",
                passageVisitLocatorJson = null,
                startedAtElapsedMs = 3L,
                reason = ReaderNavigationReason.TABLE_OF_CONTENTS,
                commitPolicy = ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR
            ),
            "chapter-8"
        )

        machine.commitExploration("chapter-8-page-2")

        assertEquals("chapter-8-page-2", machine.state.readingAnchorJson)
        assertFalse(machine.state.isExploring)
        assertEquals("chapter-8-page-2", machine.locatorForDurabilityFallback(null))
    }


    @Test
    fun explorationBlocksFinalSnapshotUntilReadingCommit() {
        val machine = ReaderNavigationSessionStateMachine("durable")
        machine.onProgrammaticSettlement(
            ReaderNavigationTransaction(
                token = 5L,
                originLocatorJson = "durable",
                targetIdentity = null,
                targetHref = "chapter.xhtml#footnote",
                passageVisitLocatorJson = null,
                startedAtElapsedMs = 5L,
                reason = ReaderNavigationReason.FOOTNOTE,
                commitPolicy = ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR
            ),
            "footnote"
        )

        assertFalse(machine.mayPersistFinalSnapshot())
        machine.onDurableReadingCommitAccepted("footnote", accepted = false)
        assertEquals("durable", machine.state.readingAnchorJson)
        assertFalse(machine.mayPersistFinalSnapshot())
        machine.onDurableReadingCommitAccepted("next-page", accepted = true)
        assertTrue(machine.mayPersistFinalSnapshot())
        assertEquals("next-page", machine.state.readingAnchorJson)
    }

    @Test
    fun explorationRejectsNonReadingCheckpoints() {
        val machine = ReaderNavigationSessionStateMachine("durable")
        machine.onProgrammaticSettlement(
            ReaderNavigationTransaction(
                token = 6L,
                originLocatorJson = "durable",
                targetIdentity = null,
                targetHref = "chapter.xhtml#note",
                passageVisitLocatorJson = null,
                startedAtElapsedMs = 6L,
                reason = ReaderNavigationReason.FOOTNOTE,
                commitPolicy = ReaderNavigationCommitPolicy.PRESERVE_READING_ANCHOR
            ),
            "note"
        )
        listOf(
            ReaderLocatorEvent.NAVIGATOR_POSITION,
            ReaderLocatorEvent.OPENING_CHECKPOINT,
            ReaderLocatorEvent.RELAYOUT_CHECKPOINT,
            ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT,
            ReaderLocatorEvent.FINAL_SNAPSHOT
        ).forEach { assertFalse(machine.mayCommitObservedEvent(it, "new-reading-position")) }
        listOf(
            ReaderLocatorEvent.NAVIGATOR_PAGE_TURN,
            ReaderLocatorEvent.NAVIGATOR_SCROLL_COMMIT,
            ReaderLocatorEvent.PAPER_COMMIT
        ).forEach { assertTrue(machine.mayCommitObservedEvent(it, "new-reading-position")) }
    }

    @Test
    fun returnPreviousSettlement_waitsForDurableAcknowledgement() {
        val machine = ReaderNavigationSessionStateMachine("anchor")
        machine.onProgrammaticSettlement(
            ReaderNavigationTransaction(
                token = 4L,
                originLocatorJson = "preview",
                targetIdentity = null,
                targetHref = null,
                passageVisitLocatorJson = null,
                startedAtElapsedMs = 4L,
                reason = ReaderNavigationReason.RETURN_PREVIOUS,
                commitPolicy = ReaderNavigationCommitPolicy.COMMIT_ON_SETTLEMENT
            ),
            "anchor"
        )

        assertEquals("anchor", machine.state.readingAnchorJson)
        assertFalse(machine.state.isExploring)
        machine.onDurableReadingCommitAccepted("restored", accepted = false)
        assertEquals("anchor", machine.state.readingAnchorJson)
        machine.onDurableReadingCommitAccepted("restored", accepted = true)
        assertEquals("restored", machine.state.readingAnchorJson)
    }
}
