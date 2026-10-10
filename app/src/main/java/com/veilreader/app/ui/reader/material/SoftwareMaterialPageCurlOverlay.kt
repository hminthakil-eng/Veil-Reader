package com.veilreader.app.ui.reader.material

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Clean-room software backend for Material Page Engine.
 *
 * The GPU path remains preferred. This backend consumes the exact same Veil-owned snapshot,
 * physics frame and release state so Paper never silently degrades to Slide/static navigation
 * while a GL context is starting, unsupported, or recovering from a driver failure.
 */
internal const val SOFTWARE_PAGE_MESH_COLUMNS = 24
internal const val SOFTWARE_PAGE_MESH_ROWS = 32

internal fun softwareMaterialPageMesh(
    widthPx: Float,
    heightPx: Float,
    frame: GpuPageCurlFrame,
    side: MaterialPageSide,
    columns: Int = SOFTWARE_PAGE_MESH_COLUMNS,
    rows: Int = SOFTWARE_PAGE_MESH_ROWS
): FloatArray {
    if (
        !widthPx.isFinite() || !heightPx.isFinite() ||
        widthPx <= 0f || heightPx <= 0f ||
        columns <= 0 || rows <= 0
    ) {
        return FloatArray(0)
    }

    val aspect = (heightPx / widthPx).coerceIn(0.45f, 2.8f)
    val tilt = frame.cylinderTilt.takeIf { it.isFinite() } ?: 0f
    val directionLength = sqrt(tilt * tilt + 1f).coerceAtLeast(0.0001f)
    val directionX = tilt / directionLength
    val directionY = 1f / directionLength
    val normalX = directionY
    val normalY = -directionX
    val radius = frame.radius.takeIf { it.isFinite() }?.coerceIn(0.018f, 0.16f) ?: 0.052f
    val cylinderY = frame.cylinderY.takeIf { it.isFinite() }?.coerceIn(0.02f, 0.98f) ?: 0.5f
    val cylinderX = frame.cylinderX.takeIf { it.isFinite() } ?: 1f
    val right = side == MaterialPageSide.RIGHT
    val vertices = FloatArray((columns + 1) * (rows + 1) * 2)

    var out = 0
    for (y in 0..rows) {
        val fy = y.toFloat() / rows.toFloat()
        for (x in 0..columns) {
            val fx = x.toFloat() / columns.toFloat()
            val pageX = if (right) fx else 1f - fx
            val pageY = fy * aspect

            val relativeX = pageX - cylinderX
            val relativeY = pageY - cylinderY * aspect
            val distanceToAxis = relativeX * normalX + relativeY * normalY

            var deformedX = pageX
            var deformedY = pageY
            var lift = 0f

            if (distanceToAxis > 0f) {
                val projectionX = pageX - normalX * distanceToAxis
                val projectionY = pageY - normalY * distanceToAxis
                val halfTurn = PI.toFloat() * radius

                if (distanceToAxis <= halfTurn) {
                    val angle = distanceToAxis / radius
                    val s = sin(angle)
                    val c = cos(angle)
                    deformedX = projectionX + normalX * (s * radius)
                    deformedY = projectionY + normalY * (s * radius)
                    lift = (1f - c) * radius
                } else {
                    val displacement = 2f * distanceToAxis - halfTurn
                    deformedX = pageX - normalX * displacement
                    deformedY = pageY - normalY * displacement
                    lift = 2f * radius
                }
            }

            // A small perspective compression makes the 2D mesh read as a lifted sheet while
            // keeping the page edge exactly controlled by the shared cylinder model.
            val liftFraction = (lift / max(2f * radius, 0.0001f)).coerceIn(0f, 1f)
            val perspective = 1f - liftFraction * 0.045f
            val pivotY = cylinderY * aspect
            deformedY = pivotY + (deformedY - pivotY) * perspective

            val outputX = if (right) deformedX else 1f - deformedX
            val outputY = deformedY / aspect
            vertices[out++] = outputX * widthPx
            vertices[out++] = outputY * heightPx
        }
    }
    return vertices
}

@Composable
internal fun SoftwareMaterialPageCurlOverlay(
    state: MaterialPageEngineState,
    modifier: Modifier = Modifier,
    highContrast: Boolean = false
) {
    val bitmap = state.snapshot
    val active = state.active
    val epoch = state.sheetEpoch
    val progress = state.progress
    val verticalBias = state.verticalBias
    val pullOriginY = state.pullOriginY
    val diagonalPull = state.diagonalPull
    val pointerTravel = state.pointerTravel
    val edgeTravel = state.edgeTravel
    val profile = state.profile
    val side = state.side
    val visualAlpha = state.visualAlpha

    LaunchedEffect(active, epoch, bitmap) {
        if (active && epoch > 0L && bitmap != null && !bitmap.isRecycled) {
            // Compose Canvas has no acquired-buffer callback like TextureView. Waiting for a
            // committed frame is the software equivalent before navigation may reveal the target.
            withFrameNanos { }
            state.acknowledgeSheetPresented(epoch)
        }
    }

    Canvas(modifier = modifier) {
        val source = bitmap
        if (!active || source == null || source.isRecycled || source.width <= 0 || source.height <= 0) {
            return@Canvas
        }

        val frame = gpuPageCurlFrame(
            progress = progress,
            verticalBias = verticalBias,
            pullOriginY = pullOriginY,
            diagonalPull = diagonalPull,
            pointerTravel = pointerTravel,
            edgeTravel = edgeTravel,
            pageAspect = source.height.toFloat() / source.width.toFloat(),
            profile = profile,
            side = side
        )
        val mesh = softwareMaterialPageMesh(
            widthPx = size.width,
            heightPx = size.height,
            frame = frame,
            side = side
        )
        if (mesh.isEmpty()) return@Canvas

        drawIntoCanvas { composeCanvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (visualAlpha.coerceIn(0f, 1f) * 255f).toInt()
            }
            composeCanvas.nativeCanvas.drawBitmapMesh(
                source,
                SOFTWARE_PAGE_MESH_COLUMNS,
                SOFTWARE_PAGE_MESH_ROWS,
                mesh,
                0,
                null,
                0,
                paint
            )
        }

        val foldXNormalized =
            if (side == MaterialPageSide.RIGHT) frame.cylinderX else 1f - frame.cylinderX
        val foldX = foldXNormalized * size.width
        val shadowHalfWidth =
            (size.width * (0.035f + frame.radius * 1.8f)).coerceAtLeast(10.dp.toPx())
        val shadowAlpha =
            (frame.shadowStrength * visualAlpha * if (highContrast) 1.18f else 1f)
                .coerceIn(0f, 0.30f)

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = shadowAlpha),
                    Color.Transparent
                ),
                startX = foldX - shadowHalfWidth,
                endX = foldX + shadowHalfWidth
            ),
            topLeft = Offset(foldX - shadowHalfWidth, 0f),
            size = Size(shadowHalfWidth * 2f, size.height)
        )

        val edgeAlpha =
            (0.10f + frame.edgeStrength * 0.24f) * visualAlpha.coerceIn(0f, 1f)
        drawLine(
            color = Color.White.copy(alpha = edgeAlpha.coerceIn(0f, 0.30f)),
            start = Offset(foldX, 0f),
            end = Offset(foldX, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
}
