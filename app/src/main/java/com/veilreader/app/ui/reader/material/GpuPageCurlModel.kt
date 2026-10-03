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
    profile: MaterialPageProfile,
    side: MaterialPageSide
): GpuPageCurlFrame {
    val p = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val vertical =
        verticalBias.takeIf { it.isFinite() }?.coerceIn(-0.18f, 0.18f) ?: 0f
    val origin =
        pullOriginY.takeIf { it.isFinite() }?.coerceIn(0.04f, 0.96f) ?: 0.5f

    val physics = profile.physics
    val optics = profile.optics
    val bend = physics.bendStiffness.coerceIn(0.35f, 1f)
    val mass = physics.apparentMass.coerceIn(0.6f, 1.5f)
    val binding = physics.bindingConstraint.coerceIn(0.65f, 1f)

    // A stiff glossy sheet bends over a broader cylinder. Softer fibrous stock
    // forms a tighter roll. Keep the radius in normalized page-width units.
    val radius = (
        0.052f +
            bend * 0.055f +
            (mass - 0.6f) * 0.018f
        ).coerceIn(0.052f, 0.125f)

    // Progress moves the virtual cylinder through the page. The extra half-turn
    // travel ensures the terminal frame actually clears the viewport instead of
    // collapsing into a folded strip at the spine.
    val terminalOvershoot = (PI.toFloat() * radius * 0.56f)
    val cylinderX =
        1f - p * (1.04f + terminalOvershoot)

    val cornerSignal = ((0.5f - origin) * 2f).coerceIn(-1f, 1f)
    val verticalSignal = (vertical / 0.18f).coerceIn(-1f, 1f)

    // Tilt is intentionally restrained near the binding. It becomes expressive
    // only when the user actually grabs a corner or drags vertically.
    val cylinderTilt = (
        cornerSignal * (0.14f + (1f - binding) * 0.08f) +
            verticalSignal * 0.075f
        ).coerceIn(-0.24f, 0.24f)

    val liftEnvelope =
        materialPageLift(p, profile)
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
        cylinderY = origin,
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
