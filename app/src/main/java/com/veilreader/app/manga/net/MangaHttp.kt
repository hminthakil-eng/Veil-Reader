package com.veilreader.app.manga.net

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

        client.newCall(builder.build()).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Manga HTTP ${response.code} for $url")
            }

            MangaHttpResponse(
                code = response.code,
                body = body,
                headers = response.headers.names().associateWith { name ->
                    response.header(name).orEmpty()
                }
            )
        }
    }
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