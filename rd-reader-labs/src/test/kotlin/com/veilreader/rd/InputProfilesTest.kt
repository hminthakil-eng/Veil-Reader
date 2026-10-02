package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class InputProfilesTest {
    @Test fun calmProfileKeepsCenterTapForChrome() {
        assertEquals(
            ReaderInputAction.TOGGLE_CHROME,
            ReaderInputProfileDefaults.calm.actionFor(TapZone.CENTER)
        )
        assertEquals(
            ReaderInputAction.PREVIOUS_PAGE,
            ReaderInputProfileDefaults.calm.actionFor(TapZone.MIDDLE_LEFT)
        )
    }
}
