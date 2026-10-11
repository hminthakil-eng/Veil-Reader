package com.veilreader.app.ui.reader.material

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/** Page-width coordinates; Y points upward. No navigation or Android state. */
internal data class NativePaperFold(
    val x: Float,
    val y: Float,
    val directionX: Float,
    val directionY: Float,
    val radius: Double,
    val flat: Boolean
)

internal const val NATIVE_PAPER_COMPLETE_TRAVEL = 2.08f

/**
 * Solve the cylindrical free edge against the actual pull instead of translating
 * a rectangle. For a partial roll d = r(theta - sin(theta)); for a complete roll
 * d = 2 * creaseDistance - PI*r. Both meet continuously at theta = PI.
 * The same sheet geometry mirrors at the binding for a left-origin turn.
 */
internal fun nativePaperFold(
    travel: Float,
    gripY: Float,
    verticalOffset: Float,
    aspect: Float,
    fromRight: Boolean
): NativePaperFold {
    val t = travel.takeIf { it.isFinite() }?.coerceIn(0f, NATIVE_PAPER_COMPLETE_TRAVEL) ?: 0f
    val a = aspect.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.35f, 4.5f) ?: 1f
    val grip = gripY.takeIf { it.isFinite() }?.coerceIn(0.02f, 0.98f) ?: 0.5f
    val edgeX = if (fromRight) 1f else 0f
    val edgeY = (1f - grip) * a
    if (t < 0.00001f) return NativePaperFold(edgeX, edgeY, if (fromRight) -1f else 1f, 0f, 0.0, true)

    // Limit diagonal pull at the binding: extreme vertical input must not detach
    // the sheet. This also keeps numerical conditioning stable at initial lift.
    val dy = verticalOffset.takeIf { it.isFinite() }?.coerceIn(-t * 0.55f, t * 0.55f) ?: 0f
    val distance = hypot(t.toDouble(), dy.toDouble())
    val dx = (if (fromRight) -t else t) / distance
    val directionY = -dy / distance
    val lift = (t / 0.14f).coerceIn(0f, 1f)
    val flatten = ((2.0f - t) / 0.4f).coerceIn(0f, 1f)
    val radius = (0.018 + 0.095 * lift) * flatten
    val creaseDistance = if (radius < 0.000001 || distance >= PI * radius) {
        (distance + PI * radius) * 0.5
    } else {
        var low = 0.0
        var high = PI
        repeat(28) {
            val mid = (low + high) * 0.5
            if (radius * (mid - sin(mid)) > distance) high = mid else low = mid
        }
        radius * (low + high) * 0.5
    }
    return NativePaperFold(
        x = (edgeX + dx * creaseDistance).toFloat(),
        y = (edgeY + directionY * creaseDistance).toFloat(),
        directionX = dx.toFloat(), directionY = directionY.toFloat(),
        radius = radius, flat = false
    )
}

/** A completed leaf must leave the viewport; a cancelled one returns to its edge. */
internal fun nativePaperSettleTravel(
    progress: Float,
    releaseProgress: Float,
    releaseTravel: Float,
    completing: Boolean
): Float {
    val p = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val origin = releaseProgress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val start = releaseTravel.takeIf { it.isFinite() }?.coerceIn(0f, NATIVE_PAPER_COMPLETE_TRAVEL) ?: 0f
    return if (p <= origin) {
        if (origin <= 0f) 0f else start * p / origin
    } else {
        val target = if (completing) NATIVE_PAPER_COMPLETE_TRAVEL else 1f
        if (origin >= 1f) target else start + (target - start) * (p - origin) / (1f - origin)
    }.coerceIn(0f, NATIVE_PAPER_COMPLETE_TRAVEL)
}
