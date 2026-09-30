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
            val annotationBonus = if (annotated) 200 else 0
            val favoriteBonus = if (book.favorite) 25 else 0
            ArrodesFragment(
                id = highlight.id,
                text = text,
                book = book,
                locatorJson = locator,
                kind = if (annotated) ArrodesFragmentKind.NOTE else ArrodesFragmentKind.HIGHLIGHT,
                resonanceScore = memory.resonanceScore + annotationBonus + favoriteBonus
            )
        }
        .sortedWith(
            compareByDescending<ArrodesFragment> { it.resonanceScore }
                .thenBy { it.id }
        )
        .take(limit)
        .toList()
}


/**
 * Builds a stable non-repeating deck for one Mirror visit.
 *
 * The strongest fragment remains first so authored-note/resonance priority is preserved. The
 * remainder is deterministically permuted from [sessionSeed], giving a fresh traversal per visit
 * without random reshuffling or repeats before the deck wraps.
 */
fun orderArrodesFragmentsForSession(
    fragments: List<ArrodesFragment>,
    sessionSeed: Int
): List<ArrodesFragment> {
    if (fragments.size <= 2) return fragments
    val strongest = fragments.first()
    val remainder = fragments.drop(1).sortedWith(
        compareBy<ArrodesFragment> { stableArrodesSessionKey(it.id, sessionSeed) }
            .thenBy { it.id }
    )
    return buildList(fragments.size) {
        add(strongest)
        addAll(remainder)
    }
}

private fun stableArrodesSessionKey(id: String, sessionSeed: Int): Long {
    var value = id.hashCode().toLong() xor (sessionSeed.toLong() shl 32)
    value = value xor (value shl 13)
    value = value xor (value ushr 7)
    value = value xor (value shl 17)
    return value
}
