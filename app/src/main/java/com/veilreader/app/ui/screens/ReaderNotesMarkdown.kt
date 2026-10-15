package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Highlight

/**
 * Stream a book's real, persisted highlights/notes to a user-chosen Markdown document.
 * No parallel note store, invisible upload, truncation, or network is involved.
 * The caller owns the output stream and must only report success after closing it.
 */
internal fun writeReaderNotesMarkdown(
    out: Appendable,
    bookId: String,
    bookTitle: String,
    highlights: List<Highlight>
): Int {
    val owned = highlights.filter { it.bookId == bookId }
        .sortedWith(compareBy<Highlight> { it.createdAtEpochMs }.thenBy { it.id })
    out.append("# Veil Reader — ")
        .append(bookTitle.replace(Regex("[\\r\\n]+"), " ").trim())
        .append("\n\n")
    owned.forEachIndexed { idx, highlight ->
        out.append("## ").append((idx + 1).toString()).append("\n\n")
        highlight.quote.lineSequence().forEach { line ->
            out.append("> ").append(line).append("\n")
        }
        if (highlight.note.isNotBlank()) {
            out.append("\n**Note / یادداشت:**\n\n")
            highlight.note.lineSequence().forEach { line ->
                out.append("> ").append(line).append("\n")
            }
        }
        out.append("\n---\n\n")
    }
    return owned.size
}
