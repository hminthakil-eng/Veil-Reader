package com.veilreader.app.domain

import kotlin.math.min

/**
 * Durable physical history for Veil's representation of a volume.
 *
 * These values never alter the publication cover bitmap itself. They only control archive-owned
 * wear, ribbons, edge memory and handling traces around it.
 */
data class BookArtifactMemory(
    val sessionCount: Int,
    val activeMillis: Long,
    val pacedPageTurns: Int,
    val highlightCount: Int,
    val annotationCount: Int,
    val bookmarkCount: Int,
    val handlingWear: Float,
    val foreEdgeWear: Float,
    val marginMemory: Float,
    val ribbonCount: Int,
    val marginFleckCount: Int
) {
    companion object {
        val EMPTY = BookArtifactMemory(
            sessionCount = 0,
            activeMillis = 0L,
            pacedPageTurns = 0,
            highlightCount = 0,
            annotationCount = 0,
            bookmarkCount = 0,
            handlingWear = 0f,
            foreEdgeWear = 0f,
            marginMemory = 0f,
            ribbonCount = 0,
            marginFleckCount = 0
        )
    }
}

fun deriveBookArtifactMemory(
    book: Book,
    sessions: List<ReadingSessionSnapshot>,
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>
): BookArtifactMemory {
    val bookSessions = sessions.filter { it.bookId == book.id }
    val bookHighlights = highlights.filter { it.bookId == book.id }
    val bookBookmarks = bookmarks.filter { it.bookId == book.id }

    val sessionCount = bookSessions.size
    val activeMillis = bookSessions.sumOf { it.activeMillis.coerceAtLeast(0L) }
    val pacedTurns = bookSessions.sumOf { it.pacedPageTurns.coerceAtLeast(0) }
    val annotations = bookHighlights.count { it.note.isNotBlank() }

    val activeHours = activeMillis / 3_600_000f
    val sessionFactor = saturation(sessionCount.toFloat(), 48f)
    val activeFactor = saturation(activeHours, 80f)
    val turnFactor = saturation(pacedTurns.toFloat(), 600f)
    val highlightFactor = saturation(bookHighlights.size.toFloat(), 32f)
    val annotationFactor = saturation(annotations.toFloat(), 14f)

    val handlingWear = (
        sessionFactor * 0.34f +
            activeFactor * 0.36f +
            turnFactor * 0.30f
        ).coerceIn(0f, 1f)

    val foreEdgeWear = (
        turnFactor * 0.52f +
            sessionFactor * 0.30f +
            activeFactor * 0.18f
        ).coerceIn(0f, 1f)

    val marginMemory = (
        highlightFactor * 0.62f +
            annotationFactor * 0.38f
        ).coerceIn(0f, 1f)

    return BookArtifactMemory(
        sessionCount = sessionCount,
        activeMillis = activeMillis,
        pacedPageTurns = pacedTurns,
        highlightCount = bookHighlights.size,
        annotationCount = annotations,
        bookmarkCount = bookBookmarks.size,
        handlingWear = handlingWear,
        foreEdgeWear = foreEdgeWear,
        marginMemory = marginMemory,
        ribbonCount = min(3, bookBookmarks.size),
        marginFleckCount = min(12, bookHighlights.size + annotations)
    )
}

private fun saturation(value: Float, target: Float): Float =
    if (target <= 0f) 0f else (value / target).coerceIn(0f, 1f)
