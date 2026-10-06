package com.veilreader.app.debug

import android.app.Activity
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
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
            val checkpointLatenciesUs = ArrayList<Long>(FAULT_COMMIT_COUNT)
            repeat(FAULT_COMMIT_COUNT) { index ->
                val sequence = index + 1L
                val locator =
                    if (sequence == FAULT_COMMIT_COUNT.toLong()) {
                        DESTINATION_LOCATOR
                    } else {
                        "{\"href\":\"fault-$sequence.xhtml\",\"locations\":{\"totalProgression\":0.61}}"
                    }
                val startedAtNanos = SystemClock.elapsedRealtimeNanos()
                val outcome = repository.saveReaderProgress(
                    lease = lease,
                    progression = DESTINATION_PROGRESS.toDouble(),
                    locatorJson = locator,
                    sequence = sequence,
                    nowEpochMs = CHECKPOINT_TIME_MS + index,
                    bypassDebounce = true
                )
                checkpointLatenciesUs +=
                    (SystemClock.elapsedRealtimeNanos() - startedAtNanos) / 1_000L
                check(outcome.accepted)
            }

            val checkpoint = requireNotNull(ReaderCrashRecoveryStore(applicationContext).read(BOOK_ID))
            check(checkpoint.locatorJson == DESTINATION_LOCATOR)
            check(checkpoint.progression == DESTINATION_PROGRESS)
            check(checkpoint.sequence == FAULT_COMMIT_COUNT.toLong())
            val expectedCheckpointTime = CHECKPOINT_TIME_MS + FAULT_COMMIT_COUNT - 1L
            check(checkpoint.committedAtEpochMs == expectedCheckpointTime)

            val sortedLatencyUs = checkpointLatenciesUs.sorted()
            val p50Us = percentile(sortedLatencyUs, 0.50)
            val p95Us = percentile(sortedLatencyUs, 0.95)
            val maxUs = sortedLatencyUs.last()

            File(filesDir, SEED_FILE).writeText(
                JSONObject()
                    .put("status", "checkpointed")
                    .put("bookId", BOOK_ID)
                    .put("sequence", checkpoint.sequence)
                    .put("commitCount", FAULT_COMMIT_COUNT)
                    .put(
                        "checkpointWriteLatencyUs",
                        JSONObject()
                            .put("p50", p50Us)
                            .put("p95", p95Us)
                            .put("max", maxUs)
                    )
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
            check(recovered.sequence == FAULT_COMMIT_COUNT.toLong())
            val seedEvidence = JSONObject(File(filesDir, SEED_FILE).readText())

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
                    .put("commitCount", seedEvidence.getInt("commitCount"))
                    .put(
                        "checkpointWriteLatencyUs",
                        seedEvidence.getJSONObject("checkpointWriteLatencyUs")
                    )
            )
        }.onFailure { error ->
            writeResult("fail", "verify_" + error::class.java.simpleName)
        }

        runOnUiThread { finish() }
    }

    private fun percentile(sorted: List<Long>, quantile: Double): Long {
        require(sorted.isNotEmpty())
        val index = ((sorted.size - 1) * quantile).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
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
        private const val FAULT_COMMIT_COUNT = 32
        private const val ORIGIN_TIME_MS = 1_000L
        private const val CHECKPOINT_TIME_MS = 2_000L
        private const val RECOVERY_TIME_MS = 3_000L

        const val SEED_FILE = "reader-crash-recovery-seed.json"
        const val RESULT_FILE = "reader-crash-recovery-fault.json"
    }
}
