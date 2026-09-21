package com.veilreader.app.manga.health

import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceHealthStoreTest {

    private val source = SourceId("health.source")

    @Test
    fun localNetworkFailuresDoNotDamageReliabilityOrOpenCircuit() {
        val store = InMemorySourceHealthStore(
            openAfterConsecutiveFailures = 3,
            circuitCooldownMillis = 1_000L
        )
        val initial = store.snapshot(source, 0L).observedReliability

        repeat(8) { index ->
            store.record(
                SourceHealthEvent(
                    sourceId = source,
                    operation = SourceOperation.SEARCH,
                    success = false,
                    latencyMillis = 500L,
                    failureKind = SourceFailureKind.NETWORK,
                    observedAtEpochMs = index.toLong()
                )
            )
        }

        val snapshot = store.snapshot(source, 20L)
        assertEquals(CircuitState.CLOSED, snapshot.circuitState)
        assertEquals(0, snapshot.consecutiveSourceFailures)
        assertEquals(0.0, snapshot.weightedFailurePenalty, 0.0001)
        assertEquals(initial, snapshot.observedReliability, 0.0001)
    }

    @Test
    fun repeatedParserFailuresOpenCircuit_thenCooldownMakesHalfOpen_thenSuccessCloses() {
        val store = InMemorySourceHealthStore(
            openAfterConsecutiveFailures = 3,
            circuitCooldownMillis = 1_000L
        )

        repeat(3) { index ->
            store.record(
                SourceHealthEvent(
                    sourceId = source,
                    operation = SourceOperation.DETAILS,
                    success = false,
                    latencyMillis = 100L,
                    failureKind = SourceFailureKind.PARSE_CHANGED,
                    observedAtEpochMs = index * 100L
                )
            )
        }

        assertEquals(CircuitState.OPEN, store.snapshot(source, 500L).circuitState)
        assertEquals(CircuitState.HALF_OPEN, store.snapshot(source, 1_500L).circuitState)

        val recovered = store.record(
            SourceHealthEvent(
                sourceId = source,
                operation = SourceOperation.DETAILS,
                success = true,
                latencyMillis = 80L,
                observedAtEpochMs = 1_600L
            )
        )

        assertEquals(CircuitState.CLOSED, recovered.circuitState)
        assertEquals(0, recovered.consecutiveSourceFailures)
        assertTrue(recovered.successfulSamples >= 1)
    }

    @Test
    fun latencyUsesEwmaInsteadOfLastSampleOnly() {
        val store = InMemorySourceHealthStore(latencyAlpha = 0.5)

        store.record(
            SourceHealthEvent(
                sourceId = source,
                operation = SourceOperation.PAGES,
                success = true,
                latencyMillis = 1_000L,
                observedAtEpochMs = 1L
            )
        )
        val second = store.record(
            SourceHealthEvent(
                sourceId = source,
                operation = SourceOperation.PAGES,
                success = true,
                latencyMillis = 2_000L,
                observedAtEpochMs = 2L
            )
        )

        assertEquals(1_500.0, second.latencyEwmaMillis!!, 0.0001)
    }
}
