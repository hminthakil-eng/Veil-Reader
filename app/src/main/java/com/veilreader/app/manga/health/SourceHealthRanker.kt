package com.veilreader.app.manga.health

import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceId
import kotlin.math.ln

data class RankedSource(
    val provider: MangaSourceProvider,
    val score: Double,
    val health: SourceHealthSnapshot,
    val explorationBonus: Double,
    val unavailableByCircuit: Boolean
) {
    init {
        require(score.isFinite() && score in 0.0..1.0)
        require(explorationBonus in 0.0..1.0)
    }
}

/**
 * Local-first source ranker.
 *
 * New/under-sampled sources receive a bounded exploration bonus so established sources cannot
 * permanently starve them. OPEN circuits remain visible at the bottom for diagnostics but are not
 * selected by [bestAvailable].
 */
class SourceHealthRanker(
    private val healthStore: SourceHealthStore,
    private val targetLatencyMillis: Double = 1_500.0,
    private val explorationSamples: Int = 12,
    private val maxExplorationBonus: Double = 0.16
) {
    init {
        require(targetLatencyMillis > 0)
        require(explorationSamples > 0)
        require(maxExplorationBonus in 0.0..0.5)
    }

    fun rank(
        providers: List<MangaSourceProvider>,
        nowEpochMs: Long
    ): List<RankedSource> = providers.map { provider ->
        val health = healthStore.snapshot(provider.descriptor.id, nowEpochMs)
        val exploration = explorationBonus(health.rankingEvidence)
        val latencyScore = latencyScore(health.latencyEwmaMillis)
        val confidence = sampleConfidence(health.rankingEvidence)

        // Prior reliability dominates. Latency improves ordering, while confidence prevents a
        // single lucky request from overwhelming proven sources.
        val base =
            (0.68 * health.observedReliability) +
                (0.18 * latencyScore) +
                (0.14 * confidence)
        val unavailable = health.circuitState == CircuitState.OPEN
        val score = if (unavailable) {
            (base * 0.20).coerceIn(0.0, 1.0)
        } else {
            (base + exploration).coerceIn(0.0, 1.0)
        }

        RankedSource(
            provider = provider,
            score = score,
            health = health,
            explorationBonus = exploration,
            unavailableByCircuit = unavailable
        )
    }.sortedWith(
        compareBy<RankedSource> { it.unavailableByCircuit }
            .thenByDescending { it.score }
            .thenBy { it.provider.descriptor.id.value }
    )

    fun bestAvailable(
        providers: List<MangaSourceProvider>,
        nowEpochMs: Long
    ): MangaSourceProvider? =
        rank(providers, nowEpochMs)
            .firstOrNull { !it.unavailableByCircuit }
            ?.provider

    private fun explorationBonus(evidence: Double): Double {
        if (evidence >= explorationSamples) return 0.0
        val remaining = (explorationSamples - evidence) / explorationSamples
        return maxExplorationBonus * remaining
    }

    private fun sampleConfidence(evidence: Double): Double =
        (ln(1.0 + evidence) / ln(1.0 + explorationSamples)).coerceIn(0.0, 1.0)

    private fun latencyScore(latencyMillis: Double?): Double {
        if (latencyMillis == null) return 0.55
        return (targetLatencyMillis / (targetLatencyMillis + latencyMillis)).coerceIn(0.0, 1.0)
    }
}
