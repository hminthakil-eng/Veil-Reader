package com.veilreader.app.ui.screens

import java.text.Normalizer
import java.util.Locale

/**
 * Normalize a query and a book's searchable fields the same way.
 * Persian and Arabic keyboard variants, optional vowel marks, and Latin accents
 * should not decide whether a local book can be found.
 */
internal fun normalizeLibrarySearchText(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        .lowercase(Locale.ROOT)
    return buildString(decomposed.length) {
        for (char in decomposed) {
            if (char == '\u0640' || Character.getType(char) == Character.NON_SPACING_MARK.toInt()) {
                continue
            }
            append(
                when (char) {
                    '\u064A', '\u0649' -> '\u06CC' // Arabic ya / alef maqsura -> Persian ye
                    '\u0643' -> '\u06A9' // Arabic kaf -> Persian kaf
                    else -> char
                }
            )
        }
    }
}
