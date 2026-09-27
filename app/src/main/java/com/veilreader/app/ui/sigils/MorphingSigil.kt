package com.veilreader.app.ui.sigils

import android.graphics.Path as AndroidPath
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.CathedralMotionClass
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.effectiveMotionDurationMs
import com.veilreader.app.ui.theme.motionBudgetFor

internal data class SigilGeometrySpec(
    val points: Int,
    val star: Boolean,
    val innerRadius: Float,
    val rotationQuarterTurns: Int
)

internal fun sigilGeometryFor(
    pathId: String,
    rankIndex: Int
): SigilGeometrySpec {
    val safeRank = rankIndex.coerceAtLeast(0)
    val base = when (pathId.lowercase()) {
        "oracle" -> SigilGeometrySpec(5, true, 0.52f, 3)
        "dreamwalker" -> SigilGeometrySpec(6, true, 0.60f, 3)
        "archivist" -> SigilGeometrySpec(4, false, 0.68f, 1)
        "vanguard" -> SigilGeometrySpec(4, true, 0.46f, 1)
        "nocturne" -> SigilGeometrySpec(7, true, 0.62f, 3)
        "artificer" -> SigilGeometrySpec(6, false, 0.70f, 1)
        else -> SigilGeometrySpec(5, false, 0.62f, 3)
    }

    // Rank changes the geometry without creating arbitrary random marks.
    val pointGrowth = (safeRank / 2).coerceAtMost(2)
    val pulse = (safeRank % 3) * 0.035f
    return base.copy(
        points = (base.points + pointGrowth).coerceIn(3, 9),
        innerRadius = (base.innerRadius - pulse).coerceIn(0.38f, 0.72f),
        rotationQuarterTurns = (base.rotationQuarterTurns + safeRank) % 4
    )
}

private fun polygonFor(spec: SigilGeometrySpec): RoundedPolygon {
    val rounding = CornerRounding(
        radius = if (spec.star) 0.075f else 0.11f,
        smoothing = 0.34f
    )
    return if (spec.star) {
        RoundedPolygon.star(
            numVerticesPerRadius = spec.points,
            radius = 1f,
            innerRadius = spec.innerRadius,
            rounding = rounding,
            innerRounding = CornerRounding(
                radius = 0.055f,
                smoothing = 0.28f
            ),
            centerX = 0f,
            centerY = 0f
        )
    } else {
        RoundedPolygon(
            numVertices = spec.points,
            radius = 1f,
            centerX = 0f,
            centerY = 0f,
            rounding = rounding
        )
    }
}

@Composable
fun MorphingPathSigil(
    pathId: String,
    fromRankIndex: Int,
    toRankIndex: Int,
    transformed: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = VeilPalette.Brass
) {
    val reducedMotion = LocalVeilReducedMotion.current
    val startSpec = remember(pathId, fromRankIndex) {
        sigilGeometryFor(pathId, fromRankIndex)
    }
    val endSpec = remember(pathId, toRankIndex) {
        sigilGeometryFor(pathId, toRankIndex)
    }

    if (reducedMotion) {
        Crossfade(
            targetState = transformed,
            modifier = modifier,
            animationSpec = tween(
                effectiveMotionDurationMs(
                    CathedralMotionClass.CEREMONIAL,
                    reducedMotion = true
                )
            ),
            label = "reduced-sigil-rank-change"
        ) { finished ->
            StaticSigil(
                spec = if (finished) endSpec else startSpec,
                tint = tint,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val progress by animateFloatAsState(
        targetValue = if (transformed) 1f else 0f,
        animationSpec = tween(
            durationMillis = motionBudgetFor(
                CathedralMotionClass.CEREMONIAL
            ).targetDurationMs
        ),
        label = "path-sigil-morph"
    )
    val morph = remember(startSpec, endSpec) {
        Morph(
            start = polygonFor(startSpec),
            end = polygonFor(endSpec)
        )
    }
    val reusablePath = remember { AndroidPath() }

    Canvas(modifier) {
        reusablePath.reset()
        val composePath = morph
            .toPath(progress = progress, path = reusablePath)
            .asComposePath()

        val radius = size.minDimension * 0.39f
        val rotation =
            (
                startSpec.rotationQuarterTurns * (1f - progress) +
                    endSpec.rotationQuarterTurns * progress
                ) * 90f

        translate(left = size.width / 2f, top = size.height / 2f) {
            rotate(rotation) {
                scale(radius, radius, pivot = Offset.Zero) {
                    drawPath(
                        path = composePath,
                        color = tint.copy(alpha = 0.94f),
                        style = Stroke(
                            width = 1.25.dp.toPx() / radius.coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        drawCircle(
            color = tint.copy(alpha = 0.16f),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension * 0.44f,
            style = Stroke(0.8.dp.toPx())
        )
        drawCircle(
            color = tint.copy(alpha = 0.08f),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension * 0.48f,
            style = Stroke(0.65.dp.toPx())
        )
    }
}

@Composable
private fun StaticSigil(
    spec: SigilGeometrySpec,
    tint: Color,
    modifier: Modifier
) {
    val polygon = remember(spec) { polygonFor(spec) }
    val reusablePath = remember { AndroidPath() }

    Canvas(modifier) {
        reusablePath.reset()
        val path = polygon.toPath(reusablePath).asComposePath()
        val radius = size.minDimension * 0.39f

        translate(left = size.width / 2f, top = size.height / 2f) {
            rotate(spec.rotationQuarterTurns * 90f) {
                scale(radius, radius, pivot = Offset.Zero) {
                    drawPath(
                        path = path,
                        color = tint.copy(alpha = 0.94f),
                        style = Stroke(
                            width = 1.25.dp.toPx() / radius.coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        drawCircle(
            color = tint.copy(alpha = 0.16f),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension * 0.44f,
            style = Stroke(0.8.dp.toPx())
        )
    }
}
