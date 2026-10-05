package com.veilreader.app.ui

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilShellPolicyTest {
    @Test
    fun compactPhone_usesBottomNavigation() {
        val size = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(390, 844)
        assertFalse(shouldUseNavigationRail(size))
        assertEquals(1040, contentMaxWidthDp(size))
    }

    @Test
    fun mediumLandscape_usesRailOnlyWhenHeightIsAlsoMedium() {
        val short = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(900, 420)
        val tall = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(900, 700)

        assertFalse(shouldUseNavigationRail(short))
        assertTrue(shouldUseNavigationRail(tall))
    }

    @Test
    fun expandedWidth_raisesContentCeiling() {
        val size = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1400, 900)
        assertEquals(1280, contentMaxWidthDp(size))
    }
}
