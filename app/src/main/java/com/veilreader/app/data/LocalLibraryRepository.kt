package com.veilreader.app.data

import android.content.Context
import androidx.room.withTransaction
import com.veilreader.app.data.db.BookCollectionCrossRef
import com.veilreader.app.data.db.BookEntity
import com.veilreader.app.data.db.CollectionEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.db.normalizeCollectionName
import com.veilreader.app.data.db.toDomain
import com.veilreader.app.data.db.toEntity
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReaderAppearance
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runtime bookshelf repository.
 *
 * Room is the structured source of truth and DataStore owns reader preferences. The public API is
 * intentionally kept compatible with the 0.6 Compose screens during Phase 1B; Phase 2 will move
 * these synchronous UI callbacks behind ViewModels/use-cases.
 */
class LocalLibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = VeilDatabase.get(appContext)
    private val settings = SettingsStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    private val _appearance = MutableStateFlow(ReaderAppearance())

    init {
        scope.launch { for (write in writes) write() }
        scope.launch {
            database.books().observeAll().collect { rows -> _books.value = rows.map { it.toDomain() } }
        }
        scope.launch {
            database.highlights().observeAll().collect { rows -> _highlights.value = rows.map { it.toDomain() } }
        }
        scope.launch {
            database.bookmarks().observeAll().collect { rows -> _bookmarks.value = rows.map { it.toDomain() } }
        }
        scope.launch {
            settings.settings.collect { _appearance.value = it.readerAppearance }
        }
    }

    fun addBookmark(bookId: String, label: String, locatorJson: String): Boolean {
        if (_bookmarks.value.any { it.bookId == bookId && it.locatorJson == locatorJson }) return false
        val bookmark = Bookmark(UUID.randomUUID().toString(), bookId, label, locatorJson)
        _bookmarks.value = listOf(bookmark) + _bookmarks.value
        enqueue { database.bookmarks().upsert(bookmark.toEntity()) }
        return true
    }

    fun deleteBookmark(id: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == id }
        enqueue { database.bookmarks().deleteById(id) }
    }

    fun updateHighlightNote(id: String, note: String) {
        val updated = _highlights.value.firstOrNull { it.id == id }?.copy(note = note.trim()) ?: return
        _highlights.value = _highlights.value.map { if (it.id == id) updated else it }
        enqueue { database.highlights().upsert(updated.toEntity()) }
    }

    fun loadAppearance(): ReaderAppearance = _appearance.value

    fun saveAppearance(value: ReaderAppearance) {
        _appearance.value = value
        enqueue { settings.saveReaderAppearance(value) }
    }

    fun addImportedBook(book: Book) {
        val existing = _books.value.firstOrNull { it.sourceUri == book.sourceUri }
        val stored = if (existing != null) book.copy(id = existing.id) else book
        _books.value = listOf(stored) + _books.value.filterNot { it.id == stored.id }
        enqueue {
            upsertBookPreservingExtendedMetadata(stored)
            setCollectionsInternal(stored.id, setOf(stored.collection))
        }
    }

    fun getBook(id: String): Book? = _books.value.firstOrNull { it.id == id }

    fun editMetadata(id: String, title: String, author: String, collection: String) {
        require(title.isNotBlank()) { "A book title cannot be empty." }
        val updated = updateBookCached(id) {
            it.copy(
                title = title.trim(),
                author = author.trim().ifEmpty { "Unknown author" },
                collection = collection.trim()
            )
        } ?: return
        enqueue {
            database.withTransaction {
                upsertBookPreservingExtendedMetadata(updated)
                setCollectionsInternal(id, setOf(updated.collection))
            }
        }
    }

    fun toggleFavorite(id: String) {
        val updated = updateBookCached(id) { it.copy(favorite = !it.favorite) } ?: return
        enqueue { upsertBookPreservingExtendedMetadata(updated) }
    }

    fun markOpened(id: String) {
        val updated = updateBookCached(id) { it.copy(lastOpenedAtEpochMs = System.currentTimeMillis()) } ?: return
        enqueue { upsertBookPreservingExtendedMetadata(updated) }
    }

    /** Returns true when this update completed the book for the first time. */
    fun saveProgress(id: String, progression: Double, locatorJson: String): Boolean {
        val current = getBook(id) ?: return false
        val safe = (if (progression.isFinite()) progression else current.progress.toDouble())
            .coerceIn(0.0, 1.0).toFloat()
        val finishedNow = safe >= 0.995f
        val newlyFinished = finishedNow && !current.finished
        val estimatedRead = if (current.totalPages > 0) {
            (current.totalPages * safe).toInt().coerceAtMost(current.totalPages)
        } else current.pagesRead
        val updated = current.copy(
            progress = safe,
            pagesRead = estimatedRead,
            locatorJson = locatorJson,
            lastOpenedAtEpochMs = System.currentTimeMillis(),
            finished = current.finished || finishedNow
        )
        replaceBookCached(updated)
        enqueue { upsertBookPreservingExtendedMetadata(updated) }
        return newlyFinished
    }

    fun addHighlight(bookId: String, quote: String, locatorJson: String): Highlight {
        val cleanQuote = quote.trim()
        _highlights.value.firstOrNull {
            it.bookId == bookId && it.locatorJson == locatorJson && it.quote == cleanQuote
        }?.let { return it }
        val record = Highlight(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            quote = cleanQuote,
            locatorJson = locatorJson
        )
        _highlights.value = listOf(record) + _highlights.value
        enqueue { database.highlights().upsert(record.toEntity()) }
        return record
    }

    fun highlightsFor(bookId: String): List<Highlight> = _highlights.value.filter { it.bookId == bookId }

    fun deleteHighlight(id: String) {
        _highlights.value = _highlights.value.filterNot { it.id == id }
        enqueue { database.highlights().deleteById(id) }
    }

    /** Ensures queued UI writes have reached Room/DataStore before export or destructive work. */
    suspend fun flushWrites() {
        val done = CompletableDeferred<Unit>()
        writes.send { done.complete(Unit) }
        done.await()
    }

    suspend fun snapshot(): LibrarySnapshot {
        flushWrites()
        return LibrarySnapshot(
            books = _books.value.toList(),
            highlights = _highlights.value.toList(),
            bookmarks = _bookmarks.value.toList(),
            appearance = _appearance.value
        )
    }

    /** Transactional replacement used by backup restore. */
    suspend fun replaceAll(snapshot: LibrarySnapshot) {
        flushWrites()
        database.withTransaction {
            database.highlights().deleteAll()
            database.bookmarks().deleteAll()
            database.collections().clearAllLinks()
            database.books().deleteAll()
            database.collections().deleteAll()
            database.readingSessions().deleteAll()

            if (snapshot.books.isNotEmpty()) database.books().upsertAll(snapshot.books.map { it.toEntity() })
            for (book in snapshot.books) setCollectionsInternal(book.id, setOf(book.collection))
            if (snapshot.highlights.isNotEmpty()) database.highlights().upsertAll(snapshot.highlights.map { it.toEntity() })
            if (snapshot.bookmarks.isNotEmpty()) database.bookmarks().upsertAll(snapshot.bookmarks.map { it.toEntity() })
        }
        settings.saveReaderAppearance(snapshot.appearance)
        _appearance.value = snapshot.appearance
    }

    private fun enqueue(block: suspend () -> Unit) {
        check(writes.trySend(block).isSuccess) { "Veil Reader storage queue is unavailable." }
    }

    private fun updateBookCached(id: String, transform: (Book) -> Book): Book? {
        val current = getBook(id) ?: return null
        val updated = transform(current)
        replaceBookCached(updated)
        return updated
    }

    private fun replaceBookCached(book: Book) {
        _books.value = _books.value.map { if (it.id == book.id) book else it }
    }

    private suspend fun upsertBookPreservingExtendedMetadata(book: Book) {
        val existing = database.books().findEntity(book.id)
        val base = book.toEntity()
        database.books().upsert(
            base.copy(
                coverCachePath = existing?.coverCachePath,
                contentFingerprint = existing?.contentFingerprint,
                seriesName = existing?.seriesName,
                seriesIndex = existing?.seriesIndex,
                language = existing?.language
            )
        )
    }

    private suspend fun setCollectionsInternal(bookId: String, names: Set<String>) {
        database.collections().clearBook(bookId)
        names.map(String::trim).filter(String::isNotEmpty).distinctBy(::normalizeCollectionName).forEach { name ->
            val normalized = normalizeCollectionName(name)
            val collection = database.collections().findByNormalizedName(normalized) ?: CollectionEntity(
                id = stableCollectionId(normalized),
                name = name,
                createdAtEpochMs = System.currentTimeMillis(),
                normalizedName = normalized
            ).also { database.collections().upsert(it) }
            database.collections().attach(BookCollectionCrossRef(bookId, collection.id))
        }
    }
}

data class LibrarySnapshot(
    val books: List<Book>,
    val highlights: List<Highlight>,
    val bookmarks: List<Bookmark>,
    val appearance: ReaderAppearance
)

private fun stableCollectionId(normalizedName: String): String = UUID.nameUUIDFromBytes(
    "veil-collection:$normalizedName".toByteArray(StandardCharsets.UTF_8)
).toString()
