package com.veilreader.app.manga.source

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceExecutionCoordinatorTest {

    @Test
    fun networkFailureSwitchesToMirror_andPreservesAttemptOrder() = runBlocking {
        val contexts = mutableListOf<SourceRequestContext>()
        val provider = provider(
            domains = listOf("primary.example", "mirror.example"),
            search = { context ->
                contexts += context
                if (context.domain == "primary.example") {
                    SourceOutcome.Failure(
                        SourceFailure(SourceFailureKind.NETWORK, "primary unavailable")
                    )
                } else {
                    SourceOutcome.Success(PagedSourceResult(emptyList()))
                }
            }
        )

        val result = coordinator().search(provider, SourceSearchRequest("veil"))

        assertTrue(result.outcome is SourceOutcome.Success)
        assertEquals("mirror.example", result.finalDomain)
        assertEquals(2, result.attempts)
        assertEquals(
            listOf(
                SourceRequestContext("primary.example", 1),
                SourceRequestContext("mirror.example", 2)
            ),
            contexts
        )
    }

    @Test
    fun retriesAreBounded_whenNoMirrorExists() = runBlocking {
        val calls = AtomicInteger()
        val provider = provider(
            search = {
                calls.incrementAndGet()
                SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.TIMEOUT, "still timing out")
                )
            }
        )

        val result = coordinator(maxAttempts = 3).search(
            provider,
            SourceSearchRequest("bounded")
        )

        assertTrue(result.outcome is SourceOutcome.Failure)
        assertEquals(3, result.attempts)
        assertEquals(3, calls.get())
    }

    @Test
    fun challengeIsResolvedAtMostOncePerDomainInOneOperation() = runBlocking {
        val providerCalls = AtomicInteger()
        val challengeCalls = AtomicInteger()
        val provider = provider(
            search = {
                providerCalls.incrementAndGet()
                SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.CHALLENGE_REQUIRED, "verify")
                )
            }
        )
        val adapter = object : SourceChallengeAdapter {
            override suspend fun resolve(
                source: MangaSourceDescriptor,
                domain: String,
                failure: SourceFailure
            ): Boolean {
                challengeCalls.incrementAndGet()
                return true
            }
        }

        val result = coordinator(
            maxAttempts = 5,
            challengeAdapter = adapter
        ).search(provider, SourceSearchRequest("challenge"))

        assertTrue(result.outcome is SourceOutcome.Failure)
        assertEquals(2, result.attempts)
        assertEquals(2, providerCalls.get())
        assertEquals(1, challengeCalls.get())
        assertEquals(SourceRecoveryAction.REQUIRE_BROWSER_CHALLENGE, result.recommendedAction)
    }

    @Test
    fun rateLimitDelayIsClamped() = runBlocking {
        val waits = mutableListOf<Long>()
        val calls = AtomicInteger()
        val provider = provider(
            search = {
                if (calls.incrementAndGet() == 1) {
                    SourceOutcome.Failure(
                        SourceFailure(
                            kind = SourceFailureKind.RATE_LIMITED,
                            message = "slow down",
                            retryAfterMillis = 120_000L
                        )
                    )
                } else {
                    SourceOutcome.Success(PagedSourceResult(emptyList()))
                }
            }
        )

        val result = SourceExecutionCoordinator(
            maxAttempts = 2,
            maxRetryDelayMillis = 5_000L,
            defaultRetryDelayMillis = 1_000L,
            sleeper = { waits += it }
        ).search(provider, SourceSearchRequest("rate"))

        assertTrue(result.outcome is SourceOutcome.Success)
        assertEquals(listOf(5_000L), waits)
    }

    @Test
    fun perSourceConcurrencyIsLimitedAcrossConcurrentRequests() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val active = AtomicInteger()
        val maxActive = AtomicInteger()

        val provider = provider(
            search = {
                val now = active.incrementAndGet()
                maxActive.updateAndGet { current -> maxOf(current, now) }
                calls.incrementAndGet()
                entered.complete(Unit)
                try {
                    release.await()
                    SourceOutcome.Success(PagedSourceResult(emptyList()))
                } finally {
                    active.decrementAndGet()
                }
            }
        )
        val coordinator = coordinator(maxParallelPerSource = 1)

        val first = launch(Dispatchers.Default) {
            coordinator.search(provider, SourceSearchRequest("first"))
        }
        withTimeout(1_000L) { entered.await() }

        val second = launch(Dispatchers.Default) {
            coordinator.search(provider, SourceSearchRequest("second"))
        }

        delay(100L)
        assertEquals(1, calls.get())
        assertEquals(1, maxActive.get())

        release.complete(Unit)
        joinAll(first, second)

        assertEquals(2, calls.get())
        assertEquals(1, maxActive.get())
    }

    @Test
    fun coroutineCancellationIsNeverConvertedIntoSourceFailure() {
        val provider = provider(
            search = { throw CancellationException("caller left") }
        )

        assertThrows(CancellationException::class.java) {
            runBlocking {
                coordinator().search(provider, SourceSearchRequest("cancel"))
            }
        }
    }

    private fun coordinator(
        maxAttempts: Int = 3,
        maxParallelPerSource: Int = 2,
        challengeAdapter: SourceChallengeAdapter? = null
    ) = SourceExecutionCoordinator(
        maxAttempts = maxAttempts,
        maxParallelPerSource = maxParallelPerSource,
        challengeAdapter = challengeAdapter,
        sleeper = {}
    )

    private fun provider(
        domains: List<String> = listOf("source.example"),
        search: suspend (SourceRequestContext) ->
            SourceOutcome<PagedSourceResult<SourceMangaSummary>>
    ): MangaSourceProvider = object : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = SourceId("test.source"),
            displayName = "Test Source",
            domains = domains,
            localeTags = setOf("en"),
            contentTypes = setOf(MangaContentType.MANGA)
        )

        override val capabilities = setOf(MangaSourceCapability.SEARCH)

        override suspend fun search(
            request: SourceSearchRequest,
            context: SourceRequestContext
        ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> = search(context)
    }
}
