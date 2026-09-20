package com.veilreader.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        ReadingSessionEntity::class,
        MangaSourceBindingEntity::class,
        MangaChapterEntity::class,
        MangaChapterBindingEntity::class,
        MangaDownloadEntity::class
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
    abstract fun mangaSourceBindings(): MangaSourceBindingDao
    abstract fun mangaChapters(): MangaChapterDao
    abstract fun mangaChapterBindings(): MangaChapterBindingDao
    abstract fun mangaDownloads(): MangaDownloadDao

    companion object {
        @Volatile private var instance: VeilDatabase? = null

        fun get(context: Context): VeilDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VeilDatabase::class.java,
                "veil_reader.db"
            )
                .addMigrations(*ALL_DATABASE_MIGRATIONS)
                .build()
                .also { instance = it }
        }
    }
}
