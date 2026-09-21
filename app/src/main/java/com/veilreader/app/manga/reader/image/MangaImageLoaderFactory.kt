package com.veilreader.app.manga.reader.image

import android.content.Context
import coil3.ImageLoader
import coil3.bitmapFactoryMaxParallelism
import coil3.memoryCacheMaxSizePercentWhileInBackground
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.allowPartialImage
import coil3.size.Precision
import coil3.size.Size
import com.veilreader.app.manga.reader.MangaReaderMode

object MangaImageLoaderFactory {

    /**
     * Dedicated reader loader:
     * - no second persistent disk cache; Veil's explicit Manga offline store owns persistence;
     * - bounded memory cache;
     * - partial/corrupt image decode disabled;
     * - only two bitmap decodes may run in parallel to reduce OOM spikes.
     */
    fun create(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(appContext, 0.15)
                    .build()
            }
            .memoryCacheMaxSizePercentWhileInBackground(0.25)
            .diskCache(null)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .precision(Precision.INEXACT)
            .allowPartialImage(false)
            .bitmapFactoryMaxParallelism(2)
            .build()
    }
}

object MangaImageDecodePolicy {
    private const val PAGED_MAX_WIDTH = 4_096
    private const val PAGED_MAX_HEIGHT = 4_096
    private const val WEBTOON_MAX_WIDTH = 4_096
    private const val WEBTOON_MAX_HEIGHT = 8_192

    fun maxBitmapSize(mode: MangaReaderMode): Size = when (mode) {
        MangaReaderMode.PAGED -> Size(PAGED_MAX_WIDTH, PAGED_MAX_HEIGHT)
        MangaReaderMode.WEBTOON -> Size(WEBTOON_MAX_WIDTH, WEBTOON_MAX_HEIGHT)
    }
}
