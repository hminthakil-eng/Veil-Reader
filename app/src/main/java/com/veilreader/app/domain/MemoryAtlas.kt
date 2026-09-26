package com.veilreader.app.domain

import java.util.Locale
import kotlin.math.min

enum class MemoryRelationKind {
    AUTHOR,
    SERIES,
    COLLECTION,
    PASSAGE_PATTERN
}

data class MemoryAtlasEdge(
    val fromBookId: String,
    val toBookId: String,
    val strength: Int,
    val reasons: Set<MemoryRelationKind>,
    val sharedPassageTerms: List<String> = emptyList()
)

data class MemoryAtlasNode(
    val book: Book,
    val engagementScore: Float,
    val connectionCount: Int
)

data class MemoryAtlas(
    val nodes: List<MemoryAtlasNode>,
    val edges: List<MemoryAtlasEdge>,
    val isolatedCount: Int
) {
    fun connectionsFor(bookId: String): List<MemoryAtlasEdge> =
        edges.filter { it.fromBookId == bookId || it.toBookId == bookId }
            .sortedWith(
                compareByDescending<MemoryAtlasEdge> { it.strength }
                    .thenBy { minOf(it.fromBookId, it.toBookId) }
                    .thenBy { maxOf(it.fromBookId, it.toBookId) }
            )
}

private data class AtlasDraft(
    val book: Book,
    val engagementScore: Float,
    val passageTerms: Set<String>
)

/**
 * Offline Memory Atlas.
 *
 * Relations are deliberately factual and reproducible:
 * same author, same series, shared user collection, or repeated vocabulary inside passages the
 * user explicitly preserved. It does not claim semantic similarity and performs no network/AI call.
 */
fun buildMemoryAtlas(
    books: List<Book>,
    highlights: List<Highlight>,
    sessions: List<ReadingSessionSnapshot>,
    maxNodes: Int = 24
): MemoryAtlas {
    if (maxNodes <= 0 || books.isEmpty()) {
        return MemoryAtlas(emptyList(), emptyList(), isolatedCount = 0)
    }

    val highlightsByBook = highlights.groupBy { it.bookId }
    val sessionsByBook = sessions
        .mapNotNull { session -> session.bookId?.let { it to session } }
        .groupBy({ it.first }, { it.second })

    val drafts = books.map { book ->
        val bookHighlights = highlightsByBook[book.id].orEmpty()
        val bookSessions = sessionsByBook[book.id].orEmpty()
        val terms = bookHighlights
            .asSequence()
            .flatMap { highlight ->
                tokenizePassage(highlight.quote + " " + highlight.note).asSequence()
            }
            .toSet()

        val engagement =
            book.progress.coerceIn(0f, 1f) * 2.0f +
                (if (book.finished) 1.5f else 0f) +
                (if (book.favorite) 0.45f else 0f) +
                min(2.5f, bookHighlights.size * 0.22f) +
                min(2.0f, bookSessions.size * 0.18f)

        AtlasDraft(
            book = book,
            engagementScore = engagement,
            passageTerms = terms
        )
    }
        .sortedWith(
            compareByDescending<AtlasDraft> { it.engagementScore }
                .thenByDescending { it.book.lastOpenedAtEpochMs }
                .thenBy { it.book.title.lowercase(Locale.ROOT) }
        )
        .take(maxNodes)

    val edges = buildList {
        for (leftIndex in 0 until drafts.lastIndex) {
            val left = drafts[leftIndex]
            for (rightIndex in leftIndex + 1 until drafts.size) {
                val right = drafts[rightIndex]
                val reasons = linkedSetOf<MemoryRelationKind>()
                var strength = 0

                val leftAuthor = normalizedRelationValue(left.book.author)
                val rightAuthor = normalizedRelationValue(right.book.author)
                if (
                    leftAuthor.isNotBlank() &&
                    leftAuthor != "unknown author" &&
                    leftAuthor == rightAuthor
                ) {
                    reasons += MemoryRelationKind.AUTHOR
                    strength += 3
                }

                val leftSeries = normalizedRelationValue(left.book.seriesName.orEmpty())
                val rightSeries = normalizedRelationValue(right.book.seriesName.orEmpty())
                if (leftSeries.isNotBlank() && leftSeries == rightSeries) {
                    reasons += MemoryRelationKind.SERIES
                    strength += 4
                }

                val sharedCollections = left.book.allCollections
                    .map(::normalizedRelationValue)
                    .filter(String::isNotBlank)
                    .toSet()
                    .intersect(
                        right.book.allCollections
                            .map(::normalizedRelationValue)
                            .filter(String::isNotBlank)
                            .toSet()
                    )
                if (sharedCollections.isNotEmpty()) {
                    reasons += MemoryRelationKind.COLLECTION
                    strength += (sharedCollections.size * 2).coerceAtMost(4)
                }

                val sharedTerms = left.passageTerms
                    .intersect(right.passageTerms)
                    .sorted()
                if (sharedTerms.size >= 3) {
                    reasons += MemoryRelationKind.PASSAGE_PATTERN
                    strength += (sharedTerms.size / 3).coerceIn(1, 3)
                }

                if (strength > 0) {
                    add(
                        MemoryAtlasEdge(
                            fromBookId = left.book.id,
                            toBookId = right.book.id,
                            strength = strength.coerceAtMost(10),
                            reasons = reasons,
                            sharedPassageTerms = sharedTerms.take(5)
                        )
                    )
                }
            }
        }
    }
        .sortedWith(
            compareByDescending<MemoryAtlasEdge> { it.strength }
                .thenBy { it.fromBookId }
                .thenBy { it.toBookId }
        )

    val connectionCounts = mutableMapOf<String, Int>()
    edges.forEach { edge ->
        connectionCounts[edge.fromBookId] = connectionCounts.getOrDefault(edge.fromBookId, 0) + 1
        connectionCounts[edge.toBookId] = connectionCounts.getOrDefault(edge.toBookId, 0) + 1
    }

    val nodes = drafts.map { draft ->
        MemoryAtlasNode(
            book = draft.book,
            engagementScore = draft.engagementScore,
            connectionCount = connectionCounts.getOrDefault(draft.book.id, 0)
        )
    }

    return MemoryAtlas(
        nodes = nodes,
        edges = edges,
        isolatedCount = nodes.count { it.connectionCount == 0 }
    )
}

private fun normalizedRelationValue(value: String): String =
    value.trim().lowercase(Locale.ROOT)

private fun tokenizePassage(text: String): Set<String> =
    Regex("[\\p{L}\\p{N}]+")
        .findAll(text.lowercase(Locale.ROOT))
        .map { it.value }
        .filter { token ->
            token.length >= 5 &&
                token !in COMMON_PASSAGE_TOKENS
        }
        .toSet()

private val COMMON_PASSAGE_TOKENS = setOf(
    "about", "after", "again", "before", "being", "could", "every", "first", "from",
    "have", "into", "other", "should", "their", "there", "these", "thing", "those",
    "through", "under", "until", "where", "which", "while", "would"
)
