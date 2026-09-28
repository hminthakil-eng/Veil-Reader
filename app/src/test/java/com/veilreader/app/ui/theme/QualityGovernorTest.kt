package com.veilreader.app.ui.theme

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityGovernorTest {
    @Test
    fun `normal runtime keeps masterpiece tier`() {
        assertEquals(
            VeilQualityTier.FULL,
            veilQualityTierFor(
                VeilRuntimeQualitySignals(
                    lowRamDevice = false,
                    powerSaveMode = false,
                    thermalStatus = PowerManager.THERMAL_STATUS_NONE
                )
            )
        )
    }

    @Test
    fun `battery saver and moderate heat reduce optional cost only`() {
        assertEquals(
            VeilQualityTier.BALANCED,
            veilQualityTierFor(
                VeilRuntimeQualitySignals(
                    lowRamDevice = false,
                    powerSaveMode = true,
                    thermalStatus = PowerManager.THERMAL_STATUS_NONE
                )
            )
        )
        assertEquals(
            VeilQualityTier.BALANCED,
            veilQualityTierFor(
                VeilRuntimeQualitySignals(
                    lowRamDevice = false,
                    powerSaveMode = false,
                    thermalStatus = PowerManager.THERMAL_STATUS_MODERATE
                )
            )
        )
        val policy = qualityPolicyFor(VeilQualityTier.BALANCED)
        assertFalse(policy.expensiveBlurAllowed)
        assertTrue(policy.ambientMotionEnabled)
    }

    @Test
    fun `low ram or severe heat enters essential tier`() {
        listOf(
            VeilRuntimeQualitySignals(
                lowRamDevice = true,
                powerSaveMode = false,
                thermalStatus = PowerManager.THERMAL_STATUS_NONE
            ),
            VeilRuntimeQualitySignals(
                lowRamDevice = false,
                powerSaveMode = false,
                thermalStatus = PowerManager.THERMAL_STATUS_SEVERE
            )
        ).forEach { signals ->
            assertEquals(VeilQualityTier.ESSENTIAL, veilQualityTierFor(signals))
        }
        val policy = qualityPolicyFor(VeilQualityTier.ESSENTIAL)
        assertFalse(policy.ambientMotionEnabled)
        assertFalse(policy.expensiveBlurAllowed)
    }

    @Test
    fun `essential Sanctuary keeps structural readability while dropping micro fibres`() {
        val material = sanctuaryPageMaterialFor(
            mode = com.veilreader.app.domain.ReaderNavigationMode.PAPER_CURL,
            qualityTier = VeilQualityTier.ESSENTIAL
        )
        assertTrue(material.showPhysicalPageStack)
        assertTrue(material.showEdgeFalloff)
        assertFalse(material.showMicroFibres)

        val full = sanctuarySurfaceProfileFor(
            theme = com.veilreader.app.domain.ReaderTheme.PAPER,
            mode = com.veilreader.app.domain.ReaderNavigationMode.PAPER_CURL,
            qualityTier = VeilQualityTier.FULL
        )
        val essential = sanctuarySurfaceProfileFor(
            theme = com.veilreader.app.domain.ReaderTheme.PAPER,
            mode = com.veilreader.app.domain.ReaderNavigationMode.PAPER_CURL,
            qualityTier = VeilQualityTier.ESSENTIAL
        )
        assertTrue(essential.fibreCount < full.fibreCount)
        assertTrue(essential.speckCount < full.speckCount)
        assertTrue(essential.edgeOxidationAlpha > 0f)
    }
}
