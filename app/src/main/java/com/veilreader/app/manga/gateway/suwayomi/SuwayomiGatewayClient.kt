package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaResourceRequest
import com.veilreader.app.manga.core.MangaSourceException
import com.veilreader.app.manga.net.MangaHttpClient
import java.net.URI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class SuwayomiGatewayConfig(
    val instanceId: String,
    val baseUrl: String,
    val allowCleartextHttp: Boolean = false
) {
    init {
        require(instanceId.matches(Regex("[A-Za-z0-9._-]{1,64}"))) {
            "Suwayomi instance id must be 1-64 characters using letters, numbers, '.', '_' or '-'."
        }

        val uri = URI(baseUrl)
        require(!uri.host.isNullOrBlank()) { "Suwayomi base URL must include a host." }
        require(uri.scheme == "https" || (allowCleartextHttp && uri.scheme == "http")) {
            "Suwayomi requires HTTPS unless cleartext HTTP is explicitly enabled."
        }
        require(uri.userInfo.isNullOrBlank()) {
            "Suwayomi credentials must not be embedded in the base URL."
        }
        require(uri.rawQuery.isNullOrBlank() && uri.rawFragment.isNullOrBlank()) {
            "Suwayomi base URL must not contain a query or fragment."
        }
    }

    val normalizedBaseUrl: String = baseUrl.trimEnd('/')
    val graphQlUrl: String = normalizedBaseUrl + "/api/graphql"
}

fun interface SuwayomiAuthProvider {
    suspend fun authorizationHeader(): String?
}

data class SuwayomiRemoteSource(
    val id: Long,
    val name: String,
    val displayName: String,
    val language: String,
    val supportsLatest: Boolean,
    val contentWarning: String,
    val homeUrl: String?
)

data class SuwayomiRemoteManga(
    val id: Int,
    val sourceId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val description: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val realUrl: String? = null
)

data class SuwayomiRemoteChapter(
    val id: Int,
    val mangaId: Int,
    val name: String,
    val chapterNumber: Double?,
    val scanlator: String?,
    val uploadDate: Long?,
    val sourceOrder: Int,
    val realUrl: String?
)

data class SuwayomiBrowsePage(
    val mangas: List<SuwayomiRemoteManga>,
    val hasNextPage: Boolean
)

data class SuwayomiUpdatePayload(
    val manga: SuwayomiRemoteManga?,
    val chapters: List<SuwayomiRemoteChapter>?
)

class SuwayomiGatewayClient(
    val config: SuwayomiGatewayConfig,
    private val http: MangaHttpClient,
    private val authProvider: SuwayomiAuthProvider = SuwayomiAuthProvider { null }
) {
    suspend fun sources(): List<SuwayomiRemoteSource> {
        val data = execute(
            operationName = "VeilSources",
            query = SOURCES_QUERY,
            variables = buildJsonObject { }
        )
        return data.requiredObject("sources")
            .requiredArray("nodes")
            .map { it.jsonObject.toRemoteSource() }
            .sortedWith(compareBy(SuwayomiRemoteSource::language, SuwayomiRemoteSource::displayName))
    }

    suspend fun browse(
        sourceId: Long,
        type: SuwayomiBrowseType,
        page: Int,
        query: String? = null
    ): SuwayomiBrowsePage {
        require(page > 0) { "Suwayomi browse page must be positive." }
        if (type == SuwayomiBrowseType.SEARCH) {
            require(!query.isNullOrBlank()) { "Suwayomi search query cannot be blank." }
        }

        val input = buildJsonObject {
            put("source", sourceId.toString())
            put("type", type.name)
            put("page", page)
            query?.takeIf(String::isNotBlank)?.let { put("query", it) }
        }
        val data = execute(
            operationName = "VeilFetchSourceManga",
            query = BROWSE_MUTATION,
            variables = buildJsonObject { put("input", input) }
        )
        val payload = data.requiredObject("fetchSourceManga")
        val mangas = payload.requiredArray("mangas").map { it.jsonObject.toRemoteManga() }
        if (mangas.any { it.sourceId != sourceId }) {
            throw MangaSourceException.SourceChanged(
                "Suwayomi returned manga from a different source."
            )
        }

        return SuwayomiBrowsePage(
            mangas = mangas,
            hasNextPage = payload.boolean("hasNextPage")
        )
    }

    suspend fun fetchUpdate(
        mangaId: Int,
        fetchManga: Boolean,
        fetchChapters: Boolean
    ): SuwayomiUpdatePayload {
        require(fetchManga || fetchChapters) {
            "Suwayomi update must request manga details, chapters, or both."
        }
        val variables = buildJsonObject {
            put("id", mangaId)
            put("fetchManga", fetchManga)
            put("fetchChapters", fetchChapters)
        }
        val data = execute(
            operationName = "VeilFetchMangaAndChapters",
            query = UPDATE_MUTATION,
            variables = variables
        )
        val payload = data.requiredObject("fetchMangaAndChapters")
        val manga = payload["manga"]
            ?.takeUnless { it.isJsonNull }
            ?.jsonObject
            ?.toRemoteManga()
        val chapters = payload["chapters"]
            ?.takeUnless { it.isJsonNull }
            ?.jsonArray
            ?.map { it.jsonObject.toRemoteChapter() }

        if (manga != null && manga.id != mangaId) {
            throw MangaSourceException.SourceChanged(
                "Suwayomi returned details for a different manga."
            )
        }
        if (chapters != null && chapters.any { it.mangaId != mangaId }) {
            throw MangaSourceException.SourceChanged(
                "Suwayomi returned chapters for a different manga."
            )
        }

        return SuwayomiUpdatePayload(manga = manga, chapters = chapters)
    }

    suspend fun pages(
        chapterId: Int,
        expectedMangaId: Int
    ): List<MangaResourceRequest> {
        val variables = buildJsonObject {
            put("input", buildJsonObject { put("chapterId", chapterId) })
        }
        val data = execute(
            operationName = "VeilFetchChapterPages",
            query = PAGES_MUTATION,
            variables = variables
        )
        val payload = data.requiredObject("fetchChapterPages")
        val returnedChapter = payload.requiredObject("chapter")
        val returnedChapterId = returnedChapter.int("id")
        val returnedMangaId = returnedChapter.int("mangaId")
        if (returnedChapterId != chapterId || returnedMangaId != expectedMangaId) {
            throw MangaSourceException.SourceChanged(
                "Suwayomi returned pages for a different manga/chapter."
            )
        }

        val headers = authHeaders()
        return payload.requiredArray("pages")
            .map { it.jsonPrimitive.content }
            .map { pageUrl ->
                MangaResourceRequest(
                    url = resolveResourceUrl(pageUrl),
                    headers = headers
                )
            }
    }

    suspend fun resourceRequest(pathOrUrl: String): MangaResourceRequest =
        MangaResourceRequest(
            url = resolveResourceUrl(pathOrUrl),
            headers = authHeaders()
        )

    private suspend fun execute(
        operationName: String,
        query: String,
        variables: JsonObject
    ): JsonObject {
        val body = buildJsonObject {
            put("operationName", operationName)
            put("query", query)
            put("variables", variables)
        }.toString()

        val response = http.postJson(
            url = config.graphQlUrl,
            headers = authHeaders(),
            json = body
        )

        val root = try {
            Json.parseToJsonElement(response.body).jsonObject
        } catch (error: Exception) {
            throw MangaSourceException.ParseFailure(
                message = "Suwayomi returned invalid GraphQL JSON.",
                cause = error
            )
        }

        val errors = root["errors"] as? JsonArray
        if (!errors.isNullOrEmpty()) {
            throw classifyGraphQlErrors(errors)
        }

        return root["data"]?.jsonObject
            ?: throw MangaSourceException.ParseFailure(
                "Suwayomi GraphQL response did not contain data."
            )
    }

    private suspend fun authHeaders(): Map<String, String> {
        val authorization = authProvider.authorizationHeader()?.trim().orEmpty()
        return buildMap {
            put("Accept", "application/json")
            if (authorization.isNotEmpty()) put("Authorization", authorization)
        }
    }

    private fun resolveResourceUrl(pathOrUrl: String): String {
        val resolved = runCatching {
            URI(config.normalizedBaseUrl + "/").resolve(pathOrUrl)
        }.getOrElse {
            throw MangaSourceException.Blocked("Suwayomi returned an invalid resource URL.")
        }

        val base = URI(config.normalizedBaseUrl)
        if (resolved.scheme != base.scheme || !resolved.host.equals(base.host, ignoreCase = true)) {
            throw MangaSourceException.Blocked(
                "Suwayomi resource escaped the configured server origin."
            )
        }
        if (resolved.effectivePort() != base.effectivePort()) {
            throw MangaSourceException.Blocked(
                "Suwayomi resource changed the configured server port."
            )
        }

        return resolved.toString()
    }

    private fun classifyGraphQlErrors(errors: JsonArray): MangaSourceException {
        val message = errors.joinToString(" | ") { element ->
            element.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown GraphQL error"
        }
        val lower = message.lowercase()
        return when {
            "unauthorizedexception" in lower ||
                "unauthorized" in lower ||
                "authentication" in lower ->
                MangaSourceException.AuthRequired("Suwayomi authentication is required.")

            "http 429" in lower ||
                "too many requests" in lower ||
                "rate limit" in lower ->
                MangaSourceException.RateLimited(message = "Suwayomi/source rate limit reached.")

            "not installed" in lower ||
                "not found" in lower ->
                MangaSourceException.NotFound(message)

            "cloudflare" in lower ||
                "forbidden" in lower ->
                MangaSourceException.Blocked(message)

            else -> MangaSourceException.NetworkFailure(
                message = "Suwayomi GraphQL operation failed: $message"
            )
        }
    }

    private fun JsonObject.toRemoteSource(): SuwayomiRemoteSource =
        SuwayomiRemoteSource(
            id = longString("id"),
            name = string("name"),
            displayName = string("displayName"),
            language = string("lang"),
            supportsLatest = boolean("supportsLatest"),
            contentWarning = string("contentWarning"),
            homeUrl = nullableString("homeUrl")
        )

    private fun JsonObject.toRemoteManga(): SuwayomiRemoteManga =
        SuwayomiRemoteManga(
            id = int("id"),
            sourceId = longString("sourceId"),
            title = string("title"),
            thumbnailUrl = nullableString("thumbnailUrl"),
            description = nullableString("description"),
            author = nullableString("author"),
            artist = nullableString("artist"),
            genres = arrayStrings("genre"),
            status = nullableString("status"),
            realUrl = nullableString("realUrl")
        )

    private fun JsonObject.toRemoteChapter(): SuwayomiRemoteChapter =
        SuwayomiRemoteChapter(
            id = int("id"),
            mangaId = int("mangaId"),
            name = string("name"),
            chapterNumber = this["chapterNumber"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toDoubleOrNull(),
            scanlator = nullableString("scanlator"),
            uploadDate = longStringOrNull("uploadDate")?.takeIf { it > 0L },
            sourceOrder = int("sourceOrder"),
            realUrl = nullableString("realUrl")
        )

    private fun JsonObject.requiredObject(key: String): JsonObject =
        this[key]
            ?.takeUnless { it.isJsonNull }
            ?.jsonObject
            ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing '$key'.")

    private fun JsonObject.requiredArray(key: String): JsonArray =
        this[key]?.jsonArray
            ?: throw MangaSourceException.ParseFailure("Suwayomi response is missing '$key'.")

    private fun JsonObject.string(key: String): String =
        nullableString(key)?.takeIf(String::isNotBlank)
            ?: throw MangaSourceException.ParseFailure("Suwayomi response has invalid '$key'.")

    private fun JsonObject.nullableString(key: String): String? =
        this[key]
            ?.takeUnless { it.isJsonNull }
            ?.jsonPrimitive
            ?.contentOrNull

    private fun JsonObject.int(key: String): Int =
        this[key]?.jsonPrimitive?.intOrNull
            ?: throw MangaSourceException.ParseFailure("Suwayomi response has invalid '$key'.")

    private fun JsonObject.longString(key: String): Long =
        longStringOrNull(key)
            ?: throw MangaSourceException.ParseFailure("Suwayomi response has invalid '$key'.")

    private fun JsonObject.longStringOrNull(key: String): Long? =
        this[key]
            ?.takeUnless { it.isJsonNull }
            ?.jsonPrimitive
            ?.contentOrNull
            ?.toLongOrNull()

    private fun JsonObject.boolean(key: String): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull
            ?: throw MangaSourceException.ParseFailure("Suwayomi response has invalid '$key'.")

    private fun JsonObject.arrayStrings(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()

    private companion object {
        const val SOURCES_QUERY = """
            query VeilSources {
              sources(first: 1000) {
                nodes {
                  id
                  name
                  displayName
                  lang
                  supportsLatest
                  contentWarning
                  homeUrl
                }
              }
            }
        """

        const val BROWSE_MUTATION = """
            mutation VeilFetchSourceManga($input: FetchSourceMangaInput!) {
              fetchSourceManga(input: $input) {
                hasNextPage
                mangas {
                  id
                  sourceId
                  title
                  thumbnailUrl
                }
              }
            }
        """

        const val UPDATE_MUTATION = """
            mutation VeilFetchMangaAndChapters(
              $id: Int!,
              $fetchManga: Boolean!,
              $fetchChapters: Boolean!
            ) {
              fetchMangaAndChapters(
                input: {
                  id: $id,
                  fetchManga: $fetchManga,
                  fetchChapters: $fetchChapters
                }
              ) {
                manga @include(if: $fetchManga) {
                  id
                  sourceId
                  title
                  thumbnailUrl
                  author
                  artist
                  description
                  genre
                  status
                  realUrl
                }
                chapters @include(if: $fetchChapters) {
                  id
                  mangaId
                  name
                  chapterNumber
                  scanlator
                  uploadDate
                  sourceOrder
                  realUrl
                }
              }
            }
        """

        const val PAGES_MUTATION = """
            mutation VeilFetchChapterPages($input: FetchChapterPagesInput!) {
              fetchChapterPages(input: $input) {
                chapter {
                  id
                  mangaId
                }
                pages
              }
            }
        """
    }
}

enum class SuwayomiBrowseType {
    SEARCH,
    POPULAR,
    LATEST
}

private val JsonElement.isJsonNull: Boolean
    get() = this is JsonNull

private fun URI.effectivePort(): Int = when {
    port >= 0 -> port
    scheme == "https" -> 443
    scheme == "http" -> 80
    else -> -1
}
