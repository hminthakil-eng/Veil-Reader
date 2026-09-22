package com.veilreader.app.diagnostics

import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/**
 * Debug-only, read-only Reader observability.
 *
 * Keep payloads metadata-only: never log titles, selected text, notes, or raw locator JSON.
 */
object ReaderTrace {
    private const val TAG = "VeilReaderTrace"
    private val sequence = AtomicLong(0L)

    /**
     * Stable, non-reversible locator fingerprint for QA/restore evidence.
     *
     * Only the first 12 hex characters are logged. Raw locator JSON is never emitted.
     */
    fun fingerprint(raw: String?): String? {
        val value = raw?.takeIf { it.isNotBlank() } ?: return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.take(6).joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    fun event(
        name: String,
        bookId: String? = null,
        sessionId: String? = null,
        details: String? = null
    ): Long {
        val seq = sequence.incrementAndGet()
        val payload = buildString {
            append("seq=").append(seq)
            append(" event=").append(name)
            bookId?.let { append(" bookId=").append(it) }
            sessionId?.let { append(" sessionId=").append(it) }
            details?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
        }
        Log.d(TAG, payload)
        return seq
    }
}
