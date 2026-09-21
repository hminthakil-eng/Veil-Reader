package com.veilreader.app.manga.reader.ui

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.MangaOrientationPolicy
import com.veilreader.app.manga.reader.MangaPageDirection
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.MangaReaderMode
import com.veilreader.app.manga.reader.MangaReaderSnapshot
import com.veilreader.app.manga.reader.MangaZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaReaderSavedStateCodecTest {

    @Test
    fun snapshotRoundTripsWithoutProviderObjects() {
        val snapshot = MangaReaderSnapshot(
            chapter = MangaReaderChapterRef(
                mangaId = CanonicalMangaId("canonical-77"),
                anchor = MangaChapterAnchor(
                    volume = 3.0,
                    number = 12.5,
                    languageTag = "fa",
                    normalizedTitle = "Chapter 12.5",
                    providerChapterKeyHint = "temporary-provider-key"
                )
            ),
            mode = MangaReaderMode.WEBTOON,
            direction = MangaPageDirection.RIGHT_TO_LEFT,
            orientationPolicy = MangaOrientationPolicy.PORTRAIT,
            itemIndex = 22,
            webtoonOffsetFraction = 0.42,
            zoom = MangaZoomState(
                scale = 2.2,
                centerXFraction = 0.3,
                centerYFraction = 0.7
            )
        )

        val encoded = MangaReaderSavedStateCodec.encode(snapshot)
        val decoded = MangaReaderSavedStateCodec.decode(encoded)

        assertEquals(snapshot, decoded)
        assertEquals(
            MangaReaderSavedStateCodec.keys,
            encoded.keys
        )
    }

    @Test
    fun corruptOrFuturePayloadFailsClosed() {
        val corrupt = mapOf(
            "version" to "1",
            "manga_id" to "work",
            "mode" to "PAGED"
        )
        assertNull(MangaReaderSavedStateCodec.decode(corrupt))

        val future = validPayload().toMutableMap().apply {
            put("version", "999")
        }
        assertNull(MangaReaderSavedStateCodec.decode(future))
    }

    @Test
    fun malformedOptionalChapterNumberIsRejectedInsteadOfSilentlyDropped() {
        val values = validPayload().toMutableMap().apply {
            put("chapter_number", "not-a-number")
        }

        assertNull(MangaReaderSavedStateCodec.decode(values))
    }

    @Test
    fun payloadWithNonFiniteZoomIsRejected() {
        val values = validPayload().toMutableMap().apply {
            put("zoom_scale", "NaN")
        }

        assertNull(MangaReaderSavedStateCodec.decode(values))
    }

    private fun validPayload(): Map<String, String> =
        MangaReaderSavedStateCodec.encode(
            MangaReaderSnapshot(
                chapter = MangaReaderChapterRef(
                    mangaId = CanonicalMangaId("work"),
                    anchor = MangaChapterAnchor(number = 1.0)
                ),
                mode = MangaReaderMode.PAGED,
                direction = MangaPageDirection.LEFT_TO_RIGHT,
                orientationPolicy = MangaOrientationPolicy.FOLLOW_SYSTEM,
                itemIndex = 0,
                webtoonOffsetFraction = 0.0,
                zoom = MangaZoomState()
            )
        )
}
