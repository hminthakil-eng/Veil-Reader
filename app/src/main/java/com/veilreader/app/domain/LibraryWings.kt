package com.veilreader.app.domain

import java.util.Locale

enum class ArchiveWingKind {
    COLLECTION,
    SERIES
}

data class ArchiveWing(
    val id: String,
    val kind: ArchiveWingKind,
    val name: String,
    val volumeIds: List<String>,
    val volumeCount: Int,
    val activeCount: Int,
    val completedCount: Int,
    val favoriteCount: Int,
    val lastRecordedActivityAtEpochMs: Long,
    val archivePresence: Float,
    val sealSeed: Int
)

data class LibraryWingState(
    val collectionWings: List<ArchiveWing>,
    val seriesWings: List<ArchiveWing>
) {
    val allWings: List<ArchiveWing>
        get() = collectionWings + seriesWings

    companion object {
        val EMPTY = LibraryWingState(emptyList(), emptyList())
    }
}

/**
 * Builds spatial Library wings from metadata the user already owns.
 *
 * A wing never invents a collection, series, progress state, or hierarchy. It simply turns existing
 * metadata into a navigable architectural index.
 */
fun deriveLibraryWings(
    books: List<Book>,
    maxCollectionWings: Int = 8,
    maxSeriesWings: Int = 8
): LibraryWingState {
    if (books.isEmpty()) return LibraryWingState.EMPTY

    val collectionGroups = linkedMapOf<String, MutableList<Book>>()
    val collectionNames = linkedMapOf<String, String>()
    books.forEach { book ->
        book.allCollections.forEach { raw ->
            val display = raw.trim()
            if (display.isBlank()) return@forEach
            val key = display.lowercase(Locale.ROOT)
            collectionNames.putIfAbsent(key, display)
            collectionGroups.getOrPut(key) { mutableListOf() }.add(book)
        }
    }

    val seriesGroups = linkedMapOf<String, MutableList<Book>>()
    val seriesNames = linkedMapOf<String, String>()
    books.forEach { book ->
        val display = book.seriesName?.trim().orEmpty()
        if (display.isBlank()) return@forEach
        val key = display.lowercase(Locale.ROOT)
        seriesNames.putIfAbsent(key, display)
        seriesGroups.getOrPut(key) { mutableListOf() }.add(book)
    }

    fun toWing(
        kind: ArchiveWingKind,
        key: String,
        name: String,
        members: List<Book>
    ): ArchiveWing {
        val uniqueMembers = members.distinctBy { it.id }
        val active = uniqueMembers.count { !it.finished && it.progress > 0f }
        val completed = uniqueMembers.count { it.finished }
        val favorites = uniqueMembers.count { it.favorite }
        val count = uniqueMembers.size.coerceAtLeast(1)

        val activity = uniqueMembers.maxOfOrNull { book ->
            book.lastOpenedAtEpochMs
                .takeIf { it > 0L }
                ?: book.addedAtEpochMs
        } ?: 0L

        val volumeFactor = (count / 12f).coerceIn(0f, 1f)
        val completedRatio = completed.toFloat() / count
        val activeRatio = active.toFloat() / count
        val favoriteRatio = favorites.toFloat() / count

        return ArchiveWing(
            id = "${kind.name.lowercase(Locale.ROOT)}:$key",
            kind = kind,
            name = name,
            volumeIds = uniqueMembers.map { it.id }.sorted(),
            volumeCount = uniqueMembers.size,
            activeCount = active,
            completedCount = completed,
            favoriteCount = favorites,
            lastRecordedActivityAtEpochMs = activity,
            archivePresence = (
                volumeFactor * 0.50f +
                    completedRatio * 0.24f +
                    activeRatio * 0.16f +
                    favoriteRatio * 0.10f
                ).coerceIn(0f, 1f),
            sealSeed = name.hashCode() xor uniqueMembers.size
        )
    }

    fun sortWings(wings: List<ArchiveWing>, limit: Int): List<ArchiveWing> =
        wings.sortedWith(
            compareByDescending<ArchiveWing> { it.volumeCount }
                .thenByDescending { it.lastRecordedActivityAtEpochMs }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        ).take(limit.coerceAtLeast(0))

    return LibraryWingState(
        collectionWings = sortWings(
            collectionGroups.map { (key, members) ->
                toWing(
                    kind = ArchiveWingKind.COLLECTION,
                    key = key,
                    name = collectionNames.getValue(key),
                    members = members
                )
            },
            maxCollectionWings
        ),
        seriesWings = sortWings(
            seriesGroups.map { (key, members) ->
                toWing(
                    kind = ArchiveWingKind.SERIES,
                    key = key,
                    name = seriesNames.getValue(key),
                    members = members
                )
            },
            maxSeriesWings
        )
    )
}
