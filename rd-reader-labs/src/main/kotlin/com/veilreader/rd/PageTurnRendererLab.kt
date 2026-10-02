package com.veilreader.rd

enum class PageTurnRendererKind { PAPER_2D, MESH, SLIDE, STATIC }

data class PageTurnPerformanceBudget(
    val captureP95Ms: Double,
    val frameP95Ms: Double,
    val memoryMiB: Double
) {
    init {
        require(captureP95Ms > 0)
        require(frameP95Ms > 0)
        require(memoryMiB > 0)
    }
}

data class PageTurnMeasurement(
    val captureP95Ms: Double,
    val frameP95Ms: Double,
    val memoryMiB: Double
)

object PageTurnGate {
    fun passes(measurement: PageTurnMeasurement, budget: PageTurnPerformanceBudget): Boolean =
        measurement.captureP95Ms <= budget.captureP95Ms &&
            measurement.frameP95Ms <= budget.frameP95Ms &&
            measurement.memoryMiB <= budget.memoryMiB
}
