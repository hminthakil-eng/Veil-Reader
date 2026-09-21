package com.veilreader.app.manga.core

import java.io.IOException

data class MangaSearchRequest(
    val query: String,
    val cursor: String? = null,
    val filters: MangaFilterSelection = MangaFilterSelection()
) {
    init {
        require(query.isNotBlank()) { "Search query cannot be blank." }
    }
}

data class MangaBrowseRequest(
    val cursor: String? = null,
    val filters: MangaFilterSelection = MangaFilterSelection()
)

data class MangaFilterSelection(
    val values: Map<String, List<String>> = emptyMap()
) {
    fun valuesFor(id: String): List<String> = values[id].orEmpty()
    fun isEmpty(): Boolean = values.isEmpty()
}

sealed interface MangaFilterDefinition {
    val id: String
    val label: String

    data class Text(
        override val id: String,
        override val label: String,
        val defaultValue: String = ""
    ) : MangaFilterDefinition

    data class Toggle(
        override val id: String,
        override val label: String,
        val defaultValue: Boolean = false
    ) : MangaFilterDefinition

    data class Select(
        override val id: String,
        override val label: String,
        val options: List<MangaFilterOption>,
        val multiple: Boolean = false
    ) : MangaFilterDefinition {
        init {
            require(options.map(MangaFilterOption::value).distinct().size == options.size) {
                "Manga filter option values must be unique."
            }
        }
    }
}

data class MangaFilterOption(
    val value: String,
    val label: String
) {
    init {
        require(value.isNotBlank()) { "Manga filter option value cannot be blank." }
        require(label.isNotBlank()) { "Manga filter option label cannot be blank." }
    }
}

data class MangaUpdateOptions(
    val fetchDetails: Boolean = true,
    val fetchChapters: Boolean = true
) {
    init {
        require(fetchDetails || fetchChapters) {
            "Manga update must request details, chapters, or both."
        }
    }
}

data class MangaUpdate(
    val ref: MangaRef,
    val details: MangaDetails? = null,
    val chapters: List<MangaChapter>? = null
)

sealed class MangaSourceException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause) {
    class RateLimited(
        val retryAfterMillis: Long? = null,
        message: String = "Manga source rate limit reached."
    ) : MangaSourceException(message)

    class AuthRequired(
        message: String = "Manga source authentication is required."
    ) : MangaSourceException(message)

    class Blocked(
        message: String = "Manga source request was blocked."
    ) : MangaSourceException(message)

    class NotFound(
        message: String = "Manga source item was not found."
    ) : MangaSourceException(message)

    class TemporarilyUnavailable(
        message: String = "Manga source is temporarily unavailable."
    ) : MangaSourceException(message)

    class NetworkFailure(
        val statusCode: Int? = null,
        message: String = "Manga source network request failed.",
        cause: Throwable? = null
    ) : MangaSourceException(message, cause)

    class ParseFailure(
        message: String = "Manga source response could not be parsed.",
        cause: Throwable? = null
    ) : MangaSourceException(message, cause)

    class SourceChanged(
        message: String = "Manga source structure changed."
    ) : MangaSourceException(message)

    class Unsupported(
        message: String = "Manga source operation is not supported."
    ) : MangaSourceException(message)
}
