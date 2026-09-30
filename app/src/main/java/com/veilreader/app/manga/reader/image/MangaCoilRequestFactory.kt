package com.veilreader.app.manga.reader.image

import android.content.Context
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowPartialImage
import coil3.request.maxBitmapSize
import coil3.request.addLastModifiedToFileCacheKey
import coil3.size.Precision
import com.veilreader.app.manga.reader.MangaReaderMode

object MangaCoilRequestFactory {

    fun build(
        context: Context,
        page: MangaResolvedPage,
        mode: MangaReaderMode
    ): ImageRequest {
        val builder = ImageRequest.Builder(context)
            .precision(Precision.INEXACT)
            .allowPartialImage(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .maxBitmapSize(MangaImageDecodePolicy.maxBitmapSize(mode))

        when (page) {
            is MangaResolvedPage.Local -> {
                builder
                    .data(page.file)
                    .networkCachePolicy(CachePolicy.DISABLED)
                    .addLastModifiedToFileCacheKey(true)
            }

            is MangaResolvedPage.Remote -> {
                val headers = NetworkHeaders.Builder().apply {
                    page.headers.forEach { (name, value) ->
                        set(name, value)
                    }
                }.build()

                builder
                    .data(page.url)
                    .httpHeaders(headers)
                    .networkCachePolicy(CachePolicy.ENABLED)
            }
        }

        return builder.build()
    }
}
