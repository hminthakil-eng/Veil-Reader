package com.veilreader.app.manga.sources.mangadex

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
import com.veilreader.app.manga.net.MangaHttpClient
import java.net.URLEncoder
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MangaDexSourceProvider(
    private val http: MangaHttpClient,
    private val translatedLanguage: String = "en",
    private val preferDataSaver: Boolean = false
) : MangaSourceProvider {
    override val descriptor = MangaSourceDescriptor(
        id = MangaSourceId("mangadex.$translatedLanguage"),
        providerId = MangaProviderId("mangadex"),
        version = 1,
        name = "MangaDex",
        language = translatedLanguage,
        capabilities = setOf(
            MangaSourceCapability.SEARCH,
            MangaSourceCapability.POPULAR,
            MangaSourceCapability.LATEST
        )
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> {
        requireNoFilters(request.filters.isEmpty())
        return browse(
            cursor = request.cursor,
            extra = listOf(
                "title" to request.query,
                "order[relevance]" to "desc"
            )
        )
    }

    override suspend fun popular(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        requireNoFilters(request.filters.isEmpty())
        return browse(request.cursor, listOf("order[followedCount]" to "desc"))
    }

    override suspend fun latest(request: MangaBrowseRequest): MangaResultPage<MangaSummary> {
        requireNoFilters(request.filters.isEmpty())
        return browse(request.cursor, listOf("order[latestUploadedChapter]" to "desc"))
    }

    override suspend fun fetchUpdate(
        ref: MangaRef,
        existingChapters: List<MangaChapter>,
        options: MangaUpdateOptions
    ): MangaUpdate {
        requireOwned(ref)
        return MangaUpdate(
            ref = ref,
            details = if (options.fetchDetails) fetchDetails(ref) else null,
            chapters = if (options.fetchChapters) fetchChapters(ref) else null
        )
    }

    override suspend fun pages(ref: MangaChapterRef): List<MangaPage> {
        requireOwned(ref.manga)
        val payload = root(
            http.get(api("/at-home/server/${ref.key}", emptyList()), emptyMap()).body
        )
        val baseUrl = payload.string("baseUrl")
        val chapter = payload["chapter"]?.jsonObject
            ?: throw MangaSourceException.SourceChanged("MangaDex chapter payload is missing.")
        val hash = chapter.string("hash")
        val key = if (preferDataSaver) "dataSaver" else "data"
        val folder = if (preferDataSaver) "data-saver" else "data"
        val files = chapter[key]?.jsonArray ?: JsonArray(emptyList())

        return files.mapIndexed { index, element ->
            MangaPage(
                index = index,
                image = MangaResourceRequest(
                    url = "$baseUrl/$folder/$hash/${element.jsonPrimitive.content}"
                )
            )
        }
    }

    private suspend fun fetchDetails(ref: MangaRef): MangaDetails {
        val url = api(
            "/manga/${ref.key}",
            listOf(
                "includes[]" to "cover_art",
                "includes[]" to "author",
                "includes[]" to "artist"
            )
        )
        val item = root(http.get(url, emptyMap()).body)["data"]?.jsonObject
            ?: throw MangaSourceException.SourceChanged("MangaDex details payload is missing data.")
        val attributes = item["attributes"]?.jsonObject
            ?: throw MangaSourceException.SourceChanged("MangaDex details payload is missing attributes.")
        val relationships = item["relationships"]?.jsonArray ?: JsonArray(emptyList())

        return MangaDetails(
            ref = ref,
            title = localized(attributes["title"]?.jsonObject),
            description = localized(attributes["description"]?.jsonObject),
            authors = relationships
                .filter { relationshipType(it) == "author" || relationshipType(it) == "artist" }
                .mapNotNull { relationshipName(it) }
                .distinct(),
            tags = attributes["tags"]?.jsonArray.orEmptyElements()
                .mapNotNull { tag ->
                    tag.jsonObject["attributes"]?.jsonObject
                        ?.get("name")?.jsonObject
                        ?.let(::localized)
                        ?.takeIf(String::isNotBlank)
                },
            status = parseStatus(attributes["status"]?.jsonPrimitive?.contentOrNull),
            cover = coverRequest(ref.key, relationships)
        )
    }

    private suspend fun fetchChapters(ref: MangaRef): List<MangaChapter> {
        val chapters = mutableListOf<MangaChapter>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (offset < total) {
            val url = api(
                "/manga/${ref.key}/feed",
                listOf(
                    "limit" to CHAPTER_PAGE_SIZE.toString(),
                    "offset" to offset.toString(),
                    "translatedLanguage[]" to translatedLanguage,
                    "includes[]" to "scanlation_group",
                    "order[volume]" to "asc",
                    "order[chapter]" to "asc"
                )
            )
            val payload = root(http.get(url, emptyMap()).body)
            val data = payload["data"]?.jsonArray ?: JsonArray(emptyList())
            total = payload["total"]?.jsonPrimitive?.intOrNull ?: data.size

            chapters += data.mapNotNull { element ->
                val item = element.jsonObject
                val attributes = item["attributes"]?.jsonObject
                    ?: throw MangaSourceException.SourceChanged(
                        "MangaDex chapter payload is missing attributes."
                    )
                if (attributes.stringOrNull("externalUrl") != null) {
                    return@mapNotNull null
                }

                val chapterRef = MangaChapterRef(ref, item.string("id"))
                MangaChapter(
                    ref = chapterRef,
                    title = attributes.stringOrNull("title").orEmpty(),
                    chapterNumber = attributes.stringOrNull("chapter")?.toDoubleOrNull(),
                    volumeNumber = attributes.stringOrNull("volume")?.toDoubleOrNull(),
                    publishedAtEpochMs = attributes.stringOrNull("publishAt")
                        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
                    scanlator = item["relationships"]?.jsonArray
                        ?.firstOrNull { relationshipType(it) == "scanlation_group" }
                        ?.let(::relationshipName)
                )
            }

            if (data.isEmpty()) break
            offset += data.size
        }

        return chapters
    }

    private suspend fun browse(
        cursor: String?,
        extra: List<Pair<String, String>>
    ): MangaResultPage<MangaSummary> {
        val offset = cursor?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val url = api(
            "/manga",
            listOf(
                "limit" to SEARCH_PAGE_SIZE.toString(),
                "offset" to offset.toString(),
                "includes[]" to "cover_art",
                "availableTranslatedLanguage[]" to translatedLanguage
            ) + extra
        )
        val payload = root(http.get(url, emptyMap()).body)
        val data = payload["data"]?.jsonArray ?: JsonArray(emptyList())
        val total = payload["total"]?.jsonPrimitive?.intOrNull ?: data.size
        val items = data.map { parseSummary(it.jsonObject) }
        val nextOffset = offset + data.size

        return MangaResultPage(
            items = items,
            nextCursor = nextOffset.takeIf { data.isNotEmpty() && it < total }?.toString()
        )
    }

    private fun parseSummary(item: JsonObject): MangaSummary {
        val id = item.string("id")
        val attributes = item["attributes"]?.jsonObject
            ?: throw MangaSourceException.SourceChanged("MangaDex result is missing attributes.")
        val relationships = item["relationships"]?.jsonArray ?: JsonArray(emptyList())
        return MangaSummary(
            ref = MangaRef(descriptor.id, id),
            title = localized(attributes["title"]?.jsonObject),
            cover = coverRequest(id, relationships)
        )
    }

    private fun coverRequest(
        mangaId: String,
        relationships: JsonArray
    ): MangaResourceRequest? {
        val fileName = relationships
            .firstOrNull { relationshipType(it) == "cover_art" }
            ?.jsonObject
            ?.get("attributes")?.jsonObject
            ?.stringOrNull("fileName")
            ?: return null

        return MangaResourceRequest(
            "https://uploads.mangadex.org/covers/$mangaId/$fileName.256.jpg"
        )
    }

    private fun relationshipType(element: JsonElement): String? =
        element.jsonObject.stringOrNull("type")

    private fun relationshipName(element: JsonElement): String? =
        element.jsonObject["attributes"]?.jsonObject?.stringOrNull("name")

    private fun localized(values: JsonObject?): String {
        if (values == null || values.isEmpty()) return ""
        return values[translatedLanguage]?.jsonPrimitive?.contentOrNull
            ?: values["en"]?.jsonPrimitive?.contentOrNull
            ?: values.values.firstNotNullOfOrNull { it.jsonPrimitive.contentOrNull }
            ?: ""
    }

    private fun parseStatus(value: String?): MangaStatus = when (value) {
        "ongoing" -> MangaStatus.ONGOING
        "completed" -> MangaStatus.COMPLETED
        "hiatus" -> MangaStatus.HIATUS
        "cancelled" -> MangaStatus.CANCELLED
        else -> MangaStatus.UNKNOWN
    }

    private fun requireOwned(ref: MangaRef) {
        require(ref.sourceId == descriptor.id) {
            "MangaDex adapter received a foreign source id."
        }
    }

    private fun requireNoFilters(empty: Boolean) {
        if (!empty) {
            throw MangaSourceException.Unsupported(
                "MangaDex filter mapping is not enabled in this adapter version."
            )
        }
    }

    private fun root(body: String): JsonObject = try {
        json.parseToJsonElement(body).jsonObject
    } catch (error: Exception) {
        throw MangaSourceException.ParseFailure(
            message = "MangaDex response could not be parsed.",
            cause = error
        )
    }

    private fun api(path: String, params: List<Pair<String, String>>): String {
        if (params.isEmpty()) return API_BASE + path
        return API_BASE + path + "?" + params.joinToString("&") { (key, value) ->
            encode(key) + "=" + encode(value)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun JsonObject.string(key: String): String =
        stringOrNull(key) ?: throw MangaSourceException.SourceChanged(
            "Missing MangaDex field: $key"
        )

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.jsonPrimitive?.contentOrNull

    private fun JsonArray?.orEmptyElements(): List<JsonElement> = this?.toList().orEmpty()

    private companion object {
        const val API_BASE = "https://api.mangadex.org"
        const val SEARCH_PAGE_SIZE = 20
        const val CHAPTER_PAGE_SIZE = 100
    }
}