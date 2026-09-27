package com.veilreader.app.ui.theme

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
 * Narrative architecture grammar for Veil's world-shell.
 *
 * The inspiration layer is intentionally abstract: passage, lamplight, vertical stone/iron,
 * measured machinery, archival rails, fractured reflection. It never reproduces book-specific
 * symbols, names, maps, scenes, or illustrations.
 */
data class VeilNarrativeGrammar(
    val passageDepth: Float,
    val lampCount: Int,
    val verticalPierCount: Int,
    val archiveRailCount: Int,
    val mechanicalTickCount: Int,
    val fractureCount: Int,
    val bellMarkCount: Int
)

fun narrativeGrammarFor(realm: VeilRealm): VeilNarrativeGrammar =
    when (realm) {
        VeilRealm.SANCTUARY -> VeilNarrativeGrammar(0f, 0, 0, 0, 0, 0, 0)
        VeilRealm.THRESHOLD -> VeilNarrativeGrammar(0.28f, 2, 2, 0, 5, 1, 1)
        VeilRealm.ARCHIVE -> VeilNarrativeGrammar(0.40f, 3, 4, 5, 7, 2, 1)
        VeilRealm.CASTLE,
        VeilRealm.WORLD -> VeilNarrativeGrammar(0.74f, 4, 6, 2, 9, 2, 3)
        VeilRealm.RITUAL -> VeilNarrativeGrammar(0.88f, 4, 5, 1, 12, 3, 4)
        VeilRealm.SANCTUM -> VeilNarrativeGrammar(0.68f, 3, 5, 3, 8, 4, 2)
    }

/**
 * Static spatial layer. It adds perceived architecture instead of decorative wallpaper:
 * a vanishing passage, inhabited lamp pools, measured vertical piers, archive rails,
 * clockwork ticks, restrained fractures, and distant bell rings.
 */
fun Modifier.narrativeArchitectureField(
    realm: VeilRealm,
    seed: Int = 0,
    intensity: Float = 1f
): Modifier = drawBehind {
    val grammar = narrativeGrammarFor(realm)
    val budget = visualBudgetFor(realm)
    val strength = (budget.ornament * intensity).coerceIn(0f, 1f)
    if (strength <= 0.001f) return@drawBehind

    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@drawBehind

    val brass = VeilPalette.Brass
    val spirit = VeilPalette.Spirit
    val mist = VeilPalette.Mist
    val ink = VeilPalette.Ink

    // Passage / threshold geometry: perspective lines pull the eye inward without becoming a frame.
    if (grammar.passageDepth > 0f) {
        val vanishing = Offset(
            x = w * (0.50f + (((seed % 5) - 2) * 0.012f)),
            y = h * (0.18f + (1f - grammar.passageDepth) * 0.07f)
        )
        val alpha = 0.055f * grammar.passageDepth * strength
        val floorY = h * 0.92f

        listOf(0.08f, 0.92f).forEach { xUnit ->
            drawLine(
                color = brass.copy(alpha = alpha),
                start = Offset(w * xUnit, floorY),
                end = vanishing,
                strokeWidth = 0.8.dp.toPx()
            )
        }
        listOf(0.20f, 0.80f).forEach { xUnit ->
            drawLine(
                color = mist.copy(alpha = alpha * 0.55f),
                start = Offset(w * xUnit, h * 0.04f),
                end = vanishing,
                strokeWidth = 0.65.dp.toPx()
            )
        }

        repeat(3) { index ->
            val t = (index + 1f) / 4f
            val halfWidth = w * (0.40f * (1f - t) + 0.08f)
            val y = h * (0.82f - t * 0.48f)
            drawLine(
                color = brass.copy(alpha = alpha * (0.75f - index * 0.12f)),
                start = Offset(vanishing.x - halfWidth, y),
                end = Offset(vanishing.x + halfWidth, y),
                strokeWidth = 0.65.dp.toPx()
            )
        }
    }

    // Cathedral/industrial verticality: broad structural piers rather than ornamental stripes.
    repeat(grammar.verticalPierCount) { index ->
        val x = w * ((index + 1f) / (grammar.verticalPierCount + 1f))
        val jitter = (((seed + index * 13) % 7) - 3) * 0.0025f * w
        drawLine(
            color = brass.copy(alpha = (0.020f + index % 2 * 0.008f) * strength),
            start = Offset(x + jitter, h * 0.04f),
            end = Offset(x + jitter, h * 0.88f),
            strokeWidth = if (index % 3 == 0) 1.dp.toPx() else 0.6.dp.toPx()
        )
    }

    // Inhabited lamp pools: small warm anchors, never bright enough to compete with text.
    repeat(grammar.lampCount) { index ->
        val x = w * ((index + 1f) / (grammar.lampCount + 1f))
        val y = h * (0.12f + (index % 2) * 0.055f)
        val center = Offset(x, y)
        val radius = size.minDimension * (0.10f + (index % 3) * 0.018f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brass.copy(alpha = 0.055f * strength),
                    brass.copy(alpha = 0.014f * strength),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
        drawCircle(
            color = brass.copy(alpha = 0.24f * strength),
            center = center,
            radius = 1.0.dp.toPx()
        )
    }

    // Archive rails: physical catalog strata, used most strongly by Archive/Sanctum.
    repeat(grammar.archiveRailCount) { index ->
        val y = h * (0.30f + index * 0.105f)
        val left = w * (0.045f + (index % 2) * 0.018f)
        val right = w * (0.955f - (index % 2) * 0.018f)
        drawLine(
            color = mist.copy(alpha = (0.022f + index * 0.003f) * strength),
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 0.55.dp.toPx()
        )
    }

    // Mechanical time: measured ticks suggest clocks/gears without drawing literal machinery.
    if (grammar.mechanicalTickCount > 0) {
        val railY = h * 0.065f
        repeat(grammar.mechanicalTickCount) { index ->
            val x = w * ((index + 1f) / (grammar.mechanicalTickCount + 1f))
            val long = index % 4 == 0
            drawLine(
                color = brass.copy(alpha = (if (long) 0.090f else 0.045f) * strength),
                start = Offset(x, railY),
                end = Offset(x, railY + if (long) 8.dp.toPx() else 4.dp.toPx()),
                strokeWidth = 0.7.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // Fractured reflection: a few discontinuous lines only, never a spiderweb.
    repeat(grammar.fractureCount) { index ->
        val x0 = w * (0.68f + ((seed + index * 19) % 17) / 100f)
        val y0 = h * (0.22f + index * 0.17f)
        val mid = Offset(x0 + w * 0.055f, y0 + h * 0.045f)
        drawLine(
            color = spirit.copy(alpha = 0.055f * strength),
            start = Offset(x0, y0),
            end = mid,
            strokeWidth = 0.7.dp.toPx()
        )
        drawLine(
            color = spirit.copy(alpha = 0.035f * strength),
            start = mid,
            end = Offset(mid.x - w * 0.018f, mid.y + h * 0.065f),
            strokeWidth = 0.55.dp.toPx()
        )
    }

    // Bell marks: distant concentric signals, architectural rather than mystical.
    repeat(grammar.bellMarkCount) { index ->
        val center = Offset(
            x = w * (0.13f + index * 0.17f),
            y = h * 0.15f
        )
        val radius = (8 + index * 4).dp.toPx()
        drawCircle(
            color = brass.copy(alpha = 0.040f * strength),
            center = center,
            radius = radius,
            style = Stroke(0.65.dp.toPx())
        )
    }

    // Realm silhouettes: each world surface gets a distinct architectural read even when
    // it shares the same underlying bitmap. These remain low-alpha and static so text stays primary.
    when (realm) {
        VeilRealm.THRESHOLD -> {
            val centerX = w * 0.50f
            val archTop = h * 0.11f
            val archBottom = h * 0.72f
            drawArc(
                color = brass.copy(alpha = 0.11f * strength),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.24f, archTop),
                size = Size(w * 0.52f, h * 0.34f),
                style = Stroke(1.05.dp.toPx())
            )
            drawLine(
                color = brass.copy(alpha = 0.075f * strength),
                start = Offset(w * 0.24f, h * 0.28f),
                end = Offset(w * 0.24f, archBottom),
                strokeWidth = 0.9.dp.toPx()
            )
            drawLine(
                color = brass.copy(alpha = 0.075f * strength),
                start = Offset(w * 0.76f, h * 0.28f),
                end = Offset(w * 0.76f, archBottom),
                strokeWidth = 0.9.dp.toPx()
            )
            drawCircle(
                color = brass.copy(alpha = 0.09f * strength),
                center = Offset(centerX, h * 0.22f),
                radius = size.minDimension * 0.045f,
                style = Stroke(0.85.dp.toPx())
            )
        }

        VeilRealm.ARCHIVE -> {
            val left = w * 0.10f
            val right = w * 0.90f
            repeat(4) { bay ->
                val x = left + (right - left) * (bay / 3f)
                drawLine(
                    color = brass.copy(alpha = 0.065f * strength),
                    start = Offset(x, h * 0.18f),
                    end = Offset(x, h * 0.82f),
                    strokeWidth = if (bay == 0 || bay == 3) 1.05.dp.toPx() else 0.7.dp.toPx()
                )
            }
            repeat(6) { shelf ->
                val y = h * (0.24f + shelf * 0.095f)
                drawLine(
                    color = mist.copy(alpha = 0.050f * strength),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 0.7.dp.toPx()
                )
            }
            repeat(3) { index ->
                val x = w * (0.27f + index * 0.23f)
                drawRect(
                    color = brass.copy(alpha = 0.018f * strength),
                    topLeft = Offset(x, h * 0.31f),
                    size = Size(w * 0.095f, h * 0.33f)
                )
            }
        }

        VeilRealm.CASTLE,
        VeilRealm.WORLD -> {
            val base = h * 0.82f
            val towerXs = listOf(0.20f, 0.36f, 0.50f, 0.64f, 0.80f)
            towerXs.forEachIndexed { index, fraction ->
                val towerTop = if (index == 2) h * 0.21f else h * (0.34f + (index % 2) * 0.05f)
                drawLine(
                    color = brass.copy(alpha = (if (index == 2) 0.095f else 0.055f) * strength),
                    start = Offset(w * fraction, towerTop),
                    end = Offset(w * fraction, base),
                    strokeWidth = if (index == 2) 1.2.dp.toPx() else 0.8.dp.toPx()
                )
            }
            drawArc(
                color = brass.copy(alpha = 0.085f * strength),
                startAngle = 198f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(w * 0.27f, h * 0.23f),
                size = Size(w * 0.46f, h * 0.32f),
                style = Stroke(1.05.dp.toPx())
            )
            drawLine(
                color = mist.copy(alpha = 0.040f * strength),
                start = Offset(w * 0.12f, base),
                end = Offset(w * 0.88f, base),
                strokeWidth = 0.9.dp.toPx()
            )
        }

        VeilRealm.RITUAL -> {
            val center = Offset(w * 0.50f, h * 0.36f)
            listOf(0.12f, 0.19f, 0.27f).forEachIndexed { index, fraction ->
                drawCircle(
                    color = brass.copy(alpha = (0.095f - index * 0.018f) * strength),
                    center = center,
                    radius = size.minDimension * fraction,
                    style = Stroke((1.05f - index * 0.12f).dp.toPx())
                )
            }
            repeat(8) { index ->
                val angle = Math.toRadians(-90.0 + index * 45.0)
                val inner = size.minDimension * 0.16f
                val outer = size.minDimension * 0.31f
                drawLine(
                    color = brass.copy(alpha = 0.070f * strength),
                    start = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * inner,
                        center.y + kotlin.math.sin(angle).toFloat() * inner
                    ),
                    end = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * outer,
                        center.y + kotlin.math.sin(angle).toFloat() * outer
                    ),
                    strokeWidth = 0.75.dp.toPx()
                )
            }
            drawLine(
                color = spirit.copy(alpha = 0.050f * strength),
                start = Offset(center.x, h * 0.08f),
                end = Offset(center.x, h * 0.70f),
                strokeWidth = 0.7.dp.toPx()
            )
        }

        VeilRealm.SANCTUM -> {
            val center = Offset(w * 0.50f, h * 0.34f)
            repeat(4) { index ->
                drawCircle(
                    color = (if (index % 2 == 0) spirit else brass).copy(
                        alpha = (0.072f - index * 0.010f) * strength
                    ),
                    center = center,
                    radius = size.minDimension * (0.11f + index * 0.075f),
                    style = Stroke(0.85.dp.toPx())
                )
            }
            repeat(6) { index ->
                val angle = Math.toRadians(-90.0 + index * 60.0)
                val inner = size.minDimension * 0.13f
                val outer = size.minDimension * 0.34f
                drawLine(
                    color = spirit.copy(alpha = 0.050f * strength),
                    start = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * inner,
                        center.y + kotlin.math.sin(angle).toFloat() * inner
                    ),
                    end = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * outer,
                        center.y + kotlin.math.sin(angle).toFloat() * outer
                    ),
                    strokeWidth = 0.7.dp.toPx()
                )
            }
        }

        VeilRealm.SANCTUARY -> Unit
    }

    // Lower atmospheric weight preserves the sense of depth.
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Transparent,
                ink.copy(alpha = 0.16f * strength)
            )
        ),
        topLeft = Offset(0f, h * 0.54f),
        size = Size(w, h * 0.46f)
    )
}
