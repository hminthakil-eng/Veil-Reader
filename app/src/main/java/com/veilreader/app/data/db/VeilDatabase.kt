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
        ReadingMilestoneEntity::class
    ],
    version = 2,
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

        @Volatile private var instance: VeilDatabase? = null

        fun get(context: Context): VeilDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VeilDatabase::class.java,
                "veil_reader.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
