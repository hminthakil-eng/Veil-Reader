package com.veilreader.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporalAtmosphereTest {
    @Test
    fun `phase boundaries are explicit across the local clock`() {
        assertEquals(VeilTemporalPhase.DEEP_NIGHT, temporalAtmosphereFor(0).phase)
        assertEquals(VeilTemporalPhase.DEEP_NIGHT, temporalAtmosphereFor(4).phase)
        assertEquals(VeilTemporalPhase.DAWN, temporalAtmosphereFor(5).phase)
        assertEquals(VeilTemporalPhase.DAWN, temporalAtmosphereFor(7).phase)
        assertEquals(VeilTemporalPhase.DAY, temporalAtmosphereFor(8).phase)
        assertEquals(VeilTemporalPhase.DAY, temporalAtmosphereFor(16).phase)
        assertEquals(VeilTemporalPhase.DUSK, temporalAtmosphereFor(17).phase)
        assertEquals(VeilTemporalPhase.DUSK, temporalAtmosphereFor(19).phase)
        assertEquals(VeilTemporalPhase.NIGHT, temporalAtmosphereFor(20).phase)
        assertEquals(VeilTemporalPhase.NIGHT, temporalAtmosphereFor(23).phase)
    }

    @Test
    fun `dusk is the warmest horizon and day has the least fog`() {
        val dusk = temporalAtmosphereFor(18)
        val dawn = temporalAtmosphereFor(6)
        val day = temporalAtmosphereFor(12)
        val night = temporalAtmosphereFor(22)

        assertTrue(dusk.warmth > dawn.warmth)
        assertTrue(dusk.horizonGlow > day.horizonGlow)
        assertTrue(day.fogDensity < dawn.fogDensity)
        assertTrue(day.fogDensity < night.fogDensity)
    }

    @Test
    fun `deep night strengthens lamps and moon without changing policy semantics`() {
        val deepNight = temporalAtmosphereFor(2)
        val day = temporalAtmosphereFor(13)

        assertTrue(deepNight.lampGlow > day.lampGlow)
        assertTrue(deepNight.moonlight > day.moonlight)
        assertEquals(0f, day.moonlight, 0.0001f)
    }

    @Test
    fun `hour normalization is deterministic for wrapped inputs`() {
        assertEquals(temporalAtmosphereFor(0), temporalAtmosphereFor(24))
        assertEquals(temporalAtmosphereFor(23), temporalAtmosphereFor(-1))
        assertEquals(temporalAtmosphereFor(5), temporalAtmosphereFor(29))
    }

    @Test
    fun `Sanctuary has a hard zero temporal budget`() {
        assertEquals(0f, temporalRealmWeightFor(VeilRealm.SANCTUARY), 0.0001f)
        assertTrue(temporalRealmWeightFor(VeilRealm.THRESHOLD) > 0f)
        assertTrue(temporalRealmWeightFor(VeilRealm.ARCHIVE) > 0f)
        assertTrue(temporalRealmWeightFor(VeilRealm.CASTLE) > 0f)
    }

}
