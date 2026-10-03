package com.veilreader.app.ui.reader.material

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class MaterialPagePoint(
    val x: Float,
    val y: Float
)

internal data class MaterialPageStrip(
    val sourceLeft: Float,
    val sourceRight: Float,
    val topLeft: MaterialPagePoint,
    val topRight: MaterialPagePoint,
    val bottomLeft: MaterialPagePoint,
    val bottomRight: MaterialPagePoint,
    val backFacing: Boolean,
    val lightResponse: Float,
    val lift: Float
)

internal data class MaterialPageFrame(
    val foldX: Float,
    val flatStartX: Float,
    val flatEndX: Float,
    val revealFraction: Float,
    val foldAngleRadians: Float,
    val lift: Float,
    val creaseTop: MaterialPagePoint,
    val creaseBottom: MaterialPagePoint,
    val strips: List<MaterialPageStrip>
)

internal fun materialPageGeometry(
    width: Float,
    height: Float,
    progress: Float,
    verticalBias: Float,
    profile: MaterialPageProfile,
    pullOriginY: Float = 0.5f,
    segmentCount: Int = 26
): MaterialPageFrame {
    if (width <= 0f || height <= 0f) {
        return MaterialPageFrame(
            foldX = 0f,
            flatStartX = 0f,
            flatEndX = 0f,
            revealFraction = 0f,
            foldAngleRadians = 0f,
            lift = 0f,
            creaseTop = MaterialPagePoint(0f, 0f),
            creaseBottom = MaterialPagePoint(0f, 0f),
            strips = emptyList()
        )
    }

    val p = progress.coerceIn(0f, 1f)
    if (p <= 0.0001f) {
        return MaterialPageFrame(
            foldX = width,
            flatStartX = 0f,
            flatEndX = width,
            revealFraction = 0f,
            foldAngleRadians = 0f,
            lift = 0f,
            creaseTop = MaterialPagePoint(width, 0f),
            creaseBottom = MaterialPagePoint(width, height),
            strips = emptyList()
        )
    }

    val eased = smoothStep(p)
    val binding = profile.physics.bindingConstraint.coerceIn(0.75f, 1f)
    val foldX = width * (1f - eased * (0.91f + binding * 0.07f))
    val span = (width - foldX).coerceAtLeast(width * 0.002f)
    val bend = profile.physics.bendStiffness.coerceIn(0.35f, 1f)
    val curlRange = 0.72f + (1f - bend) * 0.22f
    val thetaMax = (PI.toFloat() * (0.16f + eased * curlRange))
        .coerceIn(0.16f * PI.toFloat(), PI.toFloat())
    val radius = span / thetaMax.coerceAtLeast(0.001f)
    val lift = materialPageLift(p, profile)

    val segments = segmentCount.coerceIn(12, 36)
    val strips = ArrayList<MaterialPageStrip>(segments)
    val originY = pullOriginY.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val topOriginInfluence = 0.34f + (1f - originY) * 0.66f
    val bottomOriginInfluence = 0.34f + originY * 0.66f

    fun projected(sourceX: Float, top: Boolean): Pair<MaterialPagePoint, ProjectionSample> {
        val q = ((sourceX - foldX) / span).coerceIn(0f, 1f)
        val theta = thetaMax * q
        val x = foldX + radius * sin(theta)
        val z = radius * (1f - cos(theta))
        val zNorm = if (radius <= 0.0001f) 0f else (z / (2f * radius)).coerceIn(0f, 1f)

        // Finger height influences the curl, but binding suppresses large vertical wobble.
        val verticalShift =
            verticalBias.coerceIn(-0.18f, 0.18f) *
                height *
                (0.24f + q * 0.50f) *
                zNorm
        val bindingSkew =
            (1f - q) *
                eased *
                profile.physics.bindingConstraint.coerceIn(0f, 1f) *
                height *
                0.010f

        val originInfluence =
            if (top) topOriginInfluence else bottomOriginInfluence
        val y = if (top) {
            verticalShift * originInfluence + bindingSkew
        } else {
            height + verticalShift * originInfluence - bindingSkew
        }

        return MaterialPagePoint(x, y) to ProjectionSample(
            theta = theta,
            zNorm = zNorm
        )
    }

    repeat(segments) { index ->
        val q0 = index.toFloat() / segments.toFloat()
        val q1 = (index + 1).toFloat() / segments.toFloat()
        val sourceLeft = foldX + span * q0
        val sourceRight = foldX + span * q1

        val (topLeft, leftSample) = projected(sourceLeft, top = true)
        val (topRight, rightSample) = projected(sourceRight, top = true)
        val (bottomLeft, _) = projected(sourceLeft, top = false)
        val (bottomRight, _) = projected(sourceRight, top = false)

        val midTheta = (leftSample.theta + rightSample.theta) * 0.5f
        val midLift = (leftSample.zNorm + rightSample.zNorm) * 0.5f
        val normal = cos(midTheta)
        val backFacing = normal < 0f
        val optical = profile.optics
        val frontLight =
            normal.coerceAtLeast(0f) *
                (0.22f + optical.specularResponse * 0.46f) -
                optical.roughness * 0.08f
        val backLight =
            -normal.coerceAtMost(0f) *
                (0.08f + optical.translucency * 0.20f)

        strips += MaterialPageStrip(
            sourceLeft = sourceLeft,
            sourceRight = sourceRight,
            topLeft = topLeft,
            topRight = topRight,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight,
            backFacing = backFacing,
            lightResponse = (if (backFacing) backLight else frontLight)
                .coerceIn(-0.10f, 0.54f),
            lift = midLift
        )
    }

    val creaseShift =
        verticalBias.coerceIn(-0.18f, 0.18f) *
            height *
            0.06f *
            lift

    return MaterialPageFrame(
        foldX = foldX,
        flatStartX = 0f,
        flatEndX = foldX,
        revealFraction = (1f - foldX / width).coerceIn(0f, 1f),
        foldAngleRadians = thetaMax,
        lift = lift,
        creaseTop = MaterialPagePoint(foldX, creaseShift),
        creaseBottom = MaterialPagePoint(foldX, height + creaseShift),
        strips = strips
    )
}

internal fun mirrorMaterialPageFrame(
    frame: MaterialPageFrame,
    width: Float
): MaterialPageFrame {
    fun mirror(point: MaterialPagePoint): MaterialPagePoint =
        MaterialPagePoint(width - point.x, point.y)

    val mirroredStrips = frame.strips.map { strip ->
        MaterialPageStrip(
            sourceLeft = width - strip.sourceRight,
            sourceRight = width - strip.sourceLeft,
            topLeft = mirror(strip.topRight),
            topRight = mirror(strip.topLeft),
            bottomLeft = mirror(strip.bottomRight),
            bottomRight = mirror(strip.bottomLeft),
            backFacing = strip.backFacing,
            lightResponse = strip.lightResponse,
            lift = strip.lift
        )
    }

    return frame.copy(
        foldX = width - frame.foldX,
        flatStartX = width - frame.flatEndX,
        flatEndX = width - frame.flatStartX,
        creaseTop = mirror(frame.creaseTop),
        creaseBottom = mirror(frame.creaseBottom),
        strips = mirroredStrips
    )
}

internal fun isFiniteMaterialPageFrame(frame: MaterialPageFrame): Boolean {
    if (
        !frame.foldX.isFinite() ||
        !frame.revealFraction.isFinite() ||
        !frame.foldAngleRadians.isFinite() ||
        !frame.lift.isFinite()
    ) {
        return false
    }

    return frame.strips.all { strip ->
        strip.sourceLeft.isFinite() &&
            strip.sourceRight.isFinite() &&
            strip.topLeft.isFinite() &&
            strip.topRight.isFinite() &&
            strip.bottomLeft.isFinite() &&
            strip.bottomRight.isFinite() &&
            strip.lightResponse.isFinite() &&
            strip.lift.isFinite()
    }
}

private fun MaterialPagePoint.isFinite(): Boolean =
    x.isFinite() && y.isFinite()

private data class ProjectionSample(
    val theta: Float,
    val zNorm: Float
)

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
