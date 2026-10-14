// FILE: app/src/main/java/com/veilreader/app/ui/screens/PremiumWorldProgressionArt.kt
package com.veilreader.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.ReadingHomeStage
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilPalette

@Composable
internal fun PremiumWorldProgressionArt(
    stage: ReadingHomeStage,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalVeilReducedMotion.current
    val glimmer = if (reducedMotion) {
        0.74f
    } else {
        val transition = rememberInfiniteTransition(label = "veil-world-glimmer")
        val animated by transition.animateFloat(
            initialValue = 0.48f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "veil-world-lantern-glimmer"
        )
        animated
    }

    val terrainPath = remember { Path() }
    val structurePath = remember { Path() }
    val roofPath = remember { Path() }

    Crossfade(
        targetState = stage,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 420),
        label = "veil-world-stage-crossfade",
        modifier = modifier.semantics { hideFromAccessibility() }
    ) { currentStage ->
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (!w.isFinite() || !h.isFinite() || w <= 0f || h <= 0f) return@Canvas

            val strokeThin = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val strokeBrass = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            drawAstronomicalSky(w, h, glimmer, strokeThin)
            drawDistantRidges(w, h, terrainPath)

            when (currentStage) {
                ReadingHomeStage.CAMP ->
                    drawCampStage(w, h, glimmer, structurePath, roofPath, strokeBrass, strokeThin)
                ReadingHomeStage.HUT ->
                    drawHutStage(w, h, glimmer, structurePath, roofPath, strokeBrass, strokeThin)
                ReadingHomeStage.HOUSE ->
                    drawHouseStage(w, h, glimmer, structurePath, roofPath, strokeBrass, strokeThin)
                ReadingHomeStage.CASTLE ->
                    drawCastleStage(w, h, glimmer, structurePath, roofPath, strokeBrass, strokeThin)
            }

            drawGrayfogBands(w, h, glimmer)
        }
    }
}

private fun DrawScope.drawAstronomicalSky(
    w: Float,
    h: Float,
    glimmer: Float,
    stroke: Stroke
) {
    val brass = VeilPalette.Brass
    val ringCenter = Offset(w * 0.62f, h * 0.24f)
    val ringRadius = minOf(w, h) * 0.28f

    drawArc(
        color = brass.copy(alpha = 0.14f + 0.08f * glimmer),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(ringCenter.x - ringRadius, ringCenter.y - ringRadius),
        size = Size(ringRadius * 2f, ringRadius * 2f),
        style = stroke
    )
    val star = Offset(w * 0.76f, h * 0.16f)
    val arm = 4.5.dp.toPx()
    drawLine(brass.copy(alpha = 0.32f * glimmer), Offset(star.x - arm, star.y), Offset(star.x + arm, star.y), stroke.width)
    drawLine(brass.copy(alpha = 0.32f * glimmer), Offset(star.x, star.y - arm), Offset(star.x, star.y + arm), stroke.width)
    drawCircle(VeilPalette.Moon.copy(alpha = 0.55f * glimmer), radius = 1.6.dp.toPx(), center = star)
}

private fun DrawScope.drawDistantRidges(w: Float, h: Float, path: Path) {
    path.reset()
    path.moveTo(0f, h)
    path.lineTo(0f, h * 0.80f)
    path.lineTo(w * 0.28f, h * 0.72f)
    path.lineTo(w * 0.58f, h * 0.76f)
    path.lineTo(w * 0.84f, h * 0.68f)
    path.lineTo(w, h * 0.73f)
    path.lineTo(w, h)
    path.close()
    drawPath(path, VeilPalette.Archive.copy(alpha = 0.64f))

    path.reset()
    path.moveTo(w * 0.06f, h)
    path.lineTo(w * 0.22f, h * 0.79f)
    path.lineTo(w * 0.52f, h * 0.74f)
    path.lineTo(w * 0.88f, h * 0.78f)
    path.lineTo(w, h * 0.82f)
    path.lineTo(w, h)
    path.close()
    drawPath(path, VeilPalette.Ink.copy(alpha = 0.92f))
}

private fun DrawScope.drawCampStage(
    w: Float,
    h: Float,
    glimmer: Float,
    tentPath: Path,
    flapPath: Path,
    brassStroke: Stroke,
    thinStroke: Stroke
) {
    val baseY = h * 0.77f
    val peak = Offset(w * 0.52f, h * 0.53f)
    val leftBase = Offset(w * 0.26f, baseY)
    val rightBase = Offset(w * 0.76f, baseY)

    tentPath.reset()
    tentPath.moveTo(peak.x, peak.y)
    tentPath.lineTo(leftBase.x, leftBase.y)
    tentPath.lineTo(rightBase.x, rightBase.y)
    tentPath.close()
    drawPath(tentPath, VeilPalette.Archive)
    drawPath(tentPath, VeilPalette.Brass.copy(alpha = 0.52f), style = brassStroke)

    flapPath.reset()
    flapPath.moveTo(peak.x, peak.y + h * 0.05f)
    flapPath.lineTo(w * 0.45f, baseY)
    flapPath.lineTo(w * 0.59f, baseY)
    flapPath.close()
    drawPath(flapPath, VeilPalette.Ink)
    drawWindowGlow(Offset(w * 0.52f, baseY - h * 0.045f), 11.dp.toPx(), glimmer * 0.85f)

    val poleTop = Offset(w * 0.83f, h * 0.56f)
    drawLine(VeilPalette.Brass.copy(alpha = 0.62f), Offset(w * 0.79f, baseY), poleTop, thinStroke.width)
    val lantern = Offset(w * 0.83f, h * 0.60f)
    drawWindowGlow(lantern, 14.dp.toPx(), glimmer)
    drawCircle(VeilPalette.Moon.copy(alpha = 0.90f), radius = 2.4.dp.toPx(), center = lantern)
}

private fun DrawScope.drawHutStage(
    w: Float,
    h: Float,
    glimmer: Float,
    wallPath: Path,
    roofPath: Path,
    brassStroke: Stroke,
    thinStroke: Stroke
) {
    val baseY = h * 0.77f
    val wallTop = h * 0.55f
    val leftX = w * 0.30f
    val rightX = w * 0.78f

    drawRect(
        color = VeilPalette.Archive,
        topLeft = Offset(w * 0.66f, h * 0.41f),
        size = Size(w * 0.06f, h * 0.14f)
    )
    wallPath.reset()
    wallPath.addRect(androidx.compose.ui.geometry.Rect(leftX, wallTop, rightX, baseY))
    drawPath(wallPath, VeilPalette.Ink)
    drawPath(wallPath, VeilPalette.Brass.copy(alpha = 0.38f), style = thinStroke)

    roofPath.reset()
    roofPath.moveTo(w * 0.24f, wallTop + h * 0.015f)
    roofPath.lineTo(w * 0.54f, h * 0.40f)
    roofPath.lineTo(w * 0.84f, wallTop + h * 0.015f)
    roofPath.close()
    drawPath(roofPath, VeilPalette.Archive)
    drawPath(roofPath, VeilPalette.Brass.copy(alpha = 0.64f), style = brassStroke)

    val winCenter = Offset(w * 0.50f, h * 0.64f)
    drawWindowGlow(winCenter, 15.dp.toPx(), glimmer)
    drawRoundRect(
        color = VeilPalette.Brass.copy(alpha = 0.78f * glimmer),
        topLeft = Offset(winCenter.x - w * 0.055f, winCenter.y - h * 0.035f),
        size = Size(w * 0.11f, h * 0.07f),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawLine(VeilPalette.Ink, Offset(winCenter.x, winCenter.y - h * 0.035f), Offset(winCenter.x, winCenter.y + h * 0.035f), thinStroke.width)
}

private fun DrawScope.drawHouseStage(
    w: Float,
    h: Float,
    glimmer: Float,
    bodyPath: Path,
    roofPath: Path,
    brassStroke: Stroke,
    thinStroke: Stroke
) {
    val baseY = h * 0.77f
    val mainLeft = w * 0.24f
    val mainRight = w * 0.66f
    val mainTop = h * 0.46f
    val wingRight = w * 0.84f
    val wingTop = h * 0.55f

    bodyPath.reset()
    bodyPath.moveTo(mainLeft, baseY)
    bodyPath.lineTo(mainLeft, mainTop)
    bodyPath.lineTo(mainRight, mainTop)
    bodyPath.lineTo(mainRight, wingTop)
    bodyPath.lineTo(wingRight, wingTop)
    bodyPath.lineTo(wingRight, baseY)
    bodyPath.close()
    drawPath(bodyPath, VeilPalette.Ink)
    drawPath(bodyPath, VeilPalette.Brass.copy(alpha = 0.42f), style = thinStroke)

    roofPath.reset()
    roofPath.moveTo(mainLeft - w * 0.04f, mainTop)
    roofPath.lineTo(w * 0.45f, h * 0.30f)
    roofPath.lineTo(mainRight + w * 0.03f, mainTop)
    roofPath.lineTo(wingRight + w * 0.03f, wingTop)
    roofPath.lineTo(mainRight, h * 0.43f)
    roofPath.close()
    drawPath(roofPath, VeilPalette.Archive)
    drawPath(roofPath, VeilPalette.Brass.copy(alpha = 0.68f), style = brassStroke)

    drawLine(VeilPalette.Brass.copy(alpha = 0.72f), Offset(w * 0.45f, h * 0.30f), Offset(w * 0.45f, h * 0.23f), thinStroke.width)
    listOf(
        Offset(w * 0.37f, h * 0.56f),
        Offset(w * 0.52f, h * 0.56f),
        Offset(w * 0.45f, h * 0.68f),
        Offset(w * 0.74f, h * 0.65f)
    ).forEach { center ->
        drawWindowGlow(center, 12.dp.toPx(), glimmer)
        drawRoundRect(
            color = VeilPalette.Brass.copy(alpha = 0.80f * glimmer),
            topLeft = Offset(center.x - w * 0.032f, center.y - h * 0.032f),
            size = Size(w * 0.064f, h * 0.064f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
    }
}

private fun DrawScope.drawCastleStage(
    w: Float,
    h: Float,
    glimmer: Float,
    keepPath: Path,
    spirePath: Path,
    brassStroke: Stroke,
    thinStroke: Stroke
) {
    val baseY = h * 0.77f
    keepPath.reset()
    keepPath.moveTo(w * 0.16f, baseY)
    keepPath.lineTo(w * 0.16f, h * 0.54f)
    keepPath.lineTo(w * 0.32f, h * 0.54f)
    keepPath.lineTo(w * 0.32f, h * 0.38f)
    keepPath.lineTo(w * 0.48f, h * 0.38f)
    keepPath.lineTo(w * 0.48f, h * 0.28f)
    keepPath.lineTo(w * 0.64f, h * 0.28f)
    keepPath.lineTo(w * 0.64f, h * 0.46f)
    keepPath.lineTo(w * 0.86f, h * 0.46f)
    keepPath.lineTo(w * 0.86f, baseY)
    keepPath.close()
    drawPath(keepPath, VeilPalette.Ink)
    drawPath(keepPath, VeilPalette.Brass.copy(alpha = 0.50f), style = thinStroke)

    spirePath.reset()
    spirePath.moveTo(w * 0.45f, h * 0.28f)
    spirePath.lineTo(w * 0.56f, h * 0.12f)
    spirePath.lineTo(w * 0.67f, h * 0.28f)
    spirePath. moveTo(w * 0.29f, h * 0.38f)
    spirePath.lineTo(w * 0.40f, h * 0.25f)
    spirePath.lineTo(w * 0.51f, h * 0.38f)
    spirePath.moveTo(w * 0.62f, h * 0.46f)
    spirePath.lineTo(w * 0.75f, h * 0.34f)
    spirePath.lineTo(w * 0.88f, h * 0.46f)
    drawPath(spirePath, VeilPalette.Archive)
    drawPath(spirePath, VeilPalette.Brass.copy(alpha = 0.76f), style = brassStroke)

    val finial = Offset(w * 0.56f, h * 0.09f)
    drawCircle(VeilPalette.Brass.copy(alpha = 0.72f * glimmer), radius = 4.dp.toPx(), center = finial, style = thinStroke)

    listOf(
        Offset(w * 0.56f, h * 0.38f),
        Offset(w * 0.40f, h * 0.49f),
        Offset(w * 0.56f, h * 0.54f),
        Offset(w * 0.75f, h * 0.56f),
        Offset(w * 0.24f, h * 0.64f)
    ).forEach { win ->
        drawWindowGlow(win, 11.dp.toPx(), glimmer)
        drawRoundRect(
            color = VeilPalette.Brass.copy(alpha = 0.84f * glimmer),
            topLeft = Offset(win.x - w * 0.024f, win.y - h * 0.036f),
            size = Size(w * 0.048f, h * 0.072f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

private fun DrawScope.drawWindowGlow(center: Offset, radius: Float, glimmer: Float) {
    drawCircle(VeilPalette.Brass.copy(alpha = 0.10f * glimmer), radius = radius, center = center)
    drawCircle(VeilPalette.Brass.copy(alpha = 0.22f * glimmer), radius = radius * 0.55f, center = center)
}

private fun DrawScope.drawGrayfogBands(w: Float, h: Float, glimmer: Float) {
    val mist = VeilPalette.Mist
    val bandAlpha = 0.07f + 0.03f * glimmer
    drawRoundRect(
        color = mist.copy(alpha = bandAlpha),
        topLeft = Offset(w * 0.08f, h * 0.74f),
        size = Size(w * 0.88f, h * 0.045f),
        cornerRadius = CornerRadius(12.dp.toPx())
    )
    drawRoundRect(
        color = mist.copy(alpha = bandAlpha * 1.25f),
        topLeft = Offset(w * 0.02f, h * 0.81f),
        size = Size(w * 0.94f, h * 0.055f),
        cornerRadius = CornerRadius(14.dp.toPx())
    )
    drawLine(
        color = VeilPalette.Brass.copy(alpha = 0.34f),
        start = Offset(w * 0.12f, h * 0.77f),
        end = Offset(w * 0.92f, h * 0.77f),
        strokeWidth = 1.dp.toPx()
    )
}