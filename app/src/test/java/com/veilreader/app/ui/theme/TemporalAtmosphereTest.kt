package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TemporalAtmosphereTest {
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
