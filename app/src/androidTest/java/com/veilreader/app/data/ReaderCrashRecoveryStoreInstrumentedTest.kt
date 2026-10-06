package com.veilreader.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.Book
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderCrashRecoveryStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: ReaderCrashRecoveryStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "reader-recovery-v1").deleteRecursively()
        store = ReaderCrashRecoveryStore(context)
    }

    @After
    fun tearDown() {
        store.clearAll()
    }

    @Test
    fun latestAtomicReplacement_roundTripsAcrossOneHundredTwentyEightCommits() {
        repeat(128) { index ->
            store.write(
                checkpoint(
                    sequence = index + 1L,
                    locatorJson = "{\"href\":\"chapter-${index + 1}.xhtml\"}",
                    progression = (index + 1) / 128f,
                    committedAtEpochMs = 1_000L + index
                )
            )
        }

        val restored = store.read(BOOK_ID) ?: error("checkpoint missing")
        assertEquals(128L, restored.sequence)
        assertEquals("{\"href\":\"chapter-128.xhtml\"}", restored.locatorJson)
        assertEquals(1f, restored.progression)
        assertEquals(1_127L, restored.committedAtEpochMs)
    }

    @Test
    fun olderQueuedRoomWrite_cannotClearNewerSameSessionCheckpoint() {
        store.write(checkpoint(sequence = 8L, committedAtEpochMs = 2_000L))

        val cleared = store.clearIfCovered(
            bookId = BOOK_ID,
            sessionId = SESSION_ID,
            order = ReaderProgressWriteOrder(epoch = WRITER_EPOCH, sequence = 7L),
            persistedAtEpochMs = 1_999L
        )

        assertTrue(!cleared)
        assertEquals(8L, store.read(BOOK_ID)?.sequence)
    }

    @Test
    fun matchingOrNewerRoomWrite_clearsCheckpoint() {
        store.write(checkpoint(sequence = 8L, committedAtEpochMs = 2_000L))

        assertTrue(
            store.clearIfCovered(
                bookId = BOOK_ID,
                sessionId = SESSION_ID,
                order = ReaderProgressWriteOrder(epoch = WRITER_EPOCH, sequence = 8L),
                persistedAtEpochMs = 2_000L
            )
        )
        assertNull(store.read(BOOK_ID))
    }

    @Test
    fun staleCheckpoint_neverBeatsEqualOrNewerRoomTimestamp() {
        val checkpoint = checkpoint(sequence = 3L, committedAtEpochMs = 5_000L)
        val durableBook = Book(
            id = BOOK_ID,
            title = "Crash Recovery",
            author = "Veil",
            totalPages = 10,
            sourceUri = "file:///recovery.epub",
            locatorJson = "{\"href\":\"durable.xhtml\"}",
            lastOpenedAtEpochMs = 5_000L
        )

        assertNull(freshReaderCrashRecoveryCheckpoint(durableBook, checkpoint))
        assertEquals(
            checkpoint,
            freshReaderCrashRecoveryCheckpoint(
                durableBook.copy(lastOpenedAtEpochMs = 4_999L),
                checkpoint
            )
        )
    }

    @Test
    fun corruptedCheckpoint_isRejectedAndRemoved() {
        store.write(checkpoint(sequence = 1L))
        val root = File(context.filesDir, "reader-recovery-v1")
        val file = root.listFiles()?.firstOrNull { it.name.endsWith(".json") }
            ?: error("checkpoint file missing")
        file.writeText("{not-json")

        assertNull(store.read(BOOK_ID))
        assertNull(store.read(BOOK_ID))
    }

    private fun checkpoint(
        sequence: Long,
        locatorJson: String = "{\"href\":\"chapter.xhtml\"}",
        progression: Float = 0.4f,
        committedAtEpochMs: Long = 1_000L
    ) = ReaderCrashRecoveryCheckpoint(
        bookId = BOOK_ID,
        sessionId = SESSION_ID,
        writerEpoch = WRITER_EPOCH,
        sequence = sequence,
        locatorJson = locatorJson,
        progression = progression,
        committedAtEpochMs = committedAtEpochMs
    )

    private companion object {
        const val BOOK_ID = "reader-crash-recovery-test"
        const val SESSION_ID = "reader-session"
        const val WRITER_EPOCH = 7L
    }
}
