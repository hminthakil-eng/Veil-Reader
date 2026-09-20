package com.veilreader.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS manga_source_bindings (
                bookId TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                providerId TEXT NOT NULL,
                sourceKey TEXT NOT NULL,
                language TEXT NOT NULL,
                sourceVersion INTEGER NOT NULL,
                isPreferred INTEGER NOT NULL,
                lastSyncedAtEpochMs INTEGER NOT NULL,
                PRIMARY KEY(bookId, sourceId),
                FOREIGN KEY(bookId) REFERENCES books(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_manga_source_bindings_sourceId_sourceKey
            ON manga_source_bindings(sourceId, sourceKey)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_manga_source_bindings_providerId
            ON manga_source_bindings(providerId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS manga_chapters (
                id TEXT NOT NULL,
                bookId TEXT NOT NULL,
                title TEXT NOT NULL,
                chapterNumber REAL,
                volumeNumber REAL,
                publishedAtEpochMs INTEGER,
                displayOrder INTEGER NOT NULL,
                pageCount INTEGER NOT NULL,
                lastPageIndex INTEGER NOT NULL,
                read INTEGER NOT NULL,
                lastReadAtEpochMs INTEGER,
                PRIMARY KEY(id),
                FOREIGN KEY(bookId) REFERENCES books(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_manga_chapters_bookId_displayOrder
            ON manga_chapters(bookId, displayOrder)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS manga_chapter_bindings (
                chapterId TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                sourceChapterKey TEXT NOT NULL,
                lastSeenAtEpochMs INTEGER NOT NULL,
                PRIMARY KEY(chapterId, sourceId),
                FOREIGN KEY(chapterId) REFERENCES manga_chapters(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_manga_chapter_bindings_sourceId_sourceChapterKey
            ON manga_chapter_bindings(sourceId, sourceChapterKey)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS manga_downloads (
                chapterId TEXT NOT NULL,
                state TEXT NOT NULL,
                downloadedPages INTEGER NOT NULL,
                totalPages INTEGER NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                totalBytes INTEGER,
                rootPath TEXT,
                failureCode TEXT,
                updatedAtEpochMs INTEGER NOT NULL,
                PRIMARY KEY(chapterId),
                FOREIGN KEY(chapterId) REFERENCES manga_chapters(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_manga_downloads_state_updatedAtEpochMs
            ON manga_downloads(state, updatedAtEpochMs)
            """.trimIndent()
        )
    }
}

val ALL_DATABASE_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
