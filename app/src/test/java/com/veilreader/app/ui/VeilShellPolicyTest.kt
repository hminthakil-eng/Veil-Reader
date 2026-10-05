package com.veilreader.app.ui

import com.veilreader.app.ui.navigation.VeilTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilShellPolicyTest {
    @Test
    fun hiddenSelectedTab_fallsBackToReading() {
        assertEquals(
            VeilTab.READING,
            selectedVisibleShellTab(
                selectedTab = VeilTab.CASTLE,
                visibleTabs = listOf(VeilTab.READING, VeilTab.LIBRARY, VeilTab.PROFILE)
            )
        )
    }

    @Test
    fun visibleSelectedTab_isPreserved() {
        assertEquals(
            VeilTab.LIBRARY,
            selectedVisibleShellTab(
                selectedTab = VeilTab.LIBRARY,
                visibleTabs = listOf(VeilTab.READING, VeilTab.LIBRARY, VeilTab.PROFILE)
            )
        )
    }

    @Test
    fun hidingGame_returnsWorldTabsAndChambersToReading() {
        assertTrue(shouldReturnToReadingWhenGameHidden(false, VeilTab.CASTLE, null))
        assertTrue(shouldReturnToReadingWhenGameHidden(false, VeilTab.PATH, null))
        assertTrue(shouldReturnToReadingWhenGameHidden(false, VeilTab.PROFILE, "observatory"))
        assertTrue(shouldReturnToReadingWhenGameHidden(false, VeilTab.PROFILE, "treasury"))
        assertTrue(shouldReturnToReadingWhenGameHidden(false, VeilTab.PROFILE, "sanctum"))
    }

    @Test
    fun gameVisible_neverForcesReading() {
        assertFalse(shouldReturnToReadingWhenGameHidden(true, VeilTab.CASTLE, "sanctum"))
    }
}
