package com.veilreader.app.manga.challenge

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.PagedSourceResult
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import com.veilreader.app.manga.source.SourceSearchRequest
import com.veilreader.app.manga.source.SourceMangaSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeSessionHeaderHandoffTest {

    @Test
    fun solvedChallengeHeadersReachNextSourceAttempt() = runBlocking {
        var now = 1_000L
        val headers = ChallengeSessionHeadersStore(
            ttlMillis = 60_000L,
            clock = { now }
        )
        val sourceId = SourceId("handoff.source")
        val descriptor = MangaSourceDescriptor(
            id = sourceId,
            displayName = "Handoff",
            domains = listOf("handoff.example"),
            contentTypes = setOf(MangaContentType.MANGA)
        )
        var attempts = 0
        val provider = object : MangaSourceProvider {
            override val descriptor = descriptor
            override val capabilities = setOf(MangaSourceCapability.SEARCH)

            override suspend fun search(
                request: SourceSearchRequest,
                context: SourceRequestContext
            ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> {
                attempts += 1
                return if (context.sessionHeaders["Cookie"] == "clearance=ok" &&
                    context.sessionHeaders["User-Agent"] == "VeilChallengeUA") {
                    SourceOutcome.Success(PagedSourceResult(emptyList()))
                } else {
                    SourceOutcome.Failure(
                        SourceFailure(
                            kind = SourceFailureKind.CHALLENGE_REQUIRED,
                            message = "challenge"
                        )
                    )
                }
            }
        }

        val coordinator = SourceExecutionCoordinator(
            challengeAdapter = object : com.veilreader.app.manga.source.SourceChallengeAdapter {
                override suspend fun resolve(
                    source: MangaSourceDescriptor,
                    domain: String,
                    failure: SourceFailure
                ): Boolean {
                    headers.put(
                        ChallengeKey(source.id, domain),
                        cookieHeader = "clearance=ok",
                        userAgent = "VeilChallengeUA"
                    )
                    return true
                }
            },
            sessionHeadersProvider = headers,
            maxAttempts = 2,
            clock = { now }
        )

        val result = coordinator.search(provider, SourceSearchRequest("query"))

        assertTrue(result.outcome is SourceOutcome.Success)
        assertEquals(2, attempts)
    }

    @Test
    fun expiredHeadersAreRemoved() {
        var now = 10L
        val store = ChallengeSessionHeadersStore(
            ttlMillis = 100L,
            clock = { now }
        )
        val key = ChallengeKey(SourceId("expiry.source"), "expiry.example")
        store.put(key, "a=b", "ua")
        assertEquals("a=b", store.headersFor(key.sourceId, key.domain)["Cookie"])

        now = 111L
        assertTrue(store.headersFor(key.sourceId, key.domain).isEmpty())
        assertEquals(null, store.snapshot(key))
    }

    @Test
    fun headerSnapshotNeverPrintsSecrets() {
        val value = ChallengeSessionHeaders(
            cookieHeader = "secret-cookie",
            userAgent = "secret-agent",
            capturedAtEpochMs = 1L
        )
        val text = value.toString()
        assertTrue(!text.contains("secret-cookie"))
        assertTrue(!text.contains("secret-agent"))
    }
}