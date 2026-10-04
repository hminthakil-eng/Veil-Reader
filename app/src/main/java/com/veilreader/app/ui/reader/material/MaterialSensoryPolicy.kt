package com.veilreader.app.ui.reader.material

import com.veilreader.app.domain.PageMaterial
import kotlin.math.PI
import kotlin.math.sin

/** Semantic cues, never persistence events. The app's existing sensory preferences own output. */
enum class MaterialSensoryMoment { LIFT, THRESHOLD, COMPLETE, CANCEL }
data class MaterialSensoryCue(
    val material: PageMaterial,
    val moment: MaterialSensoryMoment,
    val speedDpPerSecond: Float = 0f
)

internal data class MaterialAcousticProfile(val seconds: Float, val filter: Float, val gain: Float)
internal fun materialAcousticProfile(cue: MaterialSensoryCue): MaterialAcousticProfile {
    val m = PageMaterials.forId(cue.material)
    val speed = ((cue.speedDpPerSecond.takeIf { it.isFinite() } ?: 0f) / 2400f).coerceIn(0f, 1f)
    val gain = when (cue.moment) {
        MaterialSensoryMoment.LIFT -> 0f // Reserve audio for terminal cues; a fast flick must not be masked by lift audio.
        MaterialSensoryMoment.THRESHOLD -> 0f // A single tactile cue; no extra audio click.
        MaterialSensoryMoment.COMPLETE -> .70f + speed * .30f
        MaterialSensoryMoment.CANCEL -> .38f
    }
    return MaterialAcousticProfile(m.soundSeconds * (1f - speed * .28f),
        (m.soundFilter - speed * .10f).coerceIn(.2f, .95f), m.soundGain * gain)
}

/** Deterministic low-level noise filtered per substrate; generated off the animation thread. */
internal fun materialCuePcm(cue: MaterialSensoryCue, volume: Float, sampleRate: Int = 22050): ShortArray {
    val p = materialAcousticProfile(cue)
    val result = ShortArray((sampleRate * p.seconds).toInt().coerceAtLeast(64))
    var seed = 0x5645494C + cue.material.ordinal * 731
    var low = 0f
    for (i in result.indices) {
        seed = seed * 1103515245 + 12345
        val white = ((seed ushr 16) and 32767) / 16383.5f - 1f
        low = low * p.filter + white * (1f - p.filter)
        val t = i.toFloat() / result.lastIndex
        val envelope = sin(PI * t).toFloat().let { it * it }
        val fibre = if (cue.material == PageMaterial.PAPYRUS) .8f + .2f * sin(i * .071f) else 1f
        result[i] = (low * envelope * fibre * p.gain * volume.coerceIn(0f, .55f) * 32767f).toInt().toShort()
    }
    return result
}
