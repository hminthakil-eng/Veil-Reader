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
class HistoricalMemoryMigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VeilDatabase::class.java
    )

    @Test
    fun migration1To2_preservesLibraryAndAddsEmptyHistoricalTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            insertVersionOneBook(this)
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            VeilDatabase.MIGRATION_1_2
        )

        migrated.query("SELECT COUNT(*) FROM books").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM reading_cycles").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM passage_visits").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM reading_milestones").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    private fun insertVersionOneBook(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO books (
                id, title, author, progress, currentChapter, totalPages, pagesRead, format,
                sourceUri, mediaType, locatorJson, addedAtEpochMs, lastOpenedAtEpochMs,
                finished, favorite, coverCachePath, contentFingerprint, seriesName,
                seriesIndex, language
            ) VALUES (
                'legacy', 'Legacy', 'Reader', 0.5, 'Chapter 2', 100, 50, 'EPUB',
                NULL, NULL, NULL, 1, 2, 0, 0, NULL, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )
    }

    private companion object {
        const val TEST_DB = "historical-memory-migration-test"
    }
}
