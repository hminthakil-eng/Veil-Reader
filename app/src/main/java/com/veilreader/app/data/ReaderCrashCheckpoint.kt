package com.veilreader.app.data

import android.content.Context
import android.os.StrictMode
import android.os.SystemClock
import android.util.AtomicFile
import com.veilreader.app.domain.Book
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.abs
import org.json.JSONObject

/**
 * Short-lived crash recovery record for one committed Reader locator.
 *
 * Room remains the canonical progress store. This journal exists only to bridge the interval between
 * a semantic Reader commit returning to the UI and the serialized Room writer completing. Records
 * contain locator metadata only; Readium text excerpts are stripped before any bytes reach disk.
 */
internal data class ReaderCrashCheckpoint(
    val bookId: String,
    val sessionId: String,
    val writerEpoch: Long,
    val sequence: Long,
    val progression: Double,
    val locatorJson: String,
    val committedAtEpochMs: Long
)

internal data class ReaderCrashCheckpointWriteResult(
    val durable: Boolean,
    val elapsedNanos: Long
)

/**
 * App-private, atomic, bounded checkpoint journal.
 *
 * AtomicFile gives every semantic commit an all-old-or-all-new record across abrupt process death.
 * Writes are deliberately synchronous because returning before durable acknowledgement recreates the
 * exact crash window this journal closes. Only discrete semantic commits use this path; scroll,
 * relayout, opening observations and animation previews never write it.
 */
internal class ReaderCrashCheckpointStore(
    context: Context
) {
    private val root = File(context.applicationContext.filesDir, DIRECTORY_NAME)

    @Synchronized
    fun write(raw: ReaderCrashCheckpoint): ReaderCrashCheckpointWriteResult {
        val checkpoint = normalizeReaderCrashCheckpoint(raw)
            ?: return ReaderCrashCheckpointWriteResult(durable = false, elapsedNanos = 0L)
        val bytes = encodeReaderCrashCheckpoint(checkpoint)
            .toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_RECORD_BYTES) {
            return ReaderCrashCheckpointWriteResult(durable = false, elapsedNanos = 0L)
        }

        val started = SystemClock.elapsedRealtimeNanos()
        val previousPolicy = StrictMode.allowThreadDiskWrites()
        return try {
            check(root.mkdirs() || root.isDirectory) {
                "Could not prepare Reader crash checkpoint directory."
            }
            val atomic = AtomicFile(fileFor(checkpoint.bookId))
            val output = atomic.startWrite()
            try {
                output.write(bytes)
                // AtomicFile logs some fsync/rename failures instead of throwing. Require an
                // observable sync and the complete new base record before durable acknowledgement.
                output.fd.sync()
                atomic.finishWrite(output)
                val committed = atomic.openRead().use {
                    readBoundedReaderCheckpoint(it, MAX_RECORD_BYTES)
                }
                check(committed?.contentEquals(bytes) == true) {
                    "Reader crash checkpoint was not committed."
                }
            } catch (error: Throwable) {
                atomic.failWrite(output)
                throw error
            }
            ReaderCrashCheckpointWriteResult(
                durable = true,
                elapsedNanos = SystemClock.elapsedRealtimeNanos() - started
            )
        } catch (_: Throwable) {
            ReaderCrashCheckpointWriteResult(
                durable = false,
                elapsedNanos = SystemClock.elapsedRealtimeNanos() - started
            )
        } finally {
            StrictMode.setThreadPolicy(previousPolicy)
        }
    }

    @Synchronized
    fun read(bookId: String): ReaderCrashCheckpoint? {
        val id = bookId.trim()
        if (id.isEmpty()) return null
        val file = fileFor(id)
        val atomic = AtomicFile(file)

        val checkpoint = runCatching {
            val bytes = atomic.openRead().use { input ->
                readBoundedReaderCheckpoint(input, MAX_RECORD_BYTES)
            } ?: return@runCatching null
            if (bytes.isEmpty()) return@runCatching null
            decodeReaderCrashCheckpoint(String(bytes, StandardCharsets.UTF_8))
                ?.takeIf { it.bookId == id }
        }.getOrNull()

        if (checkpoint == null) {
            runCatching { atomic.delete() }
        }
        return checkpoint
    }

    /** Recovery of an older open request must not erase a new turn's checkpoint. */
    @Synchronized
    fun clearIfMatches(expected: ReaderCrashCheckpoint): Boolean {
        if (read(expected.bookId) != expected) return false
        clear(expected.bookId)
        return true
    }

    @Synchronized
    fun clear(bookId: String) {
        val id = bookId.trim()
        if (id.isEmpty()) return
        runCatching { AtomicFile(fileFor(id)).delete() }
    }

    /**
     * Clears only when the Room write definitely covers the currently journaled semantic commit.
     *
     * A newer checkpoint may replace the file while an older Room write is in flight. Timestamp
     * ordering prevents that older write from deleting the newer recovery record.
     */
    @Synchronized
    fun clearIfCovered(
        bookId: String,
        persistedAtEpochMs: Long,
        persistedLocatorJson: String
    ): Boolean {
        val checkpoint = read(bookId) ?: return false
        val persistedLocator = sanitizeReaderCrashLocatorJson(persistedLocatorJson)
        val covered =
            persistedAtEpochMs > checkpoint.committedAtEpochMs ||
                (
                    persistedAtEpochMs == checkpoint.committedAtEpochMs &&
                        persistedLocator == checkpoint.locatorJson
                )
        if (!covered) return false
        clear(bookId)
        return true
    }

    @Synchronized
    fun clearAll() {
        if (!root.exists()) return
        root.listFiles().orEmpty().forEach { child ->
            if (child.isFile) runCatching { child.delete() }
        }
        runCatching { root.delete() }
    }

    private fun fileFor(bookId: String): File {
        val safe = UUID.nameUUIDFromBytes(
            "veil-reader-crash:$bookId".toByteArray(StandardCharsets.UTF_8)
        ).toString()
        return File(root, "$safe.json")
    }

    private companion object {
        const val DIRECTORY_NAME = "reader-recovery"
        const val MAX_RECORD_BYTES = 192 * 1024
    }
}

internal fun nextReaderProgressTimestamp(
    previousEpochMs: Long,
    candidateEpochMs: Long
): Long {
    val previous = previousEpochMs.coerceAtLeast(0L)
    val candidate = candidateEpochMs.coerceAtLeast(0L)
    val afterPrevious =
        if (previous == Long.MAX_VALUE) Long.MAX_VALUE
        else previous + 1L
    return maxOf(candidate, afterPrevious)
}

/** Returns null rather than persisting malformed or text-bearing locator payloads. */
internal fun sanitizeReaderCrashLocatorJson(raw: String?): String? {
    if (raw.isNullOrBlank() || raw.length > MAX_LOCATOR_CHARS) return null
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (root.optString("href").isBlank()) return null
    root.remove("text")
    return root.toString()
}

internal fun encodeReaderCrashCheckpoint(raw: ReaderCrashCheckpoint): String {
    val checkpoint = requireNotNull(normalizeReaderCrashCheckpoint(raw)) {
        "Invalid Reader crash checkpoint."
    }
    return JSONObject().apply {
        put("version", READER_CRASH_CHECKPOINT_VERSION)
        put("bookId", checkpoint.bookId)
        put("sessionId", checkpoint.sessionId)
        put("writerEpoch", checkpoint.writerEpoch)
        put("sequence", checkpoint.sequence)
        put("progression", checkpoint.progression)
        put("locator", checkpoint.locatorJson)
        put("committedAtEpochMs", checkpoint.committedAtEpochMs)
    }.toString()
}

internal fun decodeReaderCrashCheckpoint(raw: String?): ReaderCrashCheckpoint? {
    if (raw.isNullOrBlank() || raw.length > MAX_CHECKPOINT_JSON_CHARS) return null
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (root.optInt("version", -1) != READER_CRASH_CHECKPOINT_VERSION) return null

    return normalizeReaderCrashCheckpoint(
        ReaderCrashCheckpoint(
            bookId = root.optString("bookId"),
            sessionId = root.optString("sessionId"),
            writerEpoch = root.optLong("writerEpoch", -1L),
            sequence = root.optLong("sequence", -1L),
            progression = root.optDouble("progression", Double.NaN),
            locatorJson = root.optString("locator"),
            committedAtEpochMs = root.optLong("committedAtEpochMs", -1L)
        )
    )
}

internal fun ReaderCrashCheckpoint.isNewerThanRoom(book: Book): Boolean {
    if (bookId != book.id) return false
    val roomTimestamp = book.lastOpenedAtEpochMs.coerceAtLeast(0L)
    if (committedAtEpochMs > roomTimestamp) return true
    if (committedAtEpochMs < roomTimestamp) return false

    val roomLocator = sanitizeReaderCrashLocatorJson(book.locatorJson)
    val samePosition =
        roomLocator == locatorJson &&
            abs(book.progress.toDouble() - progression) <= PROGRESS_EPSILON
    return !samePosition
}

private fun normalizeReaderCrashCheckpoint(
    raw: ReaderCrashCheckpoint
): ReaderCrashCheckpoint? {
    val bookId = raw.bookId.trim()
    val sessionId = raw.sessionId.trim()
    val locator = sanitizeReaderCrashLocatorJson(raw.locatorJson)
    if (
        bookId.isEmpty() ||
        bookId.length > MAX_ID_CHARS ||
        sessionId.isEmpty() ||
        sessionId.length > MAX_SESSION_CHARS ||
        raw.writerEpoch <= 0L ||
        raw.sequence <= 0L ||
        !raw.progression.isFinite() ||
        raw.progression !in 0.0..1.0 ||
        locator == null ||
        raw.committedAtEpochMs <= 0L
    ) {
        return null
    }

    return raw.copy(
        bookId = bookId,
        sessionId = sessionId,
        progression = raw.progression.coerceIn(0.0, 1.0),
        locatorJson = locator
    )
}

private const val READER_CRASH_CHECKPOINT_VERSION = 1
private const val MAX_ID_CHARS = 512
private const val MAX_SESSION_CHARS = 512
private const val MAX_LOCATOR_CHARS = 128 * 1024
private const val MAX_CHECKPOINT_JSON_CHARS = 192 * 1024
private const val PROGRESS_EPSILON = 0.000_01


/** A capacity hint to readBytes is not a size limit; stop before allocating an oversized record. */
internal fun readBoundedReaderCheckpoint(input: InputStream, maxBytes: Int): ByteArray? {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, 8_192))
    val buffer = ByteArray(minOf(maxBytes, 8_192))
    while (true) {
        val count = input.read(buffer)
        if (count < 0) return output.toByteArray()
        if (count > maxBytes - output.size()) return null
        output.write(buffer, 0, count)
    }
}
