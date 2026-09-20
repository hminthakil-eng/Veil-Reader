package com.veilreader.app.manga.net

import com.veilreader.app.manga.core.MangaSourceException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class MangaHttpResponse(
    val code: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)

fun interface MangaHttpClient {
    suspend fun get(
        url: String,
        headers: Map<String, String>
    ): MangaHttpResponse
}

class OkHttpMangaHttpClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val userAgent: String = "VeilReader/0.10.0"
) : MangaHttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>
    ): MangaHttpResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)

        headers.forEach { (name, value) -> builder.header(name, value) }

        try {
            client.newCall(builder.build()).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw mangaHttpFailure(
                        statusCode = response.code,
                        retryAfterHeader = response.header("Retry-After"),
                        url = url
                    )
                }

                MangaHttpResponse(
                    code = response.code,
                    body = body,
                    headers = response.headers.names().associateWith { name ->
                        response.header(name).orEmpty()
                    }
                )
            }
        } catch (error: MangaSourceException) {
            throw error
        } catch (error: IOException) {
            throw MangaSourceException.NetworkFailure(
                message = "Manga network request failed for $url",
                cause = error
            )
        }
    }
}

internal fun mangaHttpFailure(
    statusCode: Int,
    retryAfterHeader: String?,
    url: String
): MangaSourceException = when (statusCode) {
    401 -> MangaSourceException.AuthRequired("Manga source requires authentication: $url")
    403 -> MangaSourceException.Blocked("Manga source blocked the request: $url")
    404 -> MangaSourceException.NotFound("Manga source item was not found: $url")
    429 -> MangaSourceException.RateLimited(
        retryAfterMillis = retryAfterHeader?.toLongOrNull()?.times(1_000L),
        message = "Manga source rate limit reached: $url"
    )
    in 500..599 -> MangaSourceException.TemporarilyUnavailable(
        "Manga source is temporarily unavailable (HTTP $statusCode): $url"
    )
    else -> MangaSourceException.NetworkFailure(
        statusCode = statusCode,
        message = "Manga HTTP $statusCode for $url"
    )
}

class PacedMangaHttpClient(
    private val delegate: MangaHttpClient,
    requestsPerSecond: Int = 4
) : MangaHttpClient {
    private val mutex = Mutex()
    private val minimumIntervalMs = 1000L / requestsPerSecond.coerceIn(1, 4)
    private var nextAllowedAtNanos = 0L

    override suspend fun get(
        url: String,
        headers: Map<String, String>
    ): MangaHttpResponse = mutex.withLock {
        val now = System.nanoTime()
        val waitNanos = nextAllowedAtNanos - now
        if (waitNanos > 0) {
            delay((waitNanos + 999_999L) / 1_000_000L)
        }

        val result = delegate.get(url, headers)
        nextAllowedAtNanos = System.nanoTime() + minimumIntervalMs * 1_000_000L
        result
    }
}