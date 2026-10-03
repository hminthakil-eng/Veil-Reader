package com.veilreader.app.ui.reader

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderHardwareAccessibilityTest {
    private fun manager() = RuntimeEnvironment.getApplication()
        .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    @Test
    fun `spoken screen reader blocks hardware actions even without touch exploration`() {
        val manager = manager()
        shadowOf(manager).setTouchExplorationEnabled(false)
        shadowOf(manager).setEnabledAccessibilityServiceList(listOf(
            AccessibilityServiceInfo().apply { feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN }
        ))
        assertTrue(readerHardwareAccessibilityActive(manager))
    }

    @Test
    fun `touch exploration blocks hardware actions`() {
        val manager = manager()
        shadowOf(manager).setTouchExplorationEnabled(true)
        assertTrue(readerHardwareAccessibilityActive(manager))
    }

    @Test
    fun `reader actions can be enabled when accessibility ownership ends`() {
        val manager = manager()
        shadowOf(manager).setTouchExplorationEnabled(false)
        shadowOf(manager).setEnabledAccessibilityServiceList(emptyList())
        assertFalse(readerHardwareAccessibilityActive(manager))
        assertFalse(readerHardwareAccessibilityActive(null))
    }
}
