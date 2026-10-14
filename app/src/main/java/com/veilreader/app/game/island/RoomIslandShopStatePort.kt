package com.veilreader.app.game.island

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

/** Separate database on purpose: never migrate or modify the existing Reader library DB. */
@Entity(tableName = "island_shop_snapshot")
internal data class IslandShopSnapshotRow(
    @PrimaryKey val singletonId: Int = 1,
    val snapshotJson: String
)

@Dao
internal interface IslandShopSnapshotDao {
    @Query("SELECT * FROM island_shop_snapshot WHERE singletonId = 1")
    suspend fun read(): IslandShopSnapshotRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: IslandShopSnapshotRow)
}

/**
 * Isolated world database: no direct reference to user books, reader locators or profile.
 * Never use destructive migration for real player data.
 */
@Database(entities = [IslandShopSnapshotRow::class], version = 1, exportSchema = true)
internal abstract class IslandShopDatabase : RoomDatabase() {
    abstract fun snapshots(): IslandShopSnapshotDao

    companion object {
        fun open(context: Context): IslandShopDatabase =
            Room.databaseBuilder(context.applicationContext, IslandShopDatabase::class.java,
                "veil_island_shop_v1.db")
                .build()
    }
}

/**
 * Serializes check -> pure reducer -> durable state + dedup IDs in ONE SQLite transaction.
 * Multiple dispatchers/process-kill/replay cannot apply the same inventory transfer twice.
 *
 * Entire immutable v1 snapshot is one row. This small 6-book slice deliberately favors
 * integrity over scaling; itemized schema migration is a separate benchmarked later gate.
 */
internal class RoomIslandShopStatePort(
    private val database: IslandShopDatabase
) : IslandShopStatePort {
    private val dao get() = database.snapshots()

    override suspend fun load(): IslandShopState = database.withTransaction {
        dao.read()?.let { IslandShopSnapshotCodec.decode(it.snapshotJson) } ?: IslandShopState()
    }

    override suspend fun dispatch(command: ShopCommand): ShopDecision = database.withTransaction {
        val previous = dao.read()?.let { IslandShopSnapshotCodec.decode(it.snapshotJson) }
            ?: IslandShopState()
        val result = reduceShop(previous, command)
        if (result.outcome == ShopOutcome.APPLIED) {
            dao.save(IslandShopSnapshotRow(snapshotJson = IslandShopSnapshotCodec.encode(result.state)))
        }
        result
    }

    /** Raw backup bytes, with explicit schema and validation. Never silently overwrite a save. */
    suspend fun exportSnapshot(): String = database.withTransaction {
        dao.read()?.snapshotJson?.also { IslandShopSnapshotCodec.decode(it) }
            ?: IslandShopSnapshotCodec.encode(IslandShopState())
    }

    /**
     * Fail-closed restore; an existing playthrough cannot be reset accidentally.
     * Actual LibraryExport archive integration remains an independently tested P0 gate.
     */
    suspend fun restoreIfEmpty(exportJson: String): Boolean {
        val restored = IslandShopSnapshotCodec.decode(exportJson)
        return database.withTransaction {
            if (dao.read() != null) false else {
                dao.save(IslandShopSnapshotRow(snapshotJson = IslandShopSnapshotCodec.encode(restored)))
                true
            }
        }
    }
}
