package com.veilreader.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Art-direction realms deliberately have different visual budgets.
 *
 * The shell can be rich; the Reader sanctuary must stay quiet.
 */
enum class VeilRealm {
    SANCTUARY,
    THRESHOLD,
    ARCHIVE,
    CASTLE,
    WORLD,
    RITUAL,
    SANCTUM
}

data class VeilVisualBudget(
    val richness: Float,
    val atmosphere: Float,
    val ornament: Float,
    val motion: Float
)

fun visualBudgetFor(realm: VeilRealm): VeilVisualBudget =
    when (realm) {
        VeilRealm.SANCTUARY -> VeilVisualBudget(
            richness = 0.05f,
            atmosphere = VeilSanctuary.atmosphereIntensity,
            ornament = 0.00f,
            motion = 0.10f
        )
        VeilRealm.THRESHOLD -> VeilVisualBudget(
            richness = 0.48f,
            atmosphere = 0.56f,
            ornament = 0.34f,
            motion = 0.34f
        )
        VeilRealm.ARCHIVE -> VeilVisualBudget(
            richness = 0.58f,
            atmosphere = 0.46f,
            ornament = 0.48f,
            motion = 0.28f
        )
        VeilRealm.CASTLE,
        VeilRealm.WORLD -> VeilVisualBudget(
            richness = 0.76f,
            atmosphere = 0.72f,
            ornament = 0.66f,
            motion = 0.52f
        )
        VeilRealm.RITUAL -> VeilVisualBudget(
            richness = 0.86f,
            atmosphere = 0.78f,
            ornament = 0.82f,
            motion = 0.74f
        )
        VeilRealm.SANCTUM -> VeilVisualBudget(
            richness = 0.82f,
            atmosphere = 0.80f,
            ornament = 0.74f,
            motion = 0.36f
        )
    }

/**
 * Lightweight procedural atmosphere: no bitmap allocation, blur, network, or random state.
 * It creates a shared visual field that can sit behind scrollable content without competing
 * with text. Richness is realm-controlled so Sanctuary never inherits Castle-level decoration.
 */
fun Modifier.grayfogAtmosphere(
    realm: VeilRealm,
    seed: Int = 0,
    intensity: Float = 1f,
    qualityTier: VeilQualityTier = VeilQualityTier.FULL
): Modifier = drawBehind {
    val budget = visualBudgetFor(realm)
    val quality = qualityPolicyFor(qualityTier)
    val atmosphere = (
        budget.atmosphere *
            intensity *
            quality.atmosphereMultiplier
        ).coerceIn(0f, 1f)
    if (atmosphere <= 0.001f) return@drawBehind

    val w = size.width
    val h = size.height
    val brass = VeilPalette.Brass
    val archive = VeilPalette.Archive
    val ink = VeilPalette.Ink

    drawRect(
        brush = Brush.verticalGradient(
            0f to archive.copy(alpha = 0.18f * atmosphere),
            0.46f to ink.copy(alpha = 0.06f * atmosphere),
            1f to ink.copy(alpha = 0.48f * atmosphere)
        ),
        size = size
    )

    val lightCenterX = w * (0.32f + ((seed and 3) * 0.11f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                brass.copy(alpha = 0.075f * atmosphere),
                brass.copy(alpha = 0.018f * atmosphere),
                Color.Transparent
            ),
            center = Offset(lightCenterX, h * 0.08f),
            radius = size.minDimension * 0.62f
        ),
        radius = size.minDimension * 0.62f,
        center = Offset(lightCenterX, h * 0.08f)
    )

    // Architectural memory: a few enormous arches rather than a decorative tiled pattern.
    repeat(3) { index ->
        val inset = w * (0.08f + index * 0.075f)
        drawArc(
            color = brass.copy(alpha = (0.035f + index * 0.012f) * atmosphere),
            startAngle = 192f,
            sweepAngle = 156f,
            useCenter = false,
            topLeft = Offset(inset, -h * (0.06f + index * 0.015f)),
            size = Size(w - inset * 2f, h * (0.42f + index * 0.06f)),
            style = Stroke(1.dp.toPx())
        )
    }

    // Fog strata are intentionally broad; narrow bands read as UI decoration rather than space.
    repeat(3) { index ->
        val y = h * (0.34f + index * 0.21f)
        val bandHeight = h * (0.12f + index * 0.025f)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    VeilPalette.Mist.copy(alpha = (0.018f + index * 0.006f) * atmosphere),
                    Color.Transparent
                ),
                startY = y,
                endY = y + bandHeight
            ),
            topLeft = Offset(0f, y),
            size = Size(w, bandHeight)
        )
    }

    // Deterministic dust points. They should be felt at rest, not read as particles.
    repeat(14) { index ->
        val xUnit = ((index * 37 + seed * 11 + 17) % 101) / 100f
        val yUnit = ((index * 61 + seed * 7 + 29) % 103) / 102f
        val radius = if (index % 4 == 0) 1.15.dp.toPx() else 0.65.dp.toPx()
        drawCircle(
            color = VeilPalette.Moon.copy(
                alpha = (0.018f + (index % 3) * 0.006f) * atmosphere
            ),
            radius = radius,
            center = Offset(w * xUnit, h * yUnit)
        )
    }

    // Edge falloff preserves focus without expensive blur.
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                ink.copy(alpha = 0.36f * atmosphere),
                Color.Transparent,
                Color.Transparent,
                ink.copy(alpha = 0.30f * atmosphere)
            )
        ),
        size = size
    )
}

@Composable
fun GrayfogOrnamentFrame(
    modifier: Modifier = Modifier,
    strength: Float = 1f
) {
    Canvas(modifier) {
        val s = strength.coerceIn(0f, 1f)
        if (s <= 0.001f) return@Canvas

        val line = VeilPalette.Brass.copy(alpha = 0.28f * s)
        val glow = VeilPalette.Brass.copy(alpha = 0.12f * s)
        val inset = 10.dp.toPx()
        val corner = 22.dp.toPx()
        val stroke = 1.dp.toPx()

        fun cornerMark(x: Float, y: Float, xDir: Float, yDir: Float) {
            drawLine(
                color = line,
                start = Offset(x, y),
                end = Offset(x + corner * xDir, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = line,
                start = Offset(x, y),
                end = Offset(x, y + corner * yDir),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = glow,
                radius = 2.2.dp.toPx(),
                center = Offset(x + 4.dp.toPx() * xDir, y + 4.dp.toPx() * yDir)
            )
        }

        cornerMark(inset, inset, 1f, 1f)
        cornerMark(size.width - inset, inset, -1f, 1f)
        cornerMark(inset, size.height - inset, 1f, -1f)
        cornerMark(size.width - inset, size.height - inset, -1f, -1f)

        drawLine(
            color = line.copy(alpha = line.alpha * 0.58f),
            start = Offset(size.width * 0.38f, inset),
            end = Offset(size.width * 0.62f, inset),
            strokeWidth = stroke
        )
    }
}
