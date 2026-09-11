package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReadingSessionSnapshot
import java.io.File
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
class BulkLibraryManagementInstrumentedTest {
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
        File(context.filesDir, "covers").deleteRecursively()
    }

    @Test
    fun bulkActions_updateSelectedBooks_andDeletionKeepsReadingHistory() = runBlocking {
        val repository = repository()
        val publications = File(context.filesDir, "publications").apply { mkdirs() }
        val covers = File(context.filesDir, "covers").apply { mkdirs() }

        val firstFile = File(publications, "bulk-first.epub").apply { writeText("first") }
        val secondFile = File(publications, "bulk-second.epub").apply { writeText("second") }
        val firstCover = File(covers, "bulk-first.jpg").apply { writeText("first-cover") }
        val secondCover = File(covers, "bulk-second.jpg").apply { writeText("second-cover") }

        val first = Book(
            id = "bulk-first",
            title = "First Tome",
            author = "Archivist",
            sourceUri = Uri.fromFile(firstFile).toString(),
            coverCachePath = firstCover.absolutePath,
            contentFingerprint = "bulk-first-fingerprint"
        )
        val second = Book(
            id = "bulk-second",
            title = "Second Tome",
            author = "Archivist",
            sourceUri = Uri.fromFile(secondFile).toString(),
            coverCachePath = secondCover.absolutePath,
            contentFingerprint = "bulk-second-fingerprint"
        )

        repository.addImportedBook(first)
        repository.addImportedBook(second)
        repository.addHighlight("bulk-first", "Delete with the book", "{\"href\":\"c1.xhtml\"}")
        assertTrue(repository.addBookmark("bulk-first", "Marked", "{\"href\":\"c1.xhtml\"}"))
        repository.saveReadingSession(
            ReadingSessionSnapshot(
                id = "bulk-session",
                bookId = "bulk-first",
                startedAtEpochMs = 100L,
                endedAtEpochMs = 200L,
                activeMillis = 80_000L,
                pacedPageTurns = 3,
                highlightCount = 1,
                noteCount = 0
            )
        )

        repository.setFavorite(setOf("bulk-first", "bulk-second"), true)
        repository.addCollection(setOf("bulk-first", "bulk-second"), "To Study")
        repository.flushWrites()

        val firstStored = db.books().findWithCollections("bulk-first") ?: error("first book missing")
        val secondStored = db.books().findWithCollections("bulk-second") ?: error("second book missing")
        assertTrue(firstStored.book.favorite)
        assertTrue(secondStored.book.favorite)
        assertTrue(firstStored.collections.any { it.name == "To Study" })
        assertTrue(secondStored.collections.any { it.name == "To Study" })

        val deleted = repository.deleteBooks(setOf("bulk-first"))

        assertEquals(1, deleted)
        assertNull(db.books().findEntity("bulk-first"))
        assertEquals(1, db.books().count())
        assertEquals(0, db.highlights().observeAll().first().size)
        assertEquals(0, db.bookmarks().observeAll().first().size)

        val retainedSession = db.readingSessions().listAll().single()
        assertEquals("bulk-session", retainedSession.id)
        assertNull(retainedSession.bookId)
        assertEquals(80_000L, retainedSession.activeMillis)

        assertFalse(firstFile.exists())
        assertFalse(firstCover.exists())
        assertTrue(secondFile.isFile)
        assertTrue(secondCover.isFile)

        val survivor = db.books().findWithCollections("bulk-second") ?: error("survivor missing")
        assertTrue(survivor.book.favorite)
        assertTrue(survivor.collections.any { it.name == "To Study" })
    }

    private fun repository(): LocalLibraryRepository = LocalLibraryRepository(
        appContext = context,
        database = db,
        settings = settings,
        runLegacyMigration = false
    )
}
