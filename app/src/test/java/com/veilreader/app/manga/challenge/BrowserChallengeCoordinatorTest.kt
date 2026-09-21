package com.veilreader.app.manga.challenge

import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserChallengeCoordinatorTest {

    @Test
    fun backgroundCallerWithoutActiveSessionCannotLaunchUi() = runBlocking {
        val launches = AtomicInteger()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinator = BrowserChallengeCoordinator(
                uiDriver = object : ChallengeUiDriver {
                    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult {
                        launches.incrementAndGet()
                        return ChallengeUiResult.SOLVED
                    }
                },
                launchPolicy = ChallengeLaunchPolicy { false },
                cooldowns = ChallengeCooldownRegistry(),
                sessionScope = scope
            )

            val resolved = coordinator.resolve(
                source("background.source"),
                "background.example",
                challengeFailure()
            )

            assertFalse(resolved)
            assertEquals(0, launches.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun backgroundCallerJoinsExistingForegroundSession_withoutLaunchingSecondUi() = runBlocking {
        val launches = AtomicInteger()
        val foregroundAllowed = AtomicBoolean(true)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val descriptor = source("shared.source")
        try {
            val coordinator = BrowserChallengeCoordinator(
                uiDriver = object : ChallengeUiDriver {
                    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult {
                        launches.incrementAndGet()
                        started.complete(Unit)
                        release.await()
                        return ChallengeUiResult.SOLVED
                    }
                },
                launchPolicy = ChallengeLaunchPolicy { foregroundAllowed.get() },
                cooldowns = ChallengeCooldownRegistry(),
                sessionScope = scope
            )

            val foreground = async {
                coordinator.resolve(descriptor, "shared.example", challengeFailure())
            }
            withTimeout(1_000L) { started.await() }

            foregroundAllowed.set(false)
            val background = async {
                coordinator.resolve(descriptor, "shared.example", challengeFailure())
            }

            delay(75L)
            assertEquals(1, launches.get())

            release.complete(Unit)

            assertTrue(foreground.await())
            assertTrue(background.await())
            assertEquals(1, launches.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun differentDomainsAreSerializedGlobally() = runBlocking {
        val launches = AtomicInteger()
        val active = AtomicInteger()
        val maxActive = AtomicInteger()
        val firstStarted = CompletableDeferred<Unit>()
        val firstRelease = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinator = BrowserChallengeCoordinator(
                uiDriver = object : ChallengeUiDriver {
                    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult {
                        val call = launches.incrementAndGet()
                        val now = active.incrementAndGet()
                        maxActive.updateAndGet { previous -> maxOf(previous, now) }
                        try {
                            if (call == 1) {
                                firstStarted.complete(Unit)
                                firstRelease.await()
                            } else {
                                secondStarted.complete(Unit)
                            }
                            return ChallengeUiResult.SOLVED
                        } finally {
                            active.decrementAndGet()
                        }
                    }
                },
                launchPolicy = ChallengeLaunchPolicy { true },
                cooldowns = ChallengeCooldownRegistry(),
                sessionScope = scope
            )

            val first = async {
                coordinator.resolve(source("one.source"), "one.example", challengeFailure())
            }
            withTimeout(1_000L) { firstStarted.await() }

            val second = async {
                coordinator.resolve(source("two.source"), "two.example", challengeFailure())
            }

            delay(75L)
            assertEquals(1, launches.get())
            assertEquals(1, maxActive.get())

            firstRelease.complete(Unit)
            assertTrue(first.await())
            withTimeout(1_000L) { secondStarted.await() }
            assertTrue(second.await())

            assertEquals(2, launches.get())
            assertEquals(1, maxActive.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failedChallengeEntersCooldownAndDoesNotReopenUi() = runBlocking {
        val launches = AtomicInteger()
        var now = 1_000L
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val cooldowns = ChallengeCooldownRegistry(
            successRecurrenceWindowMillis = 30_000L,
            failureCooldownMillis = 180_000L
        )
        val descriptor = source("failure.source")
        try {
            val coordinator = BrowserChallengeCoordinator(
                uiDriver = object : ChallengeUiDriver {
                    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult {
                        launches.incrementAndGet()
                        return ChallengeUiResult.FAILED
                    }
                },
                launchPolicy = ChallengeLaunchPolicy { true },
                cooldowns = cooldowns,
                sessionScope = scope,
                clock = { now }
            )

            assertFalse(
                coordinator.resolve(descriptor, "failure.example", challengeFailure())
            )
            now += 1_000L
            assertFalse(
                coordinator.resolve(descriptor, "failure.example", challengeFailure())
            )

            assertEquals(1, launches.get())
            assertEquals(
                ChallengeUiResult.FAILED,
                cooldowns.snapshot(ChallengeKey(descriptor.id, "failure.example")).lastResult
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun repeatedChallengeSoonAfterSolvedIsMarkedIneffective_withoutUiLoop() = runBlocking {
        val launches = AtomicInteger()
        var now = 10_000L
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val cooldowns = ChallengeCooldownRegistry(
            successRecurrenceWindowMillis = 30_000L,
            failureCooldownMillis = 180_000L
        )
        val descriptor = source("ineffective.source")
        val key = ChallengeKey(descriptor.id, "ineffective.example")
        try {
            val coordinator = BrowserChallengeCoordinator(
                uiDriver = object : ChallengeUiDriver {
                    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult {
                        launches.incrementAndGet()
                        return ChallengeUiResult.SOLVED
                    }
                },
                launchPolicy = ChallengeLaunchPolicy { true },
                cooldowns = cooldowns,
                sessionScope = scope,
                clock = { now }
            )

            assertTrue(
                coordinator.resolve(descriptor, key.domain, challengeFailure())
            )

            now += 2_000L
            assertFalse(
                coordinator.resolve(descriptor, key.domain, challengeFailure())
            )

            val snapshot = cooldowns.snapshot(key)
            assertEquals(1, launches.get())
            assertEquals(1, snapshot.ineffectiveSuccessCount)
            assertEquals(ChallengeUiResult.FAILED, snapshot.lastResult)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cancellingOneWaiterDoesNotCancelGlobalSession() = runBlocking {
        val foregroundAllowed = AtomicBoolean(true)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val launches = AtomicInteger()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val descriptor = source("cancel.source")
        try {
            val coordinator = BrowserChallengeCoordinator(
                uiDriver = object : ChallengeUiDriver {
                    override suspend fun solve(request: ChallengeRequest): ChallengeUiResult {
                        launches.incrementAndGet()
                        started.complete(Unit)
                        release.await()
                        return ChallengeUiResult.SOLVED
                    }
                },
                launchPolicy = ChallengeLaunchPolicy { foregroundAllowed.get() },
                cooldowns = ChallengeCooldownRegistry(),
                sessionScope = scope
            )

            val owner = async {
                coordinator.resolve(descriptor, "cancel.example", challengeFailure())
            }
            withTimeout(1_000L) { started.await() }

            foregroundAllowed.set(false)
            val waiter = async {
                coordinator.resolve(descriptor, "cancel.example", challengeFailure())
            }
            waiter.cancel()

            release.complete(Unit)

            assertTrue(owner.await())
            assertEquals(1, launches.get())
        } finally {
            scope.cancel()
        }
    }

    private fun source(id: String) = MangaSourceDescriptor(
        id = SourceId(id),
        displayName = id,
        domains = listOf(id.substringBefore('.') + ".example")
    )

    private fun challengeFailure() = SourceFailure(
        kind = SourceFailureKind.CHALLENGE_REQUIRED,
        message = "challenge"
    )
}
