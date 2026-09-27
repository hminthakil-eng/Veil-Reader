package com.veilreader.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

enum class VeilTemporalPhase {
    DEEP_NIGHT,
    DAWN,
    DAY,
    DUSK,
    NIGHT
}

/**
 * Presentation-only local-clock atmosphere.
 *
 * This is not weather, astronomy, circadian inference, or a claim about the user's environment.
 * For the same local clock hour the policy always produces the same visual state.
 */
data class VeilTemporalAtmosphere(
    val phase: VeilTemporalPhase,
    val localHour: Int,
    val warmth: Float,
    val lampGlow: Float,
    val moonlight: Float,
    val fogDensity: Float,
    val horizonGlow: Float
)

fun temporalAtmosphereFor(hour: Int): VeilTemporalAtmosphere {
    val safeHour = ((hour % 24) + 24) % 24
    return when (safeHour) {
        in 0..4 -> VeilTemporalAtmosphere(
            phase = VeilTemporalPhase.DEEP_NIGHT,
            localHour = safeHour,
            warmth = 0.18f,
            lampGlow = 0.92f,
            moonlight = 0.82f,
            fogDensity = 0.78f,
            horizonGlow = 0.12f
        )
        in 5..7 -> VeilTemporalAtmosphere(
            phase = VeilTemporalPhase.DAWN,
            localHour = safeHour,
            warmth = 0.58f,
            lampGlow = 0.54f,
            moonlight = 0.22f,
            fogDensity = 0.58f,
            horizonGlow = 0.72f
        )
        in 8..16 -> VeilTemporalAtmosphere(
            phase = VeilTemporalPhase.DAY,
            localHour = safeHour,
            warmth = 0.34f,
            lampGlow = 0.18f,
            moonlight = 0.00f,
            fogDensity = 0.30f,
            horizonGlow = 0.48f
        )
        in 17..19 -> VeilTemporalAtmosphere(
            phase = VeilTemporalPhase.DUSK,
            localHour = safeHour,
            warmth = 0.86f,
            lampGlow = 0.66f,
            moonlight = 0.24f,
            fogDensity = 0.46f,
            horizonGlow = 0.88f
        )
        else -> VeilTemporalAtmosphere(
            phase = VeilTemporalPhase.NIGHT,
            localHour = safeHour,
            warmth = 0.42f,
            lampGlow = 0.84f,
            moonlight = 0.70f,
            fogDensity = 0.62f,
            horizonGlow = 0.24f
        )
    }
}

/**
 * Refreshes at the next local hour boundary while this composable remains in composition.
 * No worker, alarm, network request, or background scheduler is created.
 */
@Composable
fun rememberVeilTemporalAtmosphere(): VeilTemporalAtmosphere {
    val zoneId = ZoneId.systemDefault()
    val state by produceState(
        initialValue = temporalAtmosphereFor(ZonedDateTime.now(zoneId).hour),
        key1 = zoneId
    ) {
        while (true) {
            val now = ZonedDateTime.now(zoneId)
            value = temporalAtmosphereFor(now.hour)
            val nextHour = now
                .plusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            val delayMs = Duration.between(now, nextHour)
                .toMillis()
                .coerceAtLeast(30_000L)
            delay(delayMs)
        }
    }
    return state
}

private fun temporalRealmWeight(realm: VeilRealm): Float =
    when (realm) {
        VeilRealm.SANCTUARY -> 0f
        VeilRealm.THRESHOLD -> 1f
        VeilRealm.ARCHIVE -> 0.82f
        VeilRealm.CASTLE,
        VeilRealm.WORLD -> 1f
        VeilRealm.RITUAL -> 0.32f
        VeilRealm.SANCTUM -> 0.88f
    }

/**
 * Cheap static temporal layer for Grayfog shell realms.
 *
 * Sanctuary receives a hard zero budget so Reader paper/EPUB/PDF rendering never changes with
 * clock time. The layer is intentionally procedural and contains no animation loop.
 */
fun Modifier.temporalGrayfogAtmosphere(
    state: VeilTemporalAtmosphere,
    realm: VeilRealm,
    seed: Int = 0,
    intensity: Float = 1f
): Modifier = drawBehind {
    val realmWeight = temporalRealmWeight(realm)
    val strength = (realmWeight * intensity).coerceIn(0f, 1f)
    if (strength <= 0.001f) return@drawBehind

    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@drawBehind

    val brass = VeilPalette.Brass
    val spirit = VeilPalette.Spirit
    val ink = VeilPalette.Ink

    // Local-clock horizon: strongest at dawn/dusk, restrained at day, nearly absent at deep night.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                brass.copy(alpha = state.horizonGlow * 0.075f * strength),
                brass.copy(alpha = state.warmth * 0.022f * strength),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.42f
        ),
        size = Size(w, h * 0.44f)
    )

    // Moonlight is an abstract cool source rather than an astronomical moon phase.
    if (state.moonlight > 0.001f) {
        val xShift = ((seed and 7) - 3) * 0.018f
        val center = Offset(
            x = w * (0.78f + xShift),
            y = h * 0.10f
        )
        val radius = size.minDimension * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    spirit.copy(alpha = state.moonlight * 0.070f * strength),
                    spirit.copy(alpha = state.moonlight * 0.018f * strength),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
        drawCircle(
            color = VeilPalette.Moon.copy(
                alpha = state.moonlight * 0.050f * strength
            ),
            center = center,
            radius = 9.dp.toPx(),
            style = Stroke(0.7.dp.toPx())
        )
    }

    // Lamps read as inhabited architecture. They are brightest after dark and recede in daylight.
    val lampPositions = listOf(0.19f, 0.50f, 0.81f)
    lampPositions.forEachIndexed { index, fraction ->
        val lampCenter = Offset(
            x = w * fraction,
            y = h * (0.13f + (index % 2) * 0.025f)
        )
        val radius = size.minDimension * (0.11f + state.lampGlow * 0.025f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brass.copy(alpha = state.lampGlow * 0.060f * strength),
                    brass.copy(alpha = state.lampGlow * 0.014f * strength),
                    Color.Transparent
                ),
                center = lampCenter,
                radius = radius
            ),
            center = lampCenter,
            radius = radius
        )
        drawCircle(
            color = brass.copy(alpha = state.lampGlow * 0.26f * strength),
            center = lampCenter,
            radius = 1.15.dp.toPx()
        )
    }

    // Fog remains broad and low-contrast; clock time changes depth, never interaction or access.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                VeilPalette.Mist.copy(alpha = state.fogDensity * 0.026f * strength),
                ink.copy(alpha = state.fogDensity * 0.16f * strength)
            ),
            startY = h * 0.54f,
            endY = h
        ),
        topLeft = Offset(0f, h * 0.50f),
        size = Size(w, h * 0.50f)
    )

    if (state.phase == VeilTemporalPhase.DEEP_NIGHT) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    spirit.copy(alpha = 0.018f * strength),
                    Color.Transparent,
                    Color.Transparent,
                    spirit.copy(alpha = 0.014f * strength)
                )
            ),
            size = size
        )
    }
}
