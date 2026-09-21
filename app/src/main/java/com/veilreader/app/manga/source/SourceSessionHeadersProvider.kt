package com.veilreader.app.manga.source

/**
 * Process-local source session boundary.
 *
 * Implementations may expose short-lived Cookie/User-Agent values after an interactive challenge.
 * Returned headers are execution-only and must never be persisted into Room, SavedState or logs.
 */
fun interface SourceSessionHeadersProvider {
    fun headersFor(sourceId: SourceId, domain: String): Map<String, String>
}
