package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** Reused mesh, paths, paints and colour lookup tables. No bitmap allocation on a draw frame. */
internal class MaterialPageRenderer {
    val geometry = MaterialPageGeometry()
    private val front = MaterialFaceMesh(geometry.columns, geometry.rows)
    private val back = MaterialFaceMesh(geometry.columns, geometry.rows)
    private val boundary = Path()
    private val shadowOutline = Path()
    private val horizon = IntArray(geometry.rows + 1)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val frontColors = IntArray(geometry.normals.size)
    private val backColors = IntArray(geometry.normals.size)
    private val light = FloatArray(256)
    private var source: Bitmap? = null
    private var sourceShader: BitmapShader? = null
    private var disposed = false
    private val destination = RectF()
    private var profile: PageMaterialProfile? = null

    fun dispose() {
        disposed = true
        paint.shader = null
        sourceShader = null
        source = null
    }

    fun draw(canvas: Canvas, bitmap: Bitmap, width: Float, height: Float, progress: Float,
             originY: Float, tilt: Float, mirror: Boolean, config: MaterialTurnConfiguration,
             backColor: Int, edgeColor: Int, density: Float, highContrast: Boolean = false) {
        if (disposed || bitmap.isRecycled || !width.isFinite() || !height.isFinite() ||
            width <= 0f || height <= 0f) return
        val p = finiteUnit(progress)
        if (p >= 1f) return
        if (p == 0f) {
            // The resting publication is exact: material noise never modulates readable ink.
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.alpha = 255
            destination.set(0f, 0f, width, height)
            canvas.drawBitmap(bitmap, null, destination, paint)
            return
        }
        val scale = if (density.isFinite()) density.coerceAtLeast(.1f) else 1f
        if (source !== bitmap) {
            source = bitmap
            sourceShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val m = config.material
        if (profile != m) {
            profile = m
            for (i in light.indices) {
                val n = i / 255f * 2f - 1f
                val diffuse = .83f + .17f * abs(n)
                val tangent = sqrt((1f - n * n).coerceAtLeast(0f))
                val halfLight = (abs(n) * .86f + tangent * .51f).coerceIn(0f, 1f)
                light[i] = diffuse + m.specular * halfLight.pow(8f + (1f - m.roughness) * 40f)
            }
        }
        geometry.update(width, height, progress, originY, tilt, m, mirror)
        boundary.rewind(); shadowOutline.rewind()
        val v = geometry.vertices
        val stride = geometry.columns + 1
        // Shadow the outside silhouette, never the internal tessellation edges.
        val outsideCol = if (mirror) 0 else geometry.columns
        var hasBack = false
        for (row in 0..geometry.rows) {
            var column = outsideCol
            if (mirror) {
                while (column < geometry.columns && geometry.normals[row * stride + column] < 0f) column++
            } else {
                while (column > 0 && geometry.normals[row * stride + column] < 0f) column--
            }
            horizon[row] = row * stride + column
            hasBack = hasBack || column != outsideCol
            val i = horizon[row]
            if (row == 0) shadowOutline.moveTo(v[i * 2], v[i * 2 + 1])
            else shadowOutline.lineTo(v[i * 2], v[i * 2 + 1])
        }
        for (row in geometry.rows downTo 0) {
            val i = row * stride + outsideCol
            shadowOutline.lineTo(v[i * 2], v[i * 2 + 1])
        }
        shadowOutline.close()
        val dark = Color.red(backColor) + Color.green(backColor) + Color.blue(backColor) < 260
        val age = finiteUnit(config.age) * if (dark) .25f else 1f
        for (i in geometry.normals.indices) {
            val baseShade = light[((geometry.normals[i] + 1f) * 127.5f).toInt().coerceIn(0, 255)]
            val shade = if (highContrast) baseShade + (1f - baseShade).coerceAtLeast(0f) * .4f else baseShade
            val noise = (((i * 1103515245 + 12345) ushr 16) and 255) / 255f - .5f
            val fibre = if ((i / stride) % 3 == 0) m.directionalFibre * .012f else 0f
            val grain = (noise * m.grain * (1f + age * .5f) - fibre) * if (highContrast) .35f else 1f
            frontColors[i] = tint(Color.WHITE, (shade + grain).coerceAtMost(1f), 0f)
            backColors[i] = tint(backColor, shade + grain, if (dark) 0f else m.warmth + age * .025f)
        }
        front.update(geometry, bitmap.width.toFloat(), bitmap.height.toFloat(), frontColors, true)
        back.update(geometry, bitmap.width.toFloat(), bitmap.height.toFloat(), backColors, false)
        val lift = kotlin.math.sin(Math.PI * finiteUnit(progress)).toFloat().coerceAtLeast(0f)
        val saved = canvas.save()
        canvas.clipRect(0f, 0f, width, height)
        // Tight contact shadow plus a soft, inexpensive penumbra under the lifted region.
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        for (pass in 3 downTo 1) {
            paint.strokeWidth = (pass * 5f + geometry.radius / width * 18f) * scale
            paint.color = Color.argb((lift * (if (dark) 5f else 9f)).toInt(), 0, 0, 0)
            canvas.save()
            canvas.translate((if (mirror) -1f else 1f) * 3f * scale * lift, 2f * scale * lift)
            if (hasBack) canvas.drawPath(shadowOutline, paint)
            canvas.restore()
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = 255
        // Explicit face triangles prevent source ink from an occluded face leaking through
        // a projected clipping path. Both faces share the same interpolated horizon vertices.
        paint.shader = sourceShader
        drawFace(canvas, front, front.colors)
        paint.shader = null
        drawFace(canvas, back, back.colors)
        // Ink ghosting uses ONLY the reversed back-face texture coordinates.
        paint.shader = sourceShader
        paint.alpha = (255f * m.translucency * lift * (if (dark) .45f else 1f) *
            (if (highContrast) .4f else 1f)).toInt()
        drawFace(canvas, back, null)
        paint.shader = null
        paint.alpha = 255
        // Draw only the physical outside edge, never internal mesh cell edges.
        val edgeCol = if (mirror) 0 else geometry.columns
        for (row in 0..geometry.rows) {
            val i = row * stride + edgeCol
            val irregular = m.edgeIrregularity * scale * if (row % 2 == 0) .5f else -.5f
            if (row == 0) boundary.moveTo(v[i * 2], v[i * 2 + 1])
            else boundary.lineTo(v[i * 2] + irregular, v[i * 2 + 1])
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = m.thicknessDp * scale
        paint.color = edgeColor
        paint.alpha = (lift * if (highContrast) 180f else 150f).toInt()
        canvas.drawPath(boundary, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        canvas.restoreToCount(saved)
    }

    private fun drawFace(canvas: Canvas, face: MaterialFaceMesh, colors: IntArray?) {
        if (face.count == 0) return
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, face.count * 2, face.vertices, 0,
            if (paint.shader != null) face.texture else null, 0, colors, 0, null, 0, 0, paint)
    }

    private fun tint(color: Int, brightness: Float, warmth: Float): Int = Color.rgb(
        (Color.red(color) * brightness).toInt().coerceIn(0, 255),
        (Color.green(color) * brightness * (1f - warmth * .10f)).toInt().coerceIn(0, 255),
        (Color.blue(color) * brightness * (1f - warmth * .35f)).toInt().coerceIn(0, 255)
    )
}

/** Used by both the publication overlay and inspectable review states. Decorative, never input. */
@Composable
internal fun MaterialPageSurface(
    bitmap: Bitmap,
    progress: () -> Float,
    originY: () -> Float,
    tilt: () -> Float,
    mirror: Boolean,
    config: MaterialTurnConfiguration,
    backColor: Int,
    edgeColor: Int,
    modifier: Modifier = Modifier,
    viewportMatches: (Float, Float) -> Boolean = { _, _ -> true },
    preparedRenderer: MaterialPageRenderer? = null
) {
    val renderer = preparedRenderer ?: remember { MaterialPageRenderer() }
    val highContrast = LocalVeilHighContrast.current
    if (preparedRenderer == null) {
        DisposableEffect(renderer) { onDispose { renderer.dispose() } }
    }
    ComposeCanvas(modifier.fillMaxSize()) {
        // Rotation/reflow invalidates captured text: reveal the live publication rather than
        // stretching stale ink while the navigation owner finishes cancellation.
        if (!viewportMatches(size.width, size.height)) return@ComposeCanvas
        drawIntoCanvas {
            renderer.draw(it.nativeCanvas, bitmap, size.width, size.height, progress(), originY(),
                tilt(), mirror, config, backColor, edgeColor, density, highContrast)
        }
    }
}
