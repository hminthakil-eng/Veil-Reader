package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderBrightness
import com.veilreader.app.domain.ReaderBrightnessMode

/**
 * Returns a reader-window brightness override, or null when the window should follow
 * the device/system brightness.
 */
internal fun ReaderBrightness.toWindowBrightnessOverride(): Float? =
    when (mode) {
        ReaderBrightnessMode.SYSTEM -> null
        ReaderBrightnessMode.OVERRIDE -> normalizedLevel().toFloat()
    }
