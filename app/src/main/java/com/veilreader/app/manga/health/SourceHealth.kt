package com.veilreader.app.manga.health

import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import kotlin.math.max

enum class SourceOperation {
    SEARCH,
    DETAILS,
    CHAPTERS,
    PAGES,
    RESOLVE_URL
}

enum class CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

/**
 * Privacy-safe local observation.
 *
 * Deliberately contains no query, manga title/key, chapter key, URL, user identity or precise
 * request payload. It can be persisted later as aggregates without leaking reading history.
 */
data class SourceHealthEvent(
    val sourceId: SourceId,
    val operation: SourceOperation,
    val success: Boolean,
    val latencyMillis: Long,
    val failureKind: SourceFailureKind? = null,
    val observedAtEpochMs: Long
) {
    init {
        require(latencyMillis >= 0) { "Latency cannot be negative" }
        require(success == (failureKind == null)) {
            "Successful events cannot have a failure kind and failures must declare one"
        }
    }
}

data class SourceHealthSnapshot(
    val sourceId: SourceId,
    val sampleCount: Int,
    val successfulSamples: Int,
    val weightedFailurePenalty: Double,
    val latencyEwmaMillis: Double?,
    val consecutiveSourceFailures: Int,
    val circuitState: CircuitState,
    val circuitOpenedAtEpochMs: Long? = null,
    val lastObservedAtEpochMs: Long? = null
) {
    init {
        require(sampleCount >= 0)
        require(successfulSamples in 0..sampleCount)
        require(weightedFailurePenalty >= 0.0)
        require(consecutiveSourceFailures >= 0)
        require(latencyEwmaMillis == null || latencyEwmaMillis >= 0.0)
    }

    val observedReliability: Double
        get() {
            if (sampleCount == 0) return 0.65
            val effectiveFailures = weightedFailurePenalty.coerceAtMost(sampleCount.toDouble())
            return ((successfulSamples + 1.5) /
                (successfulSamples + effectiveFailures + 3.0)).coerceIn(0.0, 1.0)
        }
}

interface SourceHealthStore {
    fun snapshot(sourceId: SourceId, nowEpochMs: Long): SourceHealthSnapshot
    fun record(event: SourceHealthEvent): SourceHealthSnapshot
    fun all(nowEpochMs: Long): List<SourceHealthSnapshot>
}

class InMemorySourceHealthStore(
    private val openAfterConsecutiveFailures: Int = 3,
    private val circuitCooldownMillis: Long = 5 * 60_000L,
    private val latencyAlpha: Double = 0.25
) : SourceHealthStore {

    init {
        require(openAfterConsecutiveFailures > 0)
        require(circuitCooldownMillis > 0)
        require(latencyAlpha in 0.0..1.0 && latencyAlpha > 0.0)
    }

    private data class MutableHealth(
        var samples: Int = 0,
        var successes: Int = 0,
        var weightedPenalty: Double = 0.0,
        var latencyEwma: Double? = null,
        var consecutiveSourceFailures: Int = 0,
        var circuitState: CircuitState = CircuitState.CLOSED,
        var circuitOpenedAt: Long? = null,
        var lastObservedAt: Long? = null
    )

    private val values = linkedMapOf<SourceId, MutableHealth>()

    @Synchronized
    override fun snapshot(sourceId: SourceId, nowEpochMs: Long): SourceHealthSnapshot {
        val state = values.getOrPut(sourceId) { MutableHealth() }
        refreshCircuit(state, nowEpochMs)
        return state.toSnapshot(sourceId)
    }

    @Synchronized
    override fun record(event: SourceHealthEvent): SourceHealthSnapshot {
        val state = values.getOrPut(event.sourceId) { MutableHealth() }
        refreshCircuit(state, event.observedAtEpochMs)

        state.samples += 1
        state.lastObservedAt = event.observedAtEpochMs
        state.latencyEwma = state.latencyEwma?.let { previous ->
            previous + latencyAlpha * (event.latencyMillis - previous)
        } ?: event.latencyMillis.toDouble()

        if (event.success) {
            state.successes += 1
            state.consecutiveSourceFailures = 0
            state.circuitState = CircuitState.CLOSED
            state.circuitOpenedAt = null
        } else {
            val kind = requireNotNull(event.failureKind)
            val penalty = SourceFailureWeights.penalty(kind)
            state.weightedPenalty += penalty

            if (SourceFailureWeights.countsTowardCircuit(kind)) {
                state.consecutiveSourceFailures += 1
                if (state.consecutiveSourceFailures >= openAfterConsecutiveFailures) {
                    state.circuitState = CircuitState.OPEN
                    state.circuitOpenedAt = event.observedAtEpochMs
                }
            }
        }

        return state.toSnapshot(event.sourceId)
    }

    @Synchronized
    override fun all(nowEpochMs: Long): List<SourceHealthSnapshot> =
        values.keys.map { snapshot(it, nowEpochMs) }

    private fun refreshCircuit(state: MutableHealth, nowEpochMs: Long) {
        val openedAt = state.circuitOpenedAt ?: return
        if (
            state.circuitState == CircuitState.OPEN &&
            nowEpochMs - openedAt >= circuitCooldownMillis
        ) {
            state.circuitState = CircuitState.HALF_OPEN
        }
    }

    private fun MutableHealth.toSnapshot(sourceId: SourceId) = SourceHealthSnapshot(
        sourceId = sourceId,
        sampleCount = samples,
        successfulSamples = successes,
        weightedFailurePenalty = weightedPenalty,
        latencyEwmaMillis = latencyEwma,
        consecutiveSourceFailures = consecutiveSourceFailures,
        circuitState = circuitState,
        circuitOpenedAtEpochMs = circuitOpenedAt,
        lastObservedAtEpochMs = lastObservedAt
    )
}

object SourceFailureWeights {
    /**
     * NETWORK is intentionally zero: local connectivity must not damage a source's reputation.
     * NOT_FOUND/AUTH/UNSUPPORTED are mostly request/account/capability facts, not source health.
     */
    fun penalty(kind: SourceFailureKind): Double = when (kind) {
        SourceFailureKind.NETWORK -> 0.0
        SourceFailureKind.TIMEOUT -> 0.25
        SourceFailureKind.RATE_LIMITED -> 0.55
        SourceFailureKind.BLOCKED -> 0.75
        SourceFailureKind.CHALLENGE_REQUIRED -> 0.45
        SourceFailureKind.AUTH_REQUIRED -> 0.05
        SourceFailureKind.NOT_FOUND -> 0.10
        SourceFailureKind.PARSE_CHANGED -> 1.0
        SourceFailureKind.SOURCE_REMOVED -> 1.0
        SourceFailureKind.UNSUPPORTED -> 0.0
        SourceFailureKind.UNKNOWN -> 0.35
    }

    fun countsTowardCircuit(kind: SourceFailureKind): Boolean = when (kind) {
        SourceFailureKind.RATE_LIMITED,
        SourceFailureKind.BLOCKED,
        SourceFailureKind.CHALLENGE_REQUIRED,
        SourceFailureKind.PARSE_CHANGED,
        SourceFailureKind.SOURCE_REMOVED -> true

        SourceFailureKind.NETWORK,
        SourceFailureKind.TIMEOUT,
        SourceFailureKind.AUTH_REQUIRED,
        SourceFailureKind.NOT_FOUND,
        SourceFailureKind.UNSUPPORTED,
        SourceFailureKind.UNKNOWN -> false
    }
}
