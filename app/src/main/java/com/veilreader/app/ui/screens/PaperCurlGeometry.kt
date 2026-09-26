package com.veilreader.app.ui.screens

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.readium.r2.navigator.preferences.ReadingProgression

internal data class PaperCurlEdge(
    val top: Offset,
    val bottom: Offset
) {
    companion object {
        val VectorConverter = TwoWayConverter<PaperCurlEdge, AnimationVector4D>(
            convertToVector = {
                AnimationVector4D(it.top.x, it.top.y, it.bottom.x, it.bottom.y)
            },
            convertFromVector = {
                PaperCurlEdge(
                    Offset(it.v1, it.v2),
                    Offset(it.v3, it.v4)
                )
            }
        )

        val VisibilityThreshold = PaperCurlEdge(
            Offset.VisibilityThreshold,
            Offset.VisibilityThreshold
        )
    }
}
internal enum class PaperReleaseRegime { MANIPULATION, FLING }

internal data class PaperReleaseProfile(
    val regime: PaperReleaseRegime,
    val durationMillis: Int,
    val completionBias: Float
)

internal data class PaperPageStackDepth(
    val leftDp: Float,
    val rightDp: Float
)

internal data class PaperCurlPolygon(
    val vertices: List<Offset>
) {
    private val size = vertices.size

    fun translate(offset: Offset): PaperCurlPolygon =
        PaperCurlPolygon(vertices.map { it + offset })

    fun offset(value: Float): PaperCurlPolygon {
        val edgeNormals = List(size) {
            val edge = vertices[index(it + 1)] - vertices[index(it)]
            Offset(edge.y, -edge.x).normalized()
        }
        val vertexNormals = List(size) {
            (edgeNormals[index(it - 1)] + edgeNormals[index(it)]).normalized()
        }
        return PaperCurlPolygon(
            vertices.mapIndexed { i, vertex ->
                vertex + vertexNormals[i] * value
            }
        )
    }

    fun toPath(): Path = Path().apply {
        vertices.forEachIndexed { i, vertex ->
            if (i == 0) moveTo(vertex.x, vertex.y)
            else lineTo(vertex.x, vertex.y)
        }
        close()
    }

    private fun index(i: Int): Int = ((i % size) + size) % size
}

internal fun paperReleaseProfile(releaseVelocityDpPerSec: Float): PaperReleaseProfile {
    val speed = releaseVelocityDpPerSec.coerceAtLeast(0f)
    return if (speed >= 900f) {
        val normalized = ((speed - 900f) / 2200f).coerceIn(0f, 1f)
        PaperReleaseProfile(
            regime = PaperReleaseRegime.FLING,
            durationMillis = (210f - 75f * normalized).toInt(),
            completionBias = 0.70f + 0.20f * normalized
        )
    } else {
        PaperReleaseProfile(
            regime = PaperReleaseRegime.MANIPULATION,
            durationMillis = 360,
            completionBias = 0.56f
        )
    }
}

internal fun paperPageStackDepth(
    progress: Float,
    progression: ReadingProgression
): PaperPageStackDepth {
    val p = progress.coerceIn(0f, 1f)
    val consumed = 2f + 6f * p
    val remaining = 2f + 6f * (1f - p)
    return if (progression == ReadingProgression.RTL) {
        PaperPageStackDepth(leftDp = remaining, rightDp = consumed)
    } else {
        PaperPageStackDepth(leftDp = consumed, rightDp = remaining)
    }
}

internal fun paperFoldLift(progress: Float): Float =
    sin(progress.coerceIn(0f, 1f).toDouble() * PI)
        .toFloat()
        .coerceIn(0f, 1f)

internal fun paperWeightedDragCurrent(
    start: Offset,
    current: Offset,
    response: Float = 0.90f,
    verticalResponse: Float = 0.68f
): Offset {
    val safeResponse = response.coerceIn(0f, 1f)
    val safeVerticalResponse = verticalResponse.coerceIn(0f, 1f)
    val delta = current - start
    return Offset(
        x = start.x + delta.x * safeResponse,
        y = start.y + delta.y * safeVerticalResponse
    )
}

/**
 * Real paper resists the first pull, then yields as the fold becomes established.
 * Keep this deterministic: the visual sheet must never outrun the user's finger.
 */
internal fun paperHorizontalDragResponse(inwardFraction: Float): Float {
    val t = inwardFraction.coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 0.74f + smooth * 0.20f
}

/**
 * Vertical finger wobble should influence the fold, but much less than the inward pull.
 * The fold loosens slightly once the page is already moving.
 */
internal fun paperVerticalDragResponse(inwardFraction: Float): Float {
    val t = inwardFraction.coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 0.46f + smooth * 0.20f
}

internal fun paperInwardDragFraction(
    start: Offset,
    current: Offset,
    pageWidth: Float
): Float {
    if (pageWidth <= 0f) return 0f
    return (abs(current.x - start.x) / pageWidth).coerceIn(0f, 1f)
}

/** Visible paper-edge thickness is strongest around the middle of a turn. */
internal fun paperEdgeThicknessIntensity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val centerWeight = 1f - abs(p - 0.5f) * 2f
    return (paperFoldLift(p) * (0.70f + 0.30f * centerWeight)).coerceIn(0f, 1f)
}

/** Backside ink is most visible while light can pass through a lifted sheet. */
internal fun paperBacksideInkIntensity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val lift = paperFoldLift(p)
    return (lift * (0.72f + 0.28f * p)).coerceIn(0f, 1f)
}

internal fun paperCreaseIntensity(progress: Float): Float {
    val lift = paperFoldLift(progress)
    val p = progress.coerceIn(0f, 1f)
    return (lift * (0.78f + p * 0.22f)).coerceIn(0f, 1f)
}

internal fun paperContactShadowIntensity(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val lift = paperFoldLift(p)
    return (lift * (0.62f + 0.38f * p)).coerceIn(0f, 1f)
}

internal fun Offset.paperRotate(angle: Float): Offset {
    val sine = sin(angle)
    val cosine = cos(angle)
    return Offset(
        x * cosine - y * sine,
        x * sine + y * cosine
    )
}

internal fun paperLineIntersection(
    line1a: Offset,
    line1b: Offset,
    line2a: Offset,
    line2b: Offset
): Offset? {
    val denominator =
        (line1a.x - line1b.x) * (line2a.y - line2b.y) -
            (line1a.y - line1b.y) * (line2a.x - line2b.x)
    if (denominator == 0f) return null

    val first =
        (line1a.x * line1b.y - line1a.y * line1b.x) *
            (line2a.x - line2b.x)
    val second =
        (line1a.x - line1b.x) *
            (line2a.x * line2b.y - line2a.y * line2b.x)
    val x = (first - second) / denominator

    val third =
        (line1a.x * line1b.y - line1a.y * line1b.x) *
            (line2a.y - line2b.y)
    val fourth =
        (line1a.y - line1b.y) *
            (line2a.x * line2b.y - line2a.y * line2b.x)
    val y = (third - fourth) / denominator
    return Offset(x, y)
}

private fun Offset.normalized(): Offset {
    val distance = getDistance()
    return if (distance != 0f) this / distance else this
}
