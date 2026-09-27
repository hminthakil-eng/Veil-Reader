package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderDossierHistoryTest {
    @Test
    fun `dossier history uses only durable book session and cycle records`() {
        val books = listOf(
            Book(
                id = "a",
                title = "A",
                author = "Author",
                addedAtEpochMs = 100L,
                lastOpenedAtEpochMs = 900L
            ),
            Book(
                id = "b",
                title = "B",
                author = "Author",
                addedAtEpochMs = 300L,
                lastOpenedAtEpochMs = 0L
            )
        )
        val sessions = listOf(
            ReadingSessionSnapshot(
                id = "s1",
                bookId = "a",
                startedAtEpochMs = 200L,
                endedAtEpochMs = 400L,
                activeMillis = 120_000L,
                pacedPageTurns = 3,
                highlightCount = 1,
                noteCount = 0
            ),
            ReadingSessionSnapshot(
                id = "s2",
                bookId = "a",
                startedAtEpochMs = 500L,
                endedAtEpochMs = 800L,
                activeMillis = 180_000L,
                pacedPageTurns = 4,
                highlightCount = 0,
                noteCount = 0
            )
        )
        val cycles = listOf(
            ReadingCycleRecord(
                id = "c1",
                bookId = "a",
                cycleIndex = 1,
                titleSnapshot = "A",
                authorSnapshot = "Author",
                startedAtEpochMs = 200L,
                completedAtEpochMs = 700L,
                finalLocatorJson = "{}",
                sessionCount = 1,
                totalActiveMillis = 120_000L,
                pacedPageTurns = 3,
                highlightCount = 1,
                noteCount = 0,
                bookmarkCount = 0,
                sealCode = "seal1",
                timeline = emptyList()
            ),
            ReadingCycleRecord(
                id = "c2",
                bookId = "a",
                cycleIndex = 2,
                titleSnapshot = "A",
                authorSnapshot = "Author",
                startedAtEpochMs = 500L,
                completedAtEpochMs = 850L,
                finalLocatorJson = "{}",
                sessionCount = 1,
                totalActiveMillis = 180_000L,
                pacedPageTurns = 4,
                highlightCount = 0,
                noteCount = 0,
                bookmarkCount = 0,
                sealCode = "seal2",
                timeline = emptyList()
            )
        )

        val history = deriveReaderDossierHistory(
            books = books,
            sessions = sessions,
            cycles = cycles
        )

        assertEquals(2, history.archivedVolumeCount)
        assertEquals(2, history.recordedSessionCount)
        assertEquals(300_000L, history.recordedActiveMillis)
        assertEquals(2, history.completionCycleCount)
        assertEquals(1, history.rereadCycleCount)
        assertEquals(100L, history.firstRecordedAtEpochMs)
        assertEquals(900L, history.latestRecordedAtEpochMs)
    }
}
