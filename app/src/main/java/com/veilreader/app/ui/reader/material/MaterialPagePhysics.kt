package com.veilreader.app.ui.reader.material

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

internal enum class MaterialPageReleaseDecision {
    COMPLETE,
    CANCEL
}

internal data class MaterialPageDragSample(
    val progress: Float,
    val rawProgress: Float,
    val verticalBias: Float,
    val lift: Float
)

internal fun materialPageDragSample(
    inwardDistancePx: Float,
    verticalDistancePx: Float,
    widthPx: Float,
    heightPx: Float,
    profile: MaterialPageProfile
): MaterialPageDragSample {
    if (widthPx <= 0f || heightPx <= 0f) {
        return MaterialPageDragSample(
            progress = 0f,
            rawProgress = 0f,
            verticalBias = 0f,
            lift = 0f
        )
    }

    val raw = (inwardDistancePx / widthPx).coerceIn(0f, 1f)
    val resistance = profile.physics.dragResistance.coerceIn(0.45f, 1.35f)
    val stiffness = profile.physics.bendStiffness.coerceIn(0.35f, 1.0f)

    // Material resistance is strongest at lift-off, then progressively yields.
    // The curve is monotonic and never lets the visual sheet outrun the finger.
    val exponent = 1f + resistance * 0.34f + stiffness * 0.12f
    val resisted = raw.toDouble().pow(exponent.toDouble()).toFloat()
    val yield = (0.86f + (1f - resistance.coerceAtMost(1f)) * 0.10f)
        .coerceIn(0.78f, 0.94f)
    val progress = (resisted * yield + raw * (1f - yield))
        .coerceIn(0f, raw)

    val verticalLimit =
        (0.18f - profile.physics.bindingConstraint * 0.055f)
            .coerceIn(0.09f, 0.16f)
    val rawVertical =
        (verticalDistancePx / heightPx)
            .takeIf { it.isFinite() }
            ?.coerceIn(-verticalLimit, verticalLimit)
            ?: 0f
    val verticalDeadZone =
        (0.0025f + profile.physics.bindingConstraint * 0.0018f)
            .coerceAtMost(verticalLimit * 0.20f)
    val verticalMagnitude = abs(rawVertical)
    val verticalBias = if (verticalMagnitude <= verticalDeadZone) {
        0f
    } else {
        val normalized =
            ((verticalMagnitude - verticalDeadZone) /
                (verticalLimit - verticalDeadZone).coerceAtLeast(0.0001f))
                .coerceIn(0f, 1f)
        val shaped =
            normalized.toDouble()
                .pow(1.12)
                .toFloat() *
                verticalLimit
        if (rawVertical < 0f) -shaped else shaped
    }

    return MaterialPageDragSample(
        progress = progress,
        rawProgress = raw,
        verticalBias = verticalBias,
        lift = materialPageLift(progress, profile)
    )
}

internal fun materialPageLift(
    progress: Float,
    profile: MaterialPageProfile
): Float {
    val p = progress.coerceIn(0f, 1f)
    val base = sin(p.toDouble() * PI).toFloat().coerceIn(0f, 1f)
    val stiffness = profile.physics.bendStiffness.coerceIn(0f, 1f)
    val mass = profile.physics.apparentMass.coerceIn(0.6f, 1.5f)
    return (
        base *
            (0.82f + stiffness * 0.12f) /
            (0.92f + (mass - 0.6f) * 0.12f)
        ).coerceIn(0f, 1f)
}

internal fun materialPageReleaseDecision(
    progress: Float,
    inwardVelocityDpPerSec: Float,
    profile: MaterialPageProfile
): MaterialPageReleaseDecision {
    val p = progress.coerceIn(0f, 1f)
    val velocity = inwardVelocityDpPerSec
        .takeIf { it.isFinite() }
        ?.coerceIn(-4_000f, 4_000f)
        ?: 0f
    val thresholds = profile.physics

    // A deliberate reverse release is authoritative. Once the finger starts carrying
    // the sheet back toward the bound edge, distance alone must not accidentally commit it.
    val reverseCancelVelocity =
        -thresholds.flickVelocityDpPerSec * 0.42f
    if (
        velocity <= reverseCancelVelocity &&
        p < thresholds.completionThreshold + 0.12f
    ) {
        return MaterialPageReleaseDecision.CANCEL
    }

    // Project only a short physical horizon. This removes the binary feel around the
    // threshold without allowing a tiny high-speed twitch to throw an entire page.
    val mass = thresholds.apparentMass.coerceIn(0.6f, 1.5f)
    val velocityContribution =
        (velocity / thresholds.flickVelocityDpPerSec.coerceAtLeast(1f)) *
            (0.075f / mass)
    val projectedProgress =
        (p + velocityContribution.coerceIn(-0.10f, 0.12f))
            .coerceIn(0f, 1f)

    val deliberateCompletion =
        p >= thresholds.completionThreshold ||
            projectedProgress >= thresholds.completionThreshold
    val fastCompletion =
        p >= thresholds.cancelThreshold &&
            velocity >= thresholds.flickVelocityDpPerSec

    return if (deliberateCompletion || fastCompletion) {
        MaterialPageReleaseDecision.COMPLETE
    } else {
        MaterialPageReleaseDecision.CANCEL
    }
}

internal fun materialPageSettleDurationMillis(
    progress: Float,
    completing: Boolean,
    velocityDpPerSec: Float,
    profile: MaterialPageProfile
): Int {
    val p = progress.coerceIn(0f, 1f)
    val remaining = if (completing) 1f - p else p
    val mass = profile.physics.apparentMass.coerceIn(0.6f, 1.5f)
    val speed = abs(velocityDpPerSec)

    val base = when {
        speed >= 2_000f -> 118f
        speed >= 1_200f -> 152f
        speed >= 700f -> 188f
        else -> 236f
    }
    val massScale = 0.82f + mass * 0.22f
    val distanceScale = 0.45f + remaining * 0.78f

    return (base * massScale * distanceScale)
        .roundToInt()
        .coerceIn(92, 360)
}

internal fun materialPageTapDurationMillis(
    profile: MaterialPageProfile
): Int =
    (176f + profile.physics.apparentMass.coerceIn(0.6f, 1.5f) * 86f)
        .roundToInt()
        .coerceIn(220, 320)

internal fun materialPageSpringStiffness(
    profile: MaterialPageProfile
): Float {
    val mass = profile.physics.apparentMass.coerceIn(0.6f, 1.5f)
    val bend = profile.physics.bendStiffness.coerceIn(0.35f, 1f)
    return (760f * (0.72f + bend * 0.55f) / mass)
        .coerceIn(420f, 1_050f)
}

internal fun materialPageSpringDamping(
    profile: MaterialPageProfile,
    cancelling: Boolean
): Float {
    val authored = profile.physics.settleDamping.coerceIn(0.82f, 0.98f)
    val materialBody =
        profile.physics.apparentMass.coerceIn(0.6f, 1.5f)
    val criticalBias =
        (0.98f - authored).coerceAtLeast(0f) * 0.45f
    val massBias =
        ((materialBody - 0.6f) / 0.9f).coerceIn(0f, 1f) * 0.025f

    // Page settling must never read as a bouncy UI spring. Completion is critical
    // or slightly overdamped; cancellation is a touch firmer so the sheet seats cleanly.
    return if (cancelling) {
        (1.035f + criticalBias + massBias).coerceIn(1.035f, 1.11f)
    } else {
        (1.0f + criticalBias + massBias).coerceIn(1.0f, 1.075f)
    }
}

internal fun materialPageReducedMotionAlpha(
    progress: Float,
    completing: Boolean
): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (completing) {
        (1f - p * 0.92f).coerceIn(0.08f, 1f)
    } else {
        (0.94f + (1f - p) * 0.06f).coerceIn(0.94f, 1f)
    }
}
