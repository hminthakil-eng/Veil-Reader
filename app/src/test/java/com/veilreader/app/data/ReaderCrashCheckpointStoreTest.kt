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
    fun legacyAtomicBackupIsRecoveredEvenWhenBaseFileIsMissing() {
        val first = checkpoint(1, 100, "legacy.xhtml")
        assertTrue(store.write(first).durable)
        val root = java.io.File(RuntimeEnvironment.getApplication().filesDir, "reader-recovery")
        val base = requireNotNull(root.listFiles()).single { it.name.endsWith(".json") }
        base.copyTo(java.io.File(base.path + ".bak"), overwrite = true)
        assertTrue(base.delete())
        assertEquals("legacy.xhtml", org.json.JSONObject(
            requireNotNull(store.read(first.bookId)).locatorJson).getString("href"))
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

    @Test
    fun failedAtomicRenameCannotAcknowledgeANewCheckpointAndCanRetry() {
        val first = checkpoint(1, 100, "first.xhtml")
        assertTrue(store.write(first).durable)
        val root = java.io.File(RuntimeEnvironment.getApplication().filesDir, "reader-recovery")
        val base = requireNotNull(root.listFiles()).single { it.name.endsWith(".json") }
        assertTrue(base.delete())
        assertTrue(base.mkdir())
        java.io.File(base, "blocker").writeText("Nonempty directory blocks atomic rename")
        try {
            val destination = checkpoint(2, 101, "destination.xhtml")
            assertFalse(store.write(destination).durable)
            assertTrue(base.isDirectory)
            assertTrue(base.deleteRecursively())
            assertTrue(store.write(destination).durable)
            assertEquals("destination.xhtml", org.json.JSONObject(
                requireNotNull(store.read(first.bookId)).locatorJson).getString("href"))
        } finally { base.deleteRecursively() }
    }

    private fun checkpoint(sequence: Long, at: Long, href: String) = ReaderCrashCheckpoint(
        bookId = "checkpoint-test", sessionId = "session", writerEpoch = 1,
        sequence = sequence, progression = 0.2, locatorJson = """{"href":"$href"}""",
        committedAtEpochMs = at
    )
}
