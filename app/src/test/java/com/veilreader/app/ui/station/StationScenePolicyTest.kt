package com.veilreader.app.ui.station

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationScenePolicyTest {
    private val enabled = StationRendererCapabilities(featureEnabled = true)

    @Test fun unapprovedGpuNeverRunsEvenWhenFeatureEnabled() {
        assertEquals(
            StationRendererMode.COMPOSE_DEPTH,
            stationRenderMode(enabled, StationSurface.THRESHOLD)
        )
        assertEquals(
            StationRendererMode.STATIC,
            stationRenderMode(enabled.copy(featureEnabled = false), StationSurface.THRESHOLD)
        )
    }

    @Test fun allRendererApprovalsRequiredAndReaderAlwaysStatic() {
        val approved = enabled.copy(
            filamentInstalled = true,
            sceneAssetsValidated = true,
            hardwareGpuApproved = true
        )
        assertEquals(StationRendererMode.FILAMENT_3D, stationRenderMode(approved, StationSurface.THRESHOLD))
        assertEquals(StationRendererMode.STATIC, stationRenderMode(approved, StationSurface.READER))
        assertEquals(StationRendererMode.COMPOSE_DEPTH, stationRenderMode(approved.copy(sceneAssetsValidated = false), StationSurface.THRESHOLD))
        assertEquals(StationRendererMode.COMPOSE_DEPTH, stationRenderMode(approved.copy(hardwareGpuApproved = false), StationSurface.THRESHOLD))
    }

    @Test fun accessibilityAndPowerSafetyOverrideTrue3d() {
        val approved = enabled.copy(
            filamentInstalled = true,
            sceneAssetsValidated = true,
            hardwareGpuApproved = true
        )
        listOf(
            approved.copy(reducedMotion = true),
            approved.copy(highContrast = true),
            approved.copy(powerSaveMode = true),
            approved.copy(trimMemoryCritical = true)
        ).forEach {
            assertEquals(StationRendererMode.STATIC, stationRenderMode(it, StationSurface.ARCHIVE))
        }
    }

    @Test fun lifecycleStopsRenderingAndNoPerspectiveOnReaderText() {
        assertTrue(stationCanRenderFrames(StationRendererMode.FILAMENT_3D, true, true))
        assertFalse(stationCanRenderFrames(StationRendererMode.FILAMENT_3D, false, true))
        assertFalse(stationCanRenderFrames(StationRendererMode.COMPOSE_DEPTH, true, false))
        assertFalse(stationCanRenderFrames(StationRendererMode.STATIC, true, true))
        assertFalse(stationAllowsPerspectiveOnText(StationSurface.READER))
        assertFalse(stationAllowsPerspectiveOnText(StationSurface.THRESHOLD))
        assertEquals(0, stationInputMotionMillis(StationRendererMode.STATIC))
    }
}
