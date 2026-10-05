package com.veilreader.app.ui

import androidx.window.core.layout.WindowSizeClass

internal fun shouldUseNavigationRail(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

internal fun contentMaxWidthDp(windowSizeClass: WindowSizeClass): Int =
    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        1280
    } else {
        1040
    }
