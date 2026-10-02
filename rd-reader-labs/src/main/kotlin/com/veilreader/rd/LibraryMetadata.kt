package com.veilreader.rd

data class ExtendedBookMetadata(
    val rating: Int? = null,
    val tags: Set<String> = emptySet(),
    val customCoverRef: String? = null
) {
    fun normalized(): ExtendedBookMetadata = copy(
        rating = rating?.coerceIn(1, 5),
        tags = tags
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(String::lowercase)
            .toSortedSet(),
        customCoverRef = customCoverRef?.trim()?.takeIf(String::isNotEmpty)
    )
}
