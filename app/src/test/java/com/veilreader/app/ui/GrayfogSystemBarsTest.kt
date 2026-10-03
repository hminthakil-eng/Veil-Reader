package com.veilreader.app.ui

import android.view.View
import android.view.Window
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GrayfogSystemBarsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paperAndShellChangeBothIconFields() {
        val light = mutableStateOf(true)
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            VeilSystemBars(light.value)
        }
        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(requireNotNull(view.context.veilWindow()), view)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
            light.value = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(requireNotNull(view.context.veilWindow()), view)
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
        }
    }

    @Test fun darkDialogDoesNotChangeUnderlyingPaperWindow() {
        lateinit var activityView: View
        lateinit var dialogView: View
        lateinit var dialogWindow: Window
        compose.setContent {
            activityView = LocalView.current
            VeilSystemBars(true)
            Dialog(onDismissRequest = {}) {
                dialogView = LocalView.current
                dialogWindow = (dialogView.parent as DialogWindowProvider).window
                VeilSystemBars(false)
                Text("Dialog window specimen")
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val activityWindow = requireNotNull(activityView.context.veilWindow())
            assertNotSame(activityWindow, dialogWindow)
            assertTrue(WindowCompat.getInsetsController(activityWindow, activityView).isAppearanceLightStatusBars)
            assertFalse(WindowCompat.getInsetsController(dialogWindow, dialogView).isAppearanceLightStatusBars)
        }
    }
}
