package com.veilreader.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.LibraryAtmosphereState

/** Slow ambient changes follow local time without changing book content or contrast. */
enum class ArchiveTimePhase(
    val lampMultiplier: Float,
    val fogMultiplier: Float
) {
    DAWN(0.95f, 0.90f),
    DAY(0.72f, 0.78f),
    DUSK(1.12f, 1.05f),
    NIGHT(1.24f, 1.10f)
}

internal fun archiveTimePhaseForHour(hour: Int): ArchiveTimePhase = when (hour) {
    in 0..5, in 20..23 -> ArchiveTimePhase.NIGHT
    in 6..8 -> ArchiveTimePhase.DAWN
    in 9..16 -> ArchiveTimePhase.DAY
    in 17..19 -> ArchiveTimePhase.DUSK
    else -> ArchiveTimePhase.DAY
}

/**
 * A procedural architectural layer for Grayfog Archive.
 *
 * Every visible increase in geometry is driven by LibraryAtmosphereState. It adds no persistent
 * state, no network work, no bitmap allocation and no animation loop.
 */
fun Modifier.libraryArchiveAtmosphere(
    state: LibraryAtmosphereState,
    seed: Int = 0,
    timePhase: ArchiveTimePhase = ArchiveTimePhase.DAY
): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@drawBehind

    val brass = VeilPalette.Brass
    val ink = VeilPalette.Ink
    val archive = VeilPalette.Archive
    val mist = VeilPalette.Mist
    val spirit = VeilPalette.Spirit

    val vanishingX = w * (0.48f + ((seed and 7) - 3) * 0.008f)
    val vanishingY = h * 0.265f

    // Distant stack planes: denser libraries reveal deeper architecture instead of more cards.
    repeat(state.distantStackLayers) { index ->
        val t = (index + 1f) / (state.distantStackLayers + 1f)
        val left = w * (0.035f + t * 0.115f)
        val right = w - left
        val top = h * (0.10f + t * 0.055f)
        val bottom = h * (0.89f - t * 0.035f)
        val alpha = 0.028f + state.archiveDensity * 0.026f

        drawLine(
            color = brass.copy(alpha = alpha),
            start = Offset(left, bottom),
            end = Offset(vanishingX, vanishingY),
            strokeWidth = 0.7.dp.toPx()
        )
        drawLine(
            color = brass.copy(alpha = alpha),
            start = Offset(right, bottom),
            end = Offset(vanishingX, vanishingY),
            strokeWidth = 0.7.dp.toPx()
        )
        drawLine(
            color = archive.copy(alpha = 0.16f + state.archiveDensity * 0.06f),
            start = Offset(left, top),
            end = Offset(left, bottom),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = archive.copy(alpha = 0.16f + state.archiveDensity * 0.06f),
            start = Offset(right, top),
            end = Offset(right, bottom),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Great arches scale with real archive density.
    repeat(state.archLayers) { index ->
        val inset = w * (0.055f + index * 0.046f)
        val topOffset = -h * (0.105f + index * 0.017f)
        val archHeight = h * (0.42f + index * 0.045f)
        drawArc(
            color = brass.copy(
                alpha = 0.026f + state.archiveDensity * (0.030f + index * 0.004f)
            ),
            startAngle = 193f,
            sweepAngle = 154f,
            useCenter = false,
            topLeft = Offset(inset, topOffset),
            size = Size(w - inset * 2f, archHeight),
            style = Stroke(
                width = if (index == 0) 1.15.dp.toPx() else 0.75.dp.toPx()
            )
        )
    }

    // Shelf ribs grow from actual archive density. They stay in the periphery.
    repeat(state.shelfBays) { index ->
        val y = h * (0.34f + index * 0.045f)
        if (y >= h * 0.88f) return@repeat
        val depth = (index + 1f) / state.shelfBays.coerceAtLeast(1)
        val alpha = 0.018f + state.archiveDensity * 0.040f
        val leftEnd = w * (0.13f + depth * 0.07f)
        val rightStart = w - leftEnd

        drawLine(
            color = brass.copy(alpha = alpha),
            start = Offset(w * 0.025f, y),
            end = Offset(leftEnd, y - h * 0.012f),
            strokeWidth = 0.65.dp.toPx()
        )
        drawLine(
            color = brass.copy(alpha = alpha),
            start = Offset(rightStart, y - h * 0.012f),
            end = Offset(w * 0.975f, y),
            strokeWidth = 0.65.dp.toPx()
        )
    }

    // Active reading warms a small number of architectural lamps.
    repeat(state.lampCount) { index ->
        val span = state.lampCount.coerceAtLeast(1)
        val x = w * (0.16f + (index + 0.5f) / span * 0.68f)
        val y = h * (0.115f + (index % 2) * 0.035f)
        val radius = size.minDimension * (0.12f + state.memoryWarmth * 0.045f)
        val lampGlow = (state.brassGlow * timePhase.lampMultiplier).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brass.copy(alpha = lampGlow),
                    brass.copy(alpha = lampGlow * 0.26f),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = radius
            ),
            center = Offset(x, y),
            radius = radius
        )
        drawCircle(
            color = brass.copy(alpha = ((0.18f + state.memoryWarmth * 0.18f) * timePhase.lampMultiplier).coerceIn(0f, 1f)),
            center = Offset(x, y),
            radius = 1.25.dp.toPx()
        )
    }

    // Completed volumes create distant sealed alcoves, never badges over book covers.
    repeat(state.completedAlcoves) { index ->
        val side = if (index % 2 == 0) 0 else 1
        val row = index / 2
        val alcoveW = w * 0.055f
        val alcoveH = h * 0.075f
        val x = if (side == 0) w * 0.075f else w - w * 0.075f - alcoveW
        val y = h * (0.20f + row * 0.095f)

        drawRoundRect(
            color = ink.copy(alpha = 0.28f),
            topLeft = Offset(x, y),
            size = Size(alcoveW, alcoveH),
            cornerRadius = CornerRadius(alcoveW * 0.46f, alcoveW * 0.46f)
        )
        drawRoundRect(
            color = brass.copy(alpha = 0.10f + state.brassGlow * 0.34f),
            topLeft = Offset(x, y),
            size = Size(alcoveW, alcoveH),
            cornerRadius = CornerRadius(alcoveW * 0.46f, alcoveW * 0.46f),
            style = Stroke(0.75.dp.toPx())
        )
        drawCircle(
            color = brass.copy(alpha = 0.16f + state.brassGlow * 0.42f),
            center = Offset(x + alcoveW * 0.5f, y + alcoveH * 0.57f),
            radius = 2.1.dp.toPx(),
            style = Stroke(0.7.dp.toPx())
        )
    }

    // Deep Shelf density opens darker side corridors. Deep quiet affects atmosphere, not access.
    repeat(state.deepCorridors) { index ->
        val leftSide = index % 2 == 0
        val row = index / 2
        val corridorW = w * (0.085f + row * 0.012f)
        val corridorH = h * (0.14f + row * 0.02f)
        val x = if (leftSide) w * 0.015f else w - w * 0.015f - corridorW
        val y = h * (0.53f + row * 0.15f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    archive.copy(alpha = 0.14f),
                    ink.copy(alpha = 0.44f + state.deepQuiet * 0.18f)
                ),
                startY = y,
                endY = y + corridorH
            ),
            topLeft = Offset(x, y),
            size = Size(corridorW, corridorH),
            cornerRadius = CornerRadius(corridorW * 0.45f, corridorW * 0.45f)
        )
        drawArc(
            color = brass.copy(alpha = 0.025f + state.deepQuiet * 0.055f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(x, y - corridorW * 0.42f),
            size = Size(corridorW, corridorW * 0.84f),
            style = Stroke(0.7.dp.toPx())
        )
    }

    // Deterministic dust becomes more legible as the archive gains physical depth and age.
    repeat(state.dustMotes) { index ->
        val xUnit = ((index * 37 + seed * 13 + 19) % 103) / 102f
        val yUnit = ((index * 61 + seed * 5 + 31) % 107) / 106f
        val larger = index % 7 == 0
        drawCircle(
            color = if (index % 5 == 0) {
                brass.copy(alpha = 0.020f + state.deepQuiet * 0.022f)
            } else {
                mist.copy(alpha = 0.014f + state.archiveDensity * 0.018f)
            },
            radius = if (larger) 1.05.dp.toPx() else 0.55.dp.toPx(),
            center = Offset(w * xUnit, h * yUnit)
        )
    }

    // A faint central sightline gives the Archive a destination rather than a tiled wallpaper.
    drawLine(
        color = spirit.copy(alpha = 0.018f + state.memoryWarmth * 0.020f),
        start = Offset(vanishingX, vanishingY),
        end = Offset(vanishingX, h * 0.92f),
        strokeWidth = 0.6.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Lower fog responds to active memory vs Deep Shelf quiet.
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.Transparent,
                mist.copy(alpha = (state.fogAlpha * 0.055f * timePhase.fogMultiplier).coerceIn(0f, 1f)),
                ink.copy(alpha = (state.fogAlpha * 0.62f * timePhase.fogMultiplier).coerceIn(0f, 1f))
            ),
            startY = h * 0.58f,
            endY = h
        ),
        topLeft = Offset(0f, h * 0.56f),
        size = Size(w, h * 0.44f)
    )

    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                ink.copy(alpha = 0.16f + state.archiveDensity * 0.09f),
                Color.Transparent,
                Color.Transparent,
                ink.copy(alpha = 0.16f + state.archiveDensity * 0.09f)
            )
        ),
        size = size
    )
}
