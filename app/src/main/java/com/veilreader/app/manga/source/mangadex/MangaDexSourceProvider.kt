package com.veilreader.app.manga.source.mangadex

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPageImage
import com.veilreader.app.manga.source.MangaPublicationStatus
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.PagedSourceResult
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import com.veilreader.app.manga.source.SourceSearchRequest
import java.io.IOException
import java.io.InterruptedIOException
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Read-only MangaDex API v5 adapter used by the first live-source controlled pilot.
 *
 * This provider is deliberately not registered in VeilApp yet. Production enablement requires a
 * fresh policy review and attribution UX because MangaDex's acceptable-use policy requires source
 * and scanlation-group credit and restricts monetized clients.
 */
class MangaDexSourceProvider internal constructor(
    private val transport: MangaDexHttpTransport,
    preferredLanguageTags: List<String> = listOf("en"),
    private val maxChapterCount: Int = 2_000,
    private val userAgent: String = "VeilReader-MangaDex-Pilot"
) : MangaSourceProvider {

    constructor(
        preferredLanguageTags: List<String> = listOf("en")
    ) : this(
        transport = OkHttpMangaDexTransport(),
        preferredLanguageTags = preferredLanguageTags
    )

    private val languages = preferredLanguageTags
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .ifEmpty { listOf("en") }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    override val descriptor = MangaSourceDescriptor(
        id = ID,
        displayName = "MangaDex",
        domains = listOf(API_DOMAIN),
        localeTags = languages.toSet(),
        contentTypes = setOf(
            MangaContentType.MANGA,
            MangaContentType.MANHWA,
            MangaContentType.MANHUA,
            MangaContentType.COMIC,
            MangaContentType.UNKNOWN
        )
    )

    override val capabilities = setOf(
        MangaSourceCapability.SEARCH,
        MangaSourceCapability.DETAILS,
        MangaSourceCapability.CHAPTERS,
        MangaSourceCapability.PAGES
    )

    override suspend fun search(
        request: SourceSearchRequest,
        context: SourceRequestContext
    ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> {
        if (context.domain !in descriptor.domains) return wrongDomain(context.domain)

        val offset = when (val token = request.continuationToken) {
            null -> 0
            else -> token.toIntOrNull()?.takeIf { it >= 0 }
                ?: return SourceOutcome.Failure(
                    SourceFailure(
                        SourceFailureKind.UNKNOWN,
                        "Invalid MangaDex continuation token"
                    )
                )
        }

        val url = apiUrl(context)
            .addPathSegment("manga")
            .addQueryParameter("title", request.query)
            .addQueryParameter("limit", request.limit.toString())
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("includes[]", "cover_art")
            .also(::addLanguageFilters)
            .also(::addConservativeContentRatings)
            .build()

        return requestJson(url) { root ->
            val items = root.array("data").map { element ->
                parseSummary(element as? JsonObject ?: payloadError("Manga entry is not an object"))
            }
            val total = root.int("total") ?: items.size
            val nextOffset = offset + items.size
            PagedSourceResult(
                items = items,
                nextContinuationToken = nextOffset
                    .takeIf { items.isNotEmpty() && it < total }
                    ?.toString()
            )
        }
    }

    override suspend fun details(
        manga: SourceMangaRef,
        context: SourceRequestContext
    ): SourceOutcome<SourceMangaDetails> {
        sourceMismatch(manga)?.let { return it }
        if (context.domain !in descriptor.domains) return wrongDomain(context.domain)

        val url = apiUrl(context)
            .addPathSegment("manga")
            .addPathSegment(manga.key)
            .addQueryParameter("includes[]", "cover_art")
            .addQueryParameter("includes[]", "author")
            .addQueryParameter("includes[]", "artist")
            .build()

        return requestJson(url) { root ->
            val data = root.obj("data") ?: payloadError("MangaDex details missing data")
            val summary = parseSummary(data)
            val attributes = data.obj("attributes") ?: payloadError("MangaDex details missing attributes")
            val relationships = data.array("relationships").mapNotNull { it as? JsonObject }

            val authors = relationships
                .filter { it.string("type") == "author" || it.string("type") == "artist" }
                .mapNotNull { it.obj("attributes")?.string("name") }
                .filter(String::isNotBlank)
                .toSet()

            val tags = attributes.array("tags")
                .mapNotNull { it as? JsonObject }
                .mapNotNull { it.obj("attributes")?.obj("name") }
                .mapNotNull(::localizedText)
                .filter(String::isNotBlank)
                .toSet()

            SourceMangaDetails(
                summary = summary,
                description = localizedText(attributes.obj("description")),
                authors = authors,
                status = publicationStatus(attributes.string("status")),
                tags = tags
            )
        }
    }

    override suspend fun chapters(
        manga: SourceMangaRef,
        context: SourceRequestContext
    ): SourceOutcome<List<SourceChapter>> {
        sourceMismatch(manga)?.let { return it }
        if (context.domain !in descriptor.domains) return wrongDomain(context.domain)

        val chapters = mutableListOf<SourceChapter>()
        var offset = 0

        while (true) {
            val url = apiUrl(context)
                .addPathSegment("manga")
                .addPathSegment(manga.key)
                .addPathSegment("feed")
                .addQueryParameter("limit", CHAPTER_PAGE_SIZE.toString())
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("includes[]", "scanlation_group")
                .addQueryParameter("order[volume]", "asc")
                .addQueryParameter("order[chapter]", "asc")
                .also(::addLanguageFilters)
                .also(::addConservativeContentRatings)
                .build()

            val page = requestJson(url) { root ->
                val data = root.array("data")
                ChapterPage(
                    total = root.int("total") ?: data.size,
                    receivedCount = data.size,
                    items = data.mapNotNull { element ->
                        parseChapter(
                            mangaKey = manga.key,
                            data = element as? JsonObject
                                ?: payloadError("MangaDex chapter is not an object")
                        )
                    }
                )
            }

            when (page) {
                is SourceOutcome.Failure -> return page
                is SourceOutcome.Success -> {
                    if (page.value.total > maxChapterCount) {
                        return SourceOutcome.Failure(
                            SourceFailure(
                                kind = SourceFailureKind.UNSUPPORTED,
                                message = "MangaDex chapter feed exceeds controlled-pilot safety bound"
                            )
                        )
                    }

                    chapters += page.value.items
                    offset += page.value.receivedCount
                    if (page.value.receivedCount == 0 || offset >= page.value.total) {
                        return SourceOutcome.Success(chapters)
                    }
                }
            }
        }
    }

    override suspend fun pages(
        chapter: SourceChapter,
        context: SourceRequestContext
    ): SourceOutcome<List<MangaPageImage>> {
        if (chapter.sourceId != ID) {
            return SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.NOT_FOUND, "Chapter belongs to another source")
            )
        }
        if (context.domain !in descriptor.domains) return wrongDomain(context.domain)

        val url = apiUrl(context)
            .addPathSegment("at-home")
            .addPathSegment("server")
            .addPathSegment(chapter.chapterKey)
            .build()

        return requestJson(url) { root ->
            val baseUrl = root.string("baseUrl")
                ?.toHttpUrlOrNull()
                ?.takeIf { it.isHttps }
                ?: payloadError("MangaDex at-home baseUrl is missing or insecure")

            val chapterObject = root.obj("chapter")
                ?: payloadError("MangaDex at-home response missing chapter")
            val hash = chapterObject.string("hash")
                ?.takeIf(String::isNotBlank)
                ?: payloadError("MangaDex at-home response missing hash")

            val original = chapterObject.array("data").mapNotNull(JsonPrimitive::contentOrNull)
            val dataSaver = chapterObject.array("dataSaver").mapNotNull(JsonPrimitive::contentOrNull)
            val filenames = original.ifEmpty { dataSaver }
            val pathSegment = if (original.isNotEmpty()) "data" else "data-saver"

            if (filenames.isEmpty()) payloadError("MangaDex at-home response has no pages")

            filenames.mapIndexed { index, fileName ->
                MangaPageImage(
                    index = index,
                    imageUrl = baseUrl.newBuilder()
                        .addPathSegment(pathSegment)
                        .addPathSegment(hash)
                        .addPathSegment(fileName)
                        .build()
                        .toString()
                )
            }
        }
    }

    override suspend fun resolvePublicUrl(
        url: String,
        context: SourceRequestContext
    ): SourceOutcome<SourceMangaRef?> {
        if (context.domain !in descriptor.domains) return wrongDomain(context.domain)

        val uri = runCatching { URI(url) }.getOrNull()
            ?: return SourceOutcome.Success(null)
        val host = uri.host?.lowercase() ?: return SourceOutcome.Success(null)
        if (uri.scheme?.lowercase() != "https") return SourceOutcome.Success(null)
        if (host != "mangadex.org" && host != "www.mangadex.org") {
            return SourceOutcome.Success(null)
        }

        val segments = uri.path.orEmpty().trim('/').split('/')
        if (segments.size < 2 || segments[0] != "title") return SourceOutcome.Success(null)
        val id = segments[1]
        if (!MANGADEX_UUID.matches(id)) return SourceOutcome.Success(null)

        return SourceOutcome.Success(
            SourceMangaRef(
                sourceId = ID,
                key = id.lowercase(),
                publicUrl = "https://mangadex.org/title/" + id.lowercase()
            )
        )
    }

    private suspend fun <T> requestJson(
        url: HttpUrl,
        parser: (JsonObject) -> T
    ): SourceOutcome<T> {
        val response = try {
            transport.get(
                url = url.toString(),
                headers = mapOf(
                    "Accept" to "application/json",
                    "User-Agent" to userAgent
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (timeout: InterruptedIOException) {
            return SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.TIMEOUT, "MangaDex request timed out", cause = timeout)
            )
        } catch (io: IOException) {
            return SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.NETWORK, "MangaDex network request failed", cause = io)
            )
        }

        if (response.statusCode !in 200..299) {
            return SourceOutcome.Failure(httpFailure(response))
        }

        return try {
            val root = json.parseToJsonElement(response.body) as? JsonObject
                ?: payloadError("MangaDex response root is not an object")
            val result = root.string("result")
            if (result != null && result != "ok") {
                payloadError("MangaDex response result is not ok")
            }
            SourceOutcome.Success(parser(root))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: PayloadException) {
            SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.PARSE_CHANGED, error.message ?: "MangaDex payload changed")
            )
        } catch (error: SerializationException) {
            SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.PARSE_CHANGED, "MangaDex JSON parsing failed", cause = error)
            )
        } catch (error: DateTimeParseException) {
            SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.PARSE_CHANGED, "MangaDex timestamp parsing failed", cause = error)
            )
        } catch (error: IllegalArgumentException) {
            SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.PARSE_CHANGED, "MangaDex payload validation failed", cause = error)
            )
        }
    }

    private fun parseSummary(data: JsonObject): SourceMangaSummary {
        val id = data.string("id")?.takeIf(MANGADEX_UUID::matches)
            ?: payloadError("MangaDex manga id is missing or invalid")
        val attributes = data.obj("attributes")
            ?: payloadError("MangaDex manga attributes are missing")
        val title = localizedText(attributes.obj("title"))
            ?: payloadError("MangaDex manga title is missing")

        val alternatives = attributes.array("altTitles")
            .mapNotNull { it as? JsonObject }
            .flatMap { alternative ->
                alternative.values.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            }
            .filter(String::isNotBlank)
            .toSet()

        val coverFile = data.array("relationships")
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.string("type") == "cover_art" }
            ?.obj("attributes")
            ?.string("fileName")

        return SourceMangaSummary(
            ref = SourceMangaRef(
                sourceId = ID,
                key = id.lowercase(),
                publicUrl = "https://mangadex.org/title/" + id.lowercase()
            ),
            title = title,
            alternativeTitles = alternatives,
            coverUrl = coverFile?.let { coverUrl(id.lowercase(), it) },
            languageTag = attributes.string("originalLanguage"),
            contentType = contentType(attributes.string("originalLanguage"))
        )
    }

    private fun parseChapter(
        mangaKey: String,
        data: JsonObject
    ): SourceChapter? {
        val id = data.string("id")?.takeIf(MANGADEX_UUID::matches)
            ?: payloadError("MangaDex chapter id is missing or invalid")
        val attributes = data.obj("attributes")
            ?: payloadError("MangaDex chapter attributes are missing")
        if (!attributes.string("externalUrl").isNullOrBlank()) {
            return null
        }

        val scanlator = data.array("relationships")
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.string("type") == "scanlation_group" }
            ?.obj("attributes")
            ?.string("name")

        return SourceChapter(
            sourceId = ID,
            mangaKey = mangaKey,
            chapterKey = id.lowercase(),
            title = attributes.string("title"),
            number = attributes.string("chapter")?.toDoubleOrNull(),
            volume = attributes.string("volume")?.toDoubleOrNull(),
            languageTag = attributes.string("translatedLanguage"),
            scanlator = scanlator,
            publishedAtEpochMs = attributes.string("publishAt")
                ?.let { Instant.parse(it).toEpochMilli() }
        )
    }

    private fun localizedText(values: JsonObject?): String? {
        if (values == null) return null

        val preferredKeys = buildList {
            languages.forEach { language ->
                add(language)
                language.substringBefore('-').takeIf { it != language }?.let(::add)
            }
            add("en")
        }.distinct()

        preferredKeys.forEach { key ->
            values.string(key)?.takeIf(String::isNotBlank)?.let { return it }
        }

        return values.values
            .asSequence()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .firstOrNull(String::isNotBlank)
    }

    private fun coverUrl(
        mangaId: String,
        fileName: String
    ): String =
        "https://uploads.mangadex.org".toHttpUrl()
            .newBuilder()
            .addPathSegment("covers")
            .addPathSegment(mangaId)
            .addPathSegment(fileName + ".256.jpg")
            .build()
            .toString()

    private fun addLanguageFilters(builder: HttpUrl.Builder) {
        languages.forEach { builder.addQueryParameter("availableTranslatedLanguage[]", it) }
    }

    private fun addConservativeContentRatings(builder: HttpUrl.Builder) {
        builder.addQueryParameter("contentRating[]", "safe")
        builder.addQueryParameter("contentRating[]", "suggestive")
    }

    private fun apiUrl(context: SourceRequestContext): HttpUrl.Builder =
        ("https://" + context.domain).toHttpUrl().newBuilder()

    private fun httpFailure(response: MangaDexHttpResponse): SourceFailure {
        val kind = when (response.statusCode) {
            401 -> SourceFailureKind.AUTH_REQUIRED
            403 -> SourceFailureKind.BLOCKED
            404 -> SourceFailureKind.NOT_FOUND
            429 -> SourceFailureKind.RATE_LIMITED
            in 500..599 -> SourceFailureKind.NETWORK
            else -> SourceFailureKind.UNKNOWN
        }
        val retryAfterMillis = if (response.statusCode == 429) {
            response.header("Retry-After")
                ?.trim()
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?.times(1_000L)
        } else {
            null
        }

        return SourceFailure(
            kind = kind,
            message = "MangaDex HTTP " + response.statusCode,
            retryAfterMillis = retryAfterMillis
        )
    }

    private fun sourceMismatch(manga: SourceMangaRef): SourceOutcome.Failure? =
        if (manga.sourceId == ID) {
            null
        } else {
            SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.NOT_FOUND, "Manga belongs to another source")
            )
        }

    private fun wrongDomain(domain: String): SourceOutcome.Failure =
        SourceOutcome.Failure(
            SourceFailure(
                SourceFailureKind.BLOCKED,
                "MangaDex request domain is not allowlisted: " + domain
            )
        )

    private fun contentType(language: String?): MangaContentType = when {
        language == "ja" -> MangaContentType.MANGA
        language == "ko" -> MangaContentType.MANHWA
        language?.startsWith("zh") == true -> MangaContentType.MANHUA
        language == null -> MangaContentType.UNKNOWN
        else -> MangaContentType.COMIC
    }

    private fun publicationStatus(value: String?): MangaPublicationStatus = when (value) {
        "ongoing" -> MangaPublicationStatus.ONGOING
        "completed" -> MangaPublicationStatus.COMPLETED
        "hiatus" -> MangaPublicationStatus.HIATUS
        "cancelled" -> MangaPublicationStatus.CANCELLED
        else -> MangaPublicationStatus.UNKNOWN
    }

    private data class ChapterPage(
        val total: Int,
        val receivedCount: Int,
        val items: List<SourceChapter>
    )

    private class PayloadException(message: String) : RuntimeException(message)

    private fun payloadError(message: String): Nothing = throw PayloadException(message)

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? =
        string(key)?.toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonObject.array(key: String): JsonArray =
        this[key] as? JsonArray ?: JsonArray(emptyList())

    companion object {
        val ID = SourceId("mangadex")
        const val API_DOMAIN = "api.mangadex.org"

        private const val CHAPTER_PAGE_SIZE = 100
        private val MANGADEX_UUID = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
        )
    }
}
