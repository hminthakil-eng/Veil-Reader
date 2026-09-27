package com.veilreader.app.ui.theme

import java.time.Instant
import java.time.ZoneId

enum class VeilRealm {
    SANCTUARY,
    THRESHOLD,
    ARCHIVE,
    CASTLE,
    WORLD,
    RITUAL,
    SANCTUM
}

enum class VeilTemporalPhase {
    DAWN,
    DAY,
    DUSK,
    NIGHT
}

/** Local clock presentation only: not weather, astronomy, or reading-history inference. */
fun temporalPhaseForHour(hour: Int): VeilTemporalPhase {
    val safeHour = ((hour % 24) + 24) % 24
    return when (safeHour) {
        in 5..8 -> VeilTemporalPhase.DAWN
        in 9..16 -> VeilTemporalPhase.DAY
        in 17..20 -> VeilTemporalPhase.DUSK
        else -> VeilTemporalPhase.NIGHT
    }
}

internal fun temporalPhaseAt(instant: Instant, zone: ZoneId): VeilTemporalPhase =
    temporalPhaseForHour(instant.atZone(zone).hour)

/** Reader material is independent of wall-clock changes, even if a caller passes a live phase. */
internal fun temporalPhaseForRealm(
    realm: VeilRealm,
    phase: VeilTemporalPhase
): VeilTemporalPhase = if (realm == VeilRealm.SANCTUARY) VeilTemporalPhase.DAY else phase
