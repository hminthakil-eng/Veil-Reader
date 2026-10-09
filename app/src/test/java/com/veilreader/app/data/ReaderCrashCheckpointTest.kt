package com.veilreader.app.data

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderCrashCheckpointTest {

    @Test
    fun recoveryReadActuallyBoundsBytesRatherThanOnlySettingCapacity() {
        val exact = byteArrayOf(1, 2, 3)
        assertTrue(exact.contentEquals(readBoundedReaderCheckpoint(exact.inputStream(), 3)))
        assertNull(readBoundedReaderCheckpoint(byteArrayOf(1, 2, 3, 4).inputStream(), 3))
        assertTrue(requireNotNull(readBoundedReaderCheckpoint(byteArrayOf().inputStream(), 3)).isEmpty())
    }

    @Test
    fun checkpointCodecStripsPublicationTextAndRoundTripsRecoveryMetadata() {
        val encoded = encodeReaderCrashCheckpoint(
            ReaderCrashCheckpoint(
                bookId = "book-a",
                sessionId = "session-a",
                writerEpoch = 7L,
                sequence = 11L,
                progression = 0.42,
                locatorJson =
                    """{"href":"chapter.xhtml","type":"application/xhtml+xml","locations":{"progression":0.42},"text":{"highlight":"private publication text"}}""",
                committedAtEpochMs = 9_001L
            )
        )

        assertFalse(encoded.contains("private publication text"))

        val decoded = requireNotNull(decodeReaderCrashCheckpoint(encoded))
        assertEquals("book-a", decoded.bookId)
        assertEquals("session-a", decoded.sessionId)
        assertEquals(7L, decoded.writerEpoch)
        assertEquals(11L, decoded.sequence)
        assertEquals(0.42, decoded.progression, 0.000_001)
        assertEquals(9_001L, decoded.committedAtEpochMs)
        assertFalse(decoded.locatorJson.contains("\"text\""))
        assertTrue(decoded.locatorJson.contains("chapter.xhtml"))
    }

    @Test
    fun malformedOrUnsafeCheckpointPayloadsFailClosed() {
        assertNull(decodeReaderCrashCheckpoint(null))
        assertNull(decodeReaderCrashCheckpoint(""))
        assertNull(decodeReaderCrashCheckpoint("{bad"))
        assertNull(
            decodeReaderCrashCheckpoint(
                """{"version":1,"bookId":"book","sessionId":"s","writerEpoch":1,"sequence":1,"progression":2.0,"locator":"{\"href\":\"c.xhtml\"}","committedAtEpochMs":10}"""
            )
        )
        assertNull(sanitizeReaderCrashLocatorJson("""{"type":"application/xhtml+xml"}"""))
    }

    @Test
    fun progressTimestampIsStrictlyMonotonicInsideOneWallClockMillisecond() {
        assertEquals(101L, nextReaderProgressTimestamp(100L, 100L))
        assertEquals(101L, nextReaderProgressTimestamp(100L, 90L))
        assertEquals(150L, nextReaderProgressTimestamp(100L, 150L))
        assertEquals(Long.MAX_VALUE, nextReaderProgressTimestamp(Long.MAX_VALUE, 1L))
    }

    @Test
    fun checkpointNewerThanDurableRoomWinsButCoveredCheckpointDoesNot() {
        val checkpoint = ReaderCrashCheckpoint(
            bookId = "book",
            sessionId = "session",
            writerEpoch = 2L,
            sequence = 3L,
            progression = 0.51,
            locatorJson = """{"href":"new.xhtml"}""",
            committedAtEpochMs = 200L
        )

        assertTrue(
            checkpoint.isNewerThanRoom(
                Book(
                    id = "book",
                    title = "T",
                    author = "A",
                    progress = 0.50f,
                    locatorJson = """{"href":"old.xhtml"}""",
                    lastOpenedAtEpochMs = 199L
                )
            )
        )

        assertFalse(
            checkpoint.isNewerThanRoom(
                Book(
                    id = "book",
                    title = "T",
                    author = "A",
                    progress = 0.51f,
                    locatorJson = """{"href":"new.xhtml"}""",
                    lastOpenedAtEpochMs = 200L
                )
            )
        )

        assertFalse(
            checkpoint.isNewerThanRoom(
                Book(
                    id = "book",
                    title = "T",
                    author = "A",
                    progress = 0.52f,
                    locatorJson = """{"href":"newer.xhtml"}""",
                    lastOpenedAtEpochMs = 201L
                )
            )
        )
    }

    @Test
    fun equalTimestampWithConflictingRoomPositionKeepsCheckpointForSafeRecovery() {
        val checkpoint = ReaderCrashCheckpoint(
            bookId = "book",
            sessionId = "session",
            writerEpoch = 4L,
            sequence = 9L,
            progression = 0.70,
            locatorJson = """{"href":"checkpoint.xhtml"}""",
            committedAtEpochMs = 500L
        )
        val room = Book(
            id = "book",
            title = "T",
            author = "A",
            progress = 0.69f,
            locatorJson = """{"href":"room.xhtml"}""",
            lastOpenedAtEpochMs = 500L
        )

        assertTrue(checkpoint.isNewerThanRoom(room))
    }
}
