package com.veilreader.app.manga.source

/**
 * Per-attempt execution context supplied by Veil's coordinator.
 *
 * A provider must use [domain] instead of hard-coding a host so mirror failover remains a runtime
 * decision rather than a parser rewrite.
 */
data class SourceRequestContext(
    val domain: String,
    val attempt: Int
) {
    init {
        require(domain.isNotBlank()) { "Source request domain cannot be blank" }
        require(attempt > 0) { "Source request attempt must be positive" }
    }
}

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
        request: SourceSearchRequest,
        context: SourceRequestContext
    ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> =
        unsupported(MangaSourceCapability.SEARCH)

    suspend fun details(
        manga: SourceMangaRef,
        context: SourceRequestContext
    ): SourceOutcome<SourceMangaDetails> =
        unsupported(MangaSourceCapability.DETAILS)

    suspend fun chapters(
        manga: SourceMangaRef,
        context: SourceRequestContext
    ): SourceOutcome<List<SourceChapter>> =
        unsupported(MangaSourceCapability.CHAPTERS)

    suspend fun pages(
        chapter: SourceChapter,
        context: SourceRequestContext
    ): SourceOutcome<List<MangaPageImage>> =
        unsupported(MangaSourceCapability.PAGES)

    /**
     * Optional reverse-link resolution for deep links or migration. Null means the URL is not owned
     * by this provider.
     */
    suspend fun resolvePublicUrl(
        url: String,
        context: SourceRequestContext
    ): SourceOutcome<SourceMangaRef?> = SourceOutcome.Success(null)

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

/**
 * Direct-call helpers use the primary domain. Production source access should normally go through
 * [SourceExecutionCoordinator] so retry/failover limits are enforced.
 */
suspend fun MangaSourceProvider.search(
    request: SourceSearchRequest
): SourceOutcome<PagedSourceResult<SourceMangaSummary>> =
    search(request, SourceRequestContext(descriptor.primaryDomain, attempt = 1))

suspend fun MangaSourceProvider.details(
    manga: SourceMangaRef
): SourceOutcome<SourceMangaDetails> =
    details(manga, SourceRequestContext(descriptor.primaryDomain, attempt = 1))

suspend fun MangaSourceProvider.chapters(
    manga: SourceMangaRef
): SourceOutcome<List<SourceChapter>> =
    chapters(manga, SourceRequestContext(descriptor.primaryDomain, attempt = 1))

suspend fun MangaSourceProvider.pages(
    chapter: SourceChapter
): SourceOutcome<List<MangaPageImage>> =
    pages(chapter, SourceRequestContext(descriptor.primaryDomain, attempt = 1))
