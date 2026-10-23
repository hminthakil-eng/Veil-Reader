package com.veilreader.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V3 schema is taken from the real exported assets, NOT reconstructed from a proposed schema.
 * The V4 schema must be generated and included by KSP to validate migration equivalence.
 */
@RunWith(AndroidJUnit4::class)
class SyncMigration3To4InstrumentedTest {
    @get:Rule val migrations = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), VeilDatabase::class.java
    )

    @Test fun preservesV3BookAndAddsEmptyOptInTables() {
        val name = "sync-migration-${System.nanoTime()}"
        migrations.createDatabase(name, 3).apply {
            execSQL(
                "INSERT INTO books (id,title,author,progress,currentChapter,totalPages,pagesRead," +
                    "format,sourceUri,mediaType,locatorJson,addedAtEpochMs,lastOpenedAtEpochMs," +
                    "finished,favorite,coverCachePath,contentFingerprint,seriesName,seriesIndex,language) " +
                    "VALUES ('sample','Sample','Author',0.42,'Chapter 3',120,50,'EPUB',NULL,NULL," +
                    "'{\"href\":\"chapter3\"}',1,2,0,0,NULL,'hash-test',NULL,NULL,'en')"
            )
            close()
        }
        migrations.runMigrationsAndValidate(name, 4, true, VeilDatabase.MIGRATION_3_4).apply {
            query("SELECT progress, locatorJson FROM books WHERE id = 'sample'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0.42, cursor.getDouble(0), 0.00001)
                assertTrue(cursor.getString(1).contains("chapter3"))
            }
            query("SELECT count(*) FROM sync_outbox").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            query("SELECT count(*) FROM sync_preferences").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }

    @Test fun v4OutboxIsIdempotentAndConsentIsOffByDefault() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            assertNull(database.sync().preference())
            val row = SyncOutboxEntity(
                eventId = "event-1", bookId = "book-1", sourceDeviceId = "device-1",
                kind = "PROGRESS", entityId = "book-1", payloadJson = "{}",
                createdAtEpochMs = 1000L
            )
            assertTrue(database.sync().enqueue(row) != -1L)
            assertEquals(-1L, database.sync().enqueue(row))
            assertEquals(1, database.sync().pending().size)
            assertEquals(1, database.sync().acknowledgeEvent("event-1"))
            assertTrue(database.sync().pending().isEmpty())
        } finally { database.close() }
    }
}
