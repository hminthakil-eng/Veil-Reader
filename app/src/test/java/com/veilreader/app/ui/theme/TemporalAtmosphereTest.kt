package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TemporalAtmosphereTest {
    @Test
    fun all_day_boundaries_preserve_the_canonical_palette_schedule() {
        val expected = List(5) { VeilTemporalPhase.NIGHT } +
            List(4) { VeilTemporalPhase.DAWN } + List(8) { VeilTemporalPhase.DAY } +
            List(4) { VeilTemporalPhase.DUSK } + List(3) { VeilTemporalPhase.NIGHT }
        assertEquals(expected, (0..23).map(::temporalPhaseForHour))
    }

    @Test
    fun phase_mapping_changes_at_the_exact_local_boundary() {
        val zone = ZoneId.of("UTC")
        assertEquals(VeilTemporalPhase.DAY, temporalPhaseAt(Instant.parse("2026-09-27T16:59:59Z"), zone))
        assertEquals(VeilTemporalPhase.DUSK, temporalPhaseAt(Instant.parse("2026-09-27T17:00:00Z"), zone))
    }

    @Test
    fun changing_zone_resamples_the_same_instant() {
        val instant = Instant.parse("2026-09-27T18:00:00Z")
        assertEquals(VeilTemporalPhase.DAY, temporalPhaseAt(instant, ZoneId.of("America/New_York")))
        assertEquals(VeilTemporalPhase.NIGHT, temporalPhaseAt(instant, ZoneId.of("Asia/Tehran")))
    }

    @Test
    fun daylight_saving_uses_the_actual_offset_for_each_date() {
        val zone = ZoneId.of("America/New_York")
        assertEquals(VeilTemporalPhase.NIGHT, temporalPhaseAt(Instant.parse("2026-01-15T09:00:00Z"), zone))
        assertEquals(VeilTemporalPhase.DAWN, temporalPhaseAt(Instant.parse("2026-07-15T09:00:00Z"), zone))
    }

    @Test
    fun sanctuary_material_never_changes_with_the_world_clock() {
        VeilTemporalPhase.entries.forEach { phase ->
            assertEquals(VeilTemporalPhase.DAY, temporalPhaseForRealm(VeilRealm.SANCTUARY, phase))
        }
    }

    @Test
    fun shell_realms_keep_the_shared_clock_phase() {
        VeilRealm.entries.filter { it != VeilRealm.SANCTUARY }.forEach { realm ->
            VeilTemporalPhase.entries.forEach { phase ->
                assertEquals(phase, temporalPhaseForRealm(realm, phase))
            }
        }
    }

    @Test
    fun local_hours_map_to_stable_world_phases() {
        assertEquals(VeilTemporalPhase.NIGHT, temporalPhaseForHour(0))
        assertEquals(VeilTemporalPhase.DAWN, temporalPhaseForHour(5))
        assertEquals(VeilTemporalPhase.DAWN, temporalPhaseForHour(8))
        assertEquals(VeilTemporalPhase.DAY, temporalPhaseForHour(9))
        assertEquals(VeilTemporalPhase.DAY, temporalPhaseForHour(16))
        assertEquals(VeilTemporalPhase.DUSK, temporalPhaseForHour(17))
        assertEquals(VeilTemporalPhase.DUSK, temporalPhaseForHour(20))
        assertEquals(VeilTemporalPhase.NIGHT, temporalPhaseForHour(21))
        assertEquals(VeilTemporalPhase.NIGHT, temporalPhaseForHour(23))
    }

    @Test
    fun hour_mapping_wraps_out_of_range_inputs() {
        assertEquals(temporalPhaseForHour(23), temporalPhaseForHour(-1))
        assertEquals(temporalPhaseForHour(1), temporalPhaseForHour(25))
    }
}
