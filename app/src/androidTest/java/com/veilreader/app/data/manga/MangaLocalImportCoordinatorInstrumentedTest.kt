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
    fun appendingSecondCbzExtendsOneBookAndReopensCompletion() = runBlocking {
        val firstArchive = testArchive("Series ch 1.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        val first = coordinator.import(Uri.fromFile(firstArchive)).getOrThrow()
        val book = first.book
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
                pageCount = 2,
                chapterProgression = 1.0,
                updatedAtEpochMs = 500L
            )
        )
        assertTrue(requireNotNull(db.books().findEntity(book.id)).finished)

        val secondArchive = testArchive("Series Vol 3 Ch 2.5.cbz") {
            addPng("001.png")
            addPng("002.png")
            addPng("003.png")
        }
        val appended = coordinator.appendChapter(
            bookId = book.id,
            uri = Uri.fromFile(secondArchive)
        ).getOrThrow()

        assertFalse(appended.duplicate)
        assertEquals(1, appended.readingOrder)

        val chapters = db.mangaCatalog().listChapters(book.id)
        assertEquals(listOf(0, 1), chapters.map { it.readingOrder })
        assertEquals(3.0, requireNotNull(chapters[1].volume), 0.000001)
        assertEquals(2.5, requireNotNull(chapters[1].number), 0.000001)
        assertEquals("Series Vol 3 Ch 2.5", chapters[1].title)

        val sourceKeys = chapters.map { chapter ->
            db.mangaCatalog().listChapterSources(chapter.id).single().mangaKey
        }
        assertEquals(
            listOf("local:" + book.id, "local:" + book.id),
            sourceKeys
        )
        assertEquals(2, db.mangaOffline().listForBook(book.id).size)

        val extendedSummary = requireNotNull(db.books().findEntity(book.id))
        assertEquals(0.5f, extendedSummary.progress, 0.000001f)
        assertFalse(extendedSummary.finished)

        val duplicateSecond = coordinator.appendChapter(
            bookId = book.id,
            uri = Uri.fromFile(secondArchive)
        ).getOrThrow()
        assertTrue(duplicateSecond.duplicate)
        assertEquals(2, db.mangaCatalog().listChapters(book.id).size)

        val duplicatePrimary = coordinator.appendChapter(
            bookId = book.id,
            uri = Uri.fromFile(firstArchive)
        ).getOrThrow()
        assertTrue(duplicatePrimary.duplicate)
        assertEquals(2, db.mangaCatalog().listChapters(book.id).size)
    }

    @Test
    fun batchImportUsesNaturalFilenameOrderAndCountsDuplicates() = runBlocking {
        val firstArchive = testArchive("Batch ch 1.cbz") {
            addPng("001.png")
        }
        val book = coordinator.import(Uri.fromFile(firstArchive))
            .getOrThrow()
            .book

        val chapter10 = testArchive("Batch ch 10.cbz") {
            addPng("001.png")
        }
        val chapter2 = testArchive("Batch ch 2.cbz") {
            addPng("001.png")
        }
        val chapter3 = testArchive("Batch ch 3.cbz") {
            addPng("001.png")
        }

        val batch = coordinator.appendChapters(
            bookId = book.id,
            uris = listOf(
                Uri.fromFile(chapter10),
                Uri.fromFile(chapter2),
                Uri.fromFile(chapter3),
                Uri.fromFile(firstArchive),
                Uri.fromFile(chapter2)
            )
        ).getOrThrow()

        assertEquals(3, batch.addedCount)
        assertEquals(1, batch.duplicateCount)
        assertEquals(3, batch.lastReadingOrder)

        val chapters = db.mangaCatalog().listChapters(book.id)
        assertEquals(listOf(0, 1, 2, 3), chapters.map { it.readingOrder })
        assertEquals(
            listOf(1.0, 2.0, 3.0, 10.0),
            chapters.map { requireNotNull(it.number) }
        )
        assertEquals(
            listOf(
                book.title,
                "Batch ch 2",
                "Batch ch 3",
                "Batch ch 10"
            ),
            chapters.map { it.normalizedTitle }
        )
    }

    @Test
    fun derivedCacheCanBeClearedAndSelfHealedWithoutLosingProgress() = runBlocking {
        val firstArchive = testArchive("cache ch 1.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        val book = coordinator.import(Uri.fromFile(firstArchive))
            .getOrThrow()
            .book
        val secondArchive = testArchive("cache ch 2.cbz") {
            addPng("001.png")
            addPng("002.png")
            addPng("003.png")
        }
        coordinator.appendChapter(
            bookId = book.id,
            uri = Uri.fromFile(secondArchive)
        ).getOrThrow()

        val second = db.mangaCatalog()
            .listChapters(book.id)
            .single { it.readingOrder == 1 }
        val secondSource = db.mangaCatalog()
            .listChapterSources(second.id)
            .single()
        val progressStore = RoomMangaProgressStore(db)
        progressStore.save(
            MangaReadingProgress(
                mangaId = CanonicalMangaId(book.id),
                chapter = MangaChapterAnchor(
                    volume = second.volume,
                    number = second.number,
                    languageTag = second.languageTag,
                    normalizedTitle = second.normalizedTitle,
                    providerChapterKeyHint = secondSource.chapterKey
                ),
                pageIndex = 1,
                pageCount = 3,
                chapterProgression = 0.5,
                updatedAtEpochMs = 999L
            )
        )

        val before = coordinator.storageSummary(book.id)
        assertEquals(2, before.chapterCount)
        assertEquals(5, before.offlinePageCount)
        assertTrue(before.sourceBytes > 0L)
        assertTrue(before.cacheBytes > 0L)

        val freed = coordinator.clearDerivedCache(book.id).getOrThrow()
        assertEquals(before.cacheBytes, freed)
        assertTrue(db.mangaOffline().listForBook(book.id).isEmpty())

        val afterClear = coordinator.storageSummary(book.id)
        assertEquals(before.sourceBytes, afterClear.sourceBytes)
        assertEquals(0L, afterClear.cacheBytes)
        assertEquals(0, afterClear.offlinePageCount)

        val progressAfterClear = requireNotNull(
            progressStore.load(CanonicalMangaId(book.id))
        )
        assertEquals(1, progressAfterClear.pageIndex)
        assertEquals(0.5, progressAfterClear.chapterProgression, 0.000001)
        assertEquals(999L, progressAfterClear.updatedAtEpochMs)

        assertEquals(2, coordinator.ensureLocalCache(book.id).getOrThrow())
        assertEquals(2, db.mangaOffline().listForBook(book.id).size)

        val afterRepair = coordinator.storageSummary(book.id)
        assertEquals(5, afterRepair.offlinePageCount)
        assertTrue(afterRepair.cacheBytes > 0L)
        assertEquals(before.sourceBytes, afterRepair.sourceBytes)

        val progressAfterRepair = requireNotNull(
            progressStore.load(CanonicalMangaId(book.id))
        )
        assertEquals(1, progressAfterRepair.pageIndex)
        assertEquals(0.5, progressAfterRepair.chapterProgression, 0.000001)
        assertEquals(999L, progressAfterRepair.updatedAtEpochMs)
    }

    @Test
    fun mangaBackupRoundTripRebuildsCatalogCacheAndExactProgress() = runBlocking {
        val archive = testArchive("backup ch 1.cbz") {
            addPng("001.png")
            addPng("002.png")
            addPng("003.png")
        }
        val imported = coordinator.import(Uri.fromFile(archive)).getOrThrow()
        val book = imported.book
        val mangaId = CanonicalMangaId(book.id)

        val secondArchive = testArchive("backup ch 2.cbz") {
            addPng("001.png")
            addPng("002.png")
            addPng("003.png")
        }
        coordinator.appendChapter(
            bookId = book.id,
            uri = Uri.fromFile(secondArchive),
            metadata = MangaLocalChapterMetadata(
                title = "Second descent",
                volume = 1.0,
                number = 2.0,
                languageTag = "en"
            )
        ).getOrThrow()

        val chaptersBefore = db.mangaCatalog().listChapters(book.id)
        val secondBefore = chaptersBefore.single { it.readingOrder == 1 }
        val secondSourceBefore = db.mangaCatalog()
            .listChapterSources(secondBefore.id)
            .single()

        val progressStore = RoomMangaProgressStore(db)
        progressStore.save(
            MangaReadingProgress(
                mangaId = mangaId,
                chapter = MangaChapterAnchor(
                    volume = secondBefore.volume,
                    number = secondBefore.number,
                    languageTag = secondBefore.languageTag,
                    normalizedTitle = secondBefore.normalizedTitle,
                    providerChapterKeyHint = secondSourceBefore.chapterKey
                ),
                pageIndex = 1,
                pageCount = 3,
                chapterProgression = 0.5,
                updatedAtEpochMs = 777L
            )
        )
        assertEquals(
            0.75f,
            requireNotNull(db.books().findEntity(book.id)).progress,
            0.000001f
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
        assertEquals(listOf(0, 1), chapters.map { it.readingOrder })
        val restoredSecond = chapters.single { it.readingOrder == 1 }
        assertEquals("Second descent", restoredSecond.title)
        assertEquals(1.0, requireNotNull(restoredSecond.volume), 0.000001)
        assertEquals(2.0, requireNotNull(restoredSecond.number), 0.000001)
        assertEquals("en", restoredSecond.languageTag)

        val offline = db.mangaOffline().listForBook(book.id)
        assertEquals(2, offline.size)
        assertTrue(
            offline.flatMap { it.pages }.all { page ->
                File(coordinator.cacheRoot, page.relativePath).isFile
            }
        )

        val restoredProgress = requireNotNull(progressStore.load(mangaId))
        assertEquals(2.0, requireNotNull(restoredProgress.chapter.number), 0.000001)
        assertEquals("Second descent", restoredProgress.chapter.normalizedTitle)
        assertEquals(1, restoredProgress.pageIndex)
        assertEquals(3, restoredProgress.pageCount)
        assertEquals(0.5, restoredProgress.chapterProgression, 0.000001)
        assertEquals(777L, restoredProgress.updatedAtEpochMs)
        assertEquals(
            0.75f,
            requireNotNull(db.books().findEntity(book.id)).progress,
            0.000001f
        )
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
