package com.veilreader.app.data

import android.content.Context
import android.util.AtomicFile
import com.veilreader.app.domain.Book
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Tiny crash-recovery journal for the last semantic Reader commit.
 *
 * Room remains the long-term source of truth. This file exists only to bridge the interval between
 * a user-visible committed navigation and the serialized Room progress write reaching storage.
 */
internal data class ReaderCrashRecoveryCheckpoint(
    val bookId: String,
    val sessionId: String,
    val writerEpoch: Long,
    val sequence: Long,
    val locatorJson: String,
    val progression: Float,
    val committedAtEpochMs: Long
) {
    fun isValid(): Boolean =
        bookId.isNotBlank() &&
            sessionId.isNotBlank() &&
            writerEpoch > 0L &&
            sequence > 0L &&
            locatorJson.isNotBlank() &&
            progression.isFinite() &&
            progression in 0f..1f &&
            committedAtEpochMs > 0L
}

/**
 * Crash recovery needs structural navigation coordinates, never publication prose.
 *
 * Readium locators may carry a `text` excerpt and a human-readable `title`. Neither is required
 * to restore the semantic position, so the emergency journal strips both before touching disk.
 */
internal fun sanitizeReaderCrashRecoveryLocatorJson(locatorJson: String): String? =
    runCatching {
        val json = JSONObject(locatorJson)
        require(json.optString("href").isNotBlank()) { "Reader locator href is missing." }
        json.remove("text")
        json.remove("title")
        json.toString()
    }.getOrNull()

/**
 * A checkpoint is authoritative only while it is strictly newer than Room's progress timestamp.
 *
 * Equal timestamps mean the corresponding progress write has already reached Room. We deliberately
 * prefer Room in that case so an old journal can never resurrect an older semantic location.
 */
internal fun freshReaderCrashRecoveryCheckpoint(
    book: Book,
    checkpoint: ReaderCrashRecoveryCheckpoint?
): ReaderCrashRecoveryCheckpoint? =
    checkpoint
        ?.takeIf { it.isValid() && it.bookId == book.id }
        ?.takeIf { it.committedAtEpochMs > book.lastOpenedAtEpochMs }

internal class ReaderCrashRecoveryStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, DIRECTORY_NAME)

    @Synchronized
    fun write(checkpoint: ReaderCrashRecoveryCheckpoint) {
        require(checkpoint.isValid()) { "Invalid Reader crash-recovery checkpoint." }
        val safeLocatorJson = requireNotNull(
            sanitizeReaderCrashRecoveryLocatorJson(checkpoint.locatorJson)
        ) { "Reader crash-recovery locator is not structurally safe." }
        if (!root.exists() && !root.mkdirs() && !root.isDirectory) {
            error("Unable to create Reader crash-recovery directory.")
        }

        val atomic = atomicFile(checkpoint.bookId)
        val payload = JSONObject()
            .put("version", FORMAT_VERSION)
            .put("bookId", checkpoint.bookId)
            .put("sessionId", checkpoint.sessionId)
            .put("writerEpoch", checkpoint.writerEpoch)
            .put("sequence", checkpoint.sequence)
            .put("locatorJson", safeLocatorJson)
            .put("progression", checkpoint.progression.toDouble())
            .put("committedAtEpochMs", checkpoint.committedAtEpochMs)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        require(payload.size <= MAX_RECORD_BYTES) { "Reader crash-recovery checkpoint is too large." }

        val stream = atomic.startWrite()
        try {
            stream.write(payload)
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    @Synchronized
    fun read(bookId: String): ReaderCrashRecoveryCheckpoint? {
        if (bookId.isBlank()) return null
        val atomic = atomicFile(bookId)
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) return null

        return runCatching {
            val raw = atomic.openRead().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    check(output.size() <= MAX_RECORD_BYTES) {
                        "Reader crash-recovery checkpoint exceeded size limit."
                    }
                }
                output.toString(StandardCharsets.UTF_8.name())
            }
            val json = JSONObject(raw)
            check(json.optInt("version", -1) == FORMAT_VERSION) {
                "Unsupported Reader crash-recovery checkpoint version."
            }
            val rawLocatorJson = json.getString("locatorJson")
            val safeLocatorJson = sanitizeReaderCrashRecoveryLocatorJson(rawLocatorJson)
                ?: error("Invalid Reader crash-recovery locator payload.")
            check(rawLocatorJson == safeLocatorJson) {
                "Reader crash-recovery payload contained non-recovery publication metadata."
            }
            ReaderCrashRecoveryCheckpoint(
                bookId = json.getString("bookId"),
                sessionId = json.getString("sessionId"),
                writerEpoch = json.getLong("writerEpoch"),
                sequence = json.getLong("sequence"),
                locatorJson = safeLocatorJson,
                progression = json.getDouble("progression").toFloat(),
                committedAtEpochMs = json.getLong("committedAtEpochMs")
            ).also { checkpoint ->
                check(checkpoint.isValid() && checkpoint.bookId == bookId) {
                    "Invalid Reader crash-recovery checkpoint payload."
                }
            }
        }.getOrElse {
            atomic.delete()
            null
        }
    }

    /**
     * Clear only when the persisted Room write is known to cover the journal entry.
     *
     * Exact writer identity protects a newer same-millisecond checkpoint from an older queued write.
     * A strictly newer Room timestamp also covers checkpoints left by an older session/process.
     */
    @Synchronized
    fun clearIfCovered(
        bookId: String,
        sessionId: String,
        order: ReaderProgressWriteOrder,
        persistedAtEpochMs: Long
    ): Boolean {
        val checkpoint = read(bookId) ?: return false
        val sameWriterCovered =
            checkpoint.sessionId == sessionId &&
                checkpoint.writerEpoch == order.epoch &&
                checkpoint.sequence <= order.sequence
        val newerDurableWriteCovered = persistedAtEpochMs > checkpoint.committedAtEpochMs
        if (!sameWriterCovered && !newerDurableWriteCovered) return false
        atomicFile(bookId).delete()
        return true
    }

    @Synchronized
    fun clear(bookId: String) {
        if (bookId.isBlank()) return
        atomicFile(bookId).delete()
    }

    @Synchronized
    fun clearAll() {
        root.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
    }

    private fun atomicFile(bookId: String): AtomicFile =
        AtomicFile(File(root, checkpointFileName(bookId)))

    private fun checkpointFileName(bookId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(bookId.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) } +
            ".json"
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val MAX_RECORD_BYTES = 256 * 1024
        const val DIRECTORY_NAME = "reader-recovery-v1"
    }
}
