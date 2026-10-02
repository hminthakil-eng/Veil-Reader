package com.veilreader.rd

enum class ContextToolKind { DICTIONARY, TRANSLATE, WEB_SEARCH, SHARE }

data class ContextToolProvider(
    val id: String,
    val kind: ContextToolKind,
    val requiresNetwork: Boolean,
    val supportsLanguages: Set<String> = emptySet()
) {
    init { require(id.isNotBlank()) }

    fun supports(languageTag: String?): Boolean =
        supportsLanguages.isEmpty() ||
            languageTag.isNullOrBlank() ||
            languageTag.lowercase() in supportsLanguages.map(String::lowercase)
}

object ContextToolRegistry {
    fun preferred(
        providers: List<ContextToolProvider>,
        kind: ContextToolKind,
        languageTag: String?,
        offlineOnly: Boolean
    ): ContextToolProvider? =
        providers
            .asSequence()
            .filter { it.kind == kind }
            .filter { !offlineOnly || !it.requiresNetwork }
            .filter { it.supports(languageTag) }
            .sortedBy { it.requiresNetwork }
            .firstOrNull()
}
