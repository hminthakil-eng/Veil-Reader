package com.veilreader.app.manga.source.mangadex

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal data class MangaDexHttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Headers
) {
    fun header(name: String): String? = headers[name]
}

internal fun interface MangaDexHttpTransport {
    suspend fun get(
        url: String,
        headers: Map<String, String>
    ): MangaDexHttpResponse
}

internal class OkHttpMangaDexTransport(
    private val client: OkHttpClient = defaultClient(),
    private val minimumRequestIntervalMillis: Long = 250L,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val sleeper: suspend (Long) -> Unit = { delay(it) }
) : MangaDexHttpTransport {

    init {
        require(minimumRequestIntervalMillis >= 0L)
    }

    private val rateGate = Mutex()
    private var nextRequestAtMillis: Long = 0L

    override suspend fun get(
        url: String,
        headers: Map<String, String>
    ): MangaDexHttpResponse {
        rateGate.withLock {
            val waitMillis = (nextRequestAtMillis - nowMillis()).coerceAtLeast(0L)
            if (waitMillis > 0L) sleeper(waitMillis)
            nextRequestAtMillis = nowMillis() + minimumRequestIntervalMillis
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        return client.await(requestBuilder.build())
    }

    private suspend fun OkHttpClient.await(request: Request): MangaDexHttpResponse =
        suspendCancellableCoroutine { continuation ->
            val call = newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val result = MangaDexHttpResponse(
                                statusCode = response.code,
                                body = response.body.string(),
                                headers = response.headers
                            )
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }
                    }
                }
            )
        }

    private companion object {
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .callTimeout(20L, TimeUnit.SECONDS)
                .connectTimeout(10L, TimeUnit.SECONDS)
                .readTimeout(20L, TimeUnit.SECONDS)
                .build()
    }
}
