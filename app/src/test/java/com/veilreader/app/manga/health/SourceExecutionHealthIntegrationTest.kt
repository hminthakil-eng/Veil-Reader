package com.veilreader.app.manga.health

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.PagedSourceResult
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaSummary
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import com.veilreader.app.manga.source.SourceSearchRequest
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceExecutionHealthIntegrationTest {

    @Test
    fun coordinatorRecordsEachAttemptButNeverRequestPayload() = runBlocking {
        val events = mutableListOf<SourceHealthEvent>()
        var clock = 1_000L
        var calls = 0
        val provider = provider { _ ->
            calls += 1
            clock += 25L
            if (calls == 1) {
                SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.TIMEOUT, "retry")
                )
            } else {
                SourceOutcome.Success(PagedSourceResult(emptyList()))
            }
        }

        val coordinator = SourceExecutionCoordinator(
            healthObserver = object : SourceExecutionHealthObserver {
                override fun onEvent(event: SourceHealthEvent) {
                    events += event
                }
            },
            maxAttempts = 2,
            sleeper = {},
            clock = { clock }
        )

        coordinator.search(provider, SourceSearchRequest("private-query"))

        assertEquals(2, events.size)
        assertEquals(SourceFailureKind.TIMEOUT, events[0].failureKind)
        assertEquals(null, events[1].failureKind)
        assertEquals(SourceOperation.SEARCH, events[0].operation)
        assertEquals(SourceId("integration.source"), events[0].sourceId)
        // SourceHealthEvent structurally has no query/title/url/key fields to leak.
    }

    @Test
    fun cancellationProducesNoHealthEvent() {
        val events = mutableListOf<SourceHealthEvent>()
        val provider = provider { throw CancellationException("screen closed") }
        val coordinator = SourceExecutionCoordinator(
            healthObserver = object : SourceExecutionHealthObserver {
                override fun onEvent(event: SourceHealthEvent) {
                    events += event
                }
            }
        )

        assertThrows(CancellationException::class.java) {
            runBlocking {
                coordinator.search(provider, SourceSearchRequest("cancel"))
            }
        }
        assertEquals(0, events.size)
    }

    private fun provider(
        search: suspend (SourceRequestContext) ->
            SourceOutcome<PagedSourceResult<SourceMangaSummary>>
    ): MangaSourceProvider = object : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = SourceId("integration.source"),
            displayName = "Integration",
            domains = listOf("integration.example"),
            contentTypes = setOf(MangaContentType.MANGA)
        )
        override val capabilities = setOf(MangaSourceCapability.SEARCH)

        override suspend fun search(
            request: SourceSearchRequest,
            context: SourceRequestContext
        ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> = search(context)
    }
}
