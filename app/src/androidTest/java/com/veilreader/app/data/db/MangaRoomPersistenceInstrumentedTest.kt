package com.veilreader.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.data.manga.MangaBackupCodec
import com.veilreader.app.data.manga.MangaBackupSnapshot
import com.veilreader.app.data.manga.MangaRoomRepository
import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.library.OfflineChapterId
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.library.OfflinePageEntry
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaRef
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaRoomPersistenceInstrumentedTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VeilDatabase::class.java
    )

    private lateinit var db: VeilDatabase
    private lateinit var repository: MangaRoomRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MangaRoomRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun migration1To2_preservesExistingBook_andCreatesMangaSchema() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "manga-migration-test.db"

        migrationHelper.createDatabase(name, 1).apply {
            execSQL(
                """
                INSERT INTO books(
                    id,title,author,progress,currentChapter,totalPages,pagesRead,format,
                    sourceUri,mediaType,locatorJson,addedAtEpochMs,lastOpenedAtEpochMs,
                    finished,favorite,coverCachePath,contentFingerprint,seriesName,seriesIndex,language
                ) VALUES(
                    'legacy-book','Legacy','Author',0.25,'Chapter 2',100,25,'EPUB',
                    NULL,NULL,NULL,1,2,0,1,NULL,NULL,NULL,NULL,NULL
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = Room.databaseBuilder(context, VeilDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        migrationHelper.closeWhenFinished(migrated)

        assertEquals("Legacy", migrated.books().findEntity("legacy-book")?.title)
        assertEquals(emptyList<MangaWorkWithLinks>(), migrated.mangaLibrary().listAll())

        migrated.mangaLibrary().upsertWork(
            MangaWorkEntity(
                id = "work-after-migration",
                title = "Manga",
                alternativeTitlesJson = "[]",
                createdAtEpochMs = 3L
            )
        )
        assertEquals(1, migrated.mangaLibrary().listAll().size)
    }

    @Test
    fun canonicalProgressAndOfflineManifest_roundTrip() = runBlocking {
        val manga = canonical("work-1", "source.one", "remote-work")
        repository.saveWork(manga)

        val loaded = repository.loadWork(manga.id)
        assertEquals(manga, loaded)

        val progress = MangaReadingProgress(
            mangaId = manga.id,
            chapter = MangaChapterAnchor(
                volume = 2.0,
                number = 12.5,
                languageTag = "en",
                normalizedTitle = "Chapter 12.5",
                providerChapterKeyHint = "ch-12-5"
            ),
            pageIndex = 7,
            pageCount = 20,
            chapterProgression = 7.0 / 19.0,
            updatedAtEpochMs = 100L
        )
        repository.save(progress)
        assertEquals(progress, repository.load(manga.id))

        val chapterId = OfflineChapterId(
            mangaId = manga.id,
            languageTag = "en",
            volume = 2.0,
            number = 12.5,
            discriminator = "main"
        )
        val manifest = OfflineChapterManifest(
            chapterId = chapterId,
            anchor = progress.chapter,
            pages = listOf(
                OfflinePageEntry(
                    index = 0,
                    relativePath = "manga/work-1/en/v2_0/c12_5-main/page-00000.webp",
                    byteSize = 123L,
                    contentSha256 = "a".repeat(64)
                ),
                OfflinePageEntry(
                    index = 1,
                    relativePath = "manga/work-1/en/v2_0/c12_5-main/page-00001.webp",
                    byteSize = 456L
                )
            ),
            originSourceId = SourceId("source.one"),
            originChapterKey = "ch-12-5",
            completed = true,
            updatedAtEpochMs = 200L
        )
        repository.put(manifest)

        assertEquals(manifest, repository.load(chapterId))
        assertEquals(listOf(manifest), repository.listForManga(manga.id))
    }

    @Test
    fun sameSourceWorkCannotBelongToTwoCanonicalManga() = runBlocking {
        val first = canonical("work-a", "source.one", "same-provider-key")
        val second = canonical("work-b", "source.one", "same-provider-key")

        repository.saveWork(first)

        assertThrows(Exception::class.java) {
            runBlocking { repository.saveWork(second) }
        }

        assertEquals(first, repository.loadWork(first.id))
        assertNull(repository.loadWork(second.id))
    }

    @Test
    fun backupRoundTrip_excludesOfflineManifest_andRestoresIdentityProgress() = runBlocking {
        val manga = canonical("work-backup", "source.one", "remote-backup")
        repository.saveWork(manga)
        val progress = MangaReadingProgress(
            mangaId = manga.id,
            chapter = MangaChapterAnchor(number = 3.0, languageTag = "en"),
            pageIndex = 2,
            pageCount = 10,
            chapterProgression = 2.0 / 9.0,
            updatedAtEpochMs = 99L
        )
        repository.save(progress)

        val chapterId = OfflineChapterId(
            mangaId = manga.id,
            languageTag = "en",
            volume = null,
            number = 3.0
        )
        repository.put(
            OfflineChapterManifest(
                chapterId = chapterId,
                anchor = progress.chapter,
                pages = listOf(
                    OfflinePageEntry(
                        index = 0,
                        relativePath = "manga/work-backup/en/vna/c3_0-main/page-00000.jpg",
                        byteSize = 10
                    )
                ),
                originSourceId = SourceId("source.one"),
                originChapterKey = "chapter-3",
                completed = false,
                updatedAtEpochMs = 100L
            )
        )

        val snapshot = repository.backupSnapshot()
        val json = MangaBackupCodec.toJson(snapshot)
        val decoded = MangaBackupCodec.fromJson(JSONObject(json.toString()))

        repository.replaceFromBackup(MangaBackupSnapshot())
        assertEquals(emptyList<CanonicalManga>(), repository.listWorks())

        repository.replaceFromBackup(decoded)

        assertEquals(listOf(manga), repository.listWorks())
        assertEquals(progress, repository.load(manga.id))
        assertEquals(emptyList<OfflineChapterManifest>(), repository.listForManga(manga.id))
    }

    @Test
    fun emptyMangaObject_isBackwardCompatibleWithSchemaTwoBackup() {
        val decoded = MangaBackupCodec.fromJson(JSONObject())
        assertTrue(decoded.works.isEmpty())
        assertTrue(decoded.progress.isEmpty())
    }

    private fun canonical(
        id: String,
        sourceId: String,
        sourceKey: String
    ): CanonicalManga {
        val sid = SourceId(sourceId)
        return CanonicalManga(
            id = CanonicalMangaId(id),
            title = "Work $id",
            alternativeTitles = setOf("Alt $id"),
            sourceRefs = mapOf(
                sid to SourceMangaRef(
                    sourceId = sid,
                    key = sourceKey,
                    publicUrl = "https://example.test/$sourceKey"
                )
            ),
            createdAtEpochMs = 10L
        )
    }
}
