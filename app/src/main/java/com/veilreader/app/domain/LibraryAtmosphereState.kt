package com.veilreader.app.domain

import kotlin.math.roundToInt

data class LibraryAtmosphereState(
    val volumeCount: Int,
    val completedCount: Int,
    val activeCount: Int,
    val deepShelfCount: Int,
    val collectionCount: Int,
    val sessionCount: Int,
    val totalActiveMillis: Long,
    val passageCount: Int,
    val annotationCount: Int,
    val bookmarkCount: Int,
    val archiveDensity: Float,
    val memoryWarmth: Float,
    val deepQuiet: Float,
    val shelfBays: Int,
    val archLayers: Int,
    val distantStackLayers: Int,
    val lampCount: Int,
    val completedAlcoves: Int,
    val deepCorridors: Int,
    val dustMotes: Int,
    val fogAlpha: Float,
    val brassGlow: Float
) {
    companion object {
        val EMPTY = LibraryAtmosphereState(
            volumeCount = 0,
            completedCount = 0,
            activeCount = 0,
            deepShelfCount = 0,
            collectionCount = 0,
            sessionCount = 0,
            totalActiveMillis = 0L,
            passageCount = 0,
            annotationCount = 0,
            bookmarkCount = 0,
            archiveDensity = 0f,
            memoryWarmth = 0f,
            deepQuiet = 0f,
            shelfBays = 2,
            archLayers = 1,
            distantStackLayers = 1,
            lampCount = 1,
            completedAlcoves = 0,
            deepCorridors = 0,
            dustMotes = 6,
            fogAlpha = 0.48f,
            brassGlow = 0.055f
        )
    }
}

/**
 * Converts real archive state into environmental density only.
 *
 * This is not progression and unlocks nothing. It never invents books, rooms, history, or
 * achievements; it only decides how much architecture, light, dust and fog the Library can afford
 * to reveal from data Veil already owns.
 */
fun deriveLibraryAtmosphereState(
    books: List<Book>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    sessions: List<ReadingSessionSnapshot>,
    memoryState: LibraryMemoryState
): LibraryAtmosphereState {
    if (books.isEmpty()) return LibraryAtmosphereState.EMPTY

    val completed = books.count { it.finished }
    val active = books.count { !it.finished && it.progress > 0f }
    val collections = books
        .flatMap { it.allCollections }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(String::lowercase)
        .size
    val annotations = highlights.count { it.note.isNotBlank() }
    val totalActive = sessions.sumOf { it.activeMillis.coerceAtLeast(0L) }
    val activeHours = totalActive / 3_600_000f
    val deepShelf = memoryState.deepShelfBookIds.count { id -> books.any { it.id == id } }

    val volumeFactor = saturation(books.size.toFloat(), 72f)
    val collectionFactor = saturation(collections.toFloat(), 18f)
    val sessionFactor = saturation(sessions.size.toFloat(), 120f)
    val activeHourFactor = saturation(activeHours, 180f)
    val activeBookFactor = saturation(active.toFloat(), 18f)
    val completedFactor = saturation(completed.toFloat(), 30f)
    val archiveMarkFactor = saturation(
        (highlights.size + bookmarks.size).toFloat(),
        120f
    )
    val deepRatio = if (books.isEmpty()) 0f else deepShelf.toFloat() / books.size.toFloat()

    val archiveDensity = (
        volumeFactor * 0.54f +
            collectionFactor * 0.18f +
            sessionFactor * 0.18f +
            archiveMarkFactor * 0.10f
        ).coerceIn(0f, 1f)

    val memoryWarmth = (
        activeHourFactor * 0.44f +
            activeBookFactor * 0.22f +
            completedFactor * 0.20f +
            saturation(annotations.toFloat(), 30f) * 0.14f
        ).coerceIn(0f, 1f)

    val deepQuiet = (
        deepRatio * 0.72f +
            saturation(deepShelf.toFloat(), 24f) * 0.28f
        ).coerceIn(0f, 1f)

    return LibraryAtmosphereState(
        volumeCount = books.size,
        completedCount = completed,
        activeCount = active,
        deepShelfCount = deepShelf,
        collectionCount = collections,
        sessionCount = sessions.size,
        totalActiveMillis = totalActive,
        passageCount = highlights.size,
        annotationCount = annotations,
        bookmarkCount = bookmarks.size,
        archiveDensity = archiveDensity,
        memoryWarmth = memoryWarmth,
        deepQuiet = deepQuiet,
        shelfBays = (2f + archiveDensity * 10f).roundToInt().coerceIn(2, 12),
        archLayers = (1f + archiveDensity * 4f).roundToInt().coerceIn(1, 5),
        distantStackLayers = (1f + archiveDensity * 5f).roundToInt().coerceIn(1, 6),
        lampCount = (1f + memoryWarmth * 6f).roundToInt().coerceIn(1, 7),
        completedAlcoves = (completedFactor * 6f).roundToInt().coerceIn(0, 6),
        deepCorridors = (deepQuiet * 4f).roundToInt().coerceIn(0, 4),
        dustMotes = (
            6f +
                archiveDensity * 12f +
                deepQuiet * 8f
            ).roundToInt().coerceIn(6, 26),
        fogAlpha = (
            0.46f -
                memoryWarmth * 0.17f +
                deepQuiet * 0.13f
            ).coerceIn(0.22f, 0.54f),
        brassGlow = (
            0.055f +
                memoryWarmth * 0.095f +
                completedFactor * 0.035f
            ).coerceIn(0.055f, 0.185f)
    )
}

private fun saturation(value: Float, target: Float): Float =
    if (target <= 0f) 0f else (value / target).coerceIn(0f, 1f)
