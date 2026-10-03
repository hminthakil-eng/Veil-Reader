package com.veilreader.app.ui.reader.material

import kotlin.math.PI
import kotlin.math.abs
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

/**
 * Reusable primitive geometry storage for the production renderer.
 *
 * The pure [materialPageGeometry] API below intentionally remains allocation-friendly for tests
 * and inspection, while live drawing mutates this buffer in place to avoid per-frame strip/point
 * object churn.
 */
internal class MaterialPageMeshBuffer(
    maxSegments: Int = 36
) {
    private val capacity = maxSegments.coerceIn(12, 36)

    var segmentCount: Int = 0
    var foldX: Float = 0f
    var flatStartX: Float = 0f
    var flatEndX: Float = 0f
    var revealFraction: Float = 0f
    var foldAngleRadians: Float = 0f
    var lift: Float = 0f
    var creaseTopX: Float = 0f
    var creaseTopY: Float = 0f
    var creaseBottomX: Float = 0f
    var creaseBottomY: Float = 0f

    val sourceLeft = FloatArray(capacity)
    val sourceRight = FloatArray(capacity)
    val topLeftX = FloatArray(capacity)
    val topLeftY = FloatArray(capacity)
    val topRightX = FloatArray(capacity)
    val topRightY = FloatArray(capacity)
    val bottomLeftX = FloatArray(capacity)
    val bottomLeftY = FloatArray(capacity)
    val bottomRightX = FloatArray(capacity)
    val bottomRightY = FloatArray(capacity)
    val backFacing = BooleanArray(capacity)
    val lightResponse = FloatArray(capacity)
    val stripLift = FloatArray(capacity)

    fun clampedSegmentCount(requested: Int): Int =
        requested.coerceIn(12, capacity)
}

internal fun updateMaterialPageMesh(
    buffer: MaterialPageMeshBuffer,
    width: Float,
    height: Float,
    progress: Float,
    verticalBias: Float,
    profile: MaterialPageProfile,
    pullOriginY: Float = 0.5f,
    segmentCount: Int = 0
) {
    if (
        !width.isFinite() ||
        !height.isFinite() ||
        width <= 0f ||
        height <= 0f
    ) {
        buffer.resetMaterialPageMesh()
        return
    }

    val p = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    if (p <= 0.0001f) {
        buffer.segmentCount = 0
        buffer.foldX = width
        buffer.flatStartX = 0f
        buffer.flatEndX = width
        buffer.revealFraction = 0f
        buffer.foldAngleRadians = 0f
        buffer.lift = 0f
        buffer.creaseTopX = width
        buffer.creaseTopY = 0f
        buffer.creaseBottomX = width
        buffer.creaseBottomY = height
        return
    }

    val eased = smoothStep(p)
    val binding = profile.physics.bindingConstraint.coerceIn(0.75f, 1f)
    val foldX = width * (1f - eased * (0.91f + binding * 0.07f))
    val span = (width - foldX).coerceAtLeast(width * 0.002f)

    // Flexible sheets make a tighter curl; stiffer glossy stock keeps a broader radius.
    val bend = profile.physics.bendStiffness.coerceIn(0.35f, 1f)
    val curlRange = 0.72f + (1f - bend) * 0.22f
    val thetaMax = (PI.toFloat() * (0.16f + eased * curlRange))
        .coerceIn(0.16f * PI.toFloat(), PI.toFloat())
    val radius = span / thetaMax.coerceAtLeast(0.001f)
    val lift = materialPageLift(p, profile)

    val requestedSegments = if (segmentCount > 0) {
        segmentCount
    } else {
        materialPageAdaptiveSegmentCount(
            progress = p,
            profile = profile
        )
    }
    val segments = buffer.clampedSegmentCount(requestedSegments)
    val originY = pullOriginY.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    val topOriginInfluence = 0.34f + (1f - originY) * 0.66f
    val bottomOriginInfluence = 0.34f + originY * 0.66f
    val safeVerticalBias =
        verticalBias.takeIf { it.isFinite() }?.coerceIn(-0.18f, 0.18f) ?: 0f
    val cornerPull = ((0.5f - originY) * 2f).coerceIn(-1f, 1f)
    val verticalPull = (safeVerticalBias / 0.18f).coerceIn(-1f, 1f)
    val tiltSignal = (cornerPull * 0.68f + verticalPull * 0.32f)
        .coerceIn(-1f, 1f)
    val foldTilt =
        width *
            eased *
            tiltSignal *
            (0.032f + (1f - binding) * 0.035f)

    buffer.segmentCount = segments
    buffer.foldX = foldX
    buffer.flatStartX = 0f
    buffer.flatEndX = foldX
    buffer.revealFraction = (1f - foldX / width).coerceIn(0f, 1f)
    buffer.foldAngleRadians = thetaMax
    buffer.lift = lift

    for (index in 0 until segments) {
        val q0 = index.toFloat() / segments.toFloat()
        val q1 = (index + 1).toFloat() / segments.toFloat()
        val sourceLeft = foldX + span * q0
        val sourceRight = foldX + span * q1

        val leftTheta = thetaMax * q0
        val rightTheta = thetaMax * q1
        val leftZNorm = normalizedCurlDepth(leftTheta)
        val rightZNorm = normalizedCurlDepth(rightTheta)

        val leftBaseX = foldX + radius * sin(leftTheta)
        val rightBaseX = foldX + radius * sin(rightTheta)
        val leftTilt = foldTilt * (1f - q0 * 0.55f)
        val rightTilt = foldTilt * (1f - q1 * 0.55f)
        val topLeftX =
            (leftBaseX - leftTilt * topOriginInfluence)
                .coerceIn(-width * 0.25f, width * 1.25f)
        val topRightX =
            (rightBaseX - rightTilt * topOriginInfluence)
                .coerceIn(-width * 0.25f, width * 1.25f)
        val bottomLeftX =
            (leftBaseX + leftTilt * bottomOriginInfluence)
                .coerceIn(-width * 0.25f, width * 1.25f)
        val bottomRightX =
            (rightBaseX + rightTilt * bottomOriginInfluence)
                .coerceIn(-width * 0.25f, width * 1.25f)
        val leftVerticalShift =
            safeVerticalBias *
                height *
                (0.24f + q0 * 0.50f) *
                leftZNorm
        val rightVerticalShift =
            safeVerticalBias *
                height *
                (0.24f + q1 * 0.50f) *
                rightZNorm
        val leftBindingSkew =
            (1f - q0) *
                eased *
                profile.physics.bindingConstraint.coerceIn(0f, 1f) *
                height *
                0.010f
        val rightBindingSkew =
            (1f - q1) *
                eased *
                profile.physics.bindingConstraint.coerceIn(0f, 1f) *
                height *
                0.010f

        val topLeftY =
            leftVerticalShift * topOriginInfluence + leftBindingSkew
        val topRightY =
            rightVerticalShift * topOriginInfluence + rightBindingSkew
        val bottomLeftY =
            height + leftVerticalShift * bottomOriginInfluence - leftBindingSkew
        val bottomRightY =
            height + rightVerticalShift * bottomOriginInfluence - rightBindingSkew

        val midTheta = (leftTheta + rightTheta) * 0.5f
        val midLift = (leftZNorm + rightZNorm) * 0.5f
        val normal = cos(midTheta)
        val isBackFacing = normal < 0f
        val optical = profile.optics
        val facing = normal.coerceAtLeast(0f)
        val grazing = (1f - abs(normal)).coerceIn(0f, 1f)
        val diffuse =
            facing *
                (0.14f + (1f - optical.roughness) * 0.12f)
        val specularLobe =
            grazing *
                grazing *
                optical.specularResponse *
                (0.34f + (1f - optical.roughness) * 0.28f)
        val frontLight =
            diffuse +
                specularLobe -
                optical.roughness * 0.055f
        val backLight =
            -normal.coerceAtMost(0f) *
                (
                    0.055f +
                        optical.translucency * 0.24f +
                        optical.inkGhosting * 0.05f
                    )

        buffer.sourceLeft[index] = sourceLeft
        buffer.sourceRight[index] = sourceRight
        buffer.topLeftX[index] = topLeftX
        buffer.topLeftY[index] = topLeftY
        buffer.topRightX[index] = topRightX
        buffer.topRightY[index] = topRightY
        buffer.bottomLeftX[index] = bottomLeftX
        buffer.bottomLeftY[index] = bottomLeftY
        buffer.bottomRightX[index] = bottomRightX
        buffer.bottomRightY[index] = bottomRightY
        buffer.backFacing[index] = isBackFacing
        buffer.lightResponse[index] =
            (if (isBackFacing) backLight else frontLight)
                .coerceIn(-0.10f, 0.54f)
        buffer.stripLift[index] = midLift
    }

    val creaseShift =
        safeVerticalBias *
            height *
            0.06f *
            lift
    buffer.creaseTopX =
        (foldX - foldTilt * topOriginInfluence)
            .coerceIn(-width * 0.20f, width * 1.20f)
    buffer.creaseTopY = creaseShift
    buffer.creaseBottomX =
        (foldX + foldTilt * bottomOriginInfluence)
            .coerceIn(-width * 0.20f, width * 1.20f)
    buffer.creaseBottomY = height + creaseShift
}

internal fun materialPageGeometry(
    width: Float,
    height: Float,
    progress: Float,
    verticalBias: Float,
    profile: MaterialPageProfile,
    pullOriginY: Float = 0.5f,
    segmentCount: Int = 0
): MaterialPageFrame {
    val buffer = MaterialPageMeshBuffer(
        maxSegments = if (segmentCount > 0) {
            segmentCount.coerceAtLeast(12)
        } else {
            36
        }
    )
    updateMaterialPageMesh(
        buffer = buffer,
        width = width,
        height = height,
        progress = progress,
        verticalBias = verticalBias,
        profile = profile,
        pullOriginY = pullOriginY,
        segmentCount = segmentCount
    )

    val strips = ArrayList<MaterialPageStrip>(buffer.segmentCount)
    for (index in 0 until buffer.segmentCount) {
        strips += MaterialPageStrip(
            sourceLeft = buffer.sourceLeft[index],
            sourceRight = buffer.sourceRight[index],
            topLeft = MaterialPagePoint(
                buffer.topLeftX[index],
                buffer.topLeftY[index]
            ),
            topRight = MaterialPagePoint(
                buffer.topRightX[index],
                buffer.topRightY[index]
            ),
            bottomLeft = MaterialPagePoint(
                buffer.bottomLeftX[index],
                buffer.bottomLeftY[index]
            ),
            bottomRight = MaterialPagePoint(
                buffer.bottomRightX[index],
                buffer.bottomRightY[index]
            ),
            backFacing = buffer.backFacing[index],
            lightResponse = buffer.lightResponse[index],
            lift = buffer.stripLift[index]
        )
    }

    return MaterialPageFrame(
        foldX = buffer.foldX,
        flatStartX = buffer.flatStartX,
        flatEndX = buffer.flatEndX,
        revealFraction = buffer.revealFraction,
        foldAngleRadians = buffer.foldAngleRadians,
        lift = buffer.lift,
        creaseTop = MaterialPagePoint(
            buffer.creaseTopX,
            buffer.creaseTopY
        ),
        creaseBottom = MaterialPagePoint(
            buffer.creaseBottomX,
            buffer.creaseBottomY
        ),
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

internal fun materialPageAdaptiveSegmentCount(
    progress: Float,
    profile: MaterialPageProfile
): Int {
    val p = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val curvature = sin(p.toDouble() * PI).toFloat().coerceIn(0f, 1f)
    val opticalDemand =
        profile.optics.specularResponse.coerceIn(0f, 1f) * 4f +
            profile.optics.edgeBody.coerceIn(0f, 1f) * 2f
    val physicalDemand =
        (1f - profile.physics.bendStiffness.coerceIn(0f, 1f)) * 2f
    return (18f + curvature * 10f + opticalDemand + physicalDemand)
        .toInt()
        .coerceIn(18, 34)
}

private fun normalizedCurlDepth(theta: Float): Float =
    ((1f - cos(theta)) * 0.5f).coerceIn(0f, 1f)

private fun MaterialPageMeshBuffer.resetMaterialPageMesh() {
    segmentCount = 0
    foldX = 0f
    flatStartX = 0f
    flatEndX = 0f
    revealFraction = 0f
    foldAngleRadians = 0f
    lift = 0f
    creaseTopX = 0f
    creaseTopY = 0f
    creaseBottomX = 0f
    creaseBottomY = 0f
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
