@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.veilreader.app.ui.reader

import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search

/**
 * Reuses Readium's indexed/streaming text extraction for EPUB, never a second
 * homemade parser. Readium may return unsupported for non-searchable formats.
 *
 * This is a data-layer foundation, NOT a finished search UI. Session owns one
 * cursor; callers must close when changing book/query or leaving the Reader.
 * Queries, match text, and locators remain on-device.
 */
internal sealed interface ReaderBookSearchOpen {
    data class Ready(val session: ReaderBookSearchSession) : ReaderBookSearchOpen
    data object Unsupported : ReaderBookSearchOpen
    data object InvalidQuery : ReaderBookSearchOpen
    data object Failed : ReaderBookSearchOpen
}

internal sealed interface ReaderBookSearchPage {
    data class Hits(val locators: List<Locator>, val isLast: Boolean) : ReaderBookSearchPage
    data object Failed : ReaderBookSearchPage
    data object Closed : ReaderBookSearchPage
}

internal suspend fun openReaderBookSearch(
    rawQuery: String,
    cursorFactory: suspend (String) -> SearchIterator?
): ReaderBookSearchOpen {
    val query = rawQuery.trim()
    if (query.isBlank() || query.length > 128 || '\u0000' in query) {
        return ReaderBookSearchOpen.InvalidQuery
    }
    return try {
        val cursor = cursorFactory(query) ?: return ReaderBookSearchOpen.Unsupported
        ReaderBookSearchOpen.Ready(ReaderBookSearchSession(cursor))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ReaderBookSearchOpen.Failed
    }
}

/** Entrypoint used later by Sanctuary search chrome. No new engine dependency. */
internal suspend fun Publication.openVeilBookSearch(query: String): ReaderBookSearchOpen =
    openReaderBookSearch(query) { sanitized -> search(sanitized) }

/**
 * A single-consumer, serialized cursor that retains excess hits when a Readium
 * resource has more results than one Veil UI page. It never eagerly loads an
 * entire 1,000+ chapter publication or silently discards matches.
 */
internal class ReaderBookSearchSession(
    private val cursor: SearchIterator
) : Closeable {
    private val lock = Mutex()
    private val pending = ArrayDeque<Locator>()
    private var ended = false
    private var closed = false

    suspend fun nextPage(limit: Int = 40): ReaderBookSearchPage = lock.withLock {
        if (closed) return@withLock ReaderBookSearchPage.Closed
        require(limit in 1..100) { "Search page size must be between 1 and 100" }

        val hits = ArrayList<Locator>(limit)
        try {
            while (hits.size < limit) {
                currentCoroutineContext().ensureActive()

                if (pending.isNotEmpty()) {
                    hits += pending.removeFirst()
                    continue
                }
                if (ended) break

                val next = cursor.next()
                if (next.isFailure) {
                    closeUnsafe()
                    return@withLock ReaderBookSearchPage.Failed
                }
                val collection = next.getOrNull()
                if (collection == null) {
                    ended = true
                    break
                }
                pending.addAll(collection.locators)
                // Empty resource pages are allowed; Readium advances its cursor.
            }

            val isLast = ended && pending.isEmpty()
            if (isLast) closeUnsafe()
            ReaderBookSearchPage.Hits(hits, isLast)
        } catch (cancelled: CancellationException) {
            closeUnsafe()
            throw cancelled
        } catch (_: Exception) {
            closeUnsafe()
            ReaderBookSearchPage.Failed
        }
    }

    override fun close() {
        // close() intentionally does not suspend. Mark closed immediately so
        // active/pending readers cannot publish results to a retired session.
        closeUnsafe()
    }

    private fun closeUnsafe() {
        if (closed) return
        closed = true
        ended = true
        pending.clear()
        runCatching { cursor.close() }
    }
}
