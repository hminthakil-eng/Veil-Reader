package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaSourceException
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.OkHttpMangaHttpClient
import com.veilreader.app.manga.net.PacedMangaHttpClient
import java.net.URI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@JvmInline
value class SuwayomiServerId(val value: String) {
    init {
        require(value.matches(Regex("[A-Za-z0-9._-]{1,64}"))) {
            "Suwayomi server id must be 1-64 URL-safe identifier characters."
        }
    }
}

data class SuwayomiServerConfig(
    val serverId: SuwayomiServerId,
    val origin: String,
    val allowInsecureHttp: Boolean = false
) {
    val normalizedOrigin: String
    val graphQlUrl: String

    init {
        val uri = URI(origin.trim())
        require(!uri.host.isNullOrBlank()) { "Suwayomi origin must have a host." }
        require(uri.userInfo == null) { "Credentials must not be embedded in the Suwayomi URL." }
        require(uri.query == null && uri.fragment == null) {
            "Suwayomi origin must not contain query or fragment components."
        }
        val scheme = uri.scheme.lowercase()
        require(scheme == "https" || (allowInsecureHttp && scheme == "http")) {
            "Suwayomi requires HTTPS unless insecure HTTP is explicitly enabled."
        }
        normalizedOrigin = scheme + "://" + uri.rawAuthority + uri.path.trimEnd('/')
        graphQlUrl = normalizedOrigin + "/api/graphql"
    }
}

fun interface SuwayomiAccessTokenProvider {
    suspend fun accessToken(): String?
}

data class SuwayomiGraphQlRequest(
    val operationName: String,
    val query: String,
    val variables: JsonObject = JsonObject(emptyMap())
)

fun interface SuwayomiGraphQlTransport {
    suspend fun execute(request: SuwayomiGraphQlRequest): JsonObject
}

class OkHttpSuwayomiGraphQlTransport(
    private val config: SuwayomiServerConfig,
    private val tokenProvider: SuwayomiAccessTokenProvider = SuwayomiAccessTokenProvider { null },
    private val http: MangaHttpClient = PacedMangaHttpClient(
        OkHttpMangaHttpClient(),
        requestsPerSecond = 4
    ),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SuwayomiGraphQlTransport {
    override suspend fun execute(request: SuwayomiGraphQlRequest): JsonObject {
        val payload = buildJsonObject {
            put("operationName", request.operationName)
            put("query", request.query)
            put("variables", request.variables)
        }.toString()

        val headers = buildMap {
            put("Accept", "application/json")
            tokenProvider.accessToken()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { put("Authorization", "Bearer $it") }
        }

        val body = http.postJson(
            url = config.graphQlUrl,
            headers = headers,
            json = payload
        ).body

        val root = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrElse { error ->
                throw MangaSourceException.ParseFailure(
                    "Suwayomi returned invalid GraphQL JSON.",
                    error
                )
            }

        val errors = root["errors"]?.jsonArray.orEmpty()
        if (errors.isNotEmpty()) {
            throw suwayomiGraphQlFailure(errors)
        }

        return root["data"]?.jsonObject
            ?: throw MangaSourceException.ParseFailure(
                "Suwayomi GraphQL response did not contain data."
            )
    }

    private fun suwayomiGraphQlFailure(errors: List<JsonElement>): MangaSourceException {
        val messages = errors.mapNotNull { element ->
            element.jsonObject["message"]?.jsonPrimitive?.content
        }
        val combined = messages.joinToString(" | ").ifBlank { "Unknown GraphQL error." }
        val normalized = combined.lowercase()

        return when {
            "unauthorizedexception" in normalized ||
                "unauthorized" in normalized ||
                "authentication" in normalized ->
                MangaSourceException.AuthRequired("Suwayomi authentication is required.")

            "429" in normalized || "too many requests" in normalized ->
                MangaSourceException.RateLimited(message = combined)

            "not found" in normalized ->
                MangaSourceException.NotFound(combined)

            "does not support latest" in normalized ->
                MangaSourceException.Unsupported(combined)

            else -> MangaSourceException.NetworkFailure(
                message = "Suwayomi GraphQL error: $combined"
            )
        }
    }
}

internal fun SuwayomiServerConfig.resolveServerResource(raw: String): String? {
    if (raw.isBlank()) return null
    val base = URI(normalizedOrigin + "/")
    val resolved = runCatching { base.resolve(raw) }.getOrNull() ?: return null
    if (resolved.scheme != "https" && resolved.scheme != "http") return null
    if (!resolved.scheme.equals(base.scheme, ignoreCase = true)) return null
    if (!resolved.host.equals(base.host, ignoreCase = true)) return null
    if (resolved.port != base.port) return null
    return resolved.toString()
}
