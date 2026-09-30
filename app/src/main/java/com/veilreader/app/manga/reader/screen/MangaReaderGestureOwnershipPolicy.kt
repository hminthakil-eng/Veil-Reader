package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.reader.image.MangaImageDeliveryStrategy
import com.veilreader.app.manga.reader.ui.MangaReaderGestureOwner

object MangaReaderGestureOwnershipPolicy {
    fun ownerFor(strategy: MangaImageDeliveryStrategy): MangaReaderGestureOwner =
        when (strategy) {
            MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING ->
                MangaReaderGestureOwner.PAGE_RENDERER

            MangaImageDeliveryStrategy.STANDARD_COIL,
            MangaImageDeliveryStrategy.REMOTE_BOUNDED_PREVIEW ->
                MangaReaderGestureOwner.VEIL_READER
        }
}
