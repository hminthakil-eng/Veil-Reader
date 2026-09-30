package com.veilreader.app.domain

enum class ArrodesFragmentKind {
    NOTE,
    HIGHLIGHT
}

data class ArrodesFragment(
    val id: String,
    val text: String,
    val book: Book,
    val locatorJson: String,
    val kind: ArrodesFragmentKind,
    val resonanceScore: Int
)

/**
 * Converts durable Veil annotations into source-returnable mirror fragments.
 *
 * Notes outrank plain highlights, while archive resonance remains a secondary signal.
 * Every fragment keeps the exact saved locator so Arrodes can always lead back to the source.
 */
fun deriveArrodesFragments(
    highlights: List<Highlight>,
    books: List<Book>,
    passageVisits: List<PassageVisit> = emptyList(),
    limit: Int = 24
): List<ArrodesFragment> {
    if (limit <= 0) return emptyList()
    val booksById = books.associateBy(Book::id)

    return highlights.asSequence()
        .mapNotNull { highlight ->
            val book = booksById[highlight.bookId] ?: return@mapNotNull null
            val text = highlight.note.trim().takeIf(String::isNotEmpty)
                ?: highlight.quote.trim().takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val locator = highlight.locatorJson.trim().takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val memory = deriveHighlightMemory(
                highlight = highlight,
                book = book,
                passageVisits = passageVisits
            )
            val annotated = highlight.note.isNotBlank()
            ArrodesFragment(
                id = highlight.id,
                text = text,
                book = book,
                locatorJson = locator,
                kind = if (annotated) ArrodesFragmentKind.NOTE else ArrodesFragmentKind.HIGHLIGHT,
                resonanceScore = memory.resonanceScore +
                    if (annotated) 200 else 0 +
                    if (book.favorite) 25 else 0
            )
        }
        .sortedWith(
            compareByDescending<ArrodesFragment> { it.resonanceScore }
                .thenBy { it.id }
        )
        .take(limit)
        .toList()
}
