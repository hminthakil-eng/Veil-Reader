package com.veilreader.app.ui

import androidx.compose.material3.adaptive.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilShellPolicyTest {
    @Test
    fun compactPhone_usesBottomNavigation() {
        val size = WindowSizeClass.compute(DpSize(390.dp, 844.dp))
        assertFalse(shouldUseNavigationRail(size))
        assertEquals(1040, contentMaxWidthDp(size))
    }

    @Test
    fun mediumLandscape_usesRailOnlyWhenHeightIsAlsoMedium() {
        val short = WindowSizeClass.compute(DpSize(900.dp, 420.dp))
        val tall = WindowSizeClass.compute(DpSize(900.dp, 700.dp))

        assertFalse(shouldUseNavigationRail(short))
        assertTrue(shouldUseNavigationRail(tall))
    }

    @Test
    fun expandedWidth_raisesContentCeiling() {
        val size = WindowSizeClass.compute(DpSize(1400.dp, 900.dp))
        assertEquals(1280, contentMaxWidthDp(size))
    }
}
