package com.veilreader.app.manga.reader.image

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MangaImageDimensionProbe {
    suspend fun probe(page: MangaResolvedPage): MangaImageDimensions?
}

/**
 * No second network fetch is performed only to discover dimensions.
 * Remote assets return null and stay on the bounded Coil preview path until they are localized.
 */
class AndroidMangaImageDimensionProbe : MangaImageDimensionProbe {

    override suspend fun probe(page: MangaResolvedPage): MangaImageDimensions? =
        when (page) {
            is MangaResolvedPage.Remote -> null
            is MangaResolvedPage.Local -> withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(page.file.absolutePath, options)
                val width = options.outWidth
                val height = options.outHeight
                if (width > 0 && height > 0) {
                    MangaImageDimensions(width, height)
                } else {
                    null
                }
            }
        }
}
