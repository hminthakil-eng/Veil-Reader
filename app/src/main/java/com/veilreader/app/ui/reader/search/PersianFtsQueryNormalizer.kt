package com.veilreader.app.ui.reader.search

/**
 * Search-only normalization. Never persist this output as a book's display text.
 * FTS MATCH arguments must still be bound as parameters, never interpolated into SQL.
 */
object PersianFtsQueryNormalizer {
    private val whitespace = Regex("\\s+")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val result = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                'ي', 'ى' -> result.append('ی')
                'ك' -> result.append('ک')
                'ة' -> result.append('ه')
                'إ', 'أ', 'ٱ', 'آ' -> result.append('ا')
                '\u200C', '\u200B', '\uFEFF', '\u0640' -> result.append(' ')
                in '\u064B'..'\u065F', '\u0670' -> Unit
                else -> {
                    val kind = Character.getType(ch)
                    if (kind == Character.NON_SPACING_MARK.toInt() ||
                        kind == Character.COMBINING_SPACING_MARK.toInt()
                    ) {
                        continue
                    }
                    if (ch.isLetterOrDigit() || ch == '_') result.append(ch)
                    else result.append(' ')
                }
            }
        }
        return result.toString().replace(whitespace, " ").trim()
    }

    /** Quotes each token, so keywords such as OR are treated as text, not FTS operators. */
    fun toSafeFtsMatchQuery(rawQuery: String): String = normalize(rawQuery)
        .split(' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { token -> "\"$token\"*" }
}
