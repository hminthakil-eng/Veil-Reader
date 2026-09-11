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
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRuntimeRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var db: VeilDatabase
    private lateinit var settings: SettingsStore

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
        db.close()
        File(context.filesDir, "publications").deleteRecursively()
    }

    @Test
    fun runtimeRepository_serializesWrites_andRestoresSnapshotTransactionally() = runBlocking {
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
            collection = "Science Fiction"
        )

        repository.addImportedBook(book)
        repository.addHighlight("runtime-book", "A durable passage", "{\"href\":\"c1.xhtml\"}")
        assertTrue(repository.addBookmark("runtime-book", "Opening", "{\"href\":\"c1.xhtml\"}"))
        repository.saveProgress("runtime-book", 0.5, "{\"href\":\"c2.xhtml\"}")
        repository.saveAppearance(ReaderAppearance(theme = ReaderTheme.SEPIA, fontScale = 1.2))
        repository.flushWrites()

        val stored = db.books().findWithCollections("runtime-book") ?: error("runtime book missing")
        assertEquals("Runtime Tome", stored.book.title)
        assertEquals(0.5f, stored.book.progress)
        assertEquals(100, stored.book.pagesRead)
        assertEquals(listOf("Science Fiction"), stored.collections.map { it.name })
        assertEquals(1, db.highlights().observeAll().first().size)
        assertEquals(1, db.bookmarks().observeAll().first().size)
        assertEquals(ReaderTheme.SEPIA, settings.settings.first().readerAppearance.theme)

        val snapshot = repository.snapshot()
        repository.editMetadata("runtime-book", "Changed", "Different", "Temporary")
        repository.deleteHighlight(snapshot.highlights.single().id)
        repository.deleteBookmark(snapshot.bookmarks.single().id)
        repository.flushWrites()
        assertEquals("Changed", db.books().findEntity("runtime-book")?.title)
        assertEquals(0, db.highlights().observeAll().first().size)

        repository.replaceAll(snapshot)

        val restored = db.books().findWithCollections("runtime-book") ?: error("restored book missing")
        assertEquals("Runtime Tome", restored.book.title)
        assertEquals("Test Reader", restored.book.author)
        assertEquals(listOf("Science Fiction"), restored.collections.map { it.name })
        assertEquals(1, db.highlights().observeAll().first().size)
        assertEquals(1, db.bookmarks().observeAll().first().size)
    }

    @Test
    fun backupV2_roundTripsRoomState_andPublicationFile() = runBlocking {
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
            collection = "Archive"
        )
        repository.addImportedBook(book)
        repository.addHighlight("backup-book", "Preserve me", "{\"href\":\"chapter.xhtml\"}")
        repository.addBookmark("backup-book", "Saved place", "{\"href\":\"chapter.xhtml\"}")
        repository.saveAppearance(ReaderAppearance(theme = ReaderTheme.OLED, lineHeight = 1.7))
        repository.flushWrites()

        val backupFile = File(context.cacheDir, "veil-roundtrip-${UUID.randomUUID()}.zip")
        val exporter = LibraryExport(context, repository)
        exporter.writeBackup(Uri.fromFile(backupFile))
        assertTrue(backupFile.isFile && backupFile.length() > 0)

        repository.editMetadata("backup-book", "Mutated", "Changed", "Temporary")
        repository.deleteHighlight(repository.highlights.value.single().id)
        repository.deleteBookmark(repository.bookmarks.value.single().id)
        repository.flushWrites()

        val result = exporter.restoreBackup(Uri.fromFile(backupFile))

        assertEquals(1, result.booksRestored)
        assertEquals(1, result.highlightsRestored)
        val restored = db.books().findWithCollections("backup-book") ?: error("backup book missing")
        assertEquals("Backup Tome", restored.book.title)
        assertEquals("Archivist", restored.book.author)
        assertEquals(listOf("Archive"), restored.collections.map { it.name })
        val restoredFile = File(requireNotNull(Uri.parse(restored.book.sourceUri).path))
        assertTrue(restoredFile.isFile && restoredFile.readBytes().contentEquals("test publication".toByteArray()))
        assertEquals(1, db.highlights().observeAll().first().size)
        assertEquals(1, db.bookmarks().observeAll().first().size)
        assertEquals(ReaderTheme.OLED, settings.settings.first().readerAppearance.theme)
        backupFile.delete()
    }

    private fun repository(): LocalLibraryRepository = LocalLibraryRepository(
        appContext = context,
        database = db,
        settings = settings,
        runLegacyMigration = false
    )
}
