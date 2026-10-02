package com.veilreader.rd

enum class AppearanceProfileScope { GLOBAL, BOOK }

data class AppearanceProfile(
    val id: String,
    val name: String,
    val schemaVersion: Int = 1,
    val scope: AppearanceProfileScope = AppearanceProfileScope.GLOBAL,
    val bookId: String? = null,
    val values: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(schemaVersion > 0)
        require(scope != AppearanceProfileScope.BOOK || !bookId.isNullOrBlank())
    }
}

object AppearanceProfileResolver {
    fun resolve(
        globalDefaults: Map<String, String>,
        selectedProfile: AppearanceProfile?,
        bookOverride: AppearanceProfile?
    ): Map<String, String> {
        require(bookOverride == null || bookOverride.scope == AppearanceProfileScope.BOOK)
        return buildMap {
            putAll(globalDefaults)
            selectedProfile?.values?.let(::putAll)
            bookOverride?.values?.let(::putAll)
        }
    }
}
