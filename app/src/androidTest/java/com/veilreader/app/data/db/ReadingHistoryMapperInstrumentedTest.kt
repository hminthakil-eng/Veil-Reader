package com.veilreader.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingHistoryEvent
import com.veilreader.app.domain.ReadingHistoryEventKind
import com.veilreader.app.domain.ReadingMilestoneKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingHistoryMapperInstrumentedTest {

    @Test
    fun legacyEnglishTimeline_isDecodedIntoSemanticPayload() {
        val entity = baseEntity(
            timelineJson = """
                [
                  {
                    "id":"session:legacy",
                    "kind":"READING_SESSION",
                    "timestampEpochMs":100,
                    "title":"Reading session",
                    "detail":"30m active · 6 paced turns · 2 highlight events · 1 note events"
                  },
                  {
                    "id":"highlight:legacy",
                    "kind":"PASSAGE_PRESERVED",
                    "timestampEpochMs":200,
                    "title":"Annotated passage preserved",
                    "detail":"A preserved line"
                  },
                  {
                    "id":"milestone:legacy",
                    "kind":"READING_MILESTONE",
                    "timestampEpochMs":300,
                    "title":"Reached 50%",
                    "detail":null
                  }
                ]
            """.trimIndent()
        )

        val events = entity.toDomain().timeline

        val session = events.first { it.kind == ReadingHistoryEventKind.READING_SESSION }
        assertEquals(30L * 60_000L, session.activeMillis)
        assertEquals(6, session.pacedPageTurns)
        assertEquals(2, session.highlightEventCount)
        assertEquals(1, session.noteEventCount)

        val passage = events.first { it.kind == ReadingHistoryEventKind.PASSAGE_PRESERVED }
        assertTrue(passage.annotated)
        assertEquals("A preserved line", passage.excerpt)

        val milestone = events.first { it.kind == ReadingHistoryEventKind.READING_MILESTONE }
        assertEquals(ReadingMilestoneKind.PROGRESS_50, milestone.milestoneKind)
    }

    @Test
    fun semanticTimeline_roundTripsWithoutPresentationText() {
        val original = ReadingCycleRecord(
            id = "cycle",
            bookId = "book",
            cycleIndex = 1,
            titleSnapshot = "Book",
            authorSnapshot = "Author",
            startedAtEpochMs = 10L,
            completedAtEpochMs = 100L,
            finalLocatorJson = "{}",
            sessionCount = 1,
            totalActiveMillis = 90_000L,
            pacedPageTurns = 8,
            highlightCount = 1,
            noteCount = 1,
            bookmarkCount = 1,
            sealCode = "VR-TEST",
            timeline = listOf(
                ReadingHistoryEvent(
                    id = "session",
                    kind = ReadingHistoryEventKind.READING_SESSION,
                    timestampEpochMs = 20L,
                    activeMillis = 90_000L,
                    pacedPageTurns = 8,
                    highlightEventCount = 1,
                    noteEventCount = 1
                ),
                ReadingHistoryEvent(
                    id = "passage",
                    kind = ReadingHistoryEventKind.PASSAGE_PRESERVED,
                    timestampEpochMs = 40L,
                    annotated = true,
                    excerpt = "Remember this"
                ),
                ReadingHistoryEvent(
                    id = "milestone",
                    kind = ReadingHistoryEventKind.READING_MILESTONE,
                    timestampEpochMs = 60L,
                    milestoneKind = ReadingMilestoneKind.PROGRESS_75
                )
            )
        )

        val restored = original.toEntity().toDomain()

        assertEquals(original.timeline, restored.timeline)
        assertTrue(original.toEntity().timelineJson.contains("\"schemaVersion\":2"))
        assertTrue(!original.toEntity().timelineJson.contains("\"title\""))
        assertTrue(!original.toEntity().timelineJson.contains("\"detail\""))
    }

    private fun baseEntity(timelineJson: String) = ReadingCycleEntity(
        id = "cycle",
        bookId = "book",
        cycleIndex = 1,
        titleSnapshot = "Book",
        authorSnapshot = "Author",
        startedAtEpochMs = 10L,
        completedAtEpochMs = 100L,
        finalLocatorJson = "{}",
        sessionCount = 1,
        totalActiveMillis = 30L * 60_000L,
        pacedPageTurns = 6,
        highlightCount = 2,
        noteCount = 1,
        bookmarkCount = 0,
        sealCode = "VR-LEGACY",
        timelineJson = timelineJson
    )
}
