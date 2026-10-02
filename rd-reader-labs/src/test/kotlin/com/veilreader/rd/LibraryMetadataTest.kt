package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryMetadataTest {
    @Test fun metadataNormalizesRatingTagsAndCover() {
        val result = ExtendedBookMetadata(
            rating = 8,
            tags = setOf(" Fantasy ", "fantasy", "Mystery"),
            customCoverRef = "  cover.jpg "
        ).normalized()
        assertEquals(5, result.rating)
        assertEquals(setOf("fantasy", "mystery"), result.tags)
        assertEquals("cover.jpg", result.customCoverRef)
    }
}
