package com.veilreader.app.data.migration

import android.content.Context
import androidx.room.withTransaction
import com.veilreader.app.data.db.BookCollectionCrossRef
import com.veilreader.app.data.db.BookEntity
import com.veilreader.app.data.db.BookmarkEntity
import com.veilreader.app.data.db.CollectionEntity
import com.veilreader.app.data.db.HighlightEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.db.normalizeCollectionName
import com.veilreader.app.data.db.toEntity
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.flow.first

data class LegacyImportPlan(
    val books: List<BookEntity>,
    val highlights: List<HighlightEntity>,
    val bookmarks: List<BookmarkEntity>,
    val collections: List<CollectionEntity>,
    val collectionLinks: List<BookCollectionCrossRef>,
    val skippedOrphanHighlights: Int,
    val skippedOrphanBookmarks: Int
)

data class LegacyMigrationResult(
    val alreadyMigrated: Boolean,
    val importedBooks: Int = 0,
    val importedHighlights: Int = 0,
    val importedBookmarks: Int = 0,
    val importedCollections: Int = 0,
    val skippedOrphans: Int = 0
)

/** Imports the pre-Room 0.x bookshelf without deleting its rollback copy. */
class LegacyLibraryMigrator(
    private val context: Context,
    private val database: VeilDatabase = VeilDatabase.get(context),
    private val settingsStore: SettingsStore = SettingsStore(context)
) {
    suspend fun migrateIfNeeded(): LegacyMigrationResult {
        if (settingsStore.settings.first().legacyLibraryImported) {
            return LegacyMigrationResult(alreadyMigrated = true)
        }

        val legacy = LegacyLibrarySnapshotReader(context)
        val plan = buildLegacyImportPlan(
            books = legacy.books,
            highlights = legacy.highlights,
            bookmarks = legacy.bookmarks
        )

        database.withTransaction {
            if (plan.books.isNotEmpty()) database.books().upsertAll(plan.books)
            if (plan.collections.isNotEmpty()) database.collections().upsertAll(plan.collections)
            if (plan.collectionLinks.isNotEmpty()) database.collections().attachAll(plan.collectionLinks)
            if (plan.highlights.isNotEmpty()) database.highlights().upsertAll(plan.highlights)
            if (plan.bookmarks.isNotEmpty()) database.bookmarks().upsertAll(plan.bookmarks)
        }

        settingsStore.saveReaderAppearance(legacy.appearance())
        settingsStore.markLegacyLibraryImported()

        return LegacyMigrationResult(
            alreadyMigrated = false,
            importedBooks = plan.books.size,
            importedHighlights = plan.highlights.size,
            importedBookmarks = plan.bookmarks.size,
            importedCollections = plan.collections.size,
            skippedOrphans = plan.skippedOrphanHighlights + plan.skippedOrphanBookmarks
        )
    }
}

internal fun buildLegacyImportPlan(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>
): LegacyImportPlan {
    val bookIds = books.mapTo(linkedSetOf()) { it.id }
    val validHighlights = highlights.filter { it.bookId in bookIds }
    val validBookmarks = bookmarks.filter { it.bookId in bookIds }

    val collectionByKey = linkedMapOf<String, CollectionEntity>()
    val links = mutableListOf<BookCollectionCrossRef>()

    books.forEach { book ->
        val cleanName = book.collection.trim()
        if (cleanName.isEmpty()) return@forEach
        val key = normalizeCollectionName(cleanName)
        val collection = collectionByKey.getOrPut(key) {
            CollectionEntity(
                id = stableCollectionId(key),
                name = cleanName,
                createdAtEpochMs = book.addedAtEpochMs,
                normalizedName = key
            )
        }
        links += BookCollectionCrossRef(book.id, collection.id)
    }

    return LegacyImportPlan(
        books = books.map { it.toEntity() },
        highlights = validHighlights.map { it.toEntity() },
        bookmarks = validBookmarks.map { it.toEntity() },
        collections = collectionByKey.values.toList(),
        collectionLinks = links.distinct(),
        skippedOrphanHighlights = highlights.size - validHighlights.size,
        skippedOrphanBookmarks = bookmarks.size - validBookmarks.size
    )
}

private fun stableCollectionId(normalizedName: String): String = UUID.nameUUIDFromBytes(
    "veil-collection:$normalizedName".toByteArray(StandardCharsets.UTF_8)
).toString()
