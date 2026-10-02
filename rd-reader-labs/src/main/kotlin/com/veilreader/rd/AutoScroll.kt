package com.veilreader.rd

enum class AutoScrollMode { PIXEL, LINE, PAGE, ROLLING }

data class AutoScrollConfig(
    val mode: AutoScrollMode,
    val unitsPerSecond: Double,
    val paused: Boolean = false
) {
    fun normalized(): AutoScrollConfig =
        copy(unitsPerSecond = unitsPerSecond.takeIf { it.isFinite() }?.coerceIn(0.1, 20_000.0) ?: 1.0)
}

object AutoScrollEngine {
    fun delta(config: AutoScrollConfig, elapsedMillis: Long, viewportExtent: Double): Double {
        if (config.paused || elapsedMillis <= 0 || viewportExtent <= 0.0) return 0.0
        val safe = config.normalized()
        val seconds = elapsedMillis / 1000.0
        return when (safe.mode) {
            AutoScrollMode.PIXEL -> safe.unitsPerSecond * seconds
            AutoScrollMode.LINE -> safe.unitsPerSecond * seconds
            AutoScrollMode.PAGE -> viewportExtent * safe.unitsPerSecond * seconds
            AutoScrollMode.ROLLING -> viewportExtent * (safe.unitsPerSecond / 100.0) * seconds
        }
    }
}
