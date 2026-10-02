package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DisplayAndSecurityTest {
    @Test fun einkDefaultsDisableAnimation() {
        assertFalse(EInkProfile().animationsEnabled)
    }

    @Test fun appLockGracePeriodIsBounded() {
        assertEquals(
            3600,
            AppLockPolicy(AppLockMethod.BIOMETRIC, gracePeriodSeconds = 50_000).normalized().gracePeriodSeconds
        )
    }
}
