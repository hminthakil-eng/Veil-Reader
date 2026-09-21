package com.veilreader.app.data.db

import androidx.room.migration.Migration

val MIGRATION_1_2 = Migration(1, 2) { db ->
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS manga_works (
            id TEXT NOT NULL,
            title TEXT NOT NULL,
            alternativeTitlesJson TEXT NOT NULL,
            createdAtEpochMs INTEGER NOT NULL,
            PRIMARY KEY(id)
        )
        """.trimIndent()
    )

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS manga_source_links (
            mangaId TEXT NOT NULL,
            sourceId TEXT NOT NULL,
            sourceKey TEXT NOT NULL,
            publicUrl TEXT,
            PRIMARY KEY(mangaId, sourceId),
            FOREIGN KEY(mangaId) REFERENCES manga_works(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_manga_source_links_mangaId " +
            "ON manga_source_links(mangaId)"
    )
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_source_links_sourceId_sourceKey " +
            "ON manga_source_links(sourceId, sourceKey)"
    )

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS manga_progress (
            mangaId TEXT NOT NULL,
            volume REAL,
            chapterNumber REAL,
            languageTag TEXT,
            normalizedTitle TEXT,
            providerChapterKeyHint TEXT,
            pageIndex INTEGER NOT NULL,
            pageCount INTEGER,
            chapterProgression REAL NOT NULL,
            updatedAtEpochMs INTEGER NOT NULL,
            PRIMARY KEY(mangaId),
            FOREIGN KEY(mangaId) REFERENCES manga_works(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_manga_progress_updatedAtEpochMs " +
            "ON manga_progress(updatedAtEpochMs)"
    )

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS manga_offline_chapters (
            manifestId TEXT NOT NULL,
            mangaId TEXT NOT NULL,
            languageTag TEXT,
            volume REAL,
            chapterNumber REAL,
            discriminator TEXT NOT NULL,
            anchorVolume REAL,
            anchorNumber REAL,
            anchorLanguageTag TEXT,
            anchorNormalizedTitle TEXT,
            anchorProviderChapterKeyHint TEXT,
            originSourceId TEXT NOT NULL,
            originChapterKey TEXT NOT NULL,
            completed INTEGER NOT NULL,
            updatedAtEpochMs INTEGER NOT NULL,
            PRIMARY KEY(manifestId),
            FOREIGN KEY(mangaId) REFERENCES manga_works(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_manga_offline_chapters_mangaId " +
            "ON manga_offline_chapters(mangaId)"
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_manga_offline_chapters_updatedAtEpochMs " +
            "ON manga_offline_chapters(updatedAtEpochMs)"
    )

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS manga_offline_pages (
            manifestId TEXT NOT NULL,
            pageIndex INTEGER NOT NULL,
            relativePath TEXT NOT NULL,
            byteSize INTEGER NOT NULL,
            contentSha256 TEXT,
            PRIMARY KEY(manifestId, pageIndex),
            FOREIGN KEY(manifestId) REFERENCES manga_offline_chapters(manifestId)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_manga_offline_pages_manifestId " +
            "ON manga_offline_pages(manifestId)"
    )
}
