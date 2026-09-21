package com.veilreader.app.manga.hub

import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaFactory
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaCanonicalStore
import com.veilreader.app.manga.reader.presentation.MangaChapterRoute
import com.veilreader.app.manga.reader.screen.MangaReaderChapterEntry
import com.veilreader.app.manga.reader.screen.MangaReaderSession
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.PagedSourceResult
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRegistry
import com.veilreader.app.manga.source.SourceSearchRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class MangaHubCatalogService(
    private val sources: SourceRegistry,
    private val execution: SourceExecutionCoordinator,
    private val library: MangaCanonicalStore,
    private val discoverySeed: MangaHubDiscoverySeed = MangaHubDiscoverySeed { emptyList() },
    private val canonicalFactory: CanonicalMangaFactory = CanonicalMangaFactory()
) {

    suspend fun discover(): MangaHubBatchResult<MangaHubCatalogItem> = supervisorScope {
        val refs = discoverySeed.refs().distinctBy { it.sourceId to it.key }
        val deferred = refs.map { ref ->
            async {
                val provider = sources.find(ref.sourceId)
                    ?: return@async DiscoveryResolution.MissingSource(ref)
                val result = execution.details(provider, ref)
                when (val outcome = result.outcome) {
                    is SourceOutcome.Success -> DiscoveryResolution.Ready(
                        item = catalogItem(outcome.value.summary, provider)
                    )
                    is SourceOutcome.Failure -> DiscoveryResolution.Failed(
                        source = provider,
                        failure = outcome.error
                    )
                }
            }
        }

        val resolved = deferred.map { it.await() }
        MangaHubBatchResult(
            items = resolved.mapNotNull { (it as? DiscoveryResolution.Ready)?.item },
            issues = resolved.mapNotNull { value ->
                (value as? DiscoveryResolution.Failed)?.let {
                    MangaHubSourceIssue(it.source.descriptor, it.failure)
                }
            }
        )
    }

    suspend fun search(query: String): MangaHubBatchResult<MangaHubCatalogItem> = supervisorScope {
        val cleanQuery = query.trim()
        require(cleanQuery.isNotEmpty()) { "Manga search query cannot be blank" }

        val providers = sources.supporting(MangaSourceCapability.SEARCH)
        val deferred = providers.map { provider ->
            async {
                val result = execution.search(
                    provider,
                    SourceSearchRequest(cleanQuery)
                )
                provider to result.outcome
            }
        }

        val items = mutableListOf<MangaHubCatalogItem>()
        val issues = mutableListOf<MangaHubSourceIssue>()

        deferred.forEach { pending ->
            val (provider, outcome) = pending.await()
            when (outcome) {
                is SourceOutcome.Success -> {
                    outcome.value.items.forEach { summary ->
                        items += catalogItem(summary, provider)
                    }
                }
                is SourceOutcome.Failure ->
                    issues += MangaHubSourceIssue(provider.descriptor, outcome.error)
            }
        }

        MangaHubBatchResult(
            items = items.distinctBy {
                it.summary.ref.sourceId to it.summary.ref.key
            },
            issues = issues
        )
    }

    suspend fun loadDetails(ref: SourceMangaRef): MangaHubWorkDetails {
        val provider = requireProvider(ref.sourceId)
        val details = requireSuccess(
            execution.details(provider, ref).outcome,
            "Could not load Manga details."
        )
        val chapters = requireSuccess(
            execution.chapters(provider, ref).outcome,
            "Could not load Manga chapters."
        )
        val canonical = library.findWorkBySource(ref)

        return MangaHubWorkDetails(
            details = details,
            chaptersInReadingOrder = chapters,
            source = provider.descriptor,
            canonical = canonical
        )
    }

    suspend fun loadLibraryDetails(
        id: CanonicalMangaId,
        preferredSourceId: SourceId? = null
    ): MangaHubWorkDetails {
        val canonical = library.loadWork(id)
            ?: throw MangaHubException("This Manga is no longer in the local library.")

        val refs = buildList {
            if (preferredSourceId != null) {
                canonical.sourceRef(preferredSourceId)?.let(::add)
            }
            canonical.sourceRefs.values
                .filterNot { it.sourceId == preferredSourceId }
                .sortedBy { it.sourceId.value }
                .forEach(::add)
        }

        var lastFailure: MangaHubException? = null
        for (ref in refs) {
            val provider = sources.find(ref.sourceId) ?: continue
            try {
                val details = requireSuccess(
                    execution.details(provider, ref).outcome,
                    "Could not load Manga details."
                )
                val chapters = requireSuccess(
                    execution.chapters(provider, ref).outcome,
                    "Could not load Manga chapters."
                )
                return MangaHubWorkDetails(
                    details = details,
                    chaptersInReadingOrder = chapters,
                    source = provider.descriptor,
                    canonical = canonical
                )
            } catch (error: MangaHubException) {
                lastFailure = error
            }
        }

        throw lastFailure ?: MangaHubException(
            "No linked source can currently open this Manga."
        )
    }

    suspend fun addToLibrary(details: SourceMangaDetails): CanonicalManga {
        val ref = details.summary.ref
        val existing = library.findWorkBySource(ref)
        if (existing != null) return existing

        val created = canonicalFactory.create(
            title = details.summary.title,
            initialSource = ref,
            alternativeTitles = details.summary.alternativeTitles
        )
        return try {
            library.saveWork(created)
            created
        } catch (error: Exception) {
            // A concurrent Add action may have won the unique source-identity race. Return that
            // canonical work rather than creating a duplicate or surfacing a false failure.
            library.findWorkBySource(ref) ?: throw error
        }
    }

    suspend fun removeFromLibrary(id: CanonicalMangaId) {
        library.deleteWork(id)
    }

    suspend fun library(): List<CanonicalManga> = library.listWorks()

    suspend fun openFromSource(
        details: SourceMangaDetails,
        preferredChapterKey: String? = null
    ): MangaReaderSession {
        val canonical = addToLibrary(details)
        return buildSession(
            canonical = canonical,
            preferredSourceId = details.summary.ref.sourceId,
            preferredChapterKey = preferredChapterKey
        )
    }

    suspend fun openLibraryWork(
        id: CanonicalMangaId,
        preferredSourceId: SourceId? = null,
        preferredChapterKey: String? = null
    ): MangaReaderSession {
        val canonical = library.loadWork(id)
            ?: throw MangaHubException("This Manga is no longer in the local library.")
        return buildSession(
            canonical = canonical,
            preferredSourceId = preferredSourceId,
            preferredChapterKey = preferredChapterKey
        )
    }

    private suspend fun buildSession(
        canonical: CanonicalManga,
        preferredSourceId: SourceId?,
        preferredChapterKey: String?
    ): MangaReaderSession {
        val candidates = buildList {
            if (preferredSourceId != null) {
                canonical.sourceRef(preferredSourceId)?.let(::add)
            }
            canonical.sourceRefs.values
                .filterNot { it.sourceId == preferredSourceId }
                .sortedBy { it.sourceId.value }
                .forEach(::add)
        }

        if (candidates.isEmpty()) {
            throw MangaHubException("This Manga has no linked source.")
        }

        var lastFailure: MangaHubException? = null
        for (ref in candidates) {
            val provider = sources.find(ref.sourceId) ?: continue
            if (
                MangaSourceCapability.CHAPTERS !in provider.capabilities ||
                MangaSourceCapability.PAGES !in provider.capabilities
            ) {
                continue
            }

            val chapters = try {
                requireSuccess(
                    execution.chapters(provider, ref).outcome,
                    "Could not load Manga chapters."
                )
            } catch (error: MangaHubException) {
                lastFailure = error
                continue
            }

            if (chapters.isEmpty()) {
                lastFailure = MangaHubException("The source returned no chapters.")
                continue
            }

            val entries = chapters.map { chapter ->
                MangaReaderChapterEntry(
                    route = MangaChapterRoute.from(canonical.id, chapter),
                    provider = provider
                )
            }
            val initialIndex = preferredChapterKey
                ?.let { key -> chapters.indexOfFirst { it.chapterKey == key } }
                ?.takeIf { it >= 0 }
                ?: 0

            return MangaReaderSession(
                entriesInReadingOrder = entries,
                initialIndex = initialIndex
            )
        }

        throw lastFailure ?: MangaHubException(
            "No linked source can currently provide chapters and pages."
        )
    }

    private suspend fun catalogItem(
        summary: SourceMangaSummary,
        provider: MangaSourceProvider
    ): MangaHubCatalogItem =
        MangaHubCatalogItem(
            summary = summary,
            source = provider.descriptor,
            canonicalId = library.findWorkBySource(summary.ref)?.id
        )

    private fun requireProvider(sourceId: SourceId): MangaSourceProvider =
        sources.find(sourceId)
            ?: throw MangaHubException("The linked Manga source is not installed.")

    private fun <T> requireSuccess(
        outcome: SourceOutcome<T>,
        fallbackMessage: String
    ): T = when (outcome) {
        is SourceOutcome.Success -> outcome.value
        is SourceOutcome.Failure -> throw MangaHubException(
            message = outcome.error.message.ifBlank { fallbackMessage },
            failure = outcome.error
        )
    }

    private sealed interface DiscoveryResolution {
        data class Ready(
            val item: MangaHubCatalogItem
        ) : DiscoveryResolution

        data class Failed(
            val source: MangaSourceProvider,
            val failure: com.veilreader.app.manga.source.SourceFailure
        ) : DiscoveryResolution

        data class MissingSource(
            val ref: SourceMangaRef
        ) : DiscoveryResolution
    }
}
