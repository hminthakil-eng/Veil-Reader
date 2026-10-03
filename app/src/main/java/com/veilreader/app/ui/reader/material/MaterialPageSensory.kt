package com.veilreader.app.ui.reader.material

import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class MaterialPageSensoryAction {
    LIFT_THRESHOLD,
    COMPLETE,
    CANCEL,
    BOUNDARY
}

internal data class MaterialPageAcousticCue(
    val durationMillis: Int,
    val brightness: Float,
    val dryness: Float,
    val body: Float,
    val fiber: Float,
    val gain: Float
)

internal data class MaterialPageHapticCue(
    val pulseCount: Int,
    val pulseMillis: Int,
    val gapMillis: Int,
    val sharpness: Float,
    val weight: Float
)

internal data class MaterialPageSensoryCue(
    val preset: MaterialPagePreset,
    val action: MaterialPageSensoryAction,
    val acoustic: MaterialPageAcousticCue,
    val haptic: MaterialPageHapticCue
)

/**
 * Renderer-independent sensory policy. The output is intentionally data, not direct
 * platform playback, so Reader settings can keep sound/haptics optional and lifecycle safe.
 */
internal fun materialPageSensoryCue(
    profile: MaterialPageProfile,
    action: MaterialPageSensoryAction,
    velocityDpPerSec: Float = 0f
): MaterialPageSensoryCue {
    val sensory = profile.sensory
    val speed = (abs(velocityDpPerSec) / 1_800f).coerceIn(0f, 1f)
    val actionGain = when (action) {
        MaterialPageSensoryAction.LIFT_THRESHOLD -> 0.26f
        MaterialPageSensoryAction.COMPLETE -> 0.72f
        MaterialPageSensoryAction.CANCEL -> 0.34f
        MaterialPageSensoryAction.BOUNDARY -> 0.24f
    }
    val duration = when (action) {
        MaterialPageSensoryAction.LIFT_THRESHOLD -> 28
        MaterialPageSensoryAction.COMPLETE ->
            (54f + profile.physics.apparentMass * 42f + sensory.acousticFiber * 28f)
                .roundToInt()
        MaterialPageSensoryAction.CANCEL ->
            (42f + profile.physics.apparentMass * 26f).roundToInt()
        MaterialPageSensoryAction.BOUNDARY -> 38
    }.coerceIn(24, 150)

    val pulseCount = when (action) {
        MaterialPageSensoryAction.LIFT_THRESHOLD -> 1
        MaterialPageSensoryAction.COMPLETE ->
            if (sensory.acousticFiber > 0.70f || sensory.hapticWeight > 0.82f) 2 else 1
        MaterialPageSensoryAction.CANCEL -> 1
        MaterialPageSensoryAction.BOUNDARY -> 1
    }

    return MaterialPageSensoryCue(
        preset = profile.preset,
        action = action,
        acoustic = MaterialPageAcousticCue(
            durationMillis = duration,
            brightness = (sensory.acousticBrightness + speed * 0.12f).coerceIn(0f, 1f),
            dryness = sensory.acousticDryness,
            body = (sensory.acousticBody + speed * 0.08f).coerceIn(0f, 1f),
            fiber = sensory.acousticFiber,
            gain = (actionGain * (0.82f + speed * 0.18f)).coerceIn(0f, 0.78f)
        ),
        haptic = MaterialPageHapticCue(
            pulseCount = pulseCount,
            pulseMillis = (
                7f +
                    sensory.hapticWeight * 9f +
                    if (action == MaterialPageSensoryAction.COMPLETE) 3f else 0f
                ).roundToInt().coerceIn(6, 20),
            gapMillis = (8f + sensory.acousticFiber * 8f).roundToInt(),
            sharpness = sensory.hapticSharpness,
            weight = (
                sensory.hapticWeight *
                    when (action) {
                        MaterialPageSensoryAction.LIFT_THRESHOLD -> 0.36f
                        MaterialPageSensoryAction.COMPLETE -> 0.92f
                        MaterialPageSensoryAction.CANCEL -> 0.42f
                        MaterialPageSensoryAction.BOUNDARY -> 0.32f
                    }
                ).coerceIn(0f, 1f)
        )
    )
}

internal fun interface MaterialPageSensorySink {
    fun emit(cue: MaterialPageSensoryCue)
}
