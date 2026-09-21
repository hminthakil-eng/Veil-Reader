package com.veilreader.app.manga.challenge

import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.SourceChallengeAdapter
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ChallengeKey(
    val sourceId: SourceId,
    val domain: String
) {
    init {
        require(domain.isNotBlank()) { "Challenge domain cannot be blank" }
    }
}

data class ChallengeRequest(
    val source: MangaSourceDescriptor,
    val domain: String,
    val failureKind: SourceFailureKind
)

enum class ChallengeUiResult {
    SOLVED,
    FAILED,
    CANCELLED
}

/**
 * Android/WebView implementation comes later.
 *
 * The driver deliberately receives no Activity. A lifecycle-aware UI host/launcher can live behind
 * this boundary without letting sources or the coordinator retain Activity/WebView references.
 */
interface ChallengeUiDriver {
    suspend fun solve(request: ChallengeRequest): ChallengeUiResult
}

/**
 * Tells the coordinator whether this caller is currently allowed to start interactive UI.
 *
 * Background callers may join an already-running global session but can never create one.
 */
fun interface ChallengeLaunchPolicy {
    fun canLaunchInteractiveChallenge(): Boolean
}

enum class ChallengeCooldownDecision {
    ALLOW,
    RECENT_FAILURE,
    RECENT_SUCCESS_BECAME_INEFFECTIVE
}

data class ChallengeCooldownSnapshot(
    val key: ChallengeKey,
    val lastResult: ChallengeUiResult?,
    val lastResultAtEpochMs: Long?,
    val ineffectiveSuccessCount: Int
)

/**
 * Process-local cooldown state. No cookies, URLs, titles or reading identifiers are stored here.
 */
class ChallengeCooldownRegistry(
    private val successRecurrenceWindowMillis: Long = 30_000L,
    private val failureCooldownMillis: Long = 3 * 60_000L
) {
    init {
        require(successRecurrenceWindowMillis > 0)
        require(failureCooldownMillis > 0)
    }

    private data class State(
        var lastResult: ChallengeUiResult? = null,
        var lastResultAt: Long? = null,
        var ineffectiveSuccessCount: Int = 0
    )

    private val values = linkedMapOf<ChallengeKey, State>()

    @Synchronized
    fun beforeChallenge(key: ChallengeKey, nowEpochMs: Long): ChallengeCooldownDecision {
        val state = values.getOrPut(key) { State() }
        val resultAt = state.lastResultAt ?: return ChallengeCooldownDecision.ALLOW
        val age = (nowEpochMs - resultAt).coerceAtLeast(0L)

        return when (state.lastResult) {
            ChallengeUiResult.SOLVED -> {
                if (age < successRecurrenceWindowMillis) {
                    // The site immediately challenged us again after a reported success. Treat that
                    // solve as ineffective and enter failure cooldown rather than reopening UI.
                    state.lastResult = ChallengeUiResult.FAILED
                    state.lastResultAt = nowEpochMs
                    state.ineffectiveSuccessCount += 1
                    ChallengeCooldownDecision.RECENT_SUCCESS_BECAME_INEFFECTIVE
                } else {
                    ChallengeCooldownDecision.ALLOW
                }
            }

            ChallengeUiResult.FAILED,
            ChallengeUiResult.CANCELLED -> {
                if (age < failureCooldownMillis) {
                    ChallengeCooldownDecision.RECENT_FAILURE
                } else {
                    ChallengeCooldownDecision.ALLOW
                }
            }

            null -> ChallengeCooldownDecision.ALLOW
        }
    }

    @Synchronized
    fun record(
        key: ChallengeKey,
        result: ChallengeUiResult,
        observedAtEpochMs: Long
    ) {
        val state = values.getOrPut(key) { State() }
        state.lastResult = result
        state.lastResultAt = observedAtEpochMs
    }

    @Synchronized
    fun snapshot(key: ChallengeKey): ChallengeCooldownSnapshot {
        val state = values[key]
        return ChallengeCooldownSnapshot(
            key = key,
            lastResult = state?.lastResult,
            lastResultAtEpochMs = state?.lastResultAt,
            ineffectiveSuccessCount = state?.ineffectiveSuccessCount ?: 0
        )
    }
}

/**
 * Global single-flight challenge coordinator.
 *
 * Guarantees:
 * - at most one interactive challenge session is running process-wide;
 * - background callers can only join an existing session, never open UI;
 * - a caller for another source/domain waits for the global session, then re-evaluates;
 * - failed/cancelled sessions enter cooldown;
 * - a repeated challenge shortly after SOLVED is classified as ineffective and does not reopen UI;
 * - cancelling one waiter does not cancel the global solver session because the session runs in the
 *   injected application-owned [sessionScope].
 */
class BrowserChallengeCoordinator(
    private val uiDriver: ChallengeUiDriver,
    private val launchPolicy: ChallengeLaunchPolicy,
    private val cooldowns: ChallengeCooldownRegistry,
    private val sessionScope: CoroutineScope,
    private val waiterTimeoutMillis: Long = 90_000L,
    private val clock: () -> Long = System::currentTimeMillis
) : SourceChallengeAdapter {

    init {
        require(waiterTimeoutMillis > 0)
    }

    private data class ActiveSession(
        val key: ChallengeKey,
        val result: CompletableDeferred<ChallengeUiResult>
    )

    private sealed interface JoinDecision {
        data class Wait(
            val key: ChallengeKey,
            val result: CompletableDeferred<ChallengeUiResult>
        ) : JoinDecision

        data object BackgroundWithoutSession : JoinDecision
    }

    private val mutex = Mutex()
    private var activeSession: ActiveSession? = null

    override suspend fun resolve(
        source: MangaSourceDescriptor,
        domain: String,
        failure: SourceFailure
    ): Boolean {
        val key = ChallengeKey(source.id, domain)

        while (true) {
            // Joining an already-running same-key session must happen before cooldown inspection.
            // Otherwise a caller arriving as SOLVED is being recorded could falsely classify that
            // still-active session as an ineffective recurrence.
            val current = currentSession()
            if (current != null) {
                val result = withTimeoutOrNull(waiterTimeoutMillis) {
                    current.result.await()
                } ?: return false

                if (current.key == key) {
                    return result == ChallengeUiResult.SOLVED
                }
                // A different source/domain owned the global session. Re-evaluate our own key after
                // the global slot becomes available.
                continue
            }

            when (cooldowns.beforeChallenge(key, clock())) {
                ChallengeCooldownDecision.RECENT_FAILURE,
                ChallengeCooldownDecision.RECENT_SUCCESS_BECAME_INEFFECTIVE -> return false

                ChallengeCooldownDecision.ALLOW -> Unit
            }

            when (val decision = joinOrStart(source, domain, failure, key)) {
                JoinDecision.BackgroundWithoutSession -> return false
                is JoinDecision.Wait -> {
                    val result = withTimeoutOrNull(waiterTimeoutMillis) {
                        decision.result.await()
                    } ?: return false

                    if (decision.key == key) {
                        return result == ChallengeUiResult.SOLVED
                    }
                    // A session raced in between cooldown evaluation and acquisition. Loop and
                    // re-evaluate after that global session completes.
                }
            }
        }
    }

    private suspend fun currentSession(): ActiveSession? = mutex.withLock { activeSession }

    private suspend fun joinOrStart(
        source: MangaSourceDescriptor,
        domain: String,
        failure: SourceFailure,
        key: ChallengeKey
    ): JoinDecision = mutex.withLock {
        activeSession?.let { current ->
            return@withLock JoinDecision.Wait(current.key, current.result)
        }

        if (!launchPolicy.canLaunchInteractiveChallenge()) {
            return@withLock JoinDecision.BackgroundWithoutSession
        }

        val deferred = CompletableDeferred<ChallengeUiResult>()
        val created = ActiveSession(key, deferred)
        activeSession = created

        sessionScope.launch {
            val result = try {
                uiDriver.solve(
                    ChallengeRequest(
                        source = source,
                        domain = domain,
                        failureKind = failure.kind
                    )
                )
            } catch (cancelled: CancellationException) {
                ChallengeUiResult.CANCELLED
            } catch (error: Throwable) {
                ChallengeUiResult.FAILED
            }

            // Complete global state even if the app-owned session scope is being cancelled.
            withContext(NonCancellable) {
                cooldowns.record(key, result, clock())
                mutex.withLock {
                    if (activeSession === created) {
                        activeSession = null
                    }
                }
                deferred.complete(result)
            }
        }

        JoinDecision.Wait(key, deferred)
    }
}
