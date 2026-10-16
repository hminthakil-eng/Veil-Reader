package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import org.json.JSONObject
import java.util.Locale

/** One accessible book section. Only saved user material; never search future publication text. */
internal data class InvestigationDeskGroup(
    val book: Book,
    val highlights: List<Highlight>
)

/**
 * Pure index for the opt-in investigation desk.
 * Invalid, orphaned or ambiguous saved entries fail closed; source objects remain unchanged.
 */
internal fun indexInvestigationDesk(
    books: List<Book>,
    highlights: List<Highlight>,
    bookTitleQuery: String = ""
): List<InvestigationDeskGroup> {
    val availableBooks = books.asSequence()
        .filter { it.id.isNotBlank() && !it.sourceUri.isNullOrBlank() }
        .groupBy { it.id }
        .filterValues { it.size == 1 }
        .mapValues { (_, value) -> value.single() }

    val idCounts = highlights.groupingBy { it.id }.eachCount()
    val query = bookTitleQuery.trim()
    val validMarks = highlights.filter { mark ->
        mark.id.isNotBlank() &&
            idCounts[mark.id] == 1 &&
            mark.bookId in availableBooks &&
            (mark.quote.isNotBlank() || mark.note.isNotBlank()) &&
            isUsableInvestigationLocator(mark.locatorJson)
    }

    return validMarks.groupBy { it.bookId }
        .mapNotNull { (bookId, marks) ->
            val book = availableBooks[bookId] ?: return@mapNotNull null
            if (query.isNotBlank() && !book.title.contains(query, ignoreCase = true)) {
                return@mapNotNull null
            }
            InvestigationDeskGroup(
                book,
                marks.sortedWith(compareByDescending<Highlight> { it.createdAtEpochMs }
                    .thenBy { it.id })
            )
        }
        .sortedWith(compareBy<InvestigationDeskGroup> { it.book.title.lowercase(Locale.ROOT) }
            .thenBy { it.book.id })
}

/** Structure check only; the reader remains responsible for resolving the current publication locator. */
private fun isUsableInvestigationLocator(raw: String): Boolean {
    if (raw.isBlank()) return false
    return try {
        val json = JSONObject(raw)
        val href = json.optString("href")
        val type = json.optString("type")
        href.isNotBlank() && type.isNotBlank()
    } catch (_: Exception) {
        false
    }
}
