package com.veilreader.rd

data class TtsFilterRule(val pattern: Regex, val replacement: String = "")

object TtsChunker {
    fun chunks(
        text: String,
        maxChars: Int = 800,
        filters: List<TtsFilterRule> = emptyList()
    ): List<String> {
        require(maxChars >= 80)
        var cleaned = text
        filters.forEach { rule -> cleaned = cleaned.replace(rule.pattern, rule.replacement) }
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
        if (cleaned.isEmpty()) return emptyList()

        val sentences = cleaned
            .split(Regex("(?<=[.!?؟。！？])\\s+"))
            .filter(String::isNotBlank)

        val out = mutableListOf<String>()
        var current = StringBuilder()
        fun flush() {
            val value = current.toString().trim()
            if (value.isNotEmpty()) out += value
            current = StringBuilder()
        }

        for (sentence in sentences) {
            if (sentence.length > maxChars) {
                flush()
                sentence.chunked(maxChars).map(String::trim).filter(String::isNotEmpty).forEach(out::add)
            } else if (current.isEmpty()) {
                current.append(sentence)
            } else if (current.length + 1 + sentence.length <= maxChars) {
                current.append(' ').append(sentence)
            } else {
                flush()
                current.append(sentence)
            }
        }
        flush()
        return out
    }
}
