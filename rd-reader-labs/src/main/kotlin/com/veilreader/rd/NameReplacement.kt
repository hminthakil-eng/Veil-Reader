package com.veilreader.rd

data class NameReplacementRule(
    val source: String,
    val replacement: String,
    val caseSensitive: Boolean = false
) {
    init {
        require(source.isNotBlank())
        require(replacement.isNotBlank())
    }
}

object NameReplacementEngine {
    fun apply(text: String, rules: List<NameReplacementRule>): String {
        var output = text
        for (rule in rules) {
            val options = if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = Regex("(?<![\\p{L}\\p{N}_])" + Regex.escape(rule.source) + "(?![\\p{L}\\p{N}_])", options)
            output = output.replace(regex, rule.replacement)
        }
        return output
    }
}
