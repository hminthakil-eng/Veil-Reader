package com.veilreader.app.ui

import androidx.window.core.layout.WindowSizeClass
import com.veilreader.app.ui.navigation.VeilTab

private val hiddenWorldChambers = setOf("observatory", "treasury", "sanctum")

internal fun selectedVisibleShellTab(
    selectedTab: VeilTab,
    visibleTabs: List<VeilTab>
): VeilTab =
    selectedTab.takeIf { it in visibleTabs }
        ?: VeilTab.READING

internal fun shouldReturnToReadingWhenGameHidden(
    gameVisible: Boolean,
    selectedTab: VeilTab,
    activeChamber: String?
): Boolean {
    if (gameVisible) return false
    return selectedTab == VeilTab.CASTLE ||
        selectedTab == VeilTab.PATH ||
        activeChamber in hiddenWorldChambers
}

internal fun shouldUseNavigationRail(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

internal fun contentMaxWidthDp(windowSizeClass: WindowSizeClass): Int =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        1280
    } else {
        1040
    }
