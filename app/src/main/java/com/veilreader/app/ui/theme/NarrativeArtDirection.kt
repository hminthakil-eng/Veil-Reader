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
        VeilRealm.THRESHOLD -> VeilNarrativeGrammar(0.92f, 2, 2, 0, 5, 1, 1)
        VeilRealm.ARCHIVE -> VeilNarrativeGrammar(0.42f, 3, 4, 5, 7, 2, 1)
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
