package com.veilreader.app.data

import com.veilreader.app.diagnostics.ReaderTrace

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
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReadingContinuitySummary
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.ReadingMilestoneRecord
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.buildSealedReadingCycle
import com.veilreader.app.domain.crossedReadingMilestones
import com.veilreader.app.domain.deriveReadingContinuity
import com.veilreader.app.domain.firstOpenedMilestone
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
import kotlinx.coroutines.cancel
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
    private val coalescingLock = Any()
    private val pendingProgress = mutableMapOf<String, PendingProgressWrite>()
    private val progressFlushJobs = mutableMapOf<String, Job>()
    private val pendingReadingSessions = mutableMapOf<String, ReadingSessionSnapshot>()
    private val sessionFlushJobs = mutableMapOf<String, Job>()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    val highlights: StateFlow<List<Highlight>> = _highlights

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    private val _readingSessions = MutableStateFlow<List<ReadingSessionSnapshot>>(emptyList())
    val readingSessions: StateFlow<List<ReadingSessionSnapshot>> = _readingSessions

    private val _readingCycles = MutableStateFlow<List<ReadingCycleRecord>>(emptyList())
    val readingCycles: StateFlow<List<ReadingCycleRecord>> = _readingCycles

    private val _passageVisits = MutableStateFlow<List<PassageVisit>>(emptyList())
    val passageVisits: StateFlow<List<PassageVisit>> = _passageVisits

    private val _readingMilestones = MutableStateFlow<List<ReadingMilestoneRecord>>(emptyList())
    val readingMilestones: StateFlow<List<ReadingMilestoneRecord>> = _readingMilestones

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
            database.readingSessions().observeAll().collect { rows ->
                _readingSessions.value = rows.map { it.toSnapshot() }
            }
        }
        scope.launch {
            database.readingCycles().observeAll().collect { rows ->
                _readingCycles.value = rows.map { it.toDomain() }
            }
        }
        scope.launch {
            database.passageVisits().observeAll().collect { rows ->
                _passageVisits.value = rows.map { it.toDomain() }
            }
        }
        scope.launch {
            database.readingMilestones().observeAll().collect { rows ->
                _readingMilestones.value = rows.map { it.toDomain() }
            }
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
        val cleanNote = note.trim()
        _highlights.value.firstOrNull { it.id == id }?.copy(note = cleanNote)?.let { updated ->
            _highlights.value = _highlights.value.map { if (it.id == id) updated else it }
        }
        enqueue {
            val persisted = database.highlights().findById(id)?.toDomain() ?: return@enqueue
            database.highlights().upsert(persisted.copy(note = cleanNote).toEntity())
        }
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
     * Permanently removes one imported publication after all queued reader/session writes reach
     * Room. Reading sessions intentionally survive as historical records with a null bookId;
     * book-owned annotations, cycles, milestones and format extensions follow database cascades.
     */
    suspend fun deleteImportedBook(bookId: String): Book? {
        if (bookId.isBlank()) return null

        flushWrites()
        val deleted = orderedWrite {
            val stored = database.books().findWithCollections(bookId)?.toDomain()
                ?: return@orderedWrite null
            database.books().deleteById(bookId)
            stored
        } ?: return null

        _books.value = _books.value.filterNot { it.id == bookId }
        _highlights.value = _highlights.value.filterNot { it.bookId == bookId }
        _bookmarks.value = _bookmarks.value.filterNot { it.bookId == bookId }
        _readingCycles.value = _readingCycles.value.filterNot { it.bookId == bookId }
        _passageVisits.value = _passageVisits.value.filterNot { it.bookId == bookId }
        _readingMilestones.value = _readingMilestones.value.filterNot { it.bookId == bookId }
        _readingSessions.value = _readingSessions.value.map { session ->
            if (session.bookId == bookId) session.copy(bookId = null) else session
        }

        discardImportedArtifacts(deleted)
        return deleted
    }

    /**
     * Compensating rollback for a newly committed import when a format-specific post-commit step
     * fails. The Book row is the persistence owner, so foreign-key cascades remove dependent format
     * state before the app-private publication file is deleted.
     */
    suspend fun rollbackImportedBook(book: Book) {
        orderedWrite {
            database.books().deleteById(book.id)
        }
        _books.value = _books.value.filterNot { it.id == book.id }
        discardImportedArtifacts(book)
    }

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
        val current = getBook(id) ?: return
        val openedAtEpochMs = System.currentTimeMillis()
        val firstOpen = current.lastOpenedAtEpochMs <= 0L
        val updated = current.copy(lastOpenedAtEpochMs = openedAtEpochMs)
        replaceBookCached(updated)
        enqueue {
            database.withTransaction {
                check(database.books().updateLastOpened(id, openedAtEpochMs) == 1) {
                    "Book disappeared before its opened timestamp could be persisted: $id"
                }
                if (firstOpen) {
                    firstOpenedMilestone(
                        bookId = id,
                        openedAtEpochMs = openedAtEpochMs,
                        locatorJson = current.locatorJson
                    )?.let { milestone ->
                        database.readingMilestones().insertIfAbsent(milestone.toEntity())
                    }
                }
            }
        }
    }

    /**
     * Returns true when this update completed the book for the first time.
     *
     * The completion edge is special: progress, the latest session snapshot, and the sealed
     * reading-cycle record are committed in one Room transaction so process death cannot leave
     * "finished=true" without its historical completion record.
     */
    fun saveProgress(
        id: String,
        progression: Double,
        locatorJson: String,
        traceSequence: Long? = null,
        completionSessionSnapshot: ReadingSessionSnapshot? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
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
            lastOpenedAtEpochMs = nowEpochMs,
            finished = current.finished || finishedNow
        )
        replaceBookCached(updated)
        val crossedMilestones = crossedReadingMilestones(
            bookId = id,
            previousProgress = current.progress,
            newProgress = safe,
            reachedAtEpochMs = nowEpochMs,
            locatorJson = locatorJson
        )
        queueProgressWrite(
            PendingProgressWrite(
                id = id,
                progress = updated.progress,
                pagesRead = updated.pagesRead,
                locatorJson = locatorJson,
                lastOpenedAtEpochMs = updated.lastOpenedAtEpochMs,
                finished = updated.finished,
                traceSequence = traceSequence,
                completionAtEpochMs = nowEpochMs.takeIf { newlyFinished },
                completionSessionSnapshot = completionSessionSnapshot.takeIf { newlyFinished },
                completionBookSnapshot = updated.takeIf { newlyFinished },
                milestones = crossedMilestones
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

    /**
     * Records only a verified return to an already-preserved locator.
     *
     * The locator must match an existing highlight exactly. Recomposition, process restoration,
     * and repeated taps inside a short window are deduplicated so "revisit" remains a factual event.
     */
    fun recordPassageVisitForLocator(
        bookId: String,
        locatorJson: String,
        viewedAtEpochMs: Long = System.currentTimeMillis()
    ): PassageVisit? {
        val highlight = _highlights.value.firstOrNull {
            it.bookId == bookId && it.locatorJson == locatorJson
        } ?: return null
        if (viewedAtEpochMs <= highlight.createdAtEpochMs + PASSAGE_REVISIT_MIN_AGE_MS) return null

        val lastVisit = _passageVisits.value
            .asSequence()
            .filter { it.highlightId == highlight.id }
            .maxOfOrNull { it.viewedAtEpochMs }
        if (lastVisit != null && viewedAtEpochMs - lastVisit < PASSAGE_REVISIT_DEDUPE_MS) return null

        val visit = PassageVisit(
            id = UUID.randomUUID().toString(),
            highlightId = highlight.id,
            bookId = bookId,
            locatorJson = locatorJson,
            viewedAtEpochMs = viewedAtEpochMs
        )
        _passageVisits.value = listOf(visit) + _passageVisits.value
        enqueue { database.passageVisits().upsert(visit.toEntity()) }
        return visit
    }

    suspend fun readingContinuity(
        book: Book,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ReadingContinuitySummary {
        initialized.await()
        return withContext(Dispatchers.IO) {
            val sessions = database.readingSessions()
                .listForBook(book.id)
                .map { it.toSnapshot() }
            deriveReadingContinuity(
                book = book,
                sessions = sessions,
                nowEpochMs = nowEpochMs
            )
        }
    }

    suspend fun locatorJsonsForBook(bookId: String): Set<String> = orderedWrite {
        buildSet {
            database.books().findWithCollections(bookId)?.book?.locatorJson?.let(::add)
            database.bookmarks().listAll()
                .filter { it.bookId == bookId }
                .forEach { add(it.locatorJson) }
            database.highlights().listAll()
                .filter { it.bookId == bookId }
                .forEach { add(it.locatorJson) }
        }
    }

    /**
     * Rewrites only locators that Readium 3.4 identified as legacy PDFium values.
     *
     * Every migrated locator carries its own compatibility marker, so this transaction is safe to
     * retry after process death and restored backups do not need a separate schema flag.
     */
    suspend fun applyPdfiumLocatorMigrations(bookId: String, migrations: Map<String, String>) {
        if (migrations.isEmpty()) return
        flushProgress(bookId)

        val (migratedBook, migratedBookmarks, migratedHighlights) = orderedWrite {
            database.withTransaction {
                val currentBook = database.books().findWithCollections(bookId)?.toDomain()
                val bookUpdate = currentBook?.locatorJson
                    ?.let(migrations::get)
                    ?.let { currentBook.copy(locatorJson = it) }

                val bookmarkUpdates = database.bookmarks().listAll().mapNotNull { entity ->
                    if (entity.bookId != bookId) return@mapNotNull null
                    migrations[entity.locatorJson]?.let { entity.toDomain().copy(locatorJson = it) }
                }
                val highlightUpdates = database.highlights().listAll().mapNotNull { entity ->
                    if (entity.bookId != bookId) return@mapNotNull null
                    migrations[entity.locatorJson]?.let { entity.toDomain().copy(locatorJson = it) }
                }

                bookUpdate?.let { database.books().upsert(it.toEntity()) }
                if (bookmarkUpdates.isNotEmpty()) {
                    database.bookmarks().upsertAll(bookmarkUpdates.map { it.toEntity() })
                }
                if (highlightUpdates.isNotEmpty()) {
                    database.highlights().upsertAll(highlightUpdates.map { it.toEntity() })
                }

                Triple(bookUpdate, bookmarkUpdates, highlightUpdates)
            }
        }

        migratedBook?.let(::replaceBookCached)
        if (migratedBookmarks.isNotEmpty()) {
            val byId = migratedBookmarks.associateBy { it.id }
            _bookmarks.value = _bookmarks.value.map { byId[it.id] ?: it }
        }
        if (migratedHighlights.isNotEmpty()) {
            val byId = migratedHighlights.associateBy { it.id }
            _highlights.value = _highlights.value.map { byId[it.id] ?: it }
        }
    }

    fun deleteHighlight(id: String) {
        _highlights.value = _highlights.value.filterNot { it.id == id }
        enqueue { database.highlights().deleteById(id) }
    }

    /**
     * Session snapshots are high-frequency derived state during navigation. Keep only the latest
     * snapshot per session and persist it at most once per interval. Lifecycle boundaries call
     * [flushReadingSession] so pause/close durability remains immediate.
     */
    fun saveReadingSession(snapshot: ReadingSessionSnapshot) {
        synchronized(coalescingLock) {
            pendingReadingSessions[snapshot.id] = snapshot
            if (sessionFlushJobs[snapshot.id]?.isActive == true) return@synchronized

            sessionFlushJobs[snapshot.id] = scope.launch {
                delay(SESSION_WRITE_INTERVAL_MS)
                synchronized(coalescingLock) {
                    sessionFlushJobs.remove(snapshot.id)
                    pendingReadingSessions.remove(snapshot.id)?.let(::enqueueReadingSessionWrite)
                }
            }
        }
    }

    fun flushReadingSession(sessionId: String) {
        synchronized(coalescingLock) {
            sessionFlushJobs.remove(sessionId)?.cancel()
            pendingReadingSessions.remove(sessionId)?.let(::enqueueReadingSessionWrite)
        }
    }

    /**
     * Test-only lifecycle hook for instrumented repositories backed by short-lived in-memory DBs.
     * Production repositories live for the app process, but tests must cancel Room observers before
     * closing their database to avoid asynchronous queries against a closed connection.
     */
    internal suspend fun closeForTest() {
        scope.coroutineContext[Job]?.let { rootJob ->
            rootJob.cancel()
            rootJob.join()
        }
    }

    /** Ensures migration and all writes queued before this call have reached durable storage. */
    suspend fun flushWrites() {
        initialized.await()
        flushAllProgress()
        flushAllReadingSessions()
        val done = CompletableDeferred<Unit>()
        writes.send { done.complete(Unit) }
        done.await()
        storageFailure.get()?.let { throw IllegalStateException("A library write failed.", it) }
    }

    /** Returns one point-in-time database snapshot ordered with all normal reader writes. */
    suspend fun snapshot(): LibrarySnapshot {
        flushAllProgress()
        flushAllReadingSessions()
        return orderedWrite {
            val databaseState = database.withTransaction {
                DatabaseLibraryState(
                    books = database.books().listAllWithCollections().map { it.toDomain() },
                    highlights = database.highlights().listAll().map { it.toDomain() },
                    bookmarks = database.bookmarks().listAll().map { it.toDomain() },
                    readingSessions = database.readingSessions().listAll().map { it.toSnapshot() },
                    readingCycles = database.readingCycles().listAll().map { it.toDomain() },
                    passageVisits = database.passageVisits().listAll().map { it.toDomain() },
                    readingMilestones = database.readingMilestones().listAll().map { it.toDomain() }
                )
            }
            val appearance = settings.settings.first().readerAppearance
            LibrarySnapshot(
                books = databaseState.books,
                highlights = databaseState.highlights,
                bookmarks = databaseState.bookmarks,
                appearance = appearance,
                readingSessions = databaseState.readingSessions,
                readingCycles = databaseState.readingCycles,
                passageVisits = databaseState.passageVisits,
                readingMilestones = databaseState.readingMilestones
            )
        }
    }

    /** Transactional replacement used by backup restore, serialized with normal reader writes. */
    suspend fun replaceAll(snapshot: LibrarySnapshot) {
        discardAllPendingProgress()
        discardAllPendingReadingSessions()
        orderedWrite {
            database.withTransaction {
                database.passageVisits().deleteAll()
                database.readingCycles().deleteAll()
                database.readingMilestones().deleteAll()
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
                if (snapshot.readingCycles.isNotEmpty()) {
                    database.readingCycles().upsertAll(snapshot.readingCycles.map { it.toEntity() })
                }
                if (snapshot.passageVisits.isNotEmpty()) {
                    database.passageVisits().upsertAll(snapshot.passageVisits.map { it.toEntity() })
                }
                if (snapshot.readingMilestones.isNotEmpty()) {
                    database.readingMilestones().insertAllIfAbsent(
                        snapshot.readingMilestones.map { it.toEntity() }
                    )
                }
            }
            settings.saveReaderAppearance(snapshot.appearance)
        }
    }

    /**
     * Coalesces locator churn into at most one durable progress write per interval. The in-memory
     * book state still updates immediately, so UI and completion semantics remain synchronous.
     */
    private fun queueProgressWrite(value: PendingProgressWrite, immediate: Boolean) {
        synchronized(coalescingLock) {
            val previous = pendingProgress[value.id]
            val merged = if (previous == null) {
                value
            } else {
                value.copy(
                    milestones = (previous.milestones + value.milestones)
                        .distinctBy { it.id },
                    completionAtEpochMs =
                        value.completionAtEpochMs ?: previous.completionAtEpochMs,
                    completionSessionSnapshot =
                        value.completionSessionSnapshot ?: previous.completionSessionSnapshot,
                    completionBookSnapshot =
                        value.completionBookSnapshot ?: previous.completionBookSnapshot
                )
            }
            pendingProgress[value.id] = merged

            if (immediate) {
                progressFlushJobs.remove(value.id)?.cancel()
                pendingProgress.remove(value.id)?.let(::enqueueProgressWrite)
            } else if (progressFlushJobs[value.id]?.isActive != true) {
                progressFlushJobs[value.id] = scope.launch {
                    delay(PROGRESS_WRITE_INTERVAL_MS)
                    synchronized(coalescingLock) {
                        progressFlushJobs.remove(value.id)
                        pendingProgress.remove(value.id)?.let(::enqueueProgressWrite)
                    }
                }
            }
        }
    }

    /** Enqueues the latest pending progress for one book immediately. Safe to call from lifecycle hooks. */
    fun flushProgress(bookId: String) {
        synchronized(coalescingLock) {
            progressFlushJobs.remove(bookId)?.cancel()
            pendingProgress.remove(bookId)?.let(::enqueueProgressWrite)
        }
    }

    private fun flushAllProgress() {
        synchronized(coalescingLock) {
            progressFlushJobs.values.forEach { it.cancel() }
            progressFlushJobs.clear()
            pendingProgress.values.forEach(::enqueueProgressWrite)
            pendingProgress.clear()
        }
    }

    private fun discardAllPendingProgress() {
        synchronized(coalescingLock) {
            progressFlushJobs.values.forEach { it.cancel() }
            progressFlushJobs.clear()
            pendingProgress.clear()
        }
    }

    private fun flushAllReadingSessions() {
        synchronized(coalescingLock) {
            sessionFlushJobs.values.forEach { it.cancel() }
            sessionFlushJobs.clear()
            pendingReadingSessions.values.forEach(::enqueueReadingSessionWrite)
            pendingReadingSessions.clear()
        }
    }

    private fun discardAllPendingReadingSessions() {
        synchronized(coalescingLock) {
            sessionFlushJobs.values.forEach { it.cancel() }
            sessionFlushJobs.clear()
            pendingReadingSessions.clear()
        }
    }

    private fun enqueueReadingSessionWrite(snapshot: ReadingSessionSnapshot) {
        enqueue { database.readingSessions().upsert(snapshot.toEntity()) }
    }

    private fun enqueueProgressWrite(value: PendingProgressWrite) {
        enqueue {
            val persistProgress: suspend () -> Unit = {
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

            val completionAt = value.completionAtEpochMs
            val completionBook = value.completionBookSnapshot
            if (completionAt != null && completionBook != null) {
                database.withTransaction {
                    persistProgress()
                    if (value.milestones.isNotEmpty()) {
                        database.readingMilestones().insertAllIfAbsent(
                            value.milestones.map { it.toEntity() }
                        )
                    }
                    value.completionSessionSnapshot?.let { session ->
                        database.readingSessions().upsert(session.toEntity())
                    }

                    val cycleIndex = database.readingCycles().maxCycleIndex(value.id) + 1
                    val cycle = buildSealedReadingCycle(
                        book = completionBook,
                        cycleIndex = cycleIndex,
                        sessions = database.readingSessions().listForBook(value.id).map { it.toSnapshot() },
                        highlights = database.highlights().listAll()
                            .filter { it.bookId == value.id }
                            .map { it.toDomain() },
                        bookmarks = database.bookmarks().listAll()
                            .filter { it.bookId == value.id }
                            .map { it.toDomain() },
                        milestones = database.readingMilestones().listForBook(value.id)
                            .map { it.toDomain() },
                        completedAtEpochMs = completionAt,
                        finalLocatorJson = value.locatorJson
                    )
                    database.readingCycles().upsert(cycle.toEntity())
                }
            } else if (value.milestones.isNotEmpty()) {
                database.withTransaction {
                    persistProgress()
                    database.readingMilestones().insertAllIfAbsent(
                        value.milestones.map { it.toEntity() }
                    )
                }
            } else {
                persistProgress()
            }

            ReaderTrace.event(
                "locator_persisted",
                bookId = value.id,
                details = buildString {
                    value.traceSequence?.let { append("seq=").append(it).append(' ') }
                    append("progress=").append(value.progress)
                    if (completionAt != null) append(" completionSealed=true")
                }
            )
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
    val readingSessions: List<ReadingSessionSnapshot> = emptyList(),
    val readingCycles: List<ReadingCycleRecord> = emptyList(),
    val passageVisits: List<PassageVisit> = emptyList(),
    val readingMilestones: List<ReadingMilestoneRecord> = emptyList()
) {
    companion object
}

private data class DatabaseLibraryState(
    val books: List<Book>,
    val highlights: List<Highlight>,
    val bookmarks: List<Bookmark>,
    val readingSessions: List<ReadingSessionSnapshot>,
    val readingCycles: List<ReadingCycleRecord>,
    val passageVisits: List<PassageVisit>,
    val readingMilestones: List<ReadingMilestoneRecord>
)

private data class PendingProgressWrite(
    val id: String,
    val progress: Float,
    val pagesRead: Int,
    val locatorJson: String,
    val lastOpenedAtEpochMs: Long,
    val finished: Boolean,
    val traceSequence: Long? = null,
    val completionAtEpochMs: Long? = null,
    val completionSessionSnapshot: ReadingSessionSnapshot? = null,
    val completionBookSnapshot: Book? = null,
    val milestones: List<ReadingMilestoneRecord> = emptyList()
)

private const val PROGRESS_WRITE_INTERVAL_MS = 250L
private const val SESSION_WRITE_INTERVAL_MS = 1_000L
private const val PASSAGE_REVISIT_MIN_AGE_MS = 30_000L
private const val PASSAGE_REVISIT_DEDUPE_MS = 5L * 60L * 1000L

private fun stableCollectionId(normalizedName: String): String = UUID.nameUUIDFromBytes(
    "veil-collection:$normalizedName".toByteArray(StandardCharsets.UTF_8)
).toString()
