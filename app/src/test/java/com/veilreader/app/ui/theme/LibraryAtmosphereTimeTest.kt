package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAtmosphereTimeTest {
    @Test
    fun localHourSelectsStableAmbientPhaseAtBoundaries() {
        assertEquals(ArchiveTimePhase.NIGHT, archiveTimePhaseForHour(0))
        assertEquals(ArchiveTimePhase.NIGHT, archiveTimePhaseForHour(5))
        assertEquals(ArchiveTimePhase.DAWN, archiveTimePhaseForHour(6))
        assertEquals(ArchiveTimePhase.DAWN, archiveTimePhaseForHour(8))
        assertEquals(ArchiveTimePhase.DAY, archiveTimePhaseForHour(9))
        assertEquals(ArchiveTimePhase.DAY, archiveTimePhaseForHour(16))
        assertEquals(ArchiveTimePhase.DUSK, archiveTimePhaseForHour(17))
        assertEquals(ArchiveTimePhase.DUSK, archiveTimePhaseForHour(19))
        assertEquals(ArchiveTimePhase.NIGHT, archiveTimePhaseForHour(20))
        assertEquals(ArchiveTimePhase.DAY, archiveTimePhaseForHour(-1))
    }

    @Test
    fun nocturnalLampsWarmWithoutLargeFogShift() {
        assertTrue(ArchiveTimePhase.NIGHT.lampMultiplier > ArchiveTimePhase.DAY.lampMultiplier)
        assertTrue(ArchiveTimePhase.NIGHT.fogMultiplier <= 1.10f)
    }
}
