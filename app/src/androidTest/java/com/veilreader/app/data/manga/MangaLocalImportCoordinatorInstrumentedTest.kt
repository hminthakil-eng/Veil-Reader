package com.veilreader.app.data.manga

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.LibraryExport
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaReadingProgress
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaLocalImportCoordinatorInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: VeilDatabase
    private lateinit var settings: SettingsStore
    private lateinit var repository: LocalLibraryRepository
    private lateinit var coordinator: MangaLocalImportCoordinator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsStore(context)
        repository = LocalLibraryRepository(
            appContext = context,
            database = db,
            settings = settings,
            runLegacyMigration = false
        )
        coordinator = MangaLocalImportCoordinator(context, repository, db)
    }

    @After
    fun tearDown() {
        runBlocking {
            repository.closeForTest()
        }
        db.close()
        File(context.filesDir, "publications").deleteRecursively()
        File(context.filesDir, "covers").deleteRecursively()
        File(context.filesDir, "manga-cache").deleteRecursively()
        File(context.cacheDir, TEST_ROOT).deleteRecursively()
    }

    @Test
    fun validCbzImportsOnceAndDuplicateDoesNotForkCatalog() = runBlocking {
        val archive = testArchive("valid.cbz") {
            addPng("pages/10.png")
            addPng("pages/2.png")
            addPng("pages/001.png")
        }

        val first = coordinator.import(Uri.fromFile(archive)).getOrThrow()
        assertFalse(first.duplicate)
        assertEquals(BookFormat.COMIC, first.book.format)
        assertEquals(1, db.books().count())

        val chapters = db.mangaCatalog().listChapters(first.book.id)
        assertEquals(1, chapters.size)
        assertEquals(0, chapters.single().readingOrder)

        val offline = db.mangaOffline().listForBook(first.book.id)
        assertEquals(1, offline.size)
        assertEquals(listOf(0, 1, 2), offline.single().pages.map { it.pageIndex })
        assertTrue(offline.single().chapter.completed)
        assertTrue(
            offline.single().pages.all { page ->
                File(coordinator.cacheRoot, page.relativePath).isFile
            }
        )

        val second = coordinator.import(Uri.fromFile(archive)).getOrThrow()
        assertTrue(second.duplicate)
        assertEquals(first.book.id, second.book.id)
        assertEquals(1, db.books().count())
        assertEquals(1, db.mangaCatalog().listChapters(first.book.id).size)
    }

    @Test
    fun mangaBackupRoundTripRebuildsCatalogCacheAndExactProgress() = runBlocking {
        val archive = testArchive("backup.cbz") {
            addPng("001.png")
            addPng("002.png")
            addPng("003.png")
        }
        val imported = coordinator.import(Uri.fromFile(archive)).getOrThrow()
        val book = imported.book
        val mangaId = CanonicalMangaId(book.id)
        val progressStore = RoomMangaProgressStore(db)
        progressStore.save(
            MangaReadingProgress(
                mangaId = mangaId,
                chapter = MangaChapterAnchor(
                    number = 1.0,
                    normalizedTitle = book.title
                ),
                pageIndex = 1,
                pageCount = 3,
                chapterProgression = 0.5,
                updatedAtEpochMs = 777L
            )
        )

        val backup = File(context.cacheDir, TEST_ROOT + "/manga-backup.zip")
        LibraryExport(context, repository, db).writeBackup(Uri.fromFile(backup))
        assertTrue(backup.isFile && backup.length() > 0L)

        coordinator.deleteImportedManga(book.id).getOrThrow()
        assertEquals(0, db.books().count())
        assertTrue(File(context.filesDir, "manga-cache").walkTopDown().none { it.isFile })

        val result = LibraryExport(context, repository, db)
            .restoreBackup(Uri.fromFile(backup))
        assertEquals(1, result.booksRestored)

        val restoredBook = requireNotNull(db.books().findEntity(book.id))
        assertEquals(BookFormat.COMIC.name, restoredBook.format)
        assertTrue(
            File(requireNotNull(Uri.parse(restoredBook.sourceUri).path)).isFile
        )
        assertTrue(!restoredBook.contentFingerprint.isNullOrBlank())
        assertTrue(
            restoredBook.coverCachePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.isFile == true
        )

        val chapters = db.mangaCatalog().listChapters(book.id)
        assertEquals(1, chapters.size)
        val offline = db.mangaOffline().listForBook(book.id).single()
        assertEquals(listOf(0, 1, 2), offline.pages.map { it.pageIndex })
        assertTrue(
            offline.pages.all { page ->
                File(coordinator.cacheRoot, page.relativePath).isFile
            }
        )

        val restoredProgress = requireNotNull(progressStore.load(mangaId))
        assertEquals(1, restoredProgress.pageIndex)
        assertEquals(3, restoredProgress.pageCount)
        assertEquals(0.5, restoredProgress.chapterProgression, 0.000001)
        assertEquals(777L, restoredProgress.updatedAtEpochMs)
        assertEquals(0.5f, requireNotNull(db.books().findEntity(book.id)).progress, 0.000001f)
    }

    @Test
    fun unsafeArchiveRollsBackBookCatalogPublicationAndCache() = runBlocking {
        val archive = testArchive("unsafe.cbz") {
            addRaw("../escape.png", validPngBytes())
        }

        val result = coordinator.import(Uri.fromFile(archive))

        assertTrue(result.isFailure)
        assertEquals(0, db.books().count())
        assertTrue(repository.books.value.isEmpty())
        assertTrue(File(context.filesDir, "publications").listFiles().isNullOrEmpty())
        assertTrue(File(context.filesDir, "manga-cache").walkTopDown().none { it.isFile })
    }

    private fun testArchive(
        name: String,
        block: ArchiveBuilder.() -> Unit
    ): File {
        val root = File(context.cacheDir, TEST_ROOT).apply { mkdirs() }
        val file = File(root, name)
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            ArchiveBuilder(zip).block()
        }
        return file
    }

    private class ArchiveBuilder(
        private val zip: ZipOutputStream
    ) {
        fun addPng(name: String) {
            addRaw(name, validPngBytes())
        }

        fun addRaw(name: String, bytes: ByteArray) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }

    private companion object {
        const val TEST_ROOT = "manga-local-import-test"

        fun validPngBytes(): ByteArray {
            val bitmap = Bitmap.createBitmap(4, 6, Bitmap.Config.ARGB_8888)
            return try {
                java.io.ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
