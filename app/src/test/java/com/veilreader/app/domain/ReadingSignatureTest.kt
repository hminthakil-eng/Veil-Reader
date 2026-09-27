package com.veilreader.app.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSignatureTest {
    private val utc = ZoneId.of("UTC")

    private fun epoch(
        year: Int = 2026,
        month: Int = 9,
        day: Int,
        hour: Int
    ): Long = ZonedDateTime.of(
        year,
        month,
        day,
        hour,
        0,
        0,
        0,
        utc
    ).toInstant().toEpochMilli()

    private fun session(
        id: String,
        day: Int,
        hour: Int,
        activeMinutes: Long,
        bookId: String = "book",
        turns: Int = 0,
        highlights: Int = 0,
        notes: Int = 0
    ) = ReadingSessionSnapshot(
        id = id,
        bookId = bookId,
        startedAtEpochMs = epoch(day = day, hour = hour),
        endedAtEpochMs = epoch(day = day, hour = hour) + activeMinutes * 60_000L,
        activeMillis = activeMinutes * 60_000L,
        pacedPageTurns = turns,
        highlightCount = highlights,
        noteCount = notes
    )

    @Test
    fun `median uses positive active sessions and ignores zero duration interruptions`() {
        val signature = deriveReadingSignature(
            sessions = listOf(
                session("a", day = 1, hour = 8, activeMinutes = 10),
                session("b", day = 2, hour = 8, activeMinutes = 0),
                session("c", day = 3, hour = 8, activeMinutes = 30),
                session("d", day = 4, hour = 8, activeMinutes = 20)
            ),
            cycles = emptyList(),
            zoneId = utc
        )

        assertEquals(4, signature.recordedSessionCount)
        assertEquals(3, signature.measuredActiveSessionCount)
        assertEquals(20L * 60_000L, signature.medianActiveSessionMillis)
    }

    @Test
    fun `daypart leader needs at least three timed sessions and no tie`() {
        val tooSmall = deriveReadingSignature(
            sessions = listOf(
                session("a", day = 1, hour = 23, activeMinutes = 10),
                session("b", day = 2, hour = 23, activeMinutes = 10)
            ),
            cycles = emptyList(),
            zoneId = utc
        )
        assertNull(tooSmall.leadingDaypart)

        val tied = deriveReadingSignature(
            sessions = listOf(
                session("a", day = 1, hour = 8, activeMinutes = 10),
                session("b", day = 2, hour = 8, activeMinutes = 10),
                session("c", day = 3, hour = 20, activeMinutes = 10),
                session("d", day = 4, hour = 20, activeMinutes = 10)
            ),
            cycles = emptyList(),
            zoneId = utc
        )
        assertNull(tied.leadingDaypart)

        val morning = deriveReadingSignature(
            sessions = listOf(
                session("a", day = 1, hour = 8, activeMinutes = 10),
                session("b", day = 2, hour = 9, activeMinutes = 10),
                session("c", day = 3, hour = 10, activeMinutes = 10),
                session("d", day = 4, hour = 20, activeMinutes = 10)
            ),
            cycles = emptyList(),
            zoneId = utc
        )
        assertEquals(ReadingDaypart.MORNING, morning.leadingDaypart)
    }

    @Test
    fun `density metrics require a minimum active evidence window`() {
        val short = deriveReadingSignature(
            sessions = listOf(
                session(
                    "short",
                    day = 1,
                    hour = 8,
                    activeMinutes = 10,
                    turns = 20,
                    highlights = 3
                )
            ),
            cycles = emptyList(),
            zoneId = utc
        )
        assertNull(short.pacedPageTurnsPerActiveHour)
        assertNull(short.highlightEventsPerActiveHour)

        val supported = deriveReadingSignature(
            sessions = listOf(
                session(
                    "supported",
                    day = 1,
                    hour = 8,
                    activeMinutes = 30,
                    turns = 30,
                    highlights = 3
                )
            ),
            cycles = emptyList(),
            zoneId = utc
        )
        assertEquals(60f, supported.pacedPageTurnsPerActiveHour ?: -1f, 0.001f)
        assertEquals(6f, supported.highlightEventsPerActiveHour ?: -1f, 0.001f)
    }

    @Test
    fun `note rate is a factual event ratio rather than an inferred trait`() {
        val signature = deriveReadingSignature(
            sessions = listOf(
                session(
                    "notes",
                    day = 1,
                    hour = 14,
                    activeMinutes = 30,
                    highlights = 4,
                    notes = 2
                )
            ),
            cycles = emptyList(),
            zoneId = utc
        )

        assertEquals(0.5f, signature.notesPerHighlightEvent ?: -1f, 0.001f)
    }

    @Test
    fun `reread share uses durable completion cycles only`() {
        val cycles = listOf(
            cycle(index = 1, completedAt = epoch(day = 1, hour = 10)),
            cycle(index = 2, completedAt = epoch(day = 2, hour = 10)),
            cycle(index = 3, completedAt = epoch(day = 3, hour = 10))
        )
        val signature = deriveReadingSignature(
            sessions = emptyList(),
            cycles = cycles,
            zoneId = utc
        )

        assertEquals(3, signature.completionCycleCount)
        assertEquals(2, signature.rereadCycleCount)
        assertEquals(2f / 3f, signature.rereadCycleShare ?: -1f, 0.001f)
    }

    @Test
    fun `daypart mapping is explicit and wraps invalid hour inputs safely`() {
        assertEquals(ReadingDaypart.NIGHT, readingDaypartForHour(4))
        assertEquals(ReadingDaypart.MORNING, readingDaypartForHour(5))
        assertEquals(ReadingDaypart.AFTERNOON, readingDaypartForHour(12))
        assertEquals(ReadingDaypart.EVENING, readingDaypartForHour(17))
        assertEquals(ReadingDaypart.NIGHT, readingDaypartForHour(22))
        assertEquals(ReadingDaypart.NIGHT, readingDaypartForHour(24))
    }


    @Test
    fun `untimed legacy sessions still contribute to duration and density but not daypart`() {
        val untimed = ReadingSessionSnapshot(
            id = "legacy",
            bookId = "legacy-book",
            startedAtEpochMs = 0L,
            endedAtEpochMs = 0L,
            activeMillis = 30L * 60_000L,
            pacedPageTurns = 15,
            highlightCount = 2,
            noteCount = 1
        )

        val signature = deriveReadingSignature(
            sessions = listOf(
                untimed,
                session(
                    "timed",
                    day = 1,
                    hour = 8,
                    activeMinutes = 30,
                    turns = 15,
                    highlights = 2,
                    notes = 1
                )
            ),
            cycles = emptyList(),
            zoneId = utc
        )

        assertEquals(2, signature.recordedSessionCount)
        assertEquals(1, signature.timedSessionCount)
        assertEquals(30f, signature.pacedPageTurnsPerActiveHour ?: -1f, 0.001f)
        assertEquals(4f, signature.highlightEventsPerActiveHour ?: -1f, 0.001f)
        assertNull(signature.leadingDaypart)
    }

    private fun cycle(index: Int, completedAt: Long) = ReadingCycleRecord(
        id = "cycle:$index",
        bookId = "book",
        cycleIndex = index,
        titleSnapshot = "Book",
        authorSnapshot = "Author",
        startedAtEpochMs = completedAt - 60_000L,
        completedAtEpochMs = completedAt,
        finalLocatorJson = "{}",
        sessionCount = 1,
        totalActiveMillis = 60_000L,
        pacedPageTurns = 1,
        highlightCount = 0,
        noteCount = 0,
        bookmarkCount = 0,
        sealCode = "VR-$index",
        timeline = emptyList()
    )
}
