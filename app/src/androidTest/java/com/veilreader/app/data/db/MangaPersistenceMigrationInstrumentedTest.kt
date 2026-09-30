package com.veilreader.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaPersistenceMigrationInstrumentedTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VeilDatabase::class.java
    )

    @Test
    fun migration1To3_preservesLibraryAndAddsCascadeSafeMangaTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            insertVersionOneComic(this)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            VeilDatabase.MIGRATION_1_2,
            VeilDatabase.MIGRATION_2_3
        )
        migrated.execSQL("PRAGMA foreign_keys = ON")

        assertCount(migrated, "books", 1)
        assertCount(migrated, "manga_chapters", 0)
        assertCount(migrated, "manga_progress", 0)
        assertCount(migrated, "manga_offline_pages", 0)

        migrated.execSQL(
            """
            INSERT INTO manga_chapters (
                id, bookId, readingOrder, cacheKey, title, normalizedTitle,
                volume, number, languageTag
            ) VALUES (
                'chapter-1', 'comic', 0, 'manga/comic/en/vna/c1_0-main',
                'Chapter 1', 'chapter 1', NULL, 1.0, 'en'
            )
            """.trimIndent()
        )
        migrated.execSQL(
            """
            INSERT INTO manga_source_links (
                bookId, sourceId, mangaKey, publicUrl
            ) VALUES (
                'comic', 'local.cbz', 'comic', NULL
            )
            """.trimIndent()
        )
        migrated.execSQL(
            """
            INSERT INTO manga_chapter_sources (
                chapterId, bookId, sourceId, mangaKey, chapterKey
            ) VALUES (
                'chapter-1', 'comic', 'local.cbz', 'comic', 'chapter-1'
            )
            """.trimIndent()
        )
        migrated.execSQL(
            """
            INSERT INTO manga_progress (
                bookId, chapterId, pageIndex, pageCount, chapterProgression, updatedAtEpochMs
            ) VALUES (
                'comic', 'chapter-1', 2, 10, 0.2222, 100
            )
            """.trimIndent()
        )
        migrated.execSQL(
            """
            INSERT INTO manga_offline_chapters (
                chapterId, originSourceId, originChapterKey, completed, updatedAtEpochMs
            ) VALUES (
                'chapter-1', 'local.cbz', 'chapter-1', 1, 100
            )
            """.trimIndent()
        )
        migrated.execSQL(
            """
            INSERT INTO manga_offline_pages (
                chapterId, pageIndex, relativePath, byteSize, contentSha256
            ) VALUES (
                'chapter-1', 0, 'manga/comic/en/vna/c1_0-main/page-00000.jpg',
                123, 'abcdef'
            )
            """.trimIndent()
        )

        assertCount(migrated, "manga_chapters", 1)
        assertCount(migrated, "manga_source_links", 1)
        assertCount(migrated, "manga_chapter_sources", 1)
        assertCount(migrated, "manga_progress", 1)
        assertCount(migrated, "manga_offline_chapters", 1)
        assertCount(migrated, "manga_offline_pages", 1)

        migrated.execSQL("DELETE FROM books WHERE id = 'comic'")

        assertCount(migrated, "manga_chapters", 0)
        assertCount(migrated, "manga_source_links", 0)
        assertCount(migrated, "manga_chapter_sources", 0)
        assertCount(migrated, "manga_progress", 0)
        assertCount(migrated, "manga_offline_chapters", 0)
        assertCount(migrated, "manga_offline_pages", 0)
        migrated.close()
    }

    private fun insertVersionOneComic(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO books (
                id, title, author, progress, currentChapter, totalPages, pagesRead, format,
                sourceUri, mediaType, locatorJson, addedAtEpochMs, lastOpenedAtEpochMs,
                finished, favorite, coverCachePath, contentFingerprint, seriesName,
                seriesIndex, language
            ) VALUES (
                'comic', 'Local Comic', 'Veil', 0.25, 'Chapter 1', 0, 0, 'COMIC',
                'file:///comic.cbz', 'application/vnd.comicbook+zip', NULL,
                1, 2, 0, 0, NULL, NULL, NULL, NULL, 'en'
            )
            """.trimIndent()
        )
    }

    private fun assertCount(
        db: SupportSQLiteDatabase,
        table: String,
        expected: Int
    ) {
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            assertEquals(expected, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "manga-persistence-migration-test"
    }
}
