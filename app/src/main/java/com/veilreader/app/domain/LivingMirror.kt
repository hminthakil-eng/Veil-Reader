package com.veilreader.app.domain

import kotlin.math.abs
import kotlin.math.ln

/**
 * Factual projection used by Living Mirror.
 *
 * The Mirror never invents semantic relationships. Position, depth and rings are deterministic
 * transforms of existing book/note/revisit/cycle history.
 */
data class LivingMirrorNote(
    val highlightId: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val quote: String,
    val note: String,
    val locatorJson: String,
    val recordedAtEpochMs: Long,
    val lastRevisitedAtEpochMs: Long?,
    val revisitCount: Int,
    val cycleIndex: Int,
    val clusterX: Float,
    val clusterY: Float,
    val depth: Float,
    val proximity: Float,
    val ringCount: Int
)

fun deriveLivingMirrorNotes(
    books: List<Book>,
    highlights: List<Highlight>,
    passageVisits: List<PassageVisit>,
    readingCycles: List<ReadingCycleRecord>,
    nowEpochMs: Long = System.currentTimeMillis()
): List<LivingMirrorNote> {
    val safeNow = nowEpochMs.coerceAtLeast(1L)
    val booksById = books.associateBy { it.id }
    val cyclesByBook = readingCycles
        .groupBy { it.bookId }
        .mapValues { (_, cycles) -> cycles.maxOfOrNull { it.cycleIndex.coerceAtLeast(1) } ?: 1 }

    return highlights.asSequence()
        .filter { it.note.isNotBlank() }
        .mapNotNull { highlight ->
            val book = booksById[highlight.bookId] ?: return@mapNotNull null
            val visits = exactPassageVisits(highlight, passageVisits)
                .filter { it.viewedAtEpochMs in 1L..safeNow }
            val revisitCount = visits.size
            val lastRevisitedAt = visits.lastOrNull()?.viewedAtEpochMs
            val cycleIndex = cyclesByBook[highlight.bookId] ?: 1
            val ageMillis = (safeNow - highlight.createdAtEpochMs)
                .coerceAtLeast(0L)
            val ageDays = ageMillis / 86_400_000.0

            val bookHash = stableMirrorHash(highlight.bookId)
            val noteHash = stableMirrorHash(highlight.id)
            val bookX = 0.18f + ((bookHash and 0xFFFF) / 65535f) * 0.64f
            val bookY = 0.20f + (((bookHash ushr 16) and 0xFFFF) / 65535f) * 0.56f
            val jitterX = ((((noteHash and 0xFF) / 255f) - 0.5f) * 0.18f)
            val jitterY = (((((noteHash ushr 8) and 0xFF) / 255f) - 0.5f) * 0.16f)

            // Old notes settle deeper. Revisited notes move closer to the viewing surface.
            val ageDepth = (ln(1.0 + ageDays) / ln(1.0 + 365.0))
                .coerceIn(0.0, 1.0)
                .toFloat()
            val revisitLift = (revisitCount.coerceAtMost(8) / 8f) * 0.34f
            val depth = (ageDepth - revisitLift).coerceIn(0f, 1f)
            val proximity = (1f - depth).coerceIn(0f, 1f)

            LivingMirrorNote(
                highlightId = highlight.id,
                bookId = highlight.bookId,
                bookTitle = book.title,
                bookAuthor = book.author,
                quote = highlight.quote,
                note = highlight.note,
                locatorJson = highlight.locatorJson,
                recordedAtEpochMs = highlight.createdAtEpochMs,
                lastRevisitedAtEpochMs = lastRevisitedAt,
                revisitCount = revisitCount,
                cycleIndex = cycleIndex,
                clusterX = (bookX + jitterX).coerceIn(0.08f, 0.92f),
                clusterY = (bookY + jitterY).coerceIn(0.10f, 0.88f),
                depth = depth,
                proximity = proximity,
                ringCount = cycleIndex.coerceIn(1, 4)
            )
        }
        .sortedWith(
            compareByDescending<LivingMirrorNote> { it.proximity }
                .thenByDescending { it.lastRevisitedAtEpochMs ?: it.recordedAtEpochMs }
                .thenBy { it.highlightId }
        )
        .toList()
}

private fun stableMirrorHash(value: String): Int {
    var hash = 0x811C9DC5.toInt()
    value.forEach { char ->
        hash = hash xor char.code
        hash *= 0x01000193
    }
    return abs(hash)
}
