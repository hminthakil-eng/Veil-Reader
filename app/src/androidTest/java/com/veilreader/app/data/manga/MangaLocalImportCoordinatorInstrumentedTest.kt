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
    fun mergePreflight_verifiesArchives_andPlansIdentityRebuildWithoutMutation() = runBlocking {
        val targetArchive = testArchive("Merge target ch 1.cbz") {
            addPng("001.png")
        }
        val sourceArchive = testArchive("Merge source ch 2.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        val target = coordinator.import(Uri.fromFile(targetArchive)).getOrThrow().book
        val source = coordinator.import(Uri.fromFile(sourceArchive)).getOrThrow().book

        val beforeTarget = db.mangaCatalog().listChapters(target.id)
        val beforeSource = db.mangaCatalog().listChapters(source.id)

        val plan = coordinator.preflightLocalMerge(
            targetBookId = target.id,
            sourceBookIds = listOf(source.id)
        ).getOrThrow()

        assertEquals(target.id, plan.targetBookId)
        assertEquals(listOf(source.id), plan.sourceBookIds)
        assertEquals(1, plan.chapterActions.size)
        assertEquals(1, plan.chapterActions.single().targetReadingOrder)
        assertTrue(plan.chapterActions.single().rebuildDerivedCache)
        assertFalse(plan.mayDeleteSourceBooksBeforeVerification)
        assertEquals(source.sourceUri, plan.splitReceiptSeed.sourceSnapshots.single().sourceUri)

        // Preflight is intentionally side-effect free.
        assertEquals(beforeTarget, db.mangaCatalog().listChapters(target.id))
        assertEquals(beforeSource, db.mangaCatalog().listChapters(source.id))
        assertEquals(2, db.books().count())
    }

    @Test
    fun reversibleMerge_copiesTargetHidesSource_andSplitRestoresExactTargetProgress() =
        runBlocking {
            val targetArchive = testArchive("Reversible target ch 1.cbz") {
                addPng("001.png")
                addPng("002.png")
            }
            val sourceArchive = testArchive("Reversible source ch 2.cbz") {
                addPng("001.png")
                addPng("002.png")
                addPng("003.png")
            }
            val target = coordinator.import(Uri.fromFile(targetArchive)).getOrThrow().book
            val source = coordinator.import(Uri.fromFile(sourceArchive)).getOrThrow().book

            val sourcePrimary = db.mangaCatalog().listChapters(source.id).single()
            coordinator.updateChapterMetadata(
                bookId = source.id,
                chapterId = sourcePrimary.id,
                metadata = MangaLocalChapterMetadata(
                    title = "Reversible source ch 2",
                    number = 2.0
                )
            ).getOrThrow()

            val targetChapter = db.mangaCatalog().listChapters(target.id).single()
            val targetSource = db.mangaCatalog().listChapterSources(targetChapter.id).single()
            RoomMangaProgressStore(db).save(
                MangaReadingProgress(
                    mangaId = CanonicalMangaId(target.id),
                    chapter = MangaChapterAnchor(
                        volume = targetChapter.volume,
                        number = targetChapter.number,
                        languageTag = targetChapter.languageTag,
                        normalizedTitle = targetChapter.normalizedTitle,
                        providerChapterKeyHint = targetSource.chapterKey
                    ),
                    pageIndex = 0,
                    pageCount = 2,
                    chapterProgression = 0.5,
                    updatedAtEpochMs = 1_000L
                )
            )
            val sourceChapterBefore = db.mangaCatalog().listChapters(source.id).single()
            val sourceLinkBefore = db.mangaCatalog()
                .listChapterSources(sourceChapterBefore.id)
                .single()
            RoomMangaProgressStore(db).save(
                MangaReadingProgress(
                    mangaId = CanonicalMangaId(source.id),
                    chapter = MangaChapterAnchor(
                        volume = sourceChapterBefore.volume,
                        number = sourceChapterBefore.number,
                        languageTag = sourceChapterBefore.languageTag,
                        normalizedTitle = sourceChapterBefore.normalizedTitle,
                        providerChapterKeyHint = sourceLinkBefore.chapterKey
                    ),
                    pageIndex = 1,
                    pageCount = 3,
                    chapterProgression = 0.66,
                    updatedAtEpochMs = 2_000L
                )
            )
            val sourceFile = File(
                requireNotNull(Uri.parse(requireNotNull(source.sourceUri)).path)
            )
            assertTrue(sourceFile.isFile)

            val merged = coordinator.executeLocalMerge(
                targetBookId = target.id,
                sourceBookIds = listOf(source.id)
            ).getOrThrow()

            assertEquals(1, merged.copiedChapterCount)
            assertEquals(0, merged.deduplicatedChapterCount)
            assertEquals(2, db.books().count())
            assertEquals(
                listOf(target.id),
                db.books().observeVisible().first().map { it.book.id }
            )
            assertTrue(sourceFile.isFile)
            assertEquals(2, db.mangaCatalog().listChapters(target.id).size)
            assertEquals(1, db.mangaCatalog().listChapters(source.id).size)

            val receipt = requireNotNull(db.mangaMerges().find(merged.mergeId))
            assertEquals(target.id, receipt.merge.targetBookId)
            assertEquals(0.5f, receipt.merge.targetBookProgress, 0.000001f)
            assertEquals(targetChapter.id, receipt.merge.targetProgressChapterId)
            assertEquals(listOf(source.id), receipt.members.map { it.sourceBookId })

            val mergedChapter = db.mangaCatalog()
                .listChapters(target.id)
                .single { it.readingOrder == 1 }
            val mergedLink = db.mangaCatalog()
                .listChapterSources(mergedChapter.id)
                .single()
            val copiedArchive = requireNotNull(
                coordinator.resolveLocalArchiveFile(
                    book = target,
                    chapterKey = mergedLink.chapterKey,
                    readingOrder = mergedChapter.readingOrder
                )
            )
            assertTrue(copiedArchive.isFile)

            RoomMangaProgressStore(db).save(
                MangaReadingProgress(
                    mangaId = CanonicalMangaId(target.id),
                    chapter = MangaChapterAnchor(
                        volume = mergedChapter.volume,
                        number = mergedChapter.number,
                        languageTag = mergedChapter.languageTag,
                        normalizedTitle = mergedChapter.normalizedTitle,
                        providerChapterKeyHint = mergedLink.chapterKey
                    ),
                    pageIndex = 1,
                    pageCount = 3,
                    chapterProgression = 0.75,
                    updatedAtEpochMs = 3_000L
                )
            )
            assertTrue(requireNotNull(db.books().findEntity(target.id)).progress > 0.5f)

            val split = coordinator.splitLocalMerge(target.id).getOrThrow()

            assertEquals(merged.mergeId, split.mergeId)
            assertEquals(listOf(source.id), split.restoredSourceBookIds)
            assertEquals(1, split.removedCopiedChapterCount)
            assertEquals(2, db.books().count())
            assertEquals(
                setOf(target.id, source.id),
                db.books().observeVisible().first().map { it.book.id }.toSet()
            )
            assertEquals(1, db.mangaCatalog().listChapters(target.id).size)
            assertEquals(1, db.mangaCatalog().listChapters(source.id).size)
            assertTrue(sourceFile.isFile)
            assertFalse(copiedArchive.exists())
            assertTrue(db.mangaMerges().findForTarget(target.id) == null)

            val restoredTargetProgress = requireNotNull(db.mangaProgress().find(target.id))
            assertEquals(targetChapter.id, restoredTargetProgress.chapterId)
            assertEquals(0, restoredTargetProgress.pageIndex)
            assertEquals(0.5, restoredTargetProgress.chapterProgression, 0.000001)
            assertEquals(1_000L, restoredTargetProgress.updatedAtEpochMs)
            val restoredTargetBook = requireNotNull(db.books().findEntity(target.id))
            assertEquals(0.5f, restoredTargetBook.progress, 0.000001f)

            val preservedSourceProgress = requireNotNull(db.mangaProgress().find(source.id))
            assertEquals(sourceChapterBefore.id, preservedSourceProgress.chapterId)
            assertEquals(0.66, preservedSourceProgress.chapterProgression, 0.000001)
            assertEquals(2_000L, preservedSourceProgress.updatedAtEpochMs)
        }

    @Test
    fun mergePreflight_rejectsCorruptedSourceArchiveBeforeAnyMutation() = runBlocking {
        val targetArchive = testArchive("Integrity target ch 1.cbz") {
            addPng("001.png")
        }
        val sourceArchive = testArchive("Integrity source ch 2.cbz") {
            addPng("001.png")
        }
        val target = coordinator.import(Uri.fromFile(targetArchive)).getOrThrow().book
        val source = coordinator.import(Uri.fromFile(sourceArchive)).getOrThrow().book

        val sourceFile = File(
            requireNotNull(Uri.parse(requireNotNull(source.sourceUri)).path)
        )
        sourceFile.appendBytes(byteArrayOf(9, 8, 7, 6))

        val result = coordinator.preflightLocalMerge(
            targetBookId = target.id,
            sourceBookIds = listOf(source.id)
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MangaMergeSourceIntegrityException)
        assertEquals(1, db.mangaCatalog().listChapters(target.id).size)
        assertEquals(1, db.mangaCatalog().listChapters(source.id).size)
        assertEquals(2, db.books().count())
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
    fun metadataMigrationMovesCachePreservesProgressAndRejectsIdentityCollision() = runBlocking {
        val firstArchive = testArchive("Meta ch 1.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        val book = coordinator.import(Uri.fromFile(firstArchive))
            .getOrThrow()
            .book
        val secondArchive = testArchive("Meta ch 2.cbz") {
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
        val source = db.mangaCatalog().listChapterSources(second.id).single()
        val sourceArchive = requireNotNull(
            coordinator.resolveLocalArchiveFile(
                book = book,
                chapterKey = source.chapterKey,
                readingOrder = second.readingOrder
            )
        )
        val oldCacheDirectory = File(coordinator.cacheRoot, second.cacheKey)
        assertTrue(sourceArchive.isFile)
        assertTrue(oldCacheDirectory.isDirectory)

        val progressStore = RoomMangaProgressStore(db)
        progressStore.save(
            MangaReadingProgress(
                mangaId = CanonicalMangaId(book.id),
                chapter = MangaChapterAnchor(
                    volume = second.volume,
                    number = second.number,
                    languageTag = second.languageTag,
                    normalizedTitle = second.normalizedTitle,
                    providerChapterKeyHint = source.chapterKey
                ),
                pageIndex = 1,
                pageCount = 3,
                chapterProgression = 0.5,
                updatedAtEpochMs = 2_222L
            )
        )

        coordinator.updateChapterMetadata(
            bookId = book.id,
            chapterId = second.id,
            metadata = MangaLocalChapterMetadata(
                title = "The Twelfth Bell",
                volume = 4.0,
                number = 12.5,
                languageTag = "fa"
            )
        ).getOrThrow()

        val migrated = requireNotNull(db.mangaCatalog().findChapter(second.id))
        assertEquals(second.id, migrated.id)
        assertEquals("The Twelfth Bell", migrated.title)
        assertEquals(4.0, requireNotNull(migrated.volume), 0.000001)
        assertEquals(12.5, requireNotNull(migrated.number), 0.000001)
        assertEquals("fa", migrated.languageTag)
        assertTrue(migrated.cacheKey != second.cacheKey)
        assertFalse(oldCacheDirectory.exists())
        assertTrue(File(coordinator.cacheRoot, migrated.cacheKey).isDirectory)
        assertTrue(sourceArchive.isFile)

        val offline = requireNotNull(db.mangaOffline().findChapter(migrated.id))
        assertEquals(3, offline.pages.size)
        assertTrue(
            offline.pages.all { page ->
                File(coordinator.cacheRoot, page.relativePath).isFile
            }
        )

        val restoredProgress = requireNotNull(
            progressStore.load(CanonicalMangaId(book.id))
        )
        assertEquals(4.0, requireNotNull(restoredProgress.chapter.volume), 0.000001)
        assertEquals(12.5, requireNotNull(restoredProgress.chapter.number), 0.000001)
        assertEquals("fa", restoredProgress.chapter.languageTag)
        assertEquals("The Twelfth Bell", restoredProgress.chapter.normalizedTitle)
        assertEquals(1, restoredProgress.pageIndex)
        assertEquals(0.5, restoredProgress.chapterProgression, 0.000001)
        assertEquals(0.75f, requireNotNull(db.books().findEntity(book.id)).progress, 0.000001f)

        val beforeCollision = requireNotNull(db.mangaCatalog().findChapter(second.id))
        val collision = coordinator.updateChapterMetadata(
            bookId = book.id,
            chapterId = second.id,
            metadata = MangaLocalChapterMetadata(
                title = "Collision",
                volume = null,
                number = 1.0,
                languageTag = null
            )
        )
        assertTrue(collision.isFailure)

        val afterCollision = requireNotNull(db.mangaCatalog().findChapter(second.id))
        assertEquals(beforeCollision.cacheKey, afterCollision.cacheKey)
        assertEquals(beforeCollision.title, afterCollision.title)
        assertEquals(beforeCollision.volume, afterCollision.volume)
        assertEquals(beforeCollision.number, afterCollision.number)
        assertEquals(beforeCollision.languageTag, afterCollision.languageTag)
        assertTrue(File(coordinator.cacheRoot, afterCollision.cacheKey).isDirectory)
        assertTrue(sourceArchive.isFile)
    }

    @Test
    fun chapterManagementPreservesIdentityReordersProgressAndDeletesSafely() = runBlocking {
        val firstArchive = testArchive("Manage ch 1.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        val book = coordinator.import(Uri.fromFile(firstArchive))
            .getOrThrow()
            .book
        val secondArchive = testArchive("Manage ch 2.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        val thirdArchive = testArchive("Manage ch 3.cbz") {
            addPng("001.png")
            addPng("002.png")
        }
        coordinator.appendChapters(
            bookId = book.id,
            uris = listOf(Uri.fromFile(secondArchive), Uri.fromFile(thirdArchive))
        ).getOrThrow()

        val original = db.mangaCatalog().listChapters(book.id)
        val second = original.single { it.readingOrder == 1 }
        val third = original.single { it.readingOrder == 2 }
        val secondCacheKey = second.cacheKey
        val secondSource = db.mangaCatalog().listChapterSources(second.id).single()
        val secondArchiveFile = requireNotNull(
            coordinator.resolveLocalArchiveFile(
                book = book,
                chapterKey = secondSource.chapterKey,
                readingOrder = second.readingOrder
            )
        )
        assertTrue(secondArchiveFile.isFile)

        coordinator.renameChapter(
            bookId = book.id,
            chapterId = second.id,
            title = "The Gray Fog"
        ).getOrThrow()

        val renamed = requireNotNull(db.mangaCatalog().findChapter(second.id))
        assertEquals("The Gray Fog", renamed.title)
        assertEquals(secondCacheKey, renamed.cacheKey)
        assertEquals(
            secondSource.chapterKey,
            db.mangaCatalog().listChapterSources(second.id).single().chapterKey
        )
        assertTrue(File(coordinator.cacheRoot, renamed.cacheKey).isDirectory)

        val thirdSource = db.mangaCatalog().listChapterSources(third.id).single()
        val progressStore = RoomMangaProgressStore(db)
        progressStore.save(
            MangaReadingProgress(
                mangaId = CanonicalMangaId(book.id),
                chapter = MangaChapterAnchor(
                    volume = third.volume,
                    number = third.number,
                    languageTag = third.languageTag,
                    normalizedTitle = third.normalizedTitle,
                    providerChapterKeyHint = thirdSource.chapterKey
                ),
                pageIndex = 0,
                pageCount = 2,
                chapterProgression = 0.5,
                updatedAtEpochMs = 1_111L
            )
        )
        assertEquals(
            (2.5 / 3.0).toFloat(),
            requireNotNull(db.books().findEntity(book.id)).progress,
            0.000001f
        )

        assertEquals(
            1,
            coordinator.moveChapter(
                bookId = book.id,
                chapterId = third.id,
                direction = -1
            ).getOrThrow()
        )
        val reordered = db.mangaCatalog().listChapters(book.id)
        assertEquals(
            listOf(book.title, "Manage ch 3", "The Gray Fog"),
            reordered.map { it.normalizedTitle }
        )
        assertEquals(
            0.5f,
            requireNotNull(db.books().findEntity(book.id)).progress,
            0.000001f
        )
        assertFalse(requireNotNull(db.books().findEntity(book.id)).finished)

        coordinator.deleteChapter(
            bookId = book.id,
            chapterId = second.id
        ).getOrThrow()
        assertFalse(secondArchiveFile.exists())
        assertFalse(File(coordinator.cacheRoot, secondCacheKey).exists())

        val afterNonCurrentDelete = db.mangaCatalog().listChapters(book.id)
        assertEquals(listOf(0, 1), afterNonCurrentDelete.map { it.readingOrder })
        assertEquals(
            0.75f,
            requireNotNull(db.books().findEntity(book.id)).progress,
            0.000001f
        )

        coordinator.deleteChapter(
            bookId = book.id,
            chapterId = third.id
        ).getOrThrow()

        val finalChapters = db.mangaCatalog().listChapters(book.id)
        assertEquals(1, finalChapters.size)
        assertEquals(0, finalChapters.single().readingOrder)

        val fallbackProgress = requireNotNull(
            progressStore.load(CanonicalMangaId(book.id))
        )
        assertEquals(1.0, fallbackProgress.chapterProgression, 0.000001)
        assertEquals(1f, requireNotNull(db.books().findEntity(book.id)).progress, 0.000001f)
        assertTrue(requireNotNull(db.books().findEntity(book.id)).finished)
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
        assertEquals(2, db.mangaOffline().listForBook(book.id).size)

        val afterClear = coordinator.storageSummary(book.id)
        assertEquals(before.sourceBytes, afterClear.sourceBytes)
        assertEquals(0L, afterClear.cacheBytes)
        assertEquals(5, afterClear.offlinePageCount)

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
