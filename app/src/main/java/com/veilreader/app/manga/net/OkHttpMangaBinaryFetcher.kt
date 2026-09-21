package com.veilreader.app.manga.net

import com.veilreader.app.manga.core.MangaResourceRequest
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class OkHttpMangaBinaryFetcher(
    private val client: OkHttpClient = OkHttpClient(),
    private val userAgent: String = "VeilReader/0.10.0"
) : com.veilreader.app.manga.storage.MangaBinaryFetcher {
    override suspend fun fetch(request: MangaResourceRequest): ByteArray =
        withContext(Dispatchers.IO) {
            require(request.url.startsWith("http://") || request.url.startsWith("https://")) {
                "Binary network fetcher only accepts http(s) manga resources."
            }

            val builder = Request.Builder()
                .url(request.url)
                .header("User-Agent", userAgent)
            request.headers.forEach { (name, value) -> builder.header(name, value) }

            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Manga image HTTP ${response.code} for ${request.url}")
                }
                response.body.bytes()
            }
        }
}