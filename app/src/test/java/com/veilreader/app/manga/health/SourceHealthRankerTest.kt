package com.veilreader.app.manga.health

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceHealthRankerTest {

    @Test
    fun healthySourceRanksAheadOfRepeatedlyFailingSource() {
        val store = InMemorySourceHealthStore(openAfterConsecutiveFailures = 10)
        val healthy = provider("healthy.source")
        val failing = provider("failing.source")

        repeat(8) { index ->
            store.record(success(healthy.descriptor.id, 300L, index.toLong()))
            store.record(
                failure(
                    failing.descriptor.id,
                    SourceFailureKind.PARSE_CHANGED,
                    400L,
                    index.toLong()
                )
            )
        }

        val ranked = SourceHealthRanker(store).rank(listOf(failing, healthy), nowEpochMs = 100L)

        assertEquals(healthy.descriptor.id, ranked.first().provider.descriptor.id)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun newSourceGetsBoundedExplorationBonus() {
        val store = InMemorySourceHealthStore()
        val newSource = provider("new.source")
        val established = provider("established.source")

        repeat(12) { index ->
            store.record(success(established.descriptor.id, 700L, index.toLong()))
        }

        val ranked = SourceHealthRanker(store).rank(
            listOf(established, newSource),
            nowEpochMs = 100L
        )
        val fresh = ranked.first { it.provider.descriptor.id == newSource.descriptor.id }
        val proven = ranked.first { it.provider.descriptor.id == established.descriptor.id }

        assertTrue(fresh.explorationBonus > 0.0)
        assertEquals(0.0, proven.explorationBonus, 0.0001)
        assertFalse(fresh.unavailableByCircuit)
    }

    @Test
    fun openCircuitIsNeverBestAvailable() {
        val store = InMemorySourceHealthStore(
            openAfterConsecutiveFailures = 2,
            circuitCooldownMillis = 10_000L
        )
        val broken = provider("broken.source")
        val fallback = provider("fallback.source")

        repeat(2) { index ->
            store.record(
                failure(
                    broken.descriptor.id,
                    SourceFailureKind.SOURCE_REMOVED,
                    50L,
                    index.toLong()
                )
            )
        }
        store.record(success(fallback.descriptor.id, 900L, 5L))

        val ranker = SourceHealthRanker(store)
        val ranked = ranker.rank(listOf(broken, fallback), nowEpochMs = 100L)

        assertTrue(ranked.last { it.provider.descriptor.id == broken.descriptor.id }.unavailableByCircuit)
        assertEquals(fallback.descriptor.id, ranker.bestAvailable(listOf(broken, fallback), 100L)?.descriptor?.id)
    }

    private fun success(sourceId: SourceId, latency: Long, time: Long) = SourceHealthEvent(
        sourceId = sourceId,
        operation = SourceOperation.SEARCH,
        success = true,
        latencyMillis = latency,
        observedAtEpochMs = time
    )

    private fun failure(
        sourceId: SourceId,
        kind: SourceFailureKind,
        latency: Long,
        time: Long
    ) = SourceHealthEvent(
        sourceId = sourceId,
        operation = SourceOperation.SEARCH,
        success = false,
        latencyMillis = latency,
        failureKind = kind,
        observedAtEpochMs = time
    )

    private fun provider(id: String): MangaSourceProvider = object : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = SourceId(id),
            displayName = id,
            domains = listOf("$id.example"),
            contentTypes = setOf(MangaContentType.MANGA)
        )
        override val capabilities = setOf(MangaSourceCapability.SEARCH)
    }
}
