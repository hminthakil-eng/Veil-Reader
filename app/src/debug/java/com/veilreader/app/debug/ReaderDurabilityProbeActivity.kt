package com.veilreader.app.debug

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Process
import com.veilreader.app.data.GameRepository
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.ui.reader.ReaderLocatorEvent
import com.veilreader.app.ui.reader.ReaderViewModel
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Debug-only process-death probe for Reader locator durability.
 *
 * CI drives this activity from adb in three independent app processes:
 * seed a durable origin, submit one Reader event and kill immediately, then relaunch and verify.
 */
class ReaderDurabilityProbeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.getStringExtra(EXTRA_ACTION)
            ?: return failAndFinish("missing_action")

        try {
            runBlocking {
                when (action) {
                    ACTION_SEED -> seed()
                    ACTION_COMMIT_AND_KILL -> commitAndKill()
                    ACTION_VERIFY -> verify()
                    else -> error("Unknown probe action: $action")
                }
            }
        } catch (error: Throwable) {
            writeResult(
                "FAIL action=$action error=${error::class.java.simpleName} " +
                    "message=${sanitize(error.message)}"
            )
            finishAndRemoveTask()
        }
    }

    private suspend fun seed() {
        val originProgress = requiredProgress(EXTRA_ORIGIN_PROGRESS)
        val originKey = requiredString(EXTRA_ORIGIN_KEY)

        ensureFixtureFile()
        val repository = LocalLibraryRepository(applicationContext)
        repository.addImportedBook(
            Book(
                id = BOOK_ID,
                title = "Reader Durability Probe",
                author = "Veil QA",
                progress = originProgress.toFloat(),
                currentChapter = originKey,
                totalPages = TOTAL_PAGES,
                pagesRead = pagesFor(originProgress),
                format = BookFormat.EPUB,
                sourceUri = Uri.fromFile(fixtureFile()).toString(),
                mediaType = "application/epub+zip",
                locatorJson = locator(originKey),
                addedAtEpochMs = 1L,
                lastOpenedAtEpochMs = 1L,
                finished = false
            )
        )
        repository.flushWrites()

        val stored = requireNotNull(repository.getBook(BOOK_ID)) {
            "Seeded book did not return from repository"
        }
        check(stored.locatorJson == locator(originKey)) {
            "Seed locator mismatch: ${stored.locatorJson}"
        }
        check(abs(stored.progress.toDouble() - originProgress) <= EPSILON) {
            "Seed progress mismatch: ${stored.progress}"
        }

        writeResult(
            "PASS action=seed key=$originKey progress=${stored.progress}"
        )
        finishAndRemoveTask()
    }

    private suspend fun commitAndKill() {
        val sessionId = requiredString(EXTRA_SESSION_ID)
        val destinationProgress = requiredProgress(EXTRA_DESTINATION_PROGRESS)
        val destinationKey = requiredString(EXTRA_DESTINATION_KEY)
        val event = requiredString(EXTRA_EVENT).toReaderLocatorEvent()

        val repository = LocalLibraryRepository(applicationContext)
        repository.flushWrites()
        val current = awaitBook(repository)

        val viewModel = ReaderViewModel(
            library = repository,
            game = GameRepository(applicationContext)
        )
        check(
            viewModel.openBook(
                bookId = BOOK_ID,
                initialProgress = current.progress,
                openInstanceId = sessionId
            )
        ) {
            "ReaderViewModel refused probe open"
        }
        check(viewModel.confirmOpen(sessionId)) {
            "ReaderViewModel refused probe confirmation"
        }

        val commit = viewModel.onLocatorUpdate(
            bookId = BOOK_ID,
            expectedOpenInstanceId = sessionId,
            progression = destinationProgress,
            locatorJson = locator(destinationKey),
            locationKey = destinationKey,
            event = event
        )

        if (event.commitsLocator) {
            check(commit != null) {
                "Committed event $event did not produce a locator commit"
            }
        } else {
            check(commit == null) {
                "Observation event $event unexpectedly committed progress"
            }
        }

        // Deliberately do not call onPause(), close(), flushWrites(), finish(), or write a marker.
        Process.killProcess(Process.myPid())
    }

    private suspend fun verify() {
        val expectedProgress = requiredProgress(EXTRA_EXPECTED_PROGRESS)
        val expectedKey = requiredString(EXTRA_EXPECTED_KEY)

        val repository = LocalLibraryRepository(applicationContext)
        repository.flushWrites()
        val stored = awaitBook(repository)

        val locatorMatches = stored.locatorJson == locator(expectedKey)
        val progressMatches = abs(stored.progress.toDouble() - expectedProgress) <= EPSILON
        if (!locatorMatches || !progressMatches) {
            writeResult(
                "FAIL action=verify expectedKey=$expectedKey actualLocator=${sanitize(stored.locatorJson)} " +
                    "expectedProgress=$expectedProgress actualProgress=${stored.progress}"
            )
            finishAndRemoveTask()
            return
        }

        writeResult(
            "PASS action=verify key=$expectedKey progress=${stored.progress}"
        )
        finishAndRemoveTask()
    }

    private suspend fun awaitBook(repository: LocalLibraryRepository): Book =
        withTimeout(5_000L) {
            repository.books
                .first { books -> books.any { it.id == BOOK_ID } }
                .first { it.id == BOOK_ID }
        }

    private fun requiredString(name: String): String =
        requireNotNull(intent.getStringExtra(name)) { "Missing extra: $name" }
            .also { require(it.isNotBlank()) { "Blank extra: $name" } }

    private fun requiredProgress(name: String): Double {
        val raw = requiredString(name)
        val parsed = raw.toDoubleOrNull()
            ?: error("Invalid progress for $name: $raw")
        require(parsed.isFinite() && parsed in 0.0..1.0) {
            "Progress outside [0,1] for $name: $parsed"
        }
        return parsed
    }

    private fun String.toReaderLocatorEvent(): ReaderLocatorEvent =
        when (this) {
            "page" -> ReaderLocatorEvent.NAVIGATOR_PAGE_TURN
            "paper" -> ReaderLocatorEvent.PAPER_COMMIT
            "jump" -> ReaderLocatorEvent.NAVIGATION_JUMP_COMMIT
            "final" -> ReaderLocatorEvent.FINAL_SNAPSHOT
            "preview" -> ReaderLocatorEvent.NAVIGATOR_POSITION
            else -> error("Unknown probe event: $this")
        }

    private fun fixtureFile(): File =
        File(filesDir, "publications/reader-durability-probe.epub")

    private fun ensureFixtureFile() {
        fixtureFile().apply {
            parentFile?.mkdirs()
            if (!exists()) writeBytes(byteArrayOf(0x50, 0x4b, 0x03, 0x04))
        }
    }

    private fun locator(key: String): String =
        """{"href":"$key.xhtml","type":"application/xhtml+xml"}"""

    private fun pagesFor(progress: Double): Int =
        (TOTAL_PAGES * progress).toInt().coerceIn(0, TOTAL_PAGES)

    private fun writeResult(value: String) {
        File(filesDir, RESULT_FILE).writeText(value + "\n")
    }

    private fun failAndFinish(reason: String) {
        writeResult("FAIL action=bootstrap error=$reason")
        finishAndRemoveTask()
    }

    private fun sanitize(value: String?): String =
        value.orEmpty()
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('|', '/')
            .take(400)

    private companion object {
        const val BOOK_ID = "reader-durability-probe-book"
        const val TOTAL_PAGES = 1_000
        const val EPSILON = 0.000_01

        const val RESULT_FILE = "reader-durability-probe-result.txt"

        const val EXTRA_ACTION = "probe_action"
        const val EXTRA_EVENT = "probe_event"
        const val EXTRA_SESSION_ID = "probe_session_id"
        const val EXTRA_ORIGIN_PROGRESS = "probe_origin_progress"
        const val EXTRA_ORIGIN_KEY = "probe_origin_key"
        const val EXTRA_DESTINATION_PROGRESS = "probe_destination_progress"
        const val EXTRA_DESTINATION_KEY = "probe_destination_key"
        const val EXTRA_EXPECTED_PROGRESS = "probe_expected_progress"
        const val EXTRA_EXPECTED_KEY = "probe_expected_key"

        const val ACTION_SEED = "seed"
        const val ACTION_COMMIT_AND_KILL = "commit_and_kill"
        const val ACTION_VERIFY = "verify"
    }
}
