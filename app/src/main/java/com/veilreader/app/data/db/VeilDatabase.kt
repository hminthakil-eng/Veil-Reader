package com.veilreader.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        ReadingSessionEntity::class,
        ReadingCycleEntity::class,
        PassageVisitEntity::class,
        ReadingMilestoneEntity::class,
        MangaChapterEntity::class,
        MangaSourceLinkEntity::class,
        MangaChapterSourceEntity::class,
        MangaProgressEntity::class,
        MangaOfflineChapterEntity::class,
        MangaOfflinePageEntity::class,
        MangaWorkMergeEntity::class,
        MangaMergeMemberEntity::class,
        MangaMergeOriginalChapterEntity::class,
        MangaMergeChapterEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class VeilDatabase : RoomDatabase() {
    abstract fun books(): BookDao
    abstract fun highlights(): HighlightDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun collections(): CollectionDao
    abstract fun readingSessions(): ReadingSessionDao
    abstract fun readingCycles(): ReadingCycleDao
    abstract fun passageVisits(): PassageVisitDao
    abstract fun readingMilestones(): ReadingMilestoneDao
    abstract fun mangaCatalog(): MangaCatalogDao
    abstract fun mangaProgress(): MangaProgressDao
    abstract fun mangaOffline(): MangaOfflineDao
    abstract fun mangaMerges(): MangaMergeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_cycles (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        cycleIndex INTEGER NOT NULL,
                        titleSnapshot TEXT NOT NULL,
                        authorSnapshot TEXT NOT NULL,
                        startedAtEpochMs INTEGER,
                        completedAtEpochMs INTEGER NOT NULL,
                        finalLocatorJson TEXT NOT NULL,
                        sessionCount INTEGER NOT NULL,
                        totalActiveMillis INTEGER NOT NULL,
                        pacedPageTurns INTEGER NOT NULL,
                        highlightCount INTEGER NOT NULL,
                        noteCount INTEGER NOT NULL,
                        bookmarkCount INTEGER NOT NULL,
                        sealCode TEXT NOT NULL,
                        timelineJson TEXT NOT NULL,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_cycles_bookId ON reading_cycles(bookId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_reading_cycles_bookId_cycleIndex " +
                        "ON reading_cycles(bookId, cycleIndex)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reading_cycles_completedAtEpochMs " +
                        "ON reading_cycles(completedAtEpochMs)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS passage_visits (
                        id TEXT NOT NULL PRIMARY KEY,
                        highlightId TEXT NOT NULL,
                        bookId TEXT NOT NULL,
                        locatorJson TEXT NOT NULL,
                        viewedAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(highlightId) REFERENCES highlights(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_passage_visits_highlightId ON passage_visits(highlightId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_passage_visits_bookId ON passage_visits(bookId)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_passage_visits_viewedAtEpochMs " +
                        "ON passage_visits(viewedAtEpochMs)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_milestones (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        reachedAtEpochMs INTEGER NOT NULL,
                        progression REAL NOT NULL,
                        locatorJson TEXT,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_milestones_bookId ON reading_milestones(bookId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_reading_milestones_bookId_kind " +
                        "ON reading_milestones(bookId, kind)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reading_milestones_reachedAtEpochMs " +
                        "ON reading_milestones(reachedAtEpochMs)"
                )
            }
        }


        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_chapters (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        readingOrder INTEGER NOT NULL,
                        cacheKey TEXT NOT NULL,
                        title TEXT,
                        normalizedTitle TEXT,
                        volume REAL,
                        number REAL,
                        languageTag TEXT,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_chapters_bookId " +
                        "ON manga_chapters(bookId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_chapters_bookId_readingOrder " +
                        "ON manga_chapters(bookId, readingOrder)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_chapters_id_bookId " +
                        "ON manga_chapters(id, bookId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_chapters_cacheKey " +
                        "ON manga_chapters(cacheKey)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_source_links (
                        bookId TEXT NOT NULL,
                        sourceId TEXT NOT NULL,
                        mangaKey TEXT NOT NULL,
                        publicUrl TEXT,
                        PRIMARY KEY(bookId, sourceId),
                        FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_source_links_sourceId " +
                        "ON manga_source_links(sourceId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_chapter_sources (
                        chapterId TEXT NOT NULL,
                        bookId TEXT NOT NULL,
                        sourceId TEXT NOT NULL,
                        mangaKey TEXT NOT NULL,
                        chapterKey TEXT NOT NULL,
                        PRIMARY KEY(chapterId, sourceId),
                        FOREIGN KEY(chapterId, bookId)
                            REFERENCES manga_chapters(id, bookId)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(bookId, sourceId)
                            REFERENCES manga_source_links(bookId, sourceId)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_chapter_sources_chapterId_bookId " +
                        "ON manga_chapter_sources(chapterId, bookId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_chapter_sources_bookId_sourceId " +
                        "ON manga_chapter_sources(bookId, sourceId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_manga_chapter_sources_sourceId_mangaKey_chapterKey " +
                        "ON manga_chapter_sources(sourceId, mangaKey, chapterKey)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_progress (
                        bookId TEXT NOT NULL PRIMARY KEY,
                        chapterId TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        pageCount INTEGER,
                        chapterProgression REAL NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(bookId) REFERENCES books(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(chapterId, bookId)
                            REFERENCES manga_chapters(id, bookId)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_progress_chapterId_bookId " +
                        "ON manga_progress(chapterId, bookId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_offline_chapters (
                        chapterId TEXT NOT NULL PRIMARY KEY,
                        originSourceId TEXT NOT NULL,
                        originChapterKey TEXT NOT NULL,
                        completed INTEGER NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(chapterId) REFERENCES manga_chapters(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_offline_pages (
                        chapterId TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        relativePath TEXT NOT NULL,
                        byteSize INTEGER NOT NULL,
                        contentSha256 TEXT,
                        PRIMARY KEY(chapterId, pageIndex),
                        FOREIGN KEY(chapterId) REFERENCES manga_offline_chapters(chapterId)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_offline_pages_relativePath " +
                        "ON manga_offline_pages(relativePath)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_work_merges (
                        id TEXT NOT NULL PRIMARY KEY,
                        targetBookId TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        receiptVersion INTEGER NOT NULL,
                        targetOriginalChapterCount INTEGER NOT NULL,
                        targetBookProgress REAL NOT NULL,
                        targetBookFinished INTEGER NOT NULL,
                        targetBookLastOpenedAtEpochMs INTEGER NOT NULL,
                        targetProgressChapterId TEXT,
                        targetProgressPageIndex INTEGER,
                        targetProgressPageCount INTEGER,
                        targetProgressChapterProgression REAL,
                        targetProgressUpdatedAtEpochMs INTEGER,
                        FOREIGN KEY(targetBookId) REFERENCES books(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(targetProgressChapterId, targetBookId)
                            REFERENCES manga_chapters(id, bookId)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_work_merges_targetBookId " +
                        "ON manga_work_merges(targetBookId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_work_merges_createdAtEpochMs " +
                        "ON manga_work_merges(createdAtEpochMs)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "index_manga_work_merges_targetProgressChapterId_targetBookId " +
                        "ON manga_work_merges(targetProgressChapterId, targetBookId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_merge_members (
                        mergeId TEXT NOT NULL,
                        sourceBookId TEXT NOT NULL,
                        sourceOrder INTEGER NOT NULL,
                        PRIMARY KEY(mergeId, sourceBookId),
                        FOREIGN KEY(mergeId) REFERENCES manga_work_merges(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(sourceBookId) REFERENCES books(id)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_merge_members_mergeId " +
                        "ON manga_merge_members(mergeId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_merge_members_sourceBookId " +
                        "ON manga_merge_members(sourceBookId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_manga_merge_members_mergeId_sourceOrder " +
                        "ON manga_merge_members(mergeId, sourceOrder)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_merge_original_chapters (
                        mergeId TEXT NOT NULL,
                        readingOrder INTEGER NOT NULL,
                        chapterId TEXT NOT NULL,
                        targetBookId TEXT NOT NULL,
                        chapterKey TEXT NOT NULL,
                        PRIMARY KEY(mergeId, readingOrder),
                        FOREIGN KEY(mergeId) REFERENCES manga_work_merges(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(chapterId, targetBookId)
                            REFERENCES manga_chapters(id, bookId)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_merge_original_chapters_mergeId " +
                        "ON manga_merge_original_chapters(mergeId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_manga_merge_original_chapters_chapterId_targetBookId " +
                        "ON manga_merge_original_chapters(chapterId, targetBookId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS manga_merge_chapters (
                        mergeId TEXT NOT NULL,
                        sourceChapterId TEXT NOT NULL,
                        sourceBookId TEXT NOT NULL,
                        targetChapterId TEXT NOT NULL,
                        sourceReadingOrder INTEGER NOT NULL,
                        targetReadingOrder INTEGER NOT NULL,
                        disposition TEXT NOT NULL,
                        PRIMARY KEY(mergeId, sourceChapterId),
                        FOREIGN KEY(mergeId) REFERENCES manga_work_merges(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(sourceChapterId, sourceBookId)
                            REFERENCES manga_chapters(id, bookId)
                            ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(targetChapterId) REFERENCES manga_chapters(id)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_merge_chapters_mergeId " +
                        "ON manga_merge_chapters(mergeId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_manga_merge_chapters_sourceChapterId_sourceBookId " +
                        "ON manga_merge_chapters(sourceChapterId, sourceBookId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_merge_chapters_targetChapterId " +
                        "ON manga_merge_chapters(targetChapterId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_manga_merge_chapters_mergeId_targetReadingOrder " +
                        "ON manga_merge_chapters(mergeId, targetReadingOrder)"
                )
            }
        }


        @Volatile private var instance: VeilDatabase? = null

        fun get(context: Context): VeilDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VeilDatabase::class.java,
                "veil_reader.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}
