package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.reader.image.MangaImageDeliveryStrategy
import com.veilreader.app.manga.reader.ui.MangaReaderGestureOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaReaderGestureOwnershipPolicyTest {

    @Test
    fun onlyLocalSubsamplingTransfersGestureOwnership() {
        assertEquals(
            MangaReaderGestureOwner.PAGE_RENDERER,
            MangaReaderGestureOwnershipPolicy.ownerFor(
                MangaImageDeliveryStrategy.LOCAL_SUBSAMPLING
            )
        )
        assertEquals(
            MangaReaderGestureOwner.VEIL_READER,
            MangaReaderGestureOwnershipPolicy.ownerFor(
                MangaImageDeliveryStrategy.STANDARD_COIL
            )
        )
        assertEquals(
            MangaReaderGestureOwner.VEIL_READER,
            MangaReaderGestureOwnershipPolicy.ownerFor(
                MangaImageDeliveryStrategy.REMOTE_BOUNDED_PREVIEW
            )
        )
    }
}
