package com.veilreader.app.ui.screens

import java.text.Normalizer
import java.util.Locale

/**
 * Normalize a query and a book's searchable fields the same way.
 * Persian and Arabic keyboard variants, optional vowel marks, spacing/half-spaces,
 * and decimal digit shapes should not decide whether a local book can be found.
 */
internal fun normalizeLibrarySearchText(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        .lowercase(Locale.ROOT)
    return buildString(decomposed.length) {
        for (char in decomposed) {
            val type = Character.getType(char)
            if (
                char == '\u0640' || // tatweel
                type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.FORMAT.toInt() || // includes Persian half-space and bidi marks
                Character.isWhitespace(char) ||
                Character.isSpaceChar(char)
            ) {
                continue
            }
            append(
                when (char) {
                    '\u064A', '\u0649' -> '\u06CC' // Arabic ya / alef maqsura -> Persian ye
                    '\u0643' -> '\u06A9' // Arabic kaf -> Persian kaf
                    in '\u0660'..'\u0669' -> '0' + (char - '\u0660')
                    in '\u06F0'..'\u06F9' -> '0' + (char - '\u06F0')
                    else -> char
                }
            )
        }
    }
}

/**
 * Parses small user-entered decimal metadata across Latin, Persian and Arabic keyboards.
 * Series indices are not financial values, so a single comma may safely act as a decimal mark.
 */
internal fun parseLocalizedDecimalInput(value: String): Double? {
    val source = value.trim()
    if (source.isEmpty()) return null

    val commaAsDecimal =
        '.' !in source &&
            '\u066B' !in source &&
            source.count { it == ',' } == 1

    val normalized = buildString(source.length) {
        for (char in source) {
            when (char) {
                in '\u0660'..'\u0669' -> append('0' + (char - '\u0660'))
                in '\u06F0'..'\u06F9' -> append('0' + (char - '\u06F0'))
                '\u066B' -> append('.')
                ',' -> if (commaAsDecimal) append('.')
                '\u066C', '_', ' ', '\u00A0', '\u202F' -> Unit
                '\u2212' -> append('-')
                else -> append(char)
            }
        }
    }

    return normalized.toDoubleOrNull()?.takeIf(Double::isFinite)
}



internal data class NormalizedLibrarySearchDocument(
    val title: String,
    val author: String,
    val series: String?,
    val language: String?,
    val collections: List<String>
)

internal fun normalizedLibrarySearchDocument(
    title: String,
    author: String,
    series: String?,
    language: String?,
    collections: List<String>
): NormalizedLibrarySearchDocument =
    NormalizedLibrarySearchDocument(
        title = normalizeLibrarySearchText(title),
        author = normalizeLibrarySearchText(author),
        series = series?.let(::normalizeLibrarySearchText),
        language = language?.let(::normalizeLibrarySearchText),
        collections = collections.map(::normalizeLibrarySearchText)
    )

/**
 * Returns null when the document does not match.
 *
 * Scores are deliberately separated into non-overlapping bands so a weak title match always ranks
 * above an exact author match, an author match above series, series above collection, and
 * collection above language. This keeps local retrieval deterministic and explainable.
 */
internal fun librarySearchRelevance(
    normalizedQuery: String,
    document: NormalizedLibrarySearchDocument
): Int? {
    if (normalizedQuery.isBlank()) return 0

    fun fieldScore(value: String?, band: Int): Int? {
        val candidate = value?.takeIf(String::isNotBlank) ?: return null
        return when {
            candidate == normalizedQuery -> band + 300
            candidate.startsWith(normalizedQuery) -> band + 200
            candidate.contains(normalizedQuery) -> band + 100
            else -> null
        }
    }

    return buildList {
        fieldScore(document.title, 6_000)?.let(::add)
        fieldScore(document.author, 5_000)?.let(::add)
        fieldScore(document.series, 4_000)?.let(::add)
        document.collections.forEach { collection ->
            fieldScore(collection, 3_000)?.let(::add)
        }
        fieldScore(document.language, 2_000)?.let(::add)
    }.maxOrNull()
}
