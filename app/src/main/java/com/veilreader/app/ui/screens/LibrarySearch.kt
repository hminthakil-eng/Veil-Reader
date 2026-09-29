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
