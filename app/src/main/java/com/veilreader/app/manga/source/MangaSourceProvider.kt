package com.veilreader.app.manga.source

/**
 * Veil-owned source contract.
 *
 * Implementations may parse HTML, call a documented API, use a WebView challenge adapter, or read a
 * local source. The Reader/UI must never depend on those transport details.
 */
interface MangaSourceProvider {

    val descriptor: MangaSourceDescriptor
    val capabilities: Set<MangaSourceCapability>

    suspend fun search(
        request: SourceSearchRequest
    ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> =
        unsupported(MangaSourceCapability.SEARCH)

    suspend fun details(
        manga: SourceMangaRef
    ): SourceOutcome<SourceMangaDetails> =
        unsupported(MangaSourceCapability.DETAILS)

    suspend fun chapters(
        manga: SourceMangaRef
    ): SourceOutcome<List<SourceChapter>> =
        unsupported(MangaSourceCapability.CHAPTERS)

    suspend fun pages(
        chapter: SourceChapter
    ): SourceOutcome<List<MangaPageImage>> =
        unsupported(MangaSourceCapability.PAGES)

    /**
     * Optional reverse-link resolution for deep links or migration. Null means the URL is not owned
     * by this provider.
     */
    suspend fun resolvePublicUrl(url: String): SourceOutcome<SourceMangaRef?> =
        SourceOutcome.Success(null)

    fun alternateDomains(): List<String> = descriptor.domains.drop(1)

    private fun <T> unsupported(
        capability: MangaSourceCapability
    ): SourceOutcome<T> = SourceOutcome.Failure(SourceFailure.unsupported(capability))
}

fun MangaSourceProvider.supports(capability: MangaSourceCapability): Boolean =
    capability in capabilities

fun MangaSourceProvider.requireCapability(capability: MangaSourceCapability) {
    check(supports(capability)) {
        "${descriptor.id} does not declare capability $capability"
    }
}
