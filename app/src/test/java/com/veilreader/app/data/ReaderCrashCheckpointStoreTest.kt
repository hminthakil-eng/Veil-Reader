package com.veilreader.app.data

import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderCrashCheckpointStoreTest {
    private val store = ReaderCrashCheckpointStore(RuntimeEnvironment.getApplication())

    @After
    fun cleanup() = store.clearAll()

    @Test
    fun oldRecoveryCompletionCannotEraseANewerCommittedTurn() {
        val first = checkpoint(1, 100, "first.xhtml")
        assertTrue(store.write(first).durable)
        val recovered = requireNotNull(store.read(first.bookId))
        val second = checkpoint(2, 101, "second.xhtml")
        assertTrue(store.write(second).durable)
        assertFalse(store.clearIfMatches(recovered))
        assertEquals("second.xhtml", org.json.JSONObject(
            requireNotNull(store.read(first.bookId)).locatorJson).getString("href"))
        assertTrue(store.clearIfMatches(requireNotNull(store.read(first.bookId))))
        assertNull(store.read(first.bookId))
    }

    @Test
    fun anOlderRoomWriteCannotClearANewerCheckpoint() {
        val newer = checkpoint(2, 101, "backward.xhtml").copy(progression = 0.19)
        assertTrue(store.write(newer).durable)
        assertFalse(store.clearIfCovered(newer.bookId, 100, """{"href":"origin.xhtml"}"""))
        assertNotNull(store.read(newer.bookId))
        assertTrue(store.clearIfCovered(newer.bookId, 101, newer.locatorJson))
        assertNull(store.read(newer.bookId))
    }

    @Test
    fun invalidRecordDoesNotDestroyLastGoodRecoveryAndPublicationTextIsRemoved() {
        val first = checkpoint(1, 100, "first.xhtml").copy(
            locatorJson = """{"href":"first.xhtml","text":{"highlight":"private prose"}}"""
        )
        assertTrue(store.write(first).durable)
        assertFalse(store.write(first.copy(progression = Double.NaN)).durable)
        val recovered = requireNotNull(store.read(first.bookId))
        assertFalse(recovered.locatorJson.contains("private prose"))
        assertEquals(1L, recovered.sequence)
    }

    private fun checkpoint(sequence: Long, at: Long, href: String) = ReaderCrashCheckpoint(
        bookId = "checkpoint-test", sessionId = "session", writerEpoch = 1,
        sequence = sequence, progression = 0.2, locatorJson = """{"href":"$href"}""",
        committedAtEpochMs = at
    )
}
