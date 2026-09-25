package com.veilreader.app.ui.screens

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

internal fun paperFoldLift(progress: Float): Float =
    sin(progress.coerceIn(0f, 1f) * PI.toFloat()).coerceIn(0f, 1f)

internal fun paperWeightedDragCurrent(
    start: Offset,
    current: Offset,
    response: Float = 0.92f
): Offset {
    val safeResponse = response.coerceIn(0f, 1f)
    return start + (current - start) * safeResponse
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
