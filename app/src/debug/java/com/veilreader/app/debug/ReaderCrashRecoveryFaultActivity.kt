package com.veilreader.app.debug

import android.app.Activity
import android.os.Bundle
import android.os.Process
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.ReaderCrashRecoveryStore
import com.veilreader.app.data.VeilDatabase
import com.veilreader.app.data.db.VeilDatabase as RoomVeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Debug-only external fault injector used by CI.
 *
 * The seed phase deliberately holds SQLite's writer transaction open, creates one semantic Reader
 * commit, proves its atomic checkpoint exists, then kills the app process. The uncommitted Room
 * transaction is rolled back by process death. A fresh process runs the verify phase and must
 * recover the semantic destination from the checkpoint before promoting it back into Room.
 */
class ReaderCrashRecoveryFaultActivity : Activity() {
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var seedKillInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_SEED_KILL -> {
                seedKillInFlight = true
                work.launch { seedAndKillProcess() }
            }
            MODE_VERIFY -> work.launch { verifyFreshProcessRecovery() }
            else -> {
                writeResult("fail", "invalid_mode")
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (!seedKillInFlight) work.cancel()
        super.onDestroy()
    }

    private suspend fun seedAndKillProcess() {
        runCatching {
            File(filesDir, RESULT_FILE).delete()
            File(filesDir, SEED_FILE).delete()

            val database = RoomVeilDatabase.get(applicationContext)
            val repository = LocalLibraryRepository(
                appContext = applicationContext,
                database = database,
                settings = SettingsStore(applicationContext),
                runLegacyMigration = false
            )

            repository.addImportedBook(
                Book(
                    id = BOOK_ID,
                    title = "Reader crash recovery fault fixture",
                    author = "Veil CI",
                    totalPages = 100,
                    sourceUri = "file:///reader-crash-recovery-fault.epub",
                    addedAtEpochMs = ORIGIN_TIME_MS
                )
            )
            repository.saveProgress(
                id = BOOK_ID,
                progression = ORIGIN_PROGRESS,
                locatorJson = ORIGIN_LOCATOR,
                nowEpochMs = ORIGIN_TIME_MS
            )
            repository.flushWrites()

            val durableOrigin = requireNotNull(database.books().findEntity(BOOK_ID))
            check(durableOrigin.locatorJson == ORIGIN_LOCATOR)
            check(durableOrigin.lastOpenedAtEpochMs == ORIGIN_TIME_MS)

            // Hold SQLite's sole writer lock. The semantic progress write can enqueue, but cannot
            // reach durable Room before this process is killed.
            val sqlite = database.openHelper.writableDatabase
            sqlite.beginTransaction()
            sqlite.execSQL(
                "UPDATE books SET lastOpenedAtEpochMs = lastOpenedAtEpochMs WHERE id = ?",
                arrayOf(BOOK_ID)
            )

            val lease = repository.beginReaderProgressSession(
                bookId = BOOK_ID,
                sessionId = SEED_SESSION_ID
            )
            val outcome = repository.saveReaderProgress(
                lease = lease,
                progression = DESTINATION_PROGRESS,
                locatorJson = DESTINATION_LOCATOR,
                sequence = 1L,
                nowEpochMs = CHECKPOINT_TIME_MS,
                bypassDebounce = true
            )
            check(outcome.accepted)

            val checkpoint = requireNotNull(ReaderCrashRecoveryStore(applicationContext).read(BOOK_ID))
            check(checkpoint.locatorJson == DESTINATION_LOCATOR)
            check(checkpoint.progression == DESTINATION_PROGRESS)
            check(checkpoint.committedAtEpochMs == CHECKPOINT_TIME_MS)

            File(filesDir, SEED_FILE).writeText(
                JSONObject()
                    .put("status", "checkpointed")
                    .put("bookId", BOOK_ID)
                    .put("sequence", checkpoint.sequence)
                    .toString()
            )

            // Do not end the SQLite transaction. Process death is the fault under test and must
            // roll the uncommitted Room write back while AtomicFile survives.
            Process.killProcess(Process.myPid())
        }.onFailure { error ->
            seedKillInFlight = false
            writeResult("fail", "seed_" + error::class.java.simpleName)
            runOnUiThread { finish() }
        }
    }

    private suspend fun verifyFreshProcessRecovery() {
        runCatching {
            val database = RoomVeilDatabase.get(applicationContext)
            val repository = LocalLibraryRepository(
                appContext = applicationContext,
                database = database,
                settings = SettingsStore(applicationContext),
                runLegacyMigration = false
            )

            val durableBefore = requireNotNull(repository.loadBookForReaderOpen(BOOK_ID))
            check(durableBefore.locatorJson == ORIGIN_LOCATOR)
            check(durableBefore.lastOpenedAtEpochMs == ORIGIN_TIME_MS)

            val recovered = requireNotNull(
                repository.loadReaderCrashRecoveryCheckpoint(durableBefore)
            )
            check(recovered.locatorJson == DESTINATION_LOCATOR)
            check(recovered.progression == DESTINATION_PROGRESS)

            val promotion = repository.saveReaderOpenRecoveryProgress(
                bookId = BOOK_ID,
                sessionId = VERIFY_SESSION_ID,
                progression = recovered.progression.toDouble(),
                locatorJson = recovered.locatorJson,
                nowEpochMs = RECOVERY_TIME_MS
            )
            check(promotion.accepted)
            repository.flushWrites()

            val durableAfter = requireNotNull(repository.loadBookForReaderOpen(BOOK_ID))
            check(durableAfter.locatorJson == DESTINATION_LOCATOR)
            check(durableAfter.progress == DESTINATION_PROGRESS)
            check(durableAfter.lastOpenedAtEpochMs == RECOVERY_TIME_MS)
            check(ReaderCrashRecoveryStore(applicationContext).read(BOOK_ID) == null)

            writeResult(
                status = "pass",
                error = null,
                details = JSONObject()
                    .put("roomBeforeWasOrigin", true)
                    .put("checkpointRecovered", true)
                    .put("roomAfterWasDestination", true)
                    .put("checkpointCleared", true)
            )
        }.onFailure { error ->
            writeResult("fail", "verify_" + error::class.java.simpleName)
        }

        runOnUiThread { finish() }
    }

    private fun writeResult(
        status: String,
        error: String?,
        details: JSONObject? = null
    ) {
        val json = JSONObject()
            .put("status", status)
            .put("bookId", BOOK_ID)
        if (error != null) json.put("error", error)
        if (details != null) json.put("details", details)
        File(filesDir, RESULT_FILE).writeText(json.toString())
    }

    companion object {
        const val EXTRA_MODE = "veil.reader.crashRecovery.mode"
        const val MODE_SEED_KILL = "seed-kill"
        const val MODE_VERIFY = "verify"

        private const val BOOK_ID = "reader-crash-recovery-fault-book"
        private const val SEED_SESSION_ID = "reader-crash-recovery-seed-session"
        private const val VERIFY_SESSION_ID = "reader-crash-recovery-verify-session"
        private const val ORIGIN_LOCATOR = "{\"href\":\"origin.xhtml\",\"locations\":{\"totalProgression\":0.1}}"
        private const val DESTINATION_LOCATOR = "{\"href\":\"destination.xhtml\",\"locations\":{\"totalProgression\":0.61}}"
        private const val ORIGIN_PROGRESS = 0.10
        private const val DESTINATION_PROGRESS = 0.61f
        private const val ORIGIN_TIME_MS = 1_000L
        private const val CHECKPOINT_TIME_MS = 2_000L
        private const val RECOVERY_TIME_MS = 3_000L

        const val SEED_FILE = "reader-crash-recovery-seed.json"
        const val RESULT_FILE = "reader-crash-recovery-fault.json"
    }
}
