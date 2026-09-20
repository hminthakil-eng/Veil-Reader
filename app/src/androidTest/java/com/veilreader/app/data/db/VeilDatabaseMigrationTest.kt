package com.veilreader.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VeilDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VeilDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesLibraryAndAddsMangaTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO books(
                    id, title, author, progress, currentChapter, totalPages, pagesRead,
                    format, sourceUri, mediaType, locatorJson, addedAtEpochMs,
                    lastOpenedAtEpochMs, finished, favorite, coverCachePath,
                    contentFingerprint, seriesName, seriesIndex, language
                ) VALUES(
                    'book-1', 'Existing Book', 'Author', 0.25, 'Chapter 1', 100, 25,
                    'EPUB', NULL, NULL, '{}', 1, 2, 0, 0, NULL, NULL, NULL, NULL, 'en'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            MIGRATION_1_2
        )

        migrated.query("SELECT title, pagesRead FROM books WHERE id = 'book-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Existing Book", cursor.getString(0))
            assertEquals(25, cursor.getInt(1))
        }

        val expectedTables = setOf(
            "manga_source_bindings",
            "manga_chapters",
            "manga_chapter_bindings",
            "manga_downloads"
        )
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ).use { cursor ->
            val actual = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            assertTrue(actual.containsAll(expectedTables))
        }

        migrated.close()
    }

    private companion object {
        const val TEST_DB = "veil-migration-test"
    }
}
