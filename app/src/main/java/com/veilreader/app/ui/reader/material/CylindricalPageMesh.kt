package com.veilreader.app.ui.reader.material

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** An inextensible cylindrical strip. Arc length is preserved through the bend; the trailing
 * portion continues as a plane, rather than stretching the source or making a jelly sheet.
 * Orthographic projection avoids dramatic perspective distortion of publication text.
 */
internal class MaterialPageGeometry(val columns: Int = 80, val rows: Int = 24) {
    val vertices = FloatArray((columns + 1) * (rows + 1) * 2)
    val normals = FloatArray((columns + 1) * (rows + 1))
    val heights = FloatArray(normals.size)
    var radius = 0f
        private set
    var fold = 0f
        private set

    init { require(columns >= 8 && rows >= 2) }

    fun update(width: Float, height: Float, progress: Float, originY: Float,
               tilt: Float, material: PageMaterialProfile, mirror: Boolean) {
        require(width.isFinite() && height.isFinite() && width > 0f && height > 0f)
        val p = finiteUnit(progress)
        val lift = finiteUnit(p / .12f).let { it * it * (3f - 2f * it) }
        radius = width * material.radiusFraction * lift
        // The silhouette clears at the terminal state, not prematurely during a deep drag.
        fold = width - p * (width + radius * 1.03f)
        val origin = finiteUnit(originY)
        val pullTilt = (tilt.takeIf { it.isFinite() } ?: 0f) - (origin - .5f) * .32f
        val anchor = origin * height
        val requestedSlope = pullTilt.coerceIn(-.22f, .22f) * sin(PI.toFloat() * p)
        // On tall/narrow pages, a diagonal crease must not detach the opposite binding corner.
        // This bound follows from distance(0,y) <= 0 at the farthest binding endpoint.
        val bindingReach = if (requestedSlope >= 0f) anchor else height - anchor
        val bindingLimit = fold.coerceAtLeast(0f) / bindingReach.coerceAtLeast(1f)
        val slope = requestedSlope.coerceIn(-bindingLimit, bindingLimit)
        // Rotate the fold coordinate system so slope changes cannot stretch the surface.
        val scale = 1f / kotlin.math.sqrt(1f + slope * slope)
        val nx = scale
        val ny = -slope * scale
        for (row in 0..rows) {
            val y = height * row / rows
            for (col in 0..columns) {
                // Texture coordinates stay in publication order even for a left-origin turn.
                val sourceX = width * col / columns
                val x = if (mirror) width - sourceX else sourceX
                val distance = (x - fold) * nx + (y - anchor) * ny
                val index = row * (columns + 1) + col
                var projected = distance
                var z = 0f
                var normal = 1f
                if (distance > 0f && radius > .0001f) {
                    val angle = (distance / radius).coerceAtMost(PI.toFloat())
                    projected = radius * sin(angle) - (distance - radius * PI.toFloat()).coerceAtLeast(0f)
                    z = radius * (1f - cos(angle))
                    normal = cos(angle)
                }
                val shift = projected - distance
                val px = x + shift * nx
                vertices[index * 2] = if (mirror) width - px else px
                vertices[index * 2 + 1] = y + shift * ny
                normals[index] = normal
                heights[index] = z
            }
        }
    }
}
