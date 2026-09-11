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
        ReadingSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VeilDatabase : RoomDatabase() {
    abstract fun books(): BookDao
    abstract fun highlights(): HighlightDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun collections(): CollectionDao
    abstract fun readingSessions(): ReadingSessionDao

    companion object {
        @Volatile private var instance: VeilDatabase? = null

        fun get(context: Context): VeilDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VeilDatabase::class.java,
                "veil_reader.db"
            ).build().also { instance = it }
        }
    }
}
