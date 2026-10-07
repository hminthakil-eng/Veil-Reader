package com.veilreader.app.data

import com.veilreader.app.diagnostics.ReaderTrace

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
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
import com.veilreader.app.domain.ReadingPaceProfile
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
import java.util.concurrent.ConcurrentHashMap
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
internal data class SelectionNoteCommit(
    val highlight: Highlight,
    val created: Boolean
)

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
    private val readingPaceStore = ReadingPaceStore(appContext)
    private val readerCrashCheckpointStore = ReaderCrashCheckpointStore(appContext)
    private val readingPaceGeneration = AtomicLong(0L)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val initialized = CompletableDeferred<Unit>()
    private val storageFailure = AtomicReference<Throwable?>(null)
    private val coalescingLock = Any()
    private val pendingProgress = mutableMapOf<String, PendingProgressWrite>()
    private val progressFlushJobs = mutableMapOf<String, Job>()
    private var nextReaderProgressWriterEpoch = 0L
    private val activeReaderProgressWriters = mutableMapOf<String, ReaderProgressWriterLease>()
    private val latestReaderProgressOrderByBook = mutableMapOf<String, ReaderProgressWriteOrder>()
    private val pendingReadingSessions = mutableMapOf<String, ReadingSessionSnapshot>()
    private val sessionFlushJobs = mutableMapOf<String, Job>()
    private val deletedHighlightIds = ConcurrentHashMap.newKeySet<String>()

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

    private val _readingPaceProfiles =
        MutableStateFlow<Map<String, ReadingPaceProfile>>(emptyMap())
    val readingPaceProfiles: StateFlow<Map<String, ReadingPaceProfile>> =
        _readingPaceProfiles

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
        scope.launch {
            readingPaceStore.profiles.collect { profiles ->
                _readingPaceProfiles.value = profiles
            }
        }
        scope.launch {
            try {
                initialized.await()
                reconcileOrphanedAppPrivateArtifacts()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Orphan cleanup is maintenance, not durable user-data authority.
                // Never poison the serialized write queue because housekeeping
                // could not inspect or delete an unowned derived/private file.
                ReaderTrace.event(
                    "library_orphan_reconcile_failed",
                    details = "error=${error::class.java.simpleName}"
                )
            }
        }
    }

    internal fun recordReadingPaceInterval(
        bookId: String,
        intervalMillis: Long
    ) {
        if (bookId.isBlank() || intervalMillis <= 0L) return
        val generation = readingPaceGeneration.get()
        scope.launch {
            try {
                readingPaceStore.recordInterval(bookId, intervalMillis) {
                    generation == readingPaceGeneration.get() && _books.value.any { it.id == bookId }
                }
            } catch (_: IOException) {
                // Derived pace is disposable; an unavailable cache cannot poison durable progress.
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
        invalidateReaderProgressOwnership(bookId)
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
        try { readingPaceStore.remove(bookId) } catch (_: IOException) { }
        readerCrashCheckpointStore.clear(bookId)
        _readingPaceProfiles.value = _readingPaceProfiles.value - bookId

        discardImportedArtifacts(deleted)
        return deleted
    }

    /**
     * Compensating rollback for a newly committed import when a format-specific post-commit step
     * fails. The Book row is the persistence owner, so foreign-key cascades remove dependent format
     * state before the app-private publication file is deleted.
     */
    suspend fun rollbackImportedBook(book: Book) {
        invalidateReaderProgressOwnership(book.id)
        orderedWrite {
            database.books().deleteById(book.id)
        }
        _books.value = _books.value.filterNot { it.id == book.id }
        try { readingPaceStore.remove(book.id) } catch (_: IOException) { }
        readerCrashCheckpointStore.clear(book.id)
        _readingPaceProfiles.value = _readingPaceProfiles.value - book.id
        discardImportedArtifacts(book)
    }

    /**
     * Stores a derived app-private cover thumbnail path. An empty string is a terminal sentinel
     * meaning extraction was attempted but this publication has no usable cover.
     */
    fun updateCoverCachePath(id: String, path: String) {
        updateBookCached(id) { it.copy(coverCachePath = path) } ?: return
        enqueue {
            check(database.books().updateCoverCachePath(id, path) == 1) {
                "Cover cache path target disappeared before persistence"
            }
        }
    }

    /** Backfills the derived duplicate-detection fingerprint for pre-0.8 library entries. */
    fun updateContentFingerprint(id: String, fingerprint: String) {
        if (fingerprint.isBlank()) return
        val normalized = fingerprint.lowercase(Locale.ROOT)
        val current = getBook(id) ?: return
        if (!current.contentFingerprint.isNullOrBlank()) return
        replaceBookCached(current.copy(contentFingerprint = normalized))
        enqueue {
            database.books().updateContentFingerprintIfMissing(id, normalized)
        }
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
     * Claims progress-write ownership for one logical Reader open request.
     *
     * Any pending write from the previous owner is enqueued before the epoch changes, preserving
     * deterministic queue order across same-book reopen races.
     */
    internal fun beginReaderProgressSession(
        bookId: String,
        sessionId: String
    ): ReaderProgressWriterLease {
        require(bookId.isNotBlank())
        require(sessionId.isNotBlank())
        return synchronized(coalescingLock) {
            progressFlushJobs.remove(bookId)?.cancel()
            pendingProgress.remove(bookId)?.let(::enqueueProgressWrite)

            val nextEpoch = ++nextReaderProgressWriterEpoch
            ReaderProgressWriterLease(
                bookId = bookId,
                sessionId = sessionId,
                epoch = nextEpoch
            ).also { lease ->
                activeReaderProgressWriters[bookId] = lease
                ReaderTrace.event(
                    "progress_writer_claimed",
                    bookId = bookId,
                    sessionId = sessionId,
                    details = "epoch=${lease.epoch}"
                )
            }
        }
    }

    internal fun endReaderProgressSession(lease: ReaderProgressWriterLease) {
        synchronized(coalescingLock) {
            if (activeReaderProgressWriters[lease.bookId] != lease) return
            progressFlushJobs.remove(lease.bookId)?.cancel()
            pendingProgress.remove(lease.bookId)?.let(::enqueueProgressWrite)
            activeReaderProgressWriters.remove(lease.bookId)
            ReaderTrace.event(
                "progress_writer_released",
                bookId = lease.bookId,
                sessionId = lease.sessionId,
                details = "epoch=${lease.epoch}"
            )
        }
    }

    /**
     * Ordered Reader progress path. A stale session lease or an older logical sequence is rejected
     * before it can mutate either the in-memory Book cache or the durable coalescer.
     */
    internal fun saveReaderProgress(
        lease: ReaderProgressWriterLease,
        progression: Double,
        locatorJson: String,
        sequence: Long,
        completionSessionSnapshot: ReadingSessionSnapshot? = null,
        nowEpochMs: Long = System.currentTimeMillis(),
        bypassDebounce: Boolean = false
    ): ReaderProgressSaveOutcome =
        synchronized(coalescingLock) {
            if (activeReaderProgressWriters[lease.bookId] != lease) {
                ReaderTrace.event(
                    "locator_save_rejected_stale_writer",
                    bookId = lease.bookId,
                    sessionId = lease.sessionId,
                    details = "epoch=${lease.epoch} seq=$sequence"
                )
                return@synchronized ReaderProgressSaveOutcome(accepted = false)
            }

            if (sequence <= 0L) {
                ReaderTrace.event(
                    "locator_save_rejected_invalid_sequence",
                    bookId = lease.bookId,
                    sessionId = lease.sessionId,
                    details = "epoch=${lease.epoch} seq=$sequence"
                )
                return@synchronized ReaderProgressSaveOutcome(accepted = false)
            }

            val order = ReaderProgressWriteOrder(lease.epoch, sequence)
            val latestAccepted = latestReaderProgressOrderByBook[lease.bookId]
            if (
                latestAccepted != null &&
                !shouldReplacePendingProgress(
                    current = latestAccepted,
                    incoming = order
                )
            ) {
                ReaderTrace.event(
                    "locator_save_rejected_out_of_order",
                    bookId = lease.bookId,
                    sessionId = lease.sessionId,
                    details = "latest=$latestAccepted incoming=$order"
                )
                return@synchronized ReaderProgressSaveOutcome(accepted = false)
            }

            val saveResult = saveProgressLocked(
                id = lease.bookId,
                progression = progression,
                locatorJson = locatorJson,
                traceSequence = sequence,
                completionSessionSnapshot = completionSessionSnapshot,
                nowEpochMs = nowEpochMs,
                order = order,
                bypassDebounce = bypassDebounce,
                checkpointSessionId = lease.sessionId
            )
            if (saveResult == null) {
                ReaderTrace.event(
                    "locator_save_rejected_missing_book",
                    bookId = lease.bookId,
                    sessionId = lease.sessionId,
                    details = "epoch=${lease.epoch} seq=$sequence"
                )
                return@synchronized ReaderProgressSaveOutcome(accepted = false)
            }
            latestReaderProgressOrderByBook[lease.bookId] = order
            ReaderProgressSaveOutcome(
                accepted = true,
                newlyFinished = saveResult.newlyFinished,
                crashCheckpointDurable = saveResult.crashCheckpointDurable,
                crashCheckpointLatencyNanos = saveResult.crashCheckpointLatencyNanos
            )
        }

    /**
     * Persists the app-level restore locator before Compose takes ownership of the publication.
     *
     * A temporary ordered writer supersedes any stale Reader instance for the same book, then
     * releases ownership so the real ReaderViewModel can claim the next writer epoch.
     */
    internal fun saveReaderOpenRecoveryProgress(
        bookId: String,
        sessionId: String,
        progression: Double,
        locatorJson: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ReaderProgressSaveOutcome {
        val lease = beginReaderProgressSession(
            bookId = bookId,
            sessionId = sessionId
        )
        return try {
            saveReaderProgress(
                lease = lease,
                progression = progression,
                locatorJson = locatorJson,
                sequence = 1L,
                completionSessionSnapshot = null,
                nowEpochMs = nowEpochMs
            )
        } finally {
            endReaderProgressSession(lease)
        }
    }

    /**
     * Legacy/internal non-session progress path. Reader code should use [saveReaderProgress].
     */
    fun saveProgress(
        id: String,
        progression: Double,
        locatorJson: String,
        traceSequence: Long? = null,
        completionSessionSnapshot: ReadingSessionSnapshot? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean =
        synchronized(coalescingLock) {
            if (activeReaderProgressWriters.containsKey(id)) {
                ReaderTrace.event(
                    "legacy_progress_write_rejected_active_reader",
                    bookId = id,
                    details = "ordered Reader writer owns progress"
                )
                return@synchronized false
            }
            saveProgressLocked(
                id = id,
                progression = progression,
                locatorJson = locatorJson,
                traceSequence = traceSequence,
                completionSessionSnapshot = completionSessionSnapshot,
                nowEpochMs = nowEpochMs,
                order = null
            )?.newlyFinished ?: false
        }

    private fun saveProgressLocked(
        id: String,
        progression: Double,
        locatorJson: String,
        traceSequence: Long?,
        completionSessionSnapshot: ReadingSessionSnapshot?,
        nowEpochMs: Long,
        order: ReaderProgressWriteOrder?,
        bypassDebounce: Boolean = false,
        checkpointSessionId: String? = null
    ): ProgressSaveResult? {
        val current = getBook(id) ?: return null
        val safe = (if (progression.isFinite()) progression else current.progress.toDouble())
            .coerceIn(0.0, 1.0).toFloat()
        val committedAtEpochMs = nextReaderProgressTimestamp(
            previousEpochMs = current.lastOpenedAtEpochMs,
            candidateEpochMs = nowEpochMs
        )
        val finishedNow = safe >= 0.995f
        val newlyFinished = finishedNow && !current.finished
        val estimatedRead = if (current.totalPages > 0) {
            (current.totalPages * safe).toInt().coerceAtMost(current.totalPages)
        } else current.pagesRead
        val updated = current.copy(
            progress = safe,
            pagesRead = estimatedRead,
            locatorJson = locatorJson,
            lastOpenedAtEpochMs = committedAtEpochMs,
            finished = current.finished || finishedNow
        )

        var crashCheckpointDurable = true
        var crashCheckpointLatencyNanos: Long? = null
        if (bypassDebounce && order != null && !checkpointSessionId.isNullOrBlank()) {
            val checkpointWrite = readerCrashCheckpointStore.write(
                ReaderCrashCheckpoint(
                    bookId = id,
                    sessionId = checkpointSessionId,
                    writerEpoch = order.epoch,
                    sequence = order.sequence,
                    progression = safe.toDouble(),
                    locatorJson = locatorJson,
                    committedAtEpochMs = committedAtEpochMs
                )
            )
            crashCheckpointDurable = checkpointWrite.durable
            crashCheckpointLatencyNanos = checkpointWrite.elapsedNanos
            ReaderTrace.event(
                if (checkpointWrite.durable) {
                    "locator_crash_checkpoint_durable"
                } else {
                    "locator_crash_checkpoint_failed"
                },
                bookId = id,
                sessionId = checkpointSessionId,
                details = buildString {
                    append("epoch=").append(order.epoch)
                    append(" seq=").append(order.sequence)
                    append(" latencyUs=").append(checkpointWrite.elapsedNanos / 1_000L)
                }
            )
        }

        replaceBookCached(updated)
        val crossedMilestones = crossedReadingMilestones(
            bookId = id,
            previousProgress = current.progress,
            newProgress = safe,
            reachedAtEpochMs = committedAtEpochMs,
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
                order = order,
                completionAtEpochMs = committedAtEpochMs.takeIf { newlyFinished },
                completionSessionSnapshot = completionSessionSnapshot.takeIf { newlyFinished },
                completionBookSnapshot = updated.takeIf { newlyFinished },
                milestones = crossedMilestones
            ),
            immediate = bypassDebounce || newlyFinished
        )
        return ProgressSaveResult(
            newlyFinished = newlyFinished,
            crashCheckpointDurable = crashCheckpointDurable,
            crashCheckpointLatencyNanos = crashCheckpointLatencyNanos
        )
    }

    fun addHighlight(bookId: String, quote: String, locatorJson: String): Highlight {
        val cleanQuote = quote.trim()
        _highlights.value.firstOrNull {
            it.id !in deletedHighlightIds &&
                it.bookId == bookId &&
                it.locatorJson == locatorJson &&
                it.quote == cleanQuote
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

    /**
     * Commits a selection note against durable Room state, never the eventually-consistent Flow
     * projection. An older observer emission can temporarily repopulate [_highlights] after a
     * delete; using that cache as edit authority would resurrect a record the user already removed.
     *
     * The read and upsert share the serialized write queue, so an existing-highlight edit is valid
     * only if that exact row still exists when the commit reaches Room.
     */
    internal suspend fun commitSelectionNote(
        bookId: String,
        quote: String,
        locatorJson: String,
        existingHighlightId: String?,
        note: String
    ): SelectionNoteCommit? {
        val cleanQuote = quote.trim()
        val cleanLocator = locatorJson.trim()
        val cleanNote = note.trim()
        if (bookId.isBlank() || cleanQuote.isBlank() || cleanLocator.isBlank()) return null
        if (!existingHighlightId.isNullOrBlank() && existingHighlightId in deletedHighlightIds) {
            return null
        }

        val result = orderedWrite {
            if (!existingHighlightId.isNullOrBlank() && existingHighlightId in deletedHighlightIds) {
                return@orderedWrite null
            }
            val existing = when {
                !existingHighlightId.isNullOrBlank() ->
                    database.highlights()
                        .findById(existingHighlightId)
                        ?.toDomain()
                        ?.takeIf { it.bookId == bookId }
                        ?: return@orderedWrite null

                else ->
                    database.highlights()
                        .findByBookLocatorAndQuote(
                            bookId = bookId,
                            locatorJson = cleanLocator,
                            quote = cleanQuote
                        )
                        ?.toDomain()
            }

            val created = existing == null
            val committed = existing
                ?.copy(note = cleanNote)
                ?: Highlight(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    quote = cleanQuote,
                    locatorJson = cleanLocator,
                    note = cleanNote
                )

            database.highlights().upsert(committed.toEntity())
            SelectionNoteCommit(
                highlight = committed,
                created = created
            )
        }

        if (result == null) {
            existingHighlightId?.takeIf { it.isNotBlank() }?.let { missingId ->
                _highlights.value = _highlights.value.filterNot { it.id == missingId }
            }
            return null
        }

        val committed = result.highlight
        val currentHighlights = _highlights.value
        _highlights.value = if (result.created) {
            listOf(committed) + currentHighlights.filterNot { it.id == committed.id }
        } else if (currentHighlights.any { it.id == committed.id }) {
            currentHighlights.map { item ->
                if (item.id == committed.id) committed else item
            }
        } else {
            listOf(committed) + currentHighlights
        }
        return result
    }

    fun highlightsFor(bookId: String): List<Highlight> = _highlights.value.filter { it.bookId == bookId }

    fun passageVisitsFor(bookId: String): List<PassageVisit> =
        _passageVisits.value.filter { it.bookId == bookId }

    /**
     * Records only a verified return to an already-preserved locator.
     *
     * Historical memory must be derived from durable Room state, not from the eventually-consistent
     * UI projections. This work is placed on the same serialized queue as highlight creation, so a
     * revisit requested immediately after saving a highlight cannot be lost if a Room Flow emits an
     * older snapshot while the write is still settling.
     */
    fun recordPassageVisitForLocator(
        bookId: String,
        locatorJson: String,
        viewedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        enqueue {
            val highlight = database.highlights()
                .findByBookAndLocator(bookId = bookId, locatorJson = locatorJson)
                ?.toDomain()
                ?: return@enqueue

            if (viewedAtEpochMs <= highlight.createdAtEpochMs + PASSAGE_REVISIT_MIN_AGE_MS) {
                return@enqueue
            }

            val lastVisit = database.passageVisits().latestViewedAt(highlight.id)
            if (
                lastVisit != null &&
                viewedAtEpochMs - lastVisit < PASSAGE_REVISIT_DEDUPE_MS
            ) {
                return@enqueue
            }

            database.passageVisits().upsert(
                PassageVisit(
                    id = UUID.randomUUID().toString(),
                    highlightId = highlight.id,
                    bookId = bookId,
                    locatorJson = locatorJson,
                    viewedAtEpochMs = viewedAtEpochMs
                ).toEntity()
            )
        }
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
            readerCrashCheckpointStore.read(bookId)?.locatorJson?.let(::add)
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
        if (id.isBlank()) return
        deletedHighlightIds += id
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
     * Restores the latest durable accounting state for an app-level Reader open request.
     *
     * The pending session snapshot is forced onto the serialized write queue first, then the read is
     * placed behind it. This prevents a recreated ReaderViewModel from hydrating an older database
     * row while a newer snapshot from the previous instance is still waiting in memory.
     */
    suspend fun loadReadingSessionForResume(
        sessionId: String,
        bookId: String
    ): ReadingSessionResumeState? {
        if (sessionId.isBlank() || bookId.isBlank()) return null
        flushReadingSession(sessionId)
        return orderedWrite {
            val snapshot = database.readingSessions()
                .findById(sessionId)
                ?.toSnapshot()
                ?.takeIf { it.bookId == bookId }
                ?: return@orderedWrite null

            ReadingSessionResumeState(
                snapshot = snapshot,
                notedHighlightIds = database.highlights()
                    .listAll()
                    .asSequence()
                    .filter { it.bookId == bookId && it.note.isNotBlank() }
                    .map { it.id }
                    .toSet()
            )
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

    suspend fun loadReaderCrashCheckpoint(book: Book): ReaderCrashCheckpoint? {
        initialized.await()
        return withContext(Dispatchers.IO) {
            val durableBook = database.books().findWithCollections(book.id)?.toDomain()
                ?: return@withContext null
            readerCrashCheckpointStore.read(book.id)
                ?.takeIf { checkpoint -> checkpoint.isNewerThanRoom(durableBook) }
                ?: run {
                    readerCrashCheckpointStore.clear(book.id)
                    null
                }
        }
    }

    internal fun clearReaderCrashCheckpoint(bookId: String) {
        readerCrashCheckpointStore.clear(bookId)
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

    /**
     * Starts a storage barrier from the repository-owned IO scope.
     *
     * Lifecycle callbacks are synchronous and UI/composition coroutines can be cancelled as soon as
     * the Activity leaves the foreground. Moving this barrier onto the repository scope gives the
     * final progress/session writes a chance to reach Room independently of Reader composition.
     * This is still best-effort against an immediate OS process kill; Android offers no callback
     * that can make arbitrary asynchronous work absolutely guaranteed at that boundary.
     */
    fun requestLifecycleDurability(bookId: String, sessionId: String?) {
        ReaderTrace.event(
            "reader_lifecycle_durability_requested",
            bookId = bookId,
            sessionId = sessionId
        )
        scope.launch {
            try {
                flushWrites()
                ReaderTrace.event(
                    "reader_lifecycle_durable",
                    bookId = bookId,
                    sessionId = sessionId
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                storageFailure.compareAndSet(null, error)
                ReaderTrace.event(
                    "reader_lifecycle_durability_failed",
                    bookId = bookId,
                    sessionId = sessionId,
                    details = "error=${error::class.java.simpleName}"
                )
            }
        }
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
        readingPaceGeneration.incrementAndGet()
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
        // A restore establishes a fresh durable source of truth. In-process deletion tombstones
        // must not outlive that replacement or block edits to legitimately restored highlights.
        deletedHighlightIds.clear()
        try { readingPaceStore.clear() } catch (_: IOException) { }
        readerCrashCheckpointStore.clearAll()
        _readingPaceProfiles.value = emptyMap()
    }

    /**
     * Coalesces locator churn into at most one durable progress write per interval. The in-memory
     * book state still updates immediately, so UI and completion semantics remain synchronous.
     */
    private fun queueProgressWrite(value: PendingProgressWrite, immediate: Boolean) {
        synchronized(coalescingLock) {
            val previous = pendingProgress[value.id]
            if (
                previous != null &&
                !shouldReplacePendingProgress(
                    current = previous.order,
                    incoming = value.order
                )
            ) {
                ReaderTrace.event(
                    "locator_pending_write_rejected_out_of_order",
                    bookId = value.id,
                    details = buildString {
                        append("current=").append(previous.order)
                        append(" incoming=").append(value.order)
                    }
                )
                return@synchronized
            }
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

    private fun invalidateReaderProgressOwnership(bookId: String) {
        synchronized(coalescingLock) {
            progressFlushJobs.remove(bookId)?.cancel()
            pendingProgress.remove(bookId)
            activeReaderProgressWriters.remove(bookId)
            latestReaderProgressOrderByBook.remove(bookId)
            ReaderTrace.event(
                "progress_writer_invalidated",
                bookId = bookId
            )
        }
    }

    private fun discardAllPendingProgress() {
        synchronized(coalescingLock) {
            progressFlushJobs.values.forEach { it.cancel() }
            progressFlushJobs.clear()
            pendingProgress.clear()
            activeReaderProgressWriters.clear()
            latestReaderProgressOrderByBook.clear()
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

            runCatching {
                readerCrashCheckpointStore.clearIfCovered(
                    bookId = value.id,
                    persistedAtEpochMs = value.lastOpenedAtEpochMs,
                    persistedLocatorJson = value.locatorJson
                )
            }.onFailure { error ->
                ReaderTrace.event(
                    "locator_crash_checkpoint_clear_failed",
                    bookId = value.id,
                    details = "error=${error::class.java.simpleName}"
                )
            }

            ReaderTrace.event(
                "locator_persisted",
                bookId = value.id,
                details = buildString {
                    value.order?.let { append("order=").append(it).append(' ') }
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

    private suspend fun reconcileOrphanedAppPrivateArtifacts() {
        val books = database.books()
            .listAllWithCollections()
            .map { it.toDomain() }

        withContext(Dispatchers.IO) {
            val publicationsRoot =
                File(appContext.filesDir, "publications").canonicalFile
            val coversRoot =
                File(appContext.filesDir, "covers").canonicalFile

            val ownedPublications = books.mapNotNullTo(mutableSetOf()) { book ->
                canonicalOwnedArtifactPath(
                    rawPath = book.sourceUri
                        ?.let(Uri::parse)
                        ?.takeIf { it.scheme == "file" }
                        ?.path,
                    root = publicationsRoot
                )
            }
            val ownedCovers = books.mapNotNullTo(mutableSetOf()) { book ->
                canonicalOwnedArtifactPath(
                    rawPath = book.coverCachePath,
                    root = coversRoot
                )
            }

            val now = System.currentTimeMillis()
            val deletedPublications =
                reconcileOrphanedArtifactDirectory(
                    root = publicationsRoot,
                    ownedCanonicalPaths = ownedPublications,
                    nowEpochMs = now,
                    graceMillis = ORPHAN_ARTIFACT_GRACE_MS
                )
            val deletedCovers =
                reconcileOrphanedArtifactDirectory(
                    root = coversRoot,
                    ownedCanonicalPaths = ownedCovers,
                    nowEpochMs = now,
                    graceMillis = ORPHAN_ARTIFACT_GRACE_MS
                )

            if (deletedPublications > 0 || deletedCovers > 0) {
                ReaderTrace.event(
                    "library_orphan_reconciled",
                    details =
                        "publications=$deletedPublications covers=$deletedCovers"
                )
            }
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

data class ReadingSessionResumeState(
    val snapshot: ReadingSessionSnapshot,
    val notedHighlightIds: Set<String>
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

private data class ProgressSaveResult(
    val newlyFinished: Boolean,
    val crashCheckpointDurable: Boolean,
    val crashCheckpointLatencyNanos: Long?
)

private data class PendingProgressWrite(
    val id: String,
    val progress: Float,
    val pagesRead: Int,
    val locatorJson: String,
    val lastOpenedAtEpochMs: Long,
    val finished: Boolean,
    val traceSequence: Long? = null,
    val order: ReaderProgressWriteOrder? = null,
    val completionAtEpochMs: Long? = null,
    val completionSessionSnapshot: ReadingSessionSnapshot? = null,
    val completionBookSnapshot: Book? = null,
    val milestones: List<ReadingMilestoneRecord> = emptyList()
)

private const val PROGRESS_WRITE_INTERVAL_MS = 250L
private const val SESSION_WRITE_INTERVAL_MS = 1_000L
private const val ORPHAN_ARTIFACT_GRACE_MS = 24L * 60L * 60L * 1000L
private const val PASSAGE_REVISIT_MIN_AGE_MS = 30_000L
private const val PASSAGE_REVISIT_DEDUPE_MS = 5L * 60L * 1000L

internal fun canonicalOwnedArtifactPath(
    rawPath: String?,
    root: File
): String? {
    if (rawPath.isNullOrBlank()) return null
    return runCatching {
        val canonicalRoot = root.canonicalFile
        val candidate = File(rawPath).canonicalFile
        candidate
            .takeIf { it.toPath().startsWith(canonicalRoot.toPath()) }
            ?.absolutePath
    }.getOrNull()
}

internal fun reconcileOrphanedArtifactDirectory(
    root: File,
    ownedCanonicalPaths: Set<String>,
    nowEpochMs: Long,
    graceMillis: Long
): Int {
    if (nowEpochMs <= 0L || graceMillis < 0L) return 0
    val canonicalRoot = runCatching { root.canonicalFile }.getOrNull()
        ?: return 0
    val children = canonicalRoot.listFiles() ?: return 0
    var deleted = 0

    children.forEach { child ->
        val candidate = runCatching { child.canonicalFile }.getOrNull()
            ?: return@forEach
        // Only direct children are managed here. A symlink escaping the app-private
        // root or a nested directory is never a cleanup candidate.
        if (candidate.parentFile?.canonicalFile != canonicalRoot) return@forEach
        if (!candidate.isFile) return@forEach
        if (candidate.absolutePath in ownedCanonicalPaths) return@forEach

        val modifiedAt = candidate.lastModified()
        if (modifiedAt <= 0L || nowEpochMs < modifiedAt) return@forEach
        if (nowEpochMs - modifiedAt < graceMillis) return@forEach

        if (candidate.delete()) deleted += 1
    }
    return deleted
}

private fun stableCollectionId(normalizedName: String): String = UUID.nameUUIDFromBytes(
    "veil-collection:$normalizedName".toByteArray(StandardCharsets.UTF_8)
).toString()
