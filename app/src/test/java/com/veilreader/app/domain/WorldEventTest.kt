package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldEventTest {
    @Test
    fun `canonical vocabulary matches Cathedral W1 exactly`() {
        assertEquals(
            listOf(
                "FirstBookImported",
                "BookOpened",
                "ReadingSessionCompleted",
                "PassageMarked",
                "NoteCreated",
                "PassageRevisited",
                "BookCompleted",
                "CycleCompleted",
                "RereadCompleted",
                "DiscoveryEarned",
                "RankAdvanced",
                "ReturnedAfterSilence",
                "LongHistoryReached"
            ),
            WorldEventKind.canonical.map { it.wireName }
        )
    }

    @Test
    fun `unknown future kind survives domain parsing`() {
        val future = WorldEventKind.of("FutureCathedralEvent")
        assertEquals("FutureCathedralEvent", future.wireName)
        assertFalse(future in WorldEventKind.canonical)
    }

    @Test
    fun `known kind resolves to canonical instance`() {
        assertSame(
            WorldEventKind.RANK_ADVANCED,
            WorldEventKind.of("RankAdvanced")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `event kind rejects unstable whitespace`() {
        WorldEventKind.of(" RankAdvanced")
    }

    @Test
    fun `merge is deterministic append only and deduplicates exact facts`() {
        val first = event("a", 200L, WorldEventKind.BOOK_OPENED)
        val second = event("b", 100L, WorldEventKind.FIRST_BOOK_IMPORTED)

        val result = mergeWorldEvents(
            existing = listOf(first),
            incoming = listOf(second, first)
        )

        assertTrue(result.isClean)
        assertEquals(listOf("b", "a"), result.events.map { it.id })
    }

    @Test
    fun `conflicting reused id never overwrites retained history`() {
        val retained = event(
            id = "rank:1",
            time = 100L,
            kind = WorldEventKind.RANK_ADVANCED,
            facts = mapOf("toRankIndex" to "1")
        )
        val conflicting = retained.copy(
            facts = mapOf("toRankIndex" to "2")
        )

        val result = mergeWorldEvents(
            existing = listOf(retained),
            incoming = listOf(conflicting)
        )

        assertFalse(result.isClean)
        assertEquals(listOf(retained), result.events)
        assertEquals(retained, result.conflicts.single().retained)
        assertEquals(conflicting, result.conflicts.single().rejected)
    }

    @Test
    fun `same timestamp is ordered by stable id not insertion order`() {
        val a = event("a", 100L, WorldEventKind.BOOK_OPENED)
        val z = event("z", 100L, WorldEventKind.BOOK_OPENED)

        val result = mergeWorldEvents(
            existing = listOf(z),
            incoming = listOf(a)
        )

        assertEquals(listOf("a", "z"), result.events.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `event rejects non positive time`() {
        event("bad", 0L, WorldEventKind.BOOK_OPENED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `event rejects non canonical fact values`() {
        event(
            id = "bad-fact",
            time = 100L,
            kind = WorldEventKind.RANK_ADVANCED,
            facts = mapOf("pathId" to " oracle ")
        )
    }

    private fun event(
        id: String,
        time: Long,
        kind: WorldEventKind,
        facts: Map<String, String> = emptyMap()
    ): WorldEvent = WorldEvent(
        id = id,
        kind = kind,
        occurredAtEpochMs = time,
        subjectId = "subject",
        facts = facts
    )
}
