package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.BookMetadataUpdate
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderBrightness
import com.veilreader.app.domain.ReaderBrightnessMode
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.domain.ReadingSessionSnapshot
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRuntimeRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var db: VeilDatabase
    private lateinit var settings: SettingsStore
    private var repositoryUnderTest: LocalLibraryRepository? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsStore(context)
    }

    @After
    fun tearDown() {
        repositoryUnderTest?.closeForTest()
        repositoryUnderTest = null
        db.close()
        File(context.filesDir, "publications").deleteRecursively()
        File(context.filesDir, "covers").deleteRecursively()
    }

    @Test
    fun runtimeRepository_serializesWrites_andRestoresSnapshotTransactionally() = runBlocking<Unit> {
        val repository = repository()
        val book = Book(
            id = "runtime-book",
            title = "Runtime Tome",
            author = "Test Reader",
            totalPages = 200,
            format = BookFormat.EPUB,
            sourceUri = "file:///runtime.epub",
            mediaType = "application/epub+zip",
            addedAtEpochMs = 10L,
            contentFingerprint = "fingerprint-runtime",
            seriesName = "Veil Cycle",
            seriesIndex = 2.0,
            language = "en",
            collection = "Science Fiction",
            collections = listOf("Science Fiction", "Research")
        )

        repository.addImportedBook(book)
        repository.addHighlight("runtime-book", "A durable passage", "{\"href\":\"c1.xhtml\"}")
        assertTrue(repository.addBookmark("runtime-book", "Opening", "{\"href\":\"c1.xhtml\"}"))
        repository.saveProgress("runtime-book", 0.5, "{\"href\":\"c2.xhtml\"}")
        repository.updateCoverCachePath("runtime-book", "/covers/runtime.jpg")
        settings.saveReaderAppearance(ReaderAppearance(theme = ReaderTheme.SEPIA, fontScale = 1.2))
        repository.flushWrites()

        val stored = db.books().findWithCollections("runtime-book") ?: error("runtime book missing")
        assertEquals("Runtime Tome", stored.book.title)
        assertEquals(0.5f, stored.book.progress)
        assertEquals(100, stored.book.pagesRead)
        assertEquals("/covers/runtime.jpg", stored.book.coverCachePath)
        assertEquals("fingerprint-runtime", stored.book.contentFingerprint)
        assertEquals("Veil Cycle", stored.book.seriesName)
        assertEquals(2.0, stored.book.seriesIndex)
        assertEquals("en", stored.book.language)
        assertEquals(listOf("Research", "Science Fiction"), stored.collections.map { it.name }.sorted())
        assertEquals(1, db.highlights().observeAll().first().size)
        assertEquals(1, db.bookmarks().observeAll().first().size)
        assertEquals(ReaderTheme.SEPIA, settings.settings.first().readerAppearance.theme)

        val snapshot = repository.snapshot()
        repository.editMetadata(
            BookMetadataUpdate(
                bookId = "runtime-book",
                title = "Changed",
                author = "Different",
                collections = listOf("Temporary"),
                seriesName = "Changed Series",
                seriesIndex = 4.0,
                language = "tr"
            )
        )
        repository.deleteHighlight(snapshot.highlights.single().id)
        repository.deleteBookmark(snapshot.bookmarks.single().id)
        repository.flushWrites()
        assertEquals("Changed", db.books().findEntity("runtime-book")?.title)
        assertEquals(0, db.highlights().observeAll().first().size)

        repository.replaceAll(snapshot)

        val restored = db.books().findWithCollections("runtime-book") ?: error("restored book missing")
        assertEquals("Runtime Tome", restored.book.title)
        assertEquals("Test Reader", restored.book.author)
        assertEquals("/covers/runtime.jpg", restored.book.coverCachePath)
        assertEquals("fingerprint-runtime", restored.book.contentFingerprint)
        assertEquals("Veil Cycle", restored.book.seriesName)
        assertEquals(2.0, restored.book.seriesIndex)
        assertEquals("en", restored.book.language)
        assertEquals(listOf("Research", "Science Fiction"), restored.collections.map { it.name }.sorted())
        assertEquals(1, db.highlights().observeAll().first().size)
        assertEquals(1, db.bookmarks().observeAll().first().size)
    }

    @Test
    fun duplicateFingerprint_returnsExistingBook_andDeletesTransientImportArtifacts() = runBlocking<Unit> {
        val repository = repository()
        val publications = File(context.filesDir, "publications").apply { mkdirs() }
        val covers = File(context.filesDir, "covers").apply { mkdirs() }
        val firstFile = File(publications, "first.epub").apply { writeText("same book") }
        val duplicateFile = File(publications, "duplicate.epub").apply { writeText("same book") }
        val duplicateCover = File(covers, "duplicate.jpg").apply { writeText("cover") }

        val first = Book(
            id = "first-book",
            title = "One Copy",
            author = "Author",
            sourceUri = Uri.fromFile(firstFile).toString(),
            contentFingerprint = "same-sha256"
        )
        val duplicate = Book(
            id = "duplicate-book",
            title = "One Copy",
            author = "Author",
            sourceUri = Uri.fromFile(duplicateFile).toString(),
            coverCachePath = duplicateCover.absolutePath,
            contentFingerprint = "same-sha256"
        )

        val firstResult = repository.addImportedBook(first)
        val duplicateResult = repository.addImportedBook(duplicate)
        repository.flushWrites()

        assertFalse(firstResult.duplicate)
        assertTrue(duplicateResult.duplicate)
        assertEquals("first-book", duplicateResult.book.id)
        assertEquals(1, db.books().count())
        assertTrue(firstFile.isFile)
        assertFalse(duplicateFile.exists())
        assertFalse(duplicateCover.exists())
    }

    @Test
    fun backupV2_roundTripsRoomState_publicationFile_metadataCollections_andReadingSessions() = runBlocking<Unit> {
        val repository = repository()
        val publications = File(context.filesDir, "publications").apply { mkdirs() }
        val publication = File(publications, "roundtrip.epub").apply { writeBytes("test publication".toByteArray()) }
        val book = Book(
            id = "backup-book",
            title = "Backup Tome",
            author = "Archivist",
            totalPages = 120,
            format = BookFormat.EPUB,
            sourceUri = Uri.fromFile(publication).toString(),
            mediaType = "application/epub+zip",
            addedAtEpochMs = 50L,
            coverCachePath = File(context.filesDir, "covers/backup.jpg").absolutePath,
            contentFingerprint = "derived-before-backup",
            seriesName = "Archive Cycle",
            seriesIndex = 1.5,
            language = "fa",
            collection = "Archive",
            collections = listOf("Archive", "Reference")
        )
        repository.addImportedBook(book)
        repository.addHighlight("backup-book", "Preserve me", "{\"href\":\"chapter.xhtml\"}")
        repository.addBookmark("backup-book", "Saved place", "{\"href\":\"chapter.xhtml\"}")
        settings.saveReaderAppearance(
            ReaderAppearance(
                theme = ReaderTheme.OLED,
                lineHeight = 1.7,
                brightness = ReaderBrightness(
                    mode = ReaderBrightnessMode.OVERRIDE,
                    level = 0.42
                )
            )
        )
        repository.saveReadingSession(
            ReadingSessionSnapshot(
                id = "session-backup",
                bookId = "backup-book",
                startedAtEpochMs = 100L,
                endedAtEpochMs = 200L,
                activeMillis = 90_000L,
                pacedPageTurns = 4,
                highlightCount = 1,
                noteCount = 1
            )
        )
        repository.flushWrites()

        val backupFile = File(context.cacheDir, "veil-roundtrip-${UUID.randomUUID()}.zip")
        val exporter = LibraryExport(context, repository)
        exporter.writeBackup(Uri.fromFile(backupFile))
        assertTrue(backupFile.isFile && backupFile.length() > 0)

        repository.editMetadata(
            BookMetadataUpdate(
                bookId = "backup-book",
                title = "Mutated",
                author = "Changed",
                collections = listOf("Temporary")
            )
        )
        repository.deleteHighlight(repository.highlights.value.single().id)
        repository.deleteBookmark(repository.bookmarks.value.single().id)
        repository.flushWrites()
        db.readingSessions().deleteAll()

        val result = exporter.restoreBackup(Uri.fromFile(backupFile))

        assertEquals(1, result.booksRestored)
        assertEquals(1, result.highlightsRestored)
        val restored = db.books().findWithCollections("backup-book") ?: error("backup book missing")
        assertEquals("Backup Tome", restored.book.title)
        assertEquals("Archivist", restored.book.author)
        assertEquals("Archive Cycle", restored.book.seriesName)
        assertEquals(1.5, restored.book.seriesIndex)
        assertEquals("fa", restored.book.language)
        assertNull(restored.book.coverCachePath)
        assertNull(restored.book.contentFingerprint)
        assertEquals(listOf("Archive", "Reference"), restored.collections.map { it.name }.sorted())
        val restoredFile = File(requireNotNull(Uri.parse(restored.book.sourceUri).path))
        assertTrue(restoredFile.isFile && restoredFile.readBytes().contentEquals("test publication".toByteArray()))
        assertEquals(1, db.highlights().observeAll().first().size)
        assertEquals(1, db.bookmarks().observeAll().first().size)
        val restoredAppearance = settings.settings.first().readerAppearance
        assertEquals(ReaderTheme.OLED, restoredAppearance.theme)
        assertEquals(ReaderBrightnessMode.OVERRIDE, restoredAppearance.brightness.mode)
        assertEquals(0.42, restoredAppearance.brightness.level, 0.0001)

        val restoredSession = db.readingSessions().listAll().single()
        assertEquals("session-backup", restoredSession.id)
        assertEquals("backup-book", restoredSession.bookId)
        assertEquals(90_000L, restoredSession.activeMillis)
        assertEquals(4, restoredSession.pacedPageTurns)
        assertEquals(1, restoredSession.highlightCount)
        assertEquals(1, restoredSession.noteCount)
        backupFile.delete()
    }

    @Test
    fun pdfiumLocatorMigration_rewritesOnlyMatchingBookState_transactionally() = runBlocking<Unit> {
        val repository = repository()
        val pdf = Book(
            id = "pdf-book",
            title = "Legacy PDF",
            author = "Reader",
            format = BookFormat.PDF,
            sourceUri = "file:///legacy.pdf",
            mediaType = "application/pdf"
        )
        val other = Book(
            id = "other-book",
            title = "Other",
            author = "Reader",
            format = BookFormat.PDF,
            sourceUri = "file:///other.pdf",
            mediaType = "application/pdf"
        )
        repository.addImportedBook(pdf)
        repository.addImportedBook(other)

        val oldProgress = """{"href":"document.pdf","type":"application/pdf","locations":{"position":3}}"""
        val newProgress = """{"href":"document.pdf","type":"application/pdf","locations":{"position":2,"veilPdfiumLocatorVersion":1}}"""
        val oldBookmark = """{"href":"document.pdf","type":"application/pdf","locations":{"position":5}}"""
        val newBookmark = """{"href":"document.pdf","type":"application/pdf","locations":{"position":4,"veilPdfiumLocatorVersion":1}}"""
        val oldHighlight = """{"href":"document.pdf","type":"application/pdf","locations":{"position":7}}"""
        val newHighlight = """{"href":"document.pdf","type":"application/pdf","locations":{"position":6,"veilPdfiumLocatorVersion":1}}"""
        val untouched = """{"href":"other.pdf","type":"application/pdf","locations":{"position":9}}"""

        repository.saveProgress("pdf-book", 0.4, oldProgress)
        repository.addBookmark("pdf-book", "Legacy bookmark", oldBookmark)
        repository.addHighlight("pdf-book", "Legacy highlight", oldHighlight)
        repository.addBookmark("other-book", "Other bookmark", untouched)
        repository.flushWrites()

        repository.applyPdfiumLocatorMigrations(
            bookId = "pdf-book",
            migrations = mapOf(
                oldProgress to newProgress,
                oldBookmark to newBookmark,
                oldHighlight to newHighlight
            )
        )

        assertEquals(newProgress, db.books().findEntity("pdf-book")?.locatorJson)
        assertEquals(
            newBookmark,
            db.bookmarks().listAll().first { it.bookId == "pdf-book" }.locatorJson
        )
        assertEquals(
            newHighlight,
            db.highlights().listAll().first { it.bookId == "pdf-book" }.locatorJson
        )
        assertEquals(
            untouched,
            db.bookmarks().listAll().first { it.bookId == "other-book" }.locatorJson
        )
        assertEquals(
            setOf(newProgress, newBookmark, newHighlight),
            repository.locatorJsonsForBook("pdf-book")
        )
    }

    @Test
    fun backupV2_preservesStampedPdfiumLocators() = runBlocking<Unit> {
        val repository = repository()
        val publications = File(context.filesDir, "publications").apply { mkdirs() }
        val publication = File(publications, "locator-roundtrip.pdf").apply {
            writeBytes("pdf fixture".toByteArray())
        }
        val stampedProgress = """{"href":"document.pdf","type":"application/pdf","locations":{"position":2,"veilPdfiumLocatorVersion":1}}"""
        val stampedBookmark = """{"href":"document.pdf","type":"application/pdf","locations":{"position":4,"veilPdfiumLocatorVersion":1}}"""

        repository.addImportedBook(
            Book(
                id = "pdf-backup",
                title = "Migrated PDF",
                author = "Reader",
                format = BookFormat.PDF,
                sourceUri = Uri.fromFile(publication).toString(),
                mediaType = "application/pdf",
                locatorJson = stampedProgress
            )
        )
        repository.addBookmark("pdf-backup", "Migrated bookmark", stampedBookmark)
        repository.flushWrites()

        val backupFile = File(context.cacheDir, "veil-pdf-locator-${UUID.randomUUID()}.zip")
        val exporter = LibraryExport(context, repository)
        exporter.writeBackup(Uri.fromFile(backupFile))

        repository.replaceAll(
            LibrarySnapshot(
                books = emptyList(),
                highlights = emptyList(),
                bookmarks = emptyList(),
                appearance = ReaderAppearance()
            )
        )
        exporter.restoreBackup(Uri.fromFile(backupFile))

        assertEquals(stampedProgress, db.books().findEntity("pdf-backup")?.locatorJson)
        assertEquals(stampedBookmark, db.bookmarks().listAll().single().locatorJson)
        backupFile.delete()
    }

    @Test
    fun readerProgressHotPath_preservesMetadata_andFinishesOnlyOnce() = runBlocking<Unit> {
        val repository = repository()
        val book = Book(
            id = "hot-path-book",
            title = "Stable Metadata",
            author = "Reader",
            totalPages = 100,
            sourceUri = "file:///hot-path.epub",
            contentFingerprint = "hot-path-fingerprint",
            seriesName = "Performance Cycle",
            seriesIndex = 3.0,
            language = "en"
        )

        repository.addImportedBook(book)
        assertFalse(repository.saveProgress("hot-path-book", 0.4, "{\"href\":\"c4.xhtml\"}"))
        repository.markOpened("hot-path-book")
        assertTrue(repository.saveProgress("hot-path-book", 0.999, "{\"href\":\"final.xhtml\"}"))
        assertFalse(repository.saveProgress("hot-path-book", 1.0, "{\"href\":\"final.xhtml\"}"))
        repository.flushWrites()

        val stored = db.books().findEntity("hot-path-book") ?: error("hot-path book missing")
        assertEquals("Stable Metadata", stored.title)
        assertEquals("Reader", stored.author)
        assertEquals("hot-path-fingerprint", stored.contentFingerprint)
        assertEquals("Performance Cycle", stored.seriesName)
        assertEquals(3.0, stored.seriesIndex)
        assertEquals("en", stored.language)
        assertEquals(1.0f, stored.progress)
        assertEquals(100, stored.pagesRead)
        assertEquals("{\"href\":\"final.xhtml\"}", stored.locatorJson)
        assertTrue(stored.finished)
        assertTrue(stored.lastOpenedAtEpochMs > 0L)
    }

    @Test
    fun rapidProgressEvents_coalesceToOneDatabaseUpdate_withLatestLocator() = runBlocking<Unit> {
        val repository = repository()
        repository.addImportedBook(
            Book(
                id = "coalesce-book",
                title = "Coalesced Reader",
                author = "Performance",
                totalPages = 100,
                sourceUri = "file:///coalesce.epub"
            )
        )

        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL("CREATE TABLE progress_write_probe (writes INTEGER NOT NULL)")
        sqlite.execSQL("INSERT INTO progress_write_probe(writes) VALUES (0)")
        sqlite.execSQL(
            """
            CREATE TRIGGER progress_write_counter
            AFTER UPDATE OF progress, pagesRead, locatorJson, finished ON books
            BEGIN
                UPDATE progress_write_probe SET writes = writes + 1;
            END
            """.trimIndent()
        )

        repeat(20) { index ->
            repository.saveProgress(
                "coalesce-book",
                index / 20.0,
                "{\"href\":\"chapter-$index.xhtml\"}"
            )
        }
        repository.flushProgress("coalesce-book")
        repository.flushWrites()

        sqlite.query("SELECT writes FROM progress_write_probe").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }

        val stored = db.books().findEntity("coalesce-book") ?: error("coalesced book missing")
        assertEquals(0.95f, stored.progress)
        assertEquals(95, stored.pagesRead)
        assertEquals("{\"href\":\"chapter-19.xhtml\"}", stored.locatorJson)
        assertFalse(stored.finished)
    }

    @Test
    fun rapidReadingSessionSnapshots_coalesceToOneDatabaseWrite_withLatestState() = runBlocking<Unit> {
        val repository = repository()
        repository.addImportedBook(
            Book(
                id = "session-book",
                title = "Session Tome",
                author = "Performance",
                sourceUri = "file:///session.epub"
            )
        )
        repository.saveReadingSession(
            ReadingSessionSnapshot(
                id = "session-coalesce",
                bookId = "session-book",
                startedAtEpochMs = 100L,
                endedAtEpochMs = 100L,
                activeMillis = 0L,
                pacedPageTurns = 0,
                highlightCount = 0,
                noteCount = 0
            )
        )
        repository.flushWrites()

        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL("CREATE TABLE session_write_probe (writes INTEGER NOT NULL)")
        sqlite.execSQL("INSERT INTO session_write_probe(writes) VALUES (0)")
        sqlite.execSQL(
            """
            CREATE TRIGGER session_write_counter_insert
            AFTER INSERT ON reading_sessions
            BEGIN
                UPDATE session_write_probe SET writes = writes + 1;
            END
            """.trimIndent()
        )
        sqlite.execSQL(
            """
            CREATE TRIGGER session_write_counter_update
            AFTER UPDATE ON reading_sessions
            BEGIN
                UPDATE session_write_probe SET writes = writes + 1;
            END
            """.trimIndent()
        )

        repeat(20) { index ->
            repository.saveReadingSession(
                ReadingSessionSnapshot(
                    id = "session-coalesce",
                    bookId = "session-book",
                    startedAtEpochMs = 100L,
                    endedAtEpochMs = 200L + index,
                    activeMillis = index * 1_000L,
                    pacedPageTurns = index,
                    highlightCount = index / 2,
                    noteCount = index / 3
                )
            )
        }
        repository.flushReadingSession("session-coalesce")
        repository.flushWrites()

        sqlite.query("SELECT writes FROM session_write_probe").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }

        val stored = db.readingSessions().listAll().single { it.id == "session-coalesce" }
        assertEquals(19_000L, stored.activeMillis)
        assertEquals(19, stored.pacedPageTurns)
        assertEquals(9, stored.highlightCount)
        assertEquals(6, stored.noteCount)
        assertEquals(219L, stored.endedAtEpochMs)
    }

    private fun repository(): LocalLibraryRepository = LocalLibraryRepository(
        appContext = context,
        database = db,
        settings = settings,
        runLegacyMigration = false
    ).also { repositoryUnderTest = it }
}
