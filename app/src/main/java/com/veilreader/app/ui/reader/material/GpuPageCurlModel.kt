package com.veilreader.app.ui.reader.material

import kotlin.math.PI
import kotlin.math.abs

/**
 * Pure, renderer-independent frame description for the GPU Material Page Engine.
 *
 * The deformation follows a virtual-cylinder model: the untouched sheet remains
 * flat before the cylinder, the active span wraps around a cylinder, and material
 * past half a revolution continues as the physical back of the page. This keeps
 * gesture/physics ownership in Veil while allowing the GPU renderer to operate
 * without reimplementing Reader state.
 */
internal data class GpuPageMeshQuality(
    val columns: Int,
    val rows: Int
)

internal fun gpuPageMeshQuality(
    lowMemoryDevice: Boolean
): GpuPageMeshQuality =
    if (lowMemoryDevice) {
        // Diagonal curl needs real vertical tessellation too; an 8-row strip can
        // visibly facet on tall phones. Keep this conservative while preserving
        // enough topology for corner pulls on memory-constrained devices.
        GpuPageMeshQuality(columns = 56, rows = 22)
    } else {
        // Geometry is cheap compared with full-page texture fill. 80x32 keeps
        // diagonal cylinders smooth on tall 90/120 Hz displays while remaining
        // safely inside GLES2 16-bit index limits.
        GpuPageMeshQuality(columns = 80, rows = 32)
    }

internal fun gpuPageMeshVertexCount(quality: GpuPageMeshQuality): Int =
    (quality.columns + 1) * (quality.rows + 1)

internal fun gpuPageMeshIndexCount(quality: GpuPageMeshQuality): Int =
    quality.columns * quality.rows * 6

internal fun nextMaterialPageBufferSlot(cursor: Int): Int =
    (cursor + 1) and 1

internal fun estimatedMaterialPageStorageBytes(
    pageWidthPx: Int,
    pageHeightPx: Int,
    cpuBitmapCount: Int,
    gpuTextureCount: Int
): Long {
    if (pageWidthPx <= 0 || pageHeightPx <= 0) return 0L
    val pageBytes =
        pageWidthPx.toLong() *
            pageHeightPx.toLong() *
            4L
    return pageBytes *
        (cpuBitmapCount.coerceAtLeast(0) + gpuTextureCount.coerceAtLeast(0)).toLong()
}


internal data class GpuPageCurlFrame(
    val cylinderX: Float,
    val cylinderY: Float,
    val cylinderTilt: Float,
    val radius: Float,
    val sideSign: Float,
    val shadowStrength: Float,
    val edgeStrength: Float
)

internal fun gpuPageCurlFrame(
    progress: Float,
    verticalBias: Float,
    pullOriginY: Float,
    diagonalPull: Float = 0f,
    pointerTravel: Float = progress,
    pageAspect: Float = 1f,
    profile: MaterialPageProfile,
    side: MaterialPageSide
): GpuPageCurlFrame {
    val p = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val vertical =
        verticalBias.takeIf { it.isFinite() }?.coerceIn(-0.18f, 0.18f) ?: 0f
    val origin =
        pullOriginY.takeIf { it.isFinite() }?.coerceIn(0.04f, 0.96f) ?: 0.5f
    val diagonal =
        diagonalPull.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
    val travel =
        pointerTravel.takeIf { it.isFinite() }?.coerceIn(0f, 1.5f) ?: p
    val aspect =
        pageAspect.takeIf { it.isFinite() }?.coerceIn(0.5f, 4f) ?: 1f

    val physics = profile.physics
    val optics = profile.optics
    val bend = physics.bendStiffness.coerceIn(0.35f, 1f)
    val mass = physics.apparentMass.coerceIn(0.6f, 1.5f)
    val binding = physics.bindingConstraint.coerceIn(0.65f, 1f)

    // A stiff glossy sheet bends over a broader cylinder. Softer fibrous stock
    // forms a tighter roll. Keep the radius in normalized page-width units.
    val baseRadius = (
        0.052f +
            bend * 0.055f +
            (mass - 0.6f) * 0.018f
        ).coerceIn(0.052f, 0.125f)
    val liftEnvelope = materialPageLift(p, profile)
    val terminalT =
        ((p - 0.72f) / 0.28f).coerceIn(0f, 1f).let { t ->
            t * t * (3f - 2f * t)
        }
    val travelT =
        (travel / 0.32f).coerceIn(0f, 1f).let { t ->
            t * t * (3f - 2f * t)
        }
    val touchRadiusScale =
        0.34f + travelT * 0.66f
    val radius = (
        baseRadius *
            touchRadiusScale *
            (0.78f + liftEnvelope * (0.22f + (1f - bend) * 0.05f)) *
            (1f - terminalT * 0.48f)
        ).coerceIn(0.020f, 0.132f)

    // Progress moves the virtual cylinder through the page. Clearance is based on
    // the authored material radius so a breathing radius never traps the terminal
    // sheet near the spine.
    val terminalOvershoot = (PI.toFloat() * baseRadius * 0.62f)
    val cylinderX =
        1f - p * (1.045f + terminalOvershoot)

    val cornerSignal = ((0.5f - origin) * 2f).coerceIn(-1f, 1f)
    val verticalSignal = (vertical / 0.18f).coerceIn(-1f, 1f)

    val cylinderY = (
        origin +
            vertical * (0.54f + (1f - binding) * 0.26f)
        ).coerceIn(0.03f, 0.97f)

    // Tilt is intentionally restrained at first contact and becomes expressive
    // only after the leaf has actually lifted from the reading plane.
    val tiltEnvelope = 0.20f + liftEnvelope * 0.80f
    val requestedTilt = (
        (
            cornerSignal * (0.10f + (1f - binding) * 0.06f) +
                diagonal * 0.145f +
                verticalSignal * 0.045f
            ) * tiltEnvelope
        ).coerceIn(-0.26f, 0.26f)

    val cylinderYAspect = cylinderY * aspect
    val bindingReach =
        if (requestedTilt >= 0f) cylinderYAspect
        else aspect - cylinderYAspect
    val bindingTiltLimit =
        if (cylinderX > 0f) {
            (cylinderX / bindingReach.coerceAtLeast(0.001f)).coerceIn(0f, 0.26f)
        } else {
            0.26f
        }
    val cylinderTilt =
        requestedTilt.coerceIn(-bindingTiltLimit, bindingTiltLimit)
    val shadowStrength = (
        liftEnvelope *
            (0.11f +
                optics.edgeBody.coerceIn(0f, 1f) * 0.08f +
                (1f - optics.roughness.coerceIn(0f, 1f)) * 0.035f)
        ).coerceIn(0f, 0.24f)

    val edgeStrength = (
        0.30f +
            optics.edgeBody.coerceIn(0f, 1f) * 0.55f
        ).coerceIn(0.30f, 0.85f)

    return GpuPageCurlFrame(
        cylinderX = cylinderX,
        cylinderY = cylinderY,
        cylinderTilt = cylinderTilt,
        radius = radius,
        sideSign = if (side == MaterialPageSide.RIGHT) 1f else -1f,
        shadowStrength = shadowStrength,
        edgeStrength = edgeStrength
    )
}

internal fun isFiniteGpuPageCurlFrame(frame: GpuPageCurlFrame): Boolean =
    frame.cylinderX.isFinite() &&
        frame.cylinderY.isFinite() &&
        frame.cylinderTilt.isFinite() &&
        frame.radius.isFinite() &&
        frame.sideSign.isFinite() &&
        frame.shadowStrength.isFinite() &&
        frame.edgeStrength.isFinite() &&
        frame.cylinderY in 0f..1f &&
        frame.radius > 0f &&
        abs(frame.sideSign) == 1f
