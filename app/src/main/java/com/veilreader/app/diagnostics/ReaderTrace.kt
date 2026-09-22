package com.veilreader.app.diagnostics

import android.util.Log
import com.veilreader.app.BuildConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * Debug-only, read-only Reader observability.
 *
 * Keep payloads metadata-only: never log titles, selected text, notes, or raw locator JSON.
 */
object ReaderTrace {
    private const val TAG = "VeilReaderTrace"
    private val sequence = AtomicLong(0L)

    fun event(
        name: String,
        bookId: String? = null,
        sessionId: String? = null,
        details: String? = null
    ): Long {
        val seq = sequence.incrementAndGet()
        if (BuildConfig.DEBUG) {
            val payload = buildString {
                append("seq=").append(seq)
                append(" event=").append(name)
                bookId?.let { append(" bookId=").append(it) }
                sessionId?.let { append(" sessionId=").append(it) }
                details?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
            }
            Log.d(TAG, payload)
        }
        return seq
    }
}
