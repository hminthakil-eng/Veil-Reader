package com.veilreader.app.ui.reader.material

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

internal enum class MaterialTurnPhase { IDLE, DRAGGING, COMPLETING, CANCELLING, SETTLED }

/** Monotonic finger response: no moving target spring and no overshoot during manipulation. */
internal fun materialDragProgress(inwardFraction: Float, material: PageMaterialProfile): Float {
    val d = finiteUnit(inwardFraction)
    return finiteUnit(d * (1f - material.resistance * exp(-d * 5f)))
}

internal fun materialCompletionThreshold(material: PageMaterialProfile): Float =
    .32f + .055f * (material.mass - .72f)

/** Outward velocity cancels even after crossing the ordinary completion threshold. */
internal fun materialShouldComplete(
    inwardPx: Float, widthPx: Float, density: Float, progress: Float,
    velocityPxPerSecond: Float, material: PageMaterialProfile
): Boolean {
    if (!inwardPx.isFinite() || !widthPx.isFinite() || widthPx <= 0f || inwardPx <= 0f ||
        !density.isFinite() || !progress.isFinite() || !velocityPxPerSecond.isFinite()) return false
    val dp = density.coerceAtLeast(.1f)
    if (velocityPxPerSecond < -480f * dp && progress < .85f) return false
    val threshold = materialCompletionThreshold(material)
    val flickDistance = max(28f * dp, widthPx * .035f)
    val flick = inwardPx >= flickDistance && velocityPxPerSecond >= (850f * sqrt(material.mass)) * dp
    return progress >= threshold || flick
}

/** Analytic critically damped release with bounded launch velocity, evaluated by frame time.
 * No integrator instability on dropped frames. Endpoints are reached once and never crossed.
 */
internal class MaterialRelease(
    start: Float,
    target: Float,
    velocityPagesPerSecond: Float,
    material: PageMaterialProfile
) {
    private val start = finiteUnit(start)
    val target = finiteUnit(target)
    private val omega = 19f * sqrt(material.stiffness / material.mass)
    private val delta = this.start - this.target
    private val toward = if (this.target > this.start) 1f else -1f
    private val launch = (velocityPagesPerSecond.takeIf { it.isFinite() } ?: 0f)
        .let { if (it * toward > 0f) it else 0f }
        .coerceIn(-abs(delta) * omega, abs(delta) * omega)
    val durationSeconds = (7.5f / omega).coerceIn(.24f, .62f)
    fun position(seconds: Float): Float {
        val t = (seconds.takeIf { it.isFinite() } ?: 0f).coerceAtLeast(0f)
        if (t >= durationSeconds) return target
        val result = target + (delta + (launch + omega * delta) * t) * exp(-omega * t)
        return result.coerceIn(minOf(start, target), maxOf(start, target))
    }
}

/** Freeze this configuration at capture; a preference update cannot morph a lifted sheet. */
internal data class MaterialTurnConfiguration(
    val enabled: Boolean = false,
    val material: PageMaterialProfile = PageMaterials.matte,
    val age: Float = 0f
)
