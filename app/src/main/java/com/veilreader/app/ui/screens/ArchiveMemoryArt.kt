package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.EchoDepth
import com.veilreader.app.domain.HighlightMemory
import com.veilreader.app.ui.theme.VeilPalette

internal data class ArchiveMemoryMaterialPolicy(
    val strataCount: Int,
    val patina: Float,
    val edgeGlow: Float,
    val spiritTrace: Float,
    val resurfaced: Boolean
)

internal fun archiveMemoryMaterialFor(
    memory: HighlightMemory,
    echoMode: Boolean
): ArchiveMemoryMaterialPolicy {
    val agePatina = when (memory.echoDepth) {
        EchoDepth.FRESH -> 0.08f
        EchoDepth.TRACE -> 0.24f
        EchoDepth.ECHO -> 0.52f
        EchoDepth.DEEP_ECHO -> 0.82f
    }
    val revisit = (memory.revisitCount / 4f).coerceIn(0f, 1f)
    val annotation = if (memory.annotated) 0.16f else 0f
    val resurfaced = echoMode && memory.eligibleForEcho

    return ArchiveMemoryMaterialPolicy(
        strataCount = when (memory.echoDepth) {
            EchoDepth.FRESH -> 1
            EchoDepth.TRACE -> 2
            EchoDepth.ECHO -> 3
            EchoDepth.DEEP_ECHO -> 4
        },
        patina = (agePatina + annotation).coerceIn(0f, 1f),
        edgeGlow = (
            0.10f +
                if (resurfaced) 0.24f else 0f +
                revisit * 0.16f
            ).coerceIn(0.08f, 0.56f),
        spiritTrace = (
            revisit * 0.56f +
                if (memory.bookActivityAfterMark) 0.14f else 0f
            ).coerceIn(0f, 0.72f),
        resurfaced = resurfaced
    )
}

@Composable
internal fun ArchiveMemoryField(
    memory: HighlightMemory,
    echoMode: Boolean,
    modifier: Modifier = Modifier
) {
    val policy = archiveMemoryMaterialFor(memory, echoMode)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val patina = policy.patina
        val edgeGlow = policy.edgeGlow
        val spirit = policy.spiritTrace

        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    VeilPalette.Brass.copy(alpha = edgeGlow * 0.18f),
                    Color.Transparent,
                    VeilPalette.Spirit.copy(alpha = spirit * 0.10f)
                )
            ),
            size = size
        )

        repeat(policy.strataCount) { index ->
            val fraction = (index + 1f) / (policy.strataCount + 1f)
            val x = w * (0.018f + fraction * 0.045f)
            drawLine(
                color = VeilPalette.Brass.copy(
                    alpha = (0.035f + patina * 0.065f) * (1f - index * 0.10f)
                ),
                start = Offset(x, h * 0.08f),
                end = Offset(x, h * 0.92f),
                strokeWidth = 0.7.dp.toPx()
            )
        }

        val fleckCount = (2 + (patina * 8f).toInt()).coerceIn(2, 10)
        repeat(fleckCount) { index ->
            val xUnit = ((index * 37 + memory.ageDays * 3 + 11) % 89) / 88f
            val yUnit = ((index * 53 + memory.revisitCount * 7 + 5) % 71) / 70f
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = 0.025f + patina * 0.055f),
                radius = if (index % 3 == 0) 1.1.dp.toPx() else 0.65.dp.toPx(),
                center = Offset(
                    w * (0.08f + xUnit * 0.84f),
                    h * (0.10f + yUnit * 0.80f)
                )
            )
        }

        if (policy.resurfaced) {
            val center = Offset(w * 0.88f, h * 0.22f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VeilPalette.Brass.copy(alpha = 0.10f + edgeGlow * 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.32f
                ),
                center = center,
                radius = size.minDimension * 0.32f
            )
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = 0.18f + edgeGlow * 0.18f),
                radius = size.minDimension * 0.11f,
                center = center,
                style = Stroke(0.8.dp.toPx())
            )
            drawCircle(
                color = VeilPalette.Spirit.copy(alpha = 0.10f + spirit * 0.12f),
                radius = size.minDimension * 0.075f,
                center = center,
                style = Stroke(0.65.dp.toPx())
            )
        }

        if (memory.revisitCount > 0) {
            val width = (w * 0.18f).coerceAtMost(92.dp.toPx())
            drawRect(
                color = VeilPalette.Spirit.copy(alpha = 0.025f + spirit * 0.06f),
                topLeft = Offset(w - width, h * 0.80f),
                size = Size(width, h * 0.12f)
            )
        }
    }
}
