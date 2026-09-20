package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaBrowseRequest
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaDetails
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResourceRequest
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class SuwayomiRemoteSource(
    val remoteId: Long,
    val name: String,
    val displayName: String,
    val language: String,
    val supportsLatest: Boolean,
    val contentWarning: String?
)

class SuwayomiGateway(
    private val config: SuwayomiServerConfig,
    private val transport: SuwayomiGraphQlTransport,
    private val tokenProvider: SuwayomiAccessTokenProvider = SuwayomiAccessTokenProvider { null }
) {
    suspend fun discoverSources(limit: Int = 500): List<SuwayomiRemoteSource> {
        require(limit in 1..2_000) { "Suwayomi source discovery limit is out of range." }
        val data = transport.execute(
            SuwayomiGraphQlRequest(
                operationName = "VeilSuwayomiSources",
                query = SOURCES_QUERY,
                variables = buildJsonObject { put("first", limit) }
            )
        )
        return data.requiredObject("sources")
            .requiredArray("nodes")
            .map { node -> node.jsonObject.toRemoteSource() }
            .sortedWith(compareBy(SuwayomiRemoteSource::language, SuwayomiRemoteSource::displayName))
    }

    suspend fun discoverProviders(limit: Int = 500): List<MangaSourceProvider> =
        discoverSources(limit).map { remote ->
            SuwayomiSourceProvider(config, remote, transport, tokenProvider)
        }

    private companion object {
        val SOURCES_QUERY = """
            query VeilSuwayomiSources(\${'$'}first: Int!) {
              sources(first: \${'$'}first) {
                nodes {
                  id
                  name
                  displayName
                  lang
                  supportsLatest
                  contentWarning
                }
              }
            }
        """.trimIndent()
    }
}

class SuwayomiSourceProvider internal constructor(
    private val config: SuwayomiServerConfig,
    private val remote: SuwayomiRemoteSource,
    private val transport: SuwayomiGraphQlTransport,
    private val tokenProvider: SuwayomiAccessTokenProvider
) : MangaSourceProvider {
    override val descriptor = MangaSourceDescriptor(
        id = MangaSourceId(
            "suwayomi." + config.serverId.value + "." + remote.remoteId
        ),
        providerId = MangaProviderId("suwayomi." + config.serverId.value),
        version = 1,
        name = remote.displayName.ifBlank { remote.name },
        language = remote.language.ifBlank { "und" },
        capabilities = buildSet {
            add(MangaSourceCapability.SEARCH)
            add(MangaSourceCapability.POPULAR)
            if (remote.supportsLatest) add(MangaSourceCapability.LATEST)
        }
    )

    override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> {
        requireNoFilters(request.filters.isEmpty())
        return browse(
            type = "SEARCH",
            page = pageFromCursor(request.cursor),
            query = request.query
        )
    }

    override suspend fun popular(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        requireNoFilters(request.filters.isEmpty())
        return browse(type = "POPULAR", page = pageFromCursor(request.cursor), query = null)
    }

    override suspend fun latest(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        if (!remote.supportsLatest) {
            throw MangaSourceException.Unsupported("This Suwayomi source does not support latest updates.")
        }
        requireNoFilters(request.filters.isEmpty())
        return browse(type = "LATEST", page = pageFromCursor(request.cursor), query = null)
    }

    override suspend fun fetchUpdate(
        ref: MangaRef,
        existingChapters: List<MangaChapter>,
        options: MangaUpdateOptions
    ): MangaUpdate {
        requireOwned(ref)
        val mangaId = parseMangaKey(ref.key)
        val data = transport.execute(
            SuwayomiGraphQlRequest(
                operationName = "VeilSuwayomiUpdate",
                query = UPDATE_MUTATION,
                variables = buildJsonObject {
                    put("input", buildJsonObject {
                        put("id", mangaId)
                        put("fetchManga", options.fetchDetails)
                        put("fetchChapters", options.fetchChapters)
                    })
                }
            )
        )
        val payload = data.requiredObject("fetchMangaAndChapters")
        val headers = resourceHeaders()

        val details = if (options.fetchDetails) payload.optionalObject("manga")?.let { manga ->
            MangaDetails(
                ref = ref,
                title = manga.requiredString("title"),
                description = manga.optionalString("description").orEmpty(),
                authors = listOfNotNull(
                    manga.optionalString("author"),
                    manga.optionalString("artist")
                ).map(String::trim).filter(String::isNotEmpty).distinct(),
                tags = manga.optionalStringArray("genre"),
                status = mapStatus(manga.optionalString("status")),
                cover = manga.optionalString("thumbnailUrl")
                    ?.let(config::resolveServerResource)
                    ?.let { MangaResourceRequest(it, headers) }
            )
        } else null

        val chapters = if (options.fetchChapters) payload.optionalArray("chapters")?.map { element ->
            val chapter = element.jsonObject
            val chapterId = chapter.requiredInt("id")
            MangaChapter(
                ref = MangaChapterRef(ref, chapterKey(chapterId)),
                title = chapter.optionalString("name").orEmpty(),
                chapterNumber = chapter.optionalDouble("chapterNumber"),
                publishedAtEpochMs = chapter.optionalLong("uploadDate")?.takeIf { it > 0L },
                scanlator = chapter.optionalString("scanlator")
            )
        } else null

        return MangaUpdate(ref = ref, details = details, chapters = chapters)
    }

    override suspend fun pages(ref: MangaChapterRef): List<MangaPage> {
        requireOwned(ref.manga)
        val chapterId = parseChapterKey(ref.key)
        val data = transport.execute(
            SuwayomiGraphQlRequest(
                operationName = "VeilSuwayomiPages",
                query = PAGES_MUTATION,
                variables = buildJsonObject {
                    put("input", buildJsonObject { put("chapterId", chapterId) })
                }
            )
        )
        val payload = data.requiredObject("fetchChapterPages")
        val headers = resourceHeaders()
        return payload.requiredArray("pages")
            .mapNotNull { it.jsonPrimitive.contentOrNull }
            .mapNotNull(config::resolveServerResource)
            .distinct()
            .mapIndexed { index, url ->
                MangaPage(index = index, image = MangaResourceRequest(url, headers))
            }
    }

    private suspend fun browse(
        type: String,
        page: Int,
        query: String?
    ): MangaResultPage<MangaSummary> {
        val data = transport.execute(
            SuwayomiGraphQlRequest(
                operationName = "VeilSuwayomiBrowse",
                query = BROWSE_MUTATION,
                variables = buildJsonObject {
                    put("input", buildJsonObject {
                        put("source", remote.remoteId)
                        put("type", type)
                        put("page", page)
                        query?.let { put("query", it) }
                    })
                }
            )
        )
        val payload = data.requiredObject("fetchSourceManga")
        val headers = resourceHeaders()
        val items = payload.requiredArray("mangas").map { element ->
            val manga = element.jsonObject
            val id = manga.requiredInt("id")
            MangaSummary(
                ref = MangaRef(descriptor.id, mangaKey(id)),
                title = manga.requiredString("title"),
                cover = manga.optionalString("thumbnailUrl")
                    ?.let(config::resolveServerResource)
                    ?.let { MangaResourceRequest(it, headers) }
            )
        }.distinctBy { it.ref }

        val next = if (payload.optionalBoolean("hasNextPage") == true) {
            (page + 1).toString()
        } else {
            null
        }
        return MangaResultPage(items = items, nextCursor = next)
    }

    private suspend fun resourceHeaders(): Map<String, String> =
        tokenProvider.accessToken()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { mapOf("Authorization" to "Bearer " + it) }
            .orEmpty()

    private fun requireOwned(ref: MangaRef) {
        require(ref.sourceId == descriptor.id) {
            "Suwayomi provider received an item owned by another source."
        }
    }

    private fun requireNoFilters(empty: Boolean) {
        if (!empty) {
            throw MangaSourceException.Unsupported(
                "Suwayomi positional source filters are not mapped in gateway protocol v1."
            )
        }
    }

    private companion object {
        val BROWSE_MUTATION = """
            mutation VeilSuwayomiBrowse(\${'$'}input: FetchSourceMangaInput!) {
              fetchSourceManga(input: \${'$'}input) {
                hasNextPage
                mangas {
                  id
                  sourceId
                  title
                  thumbnailUrl
                  initialized
                }
              }
            }
        """.trimIndent()

        val UPDATE_MUTATION = """
            mutation VeilSuwayomiUpdate(\${'$'}input: FetchMangaAndChaptersInput!) {
              fetchMangaAndChapters(input: \${'$'}input) {
                manga {
                  id
                  sourceId
                  title
                  thumbnailUrl
                  initialized
                  artist
                  author
                  description
                  genre
                  status
                  realUrl
                }
                chapters {
                  id
                  name
                  scanlator
                  realUrl
                  sourceOrder
                  chapterNumber
                  uploadDate
                }
              }
            }
        """.trimIndent()

        val PAGES_MUTATION = """
            mutation VeilSuwayomiPages(\${'$'}input: FetchChapterPagesInput!) {
              fetchChapterPages(input: \${'$'}input) {
                chapter {
                  id
                  pageCount
                }
                pages
              }
            }
        """.trimIndent()
    }
}

private fun JsonObject.toRemoteSource(): SuwayomiRemoteSource = SuwayomiRemoteSource(
    remoteId = requiredLong("id"),
    name = requiredString("name"),
    displayName = optionalString("displayName").orEmpty(),
    language = optionalString("lang").orEmpty(),
    supportsLatest = optionalBoolean("supportsLatest") == true,
    contentWarning = optionalString("contentWarning")
)

private fun pageFromCursor(cursor: String?): Int =
    cursor?.toIntOrNull()?.coerceAtLeast(1) ?: 1

private fun mangaKey(id: Int): String = "m:" + id
private fun chapterKey(id: Int): String = "c:" + id

private fun parseMangaKey(key: String): Int =
    key.removePrefix("m:").takeIf { key.startsWith("m:") }?.toIntOrNull()
        ?: throw MangaSourceException.SourceChanged("Invalid Suwayomi manga identity.")

private fun parseChapterKey(key: String): Int =
    key.removePrefix("c:").takeIf { key.startsWith("c:") }?.toIntOrNull()
        ?: throw MangaSourceException.SourceChanged("Invalid Suwayomi chapter identity.")

private fun mapStatus(raw: String?): MangaStatus = when (raw) {
    "ONGOING" -> MangaStatus.ONGOING
    "COMPLETED", "PUBLISHING_FINISHED" -> MangaStatus.COMPLETED
    "ON_HIATUS" -> MangaStatus.HIATUS
    "CANCELLED" -> MangaStatus.CANCELLED
    else -> MangaStatus.UNKNOWN
}

private fun JsonObject.requiredObject(name: String): JsonObject =
    this[name] as? JsonObject
        ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing object: " + name)

private fun JsonObject.optionalObject(name: String): JsonObject? =
    this[name] as? JsonObject

private fun JsonObject.requiredArray(name: String): JsonArray =
    this[name] as? JsonArray
        ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing array: " + name)

private fun JsonObject.optionalArray(name: String): JsonArray? =
    this[name] as? JsonArray

private fun JsonObject.requiredString(name: String): String =
    optionalString(name)?.takeIf(String::isNotBlank)
        ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing string: " + name)

private fun JsonObject.optionalString(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.optionalStringArray(name: String): List<String> =
    (this[name] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.distinct()
        .orEmpty()

private fun JsonObject.requiredInt(name: String): Int =
    (this[name] as? JsonPrimitive)?.intOrNull
        ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing integer: " + name)

private fun JsonObject.requiredLong(name: String): Long =
    (this[name] as? JsonPrimitive)?.longOrNull
        ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing long: " + name)

private fun JsonObject.optionalLong(name: String): Long? =
    (this[name] as? JsonPrimitive)?.longOrNull

private fun JsonObject.optionalDouble(name: String): Double? =
    (this[name] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()

private fun JsonObject.optionalBoolean(name: String): Boolean? =
    (this[name] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()