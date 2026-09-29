package com.veilreader.app.domain

private const val DAY_MS = 86_400_000L
private const val SETTLED_AFTER_MS = 14L * DAY_MS
private const val DEEP_SHELF_AFTER_MS = 60L * DAY_MS
private const val FORGOTTEN_AFTER_MS = 180L * DAY_MS
private const val LONG_SILENCE_AFTER_MS = 45L * DAY_MS
private const val OLD_MARGIN_AFTER_MS = 90L * DAY_MS

enum class ArchiveDepth {
    SURFACE,
    SETTLED,
    DEEP_SHELF,
    FORGOTTEN
}

enum class LibraryMemoryEventKind {
    FORGOTTEN_VOLUME_RETURN,
    OLD_MARGIN_RETURN,
    LONG_SILENCE_RETURN
}

data class BookArchiveMemory(
    val bookId: String,
    val lastRecordedActivityAtEpochMs: Long,
    val inactiveMillis: Long,
    val depth: ArchiveDepth,
    val longestReturnGapMillis: Long?
) {
    val inactiveDays: Long get() = inactiveMillis / DAY_MS
    val isDeepShelf: Boolean
        get() = depth == ArchiveDepth.DEEP_SHELF || depth == ArchiveDepth.FORGOTTEN
}

data class LibraryMemoryEvent(
    val id: String,
    val kind: LibraryMemoryEventKind,
    val bookId: String,
    val atEpochMs: Long,
    val gapMillis: Long,
    val quoteExcerpt: String? = null
)

data class LibraryMemoryState(
    val byBookId: Map<String, BookArchiveMemory>,
    val deepShelfBookIds: List<String>,
    val events: List<LibraryMemoryEvent>
) {
    fun memoryFor(bookId: String): BookArchiveMemory? = byBookId[bookId]
    fun isDeepShelf(bookId: String): Boolean = byBookId[bookId]?.isDeepShelf == true

    companion object {
        val EMPTY = LibraryMemoryState(
            byBookId = emptyMap(),
            deepShelfBookIds = emptyList(),
            events = emptyList()
        )
    }
}

/**
 * Builds temporal depth for the local library using only timestamps Veil actually persisted.
 *
 * Deep Shelf is current inactivity. Rare return events are reconstructed only from durable session
 * gaps. The model never invents a "return" from a single last-opened timestamp.
 */
fun deriveLibraryMemoryState(
    books: List<Book>,
    highlights: List<Highlight>,
    sessions: List<ReadingSessionSnapshot>,
    nowEpochMs: Long = System.currentTimeMillis(),
    maxEvents: Int = 6
): LibraryMemoryState {
    if (books.isEmpty()) return LibraryMemoryState.EMPTY

    val safeNow = nowEpochMs.coerceAtLeast(0L)
    val booksById = books.associateBy { it.id }
    val sessionsByBook = sessions
        .filter { !it.bookId.isNullOrBlank() && it.bookId in booksById }
        .groupBy { requireNotNull(it.bookId) }
        .mapValues { (_, values) ->
            values.sortedWith(
                compareBy<ReadingSessionSnapshot> { it.startedAtEpochMs }
                    .thenBy { it.id }
            )
        }

    val memories = books.associate { book ->
        val bookSessions = sessionsByBook[book.id].orEmpty()
        val latestSessionAt = bookSessions.maxOfOrNull { session ->
            maxOf(session.startedAtEpochMs, session.endedAtEpochMs)
        } ?: 0L

        val hasReadingHistory =
            book.lastOpenedAtEpochMs > 0L ||
                latestSessionAt > 0L ||
                book.progress > 0f ||
                book.finished

        val lastRecorded = if (hasReadingHistory) {
            maxOf(book.lastOpenedAtEpochMs, latestSessionAt).coerceAtLeast(0L)
        } else {
            book.addedAtEpochMs.coerceAtLeast(0L)
        }

        val inactiveMillis = if (lastRecorded > 0L && safeNow >= lastRecorded) {
            safeNow - lastRecorded
        } else {
            0L
        }

        val depth = when {
            inactiveMillis >= FORGOTTEN_AFTER_MS -> ArchiveDepth.FORGOTTEN
            inactiveMillis >= DEEP_SHELF_AFTER_MS -> ArchiveDepth.DEEP_SHELF
            inactiveMillis >= SETTLED_AFTER_MS -> ArchiveDepth.SETTLED
            else -> ArchiveDepth.SURFACE
        }

        val longestGap = consecutiveSessionGaps(bookSessions).maxOrNull()

        book.id to BookArchiveMemory(
            bookId = book.id,
            lastRecordedActivityAtEpochMs = lastRecorded,
            inactiveMillis = inactiveMillis,
            depth = depth,
            longestReturnGapMillis = longestGap
        )
    }

    val rawEvents = buildList {
        sessionsByBook.forEach { (bookId, bookSessions) ->
            val book = booksById[bookId] ?: return@forEach
            consecutiveSessionPairs(bookSessions).forEach { (previous, current) ->
                val previousEnd = sessionEnd(previous)
                val gap = current.startedAtEpochMs - previousEnd
                if (gap >= FORGOTTEN_AFTER_MS) {
                    add(
                        LibraryMemoryEvent(
                            id = "forgotten:${book.id}:${current.id}",
                            kind = LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN,
                            bookId = book.id,
                            atEpochMs = current.startedAtEpochMs,
                            gapMillis = gap
                        )
                    )
                }
            }
        }

        val globalSessions = sessions
            .filter {
                !it.bookId.isNullOrBlank() &&
                    it.bookId in booksById &&
                    it.startedAtEpochMs > 0L
            }
            .sortedWith(
                compareBy<ReadingSessionSnapshot> { it.startedAtEpochMs }
                    .thenBy { it.id }
            )

        consecutiveSessionPairs(globalSessions).forEach { (previous, current) ->
            val bookId = current.bookId ?: return@forEach
            val book = booksById[bookId] ?: return@forEach
            val gap = current.startedAtEpochMs - sessionEnd(previous)
            if (gap >= LONG_SILENCE_AFTER_MS) {
                add(
                    LibraryMemoryEvent(
                        id = "silence:${current.id}",
                        kind = LibraryMemoryEventKind.LONG_SILENCE_RETURN,
                        bookId = bookId,
                        atEpochMs = current.startedAtEpochMs,
                        gapMillis = gap
                    )
                )
            }
        }

        highlights
            .asSequence()
            .filter {
                it.bookId in booksById &&
                    it.createdAtEpochMs > 0L
            }
            .forEach { highlight ->
                val book = booksById[highlight.bookId] ?: return@forEach
                val returnSession = sessionsByBook[highlight.bookId]
                    .orEmpty()
                    .firstOrNull { session ->
                        session.startedAtEpochMs - highlight.createdAtEpochMs >= OLD_MARGIN_AFTER_MS
                    }
                    ?: return@forEach

                val gap = returnSession.startedAtEpochMs - highlight.createdAtEpochMs
                val quote = highlight.quote
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(92)
                    .takeIf { it.isNotBlank() }

                add(
                    LibraryMemoryEvent(
                        id = "margin:${highlight.id}:${returnSession.id}",
                        kind = LibraryMemoryEventKind.OLD_MARGIN_RETURN,
                        bookId = book.id,
                        atEpochMs = returnSession.startedAtEpochMs,
                        gapMillis = gap,
                        quoteExcerpt = quote
                    )
                )
            }
    }

    val events = rawEvents
        .sortedWith(
            compareByDescending<LibraryMemoryEvent> { it.atEpochMs }
                .thenBy { eventPriority(it.kind) }
                .thenBy { it.id }
        )
        .distinctBy { "${it.bookId}:${it.atEpochMs}" }
        .take(maxEvents.coerceAtLeast(0))

    val deepShelfBookIds = memories.values
        .asSequence()
        .filter(BookArchiveMemory::isDeepShelf)
        .sortedWith(
            compareByDescending<BookArchiveMemory> { it.inactiveMillis }
                .thenBy { it.bookId }
        )
        .map { it.bookId }
        .toList()

    return LibraryMemoryState(
        byBookId = memories,
        deepShelfBookIds = deepShelfBookIds,
        events = events
    )
}

private fun consecutiveSessionPairs(
    sessions: List<ReadingSessionSnapshot>
): List<Pair<ReadingSessionSnapshot, ReadingSessionSnapshot>> =
    sessions.zipWithNext()

private fun consecutiveSessionGaps(
    sessions: List<ReadingSessionSnapshot>
): List<Long> =
    consecutiveSessionPairs(sessions)
        .map { (previous, current) ->
            current.startedAtEpochMs - sessionEnd(previous)
        }
        .filter { it >= 0L }

private fun sessionEnd(session: ReadingSessionSnapshot): Long =
    maxOf(session.startedAtEpochMs, session.endedAtEpochMs)

private fun eventPriority(kind: LibraryMemoryEventKind): Int = when (kind) {
    LibraryMemoryEventKind.FORGOTTEN_VOLUME_RETURN -> 0
    LibraryMemoryEventKind.OLD_MARGIN_RETURN -> 1
    LibraryMemoryEventKind.LONG_SILENCE_RETURN -> 2
}
