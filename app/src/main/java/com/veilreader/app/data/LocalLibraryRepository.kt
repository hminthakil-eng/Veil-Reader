package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.veilreader.app.data.db.BookCollectionCrossRef
import com.veilreader.app.data.db.CollectionEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.db.normalizeCollectionName
import com.veilreader.app.data.db.toDomain
import com.veilreader.app.data.db.toEntity
import com.veilreader.app.data.db.toSnapshot
import com.veilreader.app.data.migration.LegacyLibraryMigrator
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookMetadataUpdate
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReadingSessionSnapshot
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runtime bookshelf repository.
 *
 * Room is the structured source of truth and DataStore owns reader preferences. All durable writes
 * share one queue so progress, annotations, derived metadata and backup snapshots have deterministic
 * ordering.
 */
class LocalLibraryRepository internal constructor(
    private val appContext: Context,
    private val database: VeilDatabase,
    private val settings: SettingsStore,
    private val runLegacyMigration: Boolean
) {
    constructor(context: Context) : this(
        appContext = context.applicationContext,
        database = VeilDatabase.get(context.applicationContext),
        settings = SettingsStore(context.applicationContext),
        runLegacyMigration = true
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val initialized = CompletableDeferred<Unit>()
    private val storageFailure = AtomicReference<Throwable?>(null)
    private val progressLock = Any()
    private val pendingProgress = mutableMapOf<String, PendingProgressWrite>()
    private val progressFlushJobs = mutableMapOf<String, Job>()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    private val _appearance = MutableStateFlow(ReaderAppearance())

    init {
        scope.launch {
            for (write in writes) {
                try {
                    write()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    storageFailure.compareAndSet(null, error)
                }
            }
        }
        if (runLegacyMigration) {
            scope.launch {
                runCatching { LegacyLibraryMigrator(appContext, database, settings).migrateIfNeeded() }
                    .onSuccess { initialized.complete(Unit) }
                    .onFailure { initialized.completeExceptionally(it) }
            }
        } else {
            initialized.complete(Unit)
        }
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

    /**
     * Commits an inspected publication through the same serialized queue as every other library
     * write. A matching SHA-256 returns the existing book and removes the just-created temporary
     * publication/cover files, so concurrent duplicate imports cannot create duplicate records.
     */
    suspend fun addImportedBook(book: Book): BookImportResult {
        val result = orderedWrite {
            val duplicate = book.contentFingerprint
                ?.takeIf { it.isNotBlank() }
                ?.let { database.books().findByFingerprint(it)?.toDomain() }
            if (duplicate != null) {
                BookImportResult(book = duplicate, duplicate = true)
            } else {
                database.withTransaction {
                    database.books().upsert(book.toEntity())
                    setCollectionsInternal(book.id, book.allCollections.toSet())
                }
                BookImportResult(book = book, duplicate = false)
            }
        }

        if (result.duplicate) discardImportedArtifacts(book)
        val committed = result.book
        _books.value = listOf(committed) + _books.value.filterNot { it.id == committed.id }
        return result
    }

    fun getBook(id: String): Book? = _books.value.firstOrNull { it.id == id }

    /**
     * Stores a derived app-private cover thumbnail path. An empty string is a terminal sentinel
     * meaning extraction was attempted but this publication has no usable cover.
     */
    fun updateCoverCachePath(id: String, path: String) {
        val updated = updateBookCached(id) { it.copy(coverCachePath = path) } ?: return
        enqueue { database.books().upsert(updated.toEntity()) }
    }

    /** Backfills the derived duplicate-detection fingerprint for pre-0.8 library entries. */
    fun updateContentFingerprint(id: String, fingerprint: String) {
        if (fingerprint.isBlank()) return
        val updated = updateBookCached(id) { current ->
            if (!current.contentFingerprint.isNullOrBlank()) current
            else current.copy(contentFingerprint = fingerprint.lowercase(Locale.ROOT))
        } ?: return
        enqueue { database.books().upsert(updated.toEntity()) }
    }

    fun editMetadata(update: BookMetadataUpdate) {
        require(update.title.isNotBlank()) { "A book title cannot be empty." }
        val cleanCollections = update.collections
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
        val updated = updateBookCached(update.bookId) { current ->
            current.copy(
                title = update.title.trim(),
                author = update.author.trim().ifEmpty { "Unknown author" },
                seriesName = update.seriesName?.trim()?.takeIf { it.isNotEmpty() },
                seriesIndex = update.seriesIndex?.takeIf { it.isFinite() },
                language = update.language?.trim()?.takeIf { it.isNotEmpty() },
                collection = cleanCollections.firstOrNull().orEmpty(),
                collections = cleanCollections
            )
        } ?: return
        enqueue {
            database.withTransaction {
                database.books().upsert(updated.toEntity())
                setCollectionsInternal(update.bookId, cleanCollections.toSet())
            }
        }
    }

    fun toggleFavorite(id: String) {
        val updated = updateBookCached(id) { it.copy(favorite = !it.favorite) } ?: return
        enqueue { database.books().upsert(updated.toEntity()) }
    }

    fun markOpened(id: String) {
        flushProgress(id)
        val openedAtEpochMs = System.currentTimeMillis()
        updateBookCached(id) { it.copy(lastOpenedAtEpochMs = openedAtEpochMs) } ?: return
        enqueue {
            check(database.books().updateLastOpened(id, openedAtEpochMs) == 1) {
                "Book disappeared before its opened timestamp could be persisted: $id"
            }
        }
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
        queueProgressWrite(
            PendingProgressWrite(
                id = id,
                progress = updated.progress,
                pagesRead = updated.pagesRead,
                locatorJson = locatorJson,
                lastOpenedAtEpochMs = updated.lastOpenedAtEpochMs,
                finished = updated.finished
            ),
            immediate = newlyFinished
        )
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

    /** Session writes share the library queue so backup snapshots cannot pass an unfinished close. */
    fun saveReadingSession(snapshot: ReadingSessionSnapshot) {
        enqueue { database.readingSessions().upsert(snapshot.toEntity()) }
    }

    /** Ensures migration and all writes queued before this call have reached durable storage. */
    suspend fun flushWrites() {
        initialized.await()
        flushAllProgress()
        val done = CompletableDeferred<Unit>()
        writes.send { done.complete(Unit) }
        done.await()
        storageFailure.get()?.let { throw IllegalStateException("A library write failed.", it) }
    }

    /** Returns one point-in-time database snapshot ordered with all normal reader writes. */
    suspend fun snapshot(): LibrarySnapshot {
        flushAllProgress()
        return orderedWrite {
            val databaseState = database.withTransaction {
                DatabaseLibraryState(
                    books = database.books().listAllWithCollections().map { it.toDomain() },
                    highlights = database.highlights().listAll().map { it.toDomain() },
                    bookmarks = database.bookmarks().listAll().map { it.toDomain() },
                    readingSessions = database.readingSessions().listAll().map { it.toSnapshot() }
                )
            }
            val appearance = settings.settings.first().readerAppearance
            LibrarySnapshot(
                books = databaseState.books,
                highlights = databaseState.highlights,
                bookmarks = databaseState.bookmarks,
                appearance = appearance,
                readingSessions = databaseState.readingSessions
            )
        }
    }

    /** Transactional replacement used by backup restore, serialized with normal reader writes. */
    suspend fun replaceAll(snapshot: LibrarySnapshot) {
        discardAllPendingProgress()
        orderedWrite {
            database.withTransaction {
                database.highlights().deleteAll()
                database.bookmarks().deleteAll()
                database.collections().clearAllLinks()
                database.books().deleteAll()
                database.collections().deleteAll()
                database.readingSessions().deleteAll()

                if (snapshot.books.isNotEmpty()) database.books().upsertAll(snapshot.books.map { it.toEntity() })
                for (book in snapshot.books) setCollectionsInternal(book.id, book.allCollections.toSet())
                if (snapshot.highlights.isNotEmpty()) database.highlights().upsertAll(snapshot.highlights.map { it.toEntity() })
                if (snapshot.bookmarks.isNotEmpty()) database.bookmarks().upsertAll(snapshot.bookmarks.map { it.toEntity() })
                if (snapshot.readingSessions.isNotEmpty()) {
                    database.readingSessions().upsertAll(snapshot.readingSessions.map { it.toEntity() })
                }
            }
            settings.saveReaderAppearance(snapshot.appearance)
            _appearance.value = snapshot.appearance
        }
    }

    /**
     * Coalesces locator churn into at most one durable progress write per interval. The in-memory
     * book state still updates immediately, so UI and completion semantics remain synchronous.
     */
    private fun queueProgressWrite(value: PendingProgressWrite, immediate: Boolean) {
        synchronized(progressLock) {
            pendingProgress[value.id] = value
            if (!immediate && progressFlushJobs[value.id]?.isActive == true) return@synchronized

            progressFlushJobs.remove(value.id)?.cancel()
            if (!immediate) {
                progressFlushJobs[value.id] = scope.launch {
                    delay(PROGRESS_WRITE_INTERVAL_MS)
                    val pending = synchronized(progressLock) {
                        progressFlushJobs.remove(value.id)
                        pendingProgress.remove(value.id)
                    }
                    pending?.let(::enqueueProgressWrite)
                }
            }
        }
        if (immediate) flushProgress(value.id)
    }

    /** Enqueues the latest pending progress for one book immediately. Safe to call from lifecycle hooks. */
    fun flushProgress(bookId: String) {
        val pending = synchronized(progressLock) {
            progressFlushJobs.remove(bookId)?.cancel()
            pendingProgress.remove(bookId)
        }
        pending?.let(::enqueueProgressWrite)
    }

    private fun flushAllProgress() {
        val pending = synchronized(progressLock) {
            progressFlushJobs.values.forEach(Job::cancel)
            progressFlushJobs.clear()
            pendingProgress.values.toList().also { pendingProgress.clear() }
        }
        pending.forEach(::enqueueProgressWrite)
    }

    private fun discardAllPendingProgress() {
        synchronized(progressLock) {
            progressFlushJobs.values.forEach(Job::cancel)
            progressFlushJobs.clear()
            pendingProgress.clear()
        }
    }

    private fun enqueueProgressWrite(value: PendingProgressWrite) {
        enqueue {
            check(
                database.books().updateProgress(
                    id = value.id,
                    progress = value.progress,
                    pagesRead = value.pagesRead,
                    locatorJson = value.locatorJson,
                    lastOpenedAtEpochMs = value.lastOpenedAtEpochMs,
                    finished = value.finished
                ) == 1
            ) {
                "Book disappeared before its progress could be persisted: ${value.id}"
            }
        }
    }

    private fun enqueue(block: suspend () -> Unit) {
        val guarded: suspend () -> Unit = {
            initialized.await()
            storageFailure.get()?.let { throw IllegalStateException("A previous library write failed.", it) }
            block()
        }
        check(writes.trySend(guarded).isSuccess) { "Veil Reader storage queue is unavailable." }
    }

    private suspend fun <T> orderedWrite(block: suspend () -> T): T {
        initialized.await()
        val result = CompletableDeferred<T>()
        writes.send {
            try {
                storageFailure.get()?.let { throw IllegalStateException("A previous library write failed.", it) }
                result.complete(block())
            } catch (cancelled: CancellationException) {
                result.cancel(cancelled)
                throw cancelled
            } catch (error: Throwable) {
                result.completeExceptionally(error)
                throw error
            }
        }
        return result.await()
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

    private suspend fun discardImportedArtifacts(book: Book) = withContext(Dispatchers.IO) {
        deleteAppPrivateFile(book.sourceUri, "publications")
        book.coverCachePath?.takeIf { it.isNotBlank() }?.let { path ->
            deleteAppPrivatePath(path, "covers")
        }
    }

    private fun deleteAppPrivateFile(uriText: String?, child: String) {
        val path = uriText?.let(Uri::parse)?.takeIf { it.scheme == "file" }?.path ?: return
        deleteAppPrivatePath(path, child)
    }

    private fun deleteAppPrivatePath(path: String, child: String) {
        runCatching {
            val root = File(appContext.filesDir, child).canonicalFile
            val candidate = File(path).canonicalFile
            if (candidate.toPath().startsWith(root.toPath()) && candidate.isFile) candidate.delete()
        }
    }
}

data class BookImportResult(
    val book: Book,
    val duplicate: Boolean
)

data class LibrarySnapshot(
    val books: List<Book>,
    val highlights: List<Highlight>,
    val bookmarks: List<Bookmark>,
    val appearance: ReaderAppearance,
    val readingSessions: List<ReadingSessionSnapshot> = emptyList()
) {
    companion object
}

private data class DatabaseLibraryState(
    val books: List<Book>,
    val highlights: List<Highlight>,
    val bookmarks: List<Bookmark>,
    val readingSessions: List<ReadingSessionSnapshot>
)

private data class PendingProgressWrite(
    val id: String,
    val progress: Float,
    val pagesRead: Int,
    val locatorJson: String,
    val lastOpenedAtEpochMs: Long,
    val finished: Boolean
)

private const val PROGRESS_WRITE_INTERVAL_MS = 250L

private fun stableCollectionId(normalizedName: String): String = UUID.nameUUIDFromBytes(
    "veil-collection:$normalizedName".toByteArray(StandardCharsets.UTF_8)
).toString()
