package com.veilreader.app.manga.net

import com.veilreader.app.manga.core.MangaSourceException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaHttpFailureTest {
    @Test
    fun rateLimit_mapsRetryAfterSecondsToMillis() {
        val error = mangaHttpFailure(
            statusCode = 429,
            retryAfterHeader = "7",
            url = "https://source.example/api"
        )

        assertTrue(error is MangaSourceException.RateLimited)
        assertEquals(7_000L, (error as MangaSourceException.RateLimited).retryAfterMillis)
    }

    @Test
    fun commonHttpStatuses_mapToTypedFailures() {
        assertTrue(
            mangaHttpFailure(401, null, "https://source.example") is
                MangaSourceException.AuthRequired
        )
        assertTrue(
            mangaHttpFailure(403, null, "https://source.example") is
                MangaSourceException.Blocked
        )
        assertTrue(
            mangaHttpFailure(404, null, "https://source.example") is
                MangaSourceException.NotFound
        )
        assertTrue(
            mangaHttpFailure(503, null, "https://source.example") is
                MangaSourceException.TemporarilyUnavailable
        )

        val other = mangaHttpFailure(418, null, "https://source.example")
        assertTrue(other is MangaSourceException.NetworkFailure)
        assertEquals(418, (other as MangaSourceException.NetworkFailure).statusCode)
    }
}
