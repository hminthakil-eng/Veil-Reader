package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaBrowseRequest
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaDetails
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResultPage
import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceException
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.core.MangaStatus
import com.veilreader.app.manga.core.MangaSummary
import com.veilreader.app.manga.core.MangaUpdate
import com.veilreader.app.manga.core.MangaUpdateOptions

class SuwayomiGateway(
    private val client: SuwayomiGatewayClient
) {
    suspend fun discoverSources(): List<SuwayomiSourceProvider> =
        client.sources().map { remote ->
            SuwayomiSourceProvider(client, remote)
        }
}

class SuwayomiSourceProvider internal constructor(
    private val client: SuwayomiGatewayClient,
    private val remoteSource: SuwayomiRemoteSource
) : MangaSourceProvider {
    override val descriptor: MangaSourceDescriptor = MangaSourceDescriptor(
        id = sourceId(client.config.instanceId, remoteSource.id),
        providerId = MangaProviderId("suwayomi." + client.config.instanceId),
        version = 1,
        name = remoteSource.displayName.ifBlank { remoteSource.name },
        language = remoteSource.language,
        capabilities = buildSet {
            add(MangaSourceCapability.SEARCH)
            add(MangaSourceCapability.POPULAR)
            if (remoteSource.supportsLatest) add(MangaSourceCapability.LATEST)
        }
    )

    override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> =
        browse(
            type = SuwayomiBrowseType.SEARCH,
            page = request.cursor.toPageNumber(),
            query = request.query
        )

    override suspend fun popular(request: MangaBrowseRequest): MangaResultPage<MangaSummary> =
        browse(
            type = SuwayomiBrowseType.POPULAR,
            page = request.cursor.toPageNumber(),
            query = null
        )

    override suspend fun latest(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        if (!remoteSource.supportsLatest) {
            throw MangaSourceException.Unsupported(
                "This Suwayomi source does not support latest updates."
            )
        }
        return browse(
            type = SuwayomiBrowseType.LATEST,
            page = request.cursor.toPageNumber(),
            query = null
        )
    }

    override suspend fun fetchUpdate(
        ref: MangaRef,
        existingChapters: List<MangaChapter>,
        options: MangaUpdateOptions
    ): MangaUpdate {
        requireOwned(ref)
        val mangaId = ref.key.toIntOrNull()
            ?: throw MangaSourceException.SourceChanged(
                "Suwayomi manga key is no longer a numeric server id."
            )

        val payload = client.fetchUpdate(
            mangaId = mangaId,
            fetchManga = options.fetchDetails,
            fetchChapters = options.fetchChapters
        )
        payload.manga?.let { remote ->
            if (remote.sourceId != remoteSource.id) {
                throw MangaSourceException.SourceChanged(
                    "Suwayomi manga moved to a different source."
                )
            }
        }

        return MangaUpdate(
            ref = ref,
            details = payload.manga?.toDetails(ref),
            chapters = payload.chapters?.map { it.toChapter(ref) }
        )
    }

    override suspend fun pages(ref: MangaChapterRef): List<MangaPage> {
        requireOwned(ref.manga)
        val chapterId = ref.key.toIntOrNull()
            ?: throw MangaSourceException.SourceChanged(
                "Suwayomi chapter key is no longer a numeric server id."
            )

        return client.pages(chapterId).mapIndexed { index, request ->
            MangaPage(index = index, image = request)
        }
    }

    private suspend fun browse(
        type: SuwayomiBrowseType,
        page: Int,
        query: String?
    ): MangaResultPage<MangaSummary> {
        val result = client.browse(
            sourceId = remoteSource.id,
            type = type,
            page = page,
            query = query
        )

        return MangaResultPage(
            items = result.mangas.map { remote ->
                MangaSummary(
                    ref = MangaRef(descriptor.id, remote.id.toString()),
                    title = remote.title,
                    cover = remote.thumbnailUrl
                        ?.takeIf(String::isNotBlank)
                        ?.let { client.resourceRequest(it) }
                )
            },
            nextCursor = if (result.hasNextPage) (page + 1).toString() else null
        )
    }

    private suspend fun SuwayomiRemoteManga.toDetails(ref: MangaRef): MangaDetails =
        MangaDetails(
            ref = ref,
            title = title,
            description = description.orEmpty(),
            authors = listOfNotNull(author, artist)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct(),
            tags = genres
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct(),
            status = status.toVeilStatus(),
            cover = thumbnailUrl
                ?.takeIf(String::isNotBlank)
                ?.let { client.resourceRequest(it) }
        )

    private fun SuwayomiRemoteChapter.toChapter(manga: MangaRef): MangaChapter =
        MangaChapter(
            ref = MangaChapterRef(
                manga = manga,
                key = id.toString()
            ),
            title = name,
            chapterNumber = chapterNumber,
            publishedAtEpochMs = uploadDate,
            scanlator = scanlator
        )

    private fun requireOwned(ref: MangaRef) {
        require(ref.sourceId == descriptor.id) {
            "Suwayomi provider received a manga owned by another source."
        }
    }
}

internal fun sourceId(instanceId: String, remoteSourceId: Long): MangaSourceId =
    MangaSourceId("suwayomi.$instanceId.$remoteSourceId")

internal fun String?.toVeilStatus(): MangaStatus = when (this) {
    "ONGOING" -> MangaStatus.ONGOING
    "COMPLETED", "PUBLISHING_FINISHED" -> MangaStatus.COMPLETED
    "ON_HIATUS" -> MangaStatus.HIATUS
    "CANCELLED" -> MangaStatus.CANCELLED
    else -> MangaStatus.UNKNOWN
}

private fun String?.toPageNumber(): Int =
    this?.toIntOrNull()?.coerceAtLeast(1) ?: 1
