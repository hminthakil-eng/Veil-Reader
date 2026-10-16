package com.veilreader.app.investigation.ledger

import com.veilreader.app.domain.Highlight

/** Explicit caller assertion associating an existing Book ID with an exact source version.
 * No automatic trust is placed in Book.contentFingerprint, metadata, URI or filename.
 */
data class PublicationBinding(val bookId: String, val publication: PublicationIdentity) {
    init { require(bookId.isNotBlank()) }
}

class HighlightSnapshot(
    highlights: List<Highlight>,
    bindings: List<PublicationBinding>,
    deletedHighlightIds: Set<String> = emptySet()
) {
    val highlights = highlights.toList()
    val bindings = bindings.toList()
    val deletedHighlightIds = deletedHighlightIds.toSet()
}

enum class HighlightReferenceStatus { AVAILABLE, MISSING }

/** Domain-only result. Contains the existing sensitive record; never pass this to a UI summary. */
class ResolvedHighlight internal constructor(val id: String, val highlight: Highlight?) {
    val status: HighlightReferenceStatus
        get() = if (highlight == null) HighlightReferenceStatus.MISSING else HighlightReferenceStatus.AVAILABLE
}

/** Missing/deleted/stale/ambiguous references produce tombstones, preserving request order.
 * Duplicate IDs in either requested references or source records are all treated as missing.
 * An absent binding (including a deleted book) or duplicate binding also fails closed.
 */
fun resolveHighlights(
    publication: PublicationIdentity,
    highlightIds: List<String>,
    snapshot: HighlightSnapshot
): List<ResolvedHighlight> {
    val requestCounts = highlightIds.groupingBy { it }.eachCount()
    val records = snapshot.highlights.groupBy { it.id }
    val bindings = snapshot.bindings.groupBy { it.bookId }
    return highlightIds.map { id ->
        val record = records[id]?.singleOrNull()
        val binding = record?.let { bindings[it.bookId]?.singleOrNull() }
        val available = id.isNotBlank() && requestCounts[id] == 1 &&
            id !in snapshot.deletedHighlightIds && binding?.publication == publication
        ResolvedHighlight(id, record.takeIf { available })
    }
}
