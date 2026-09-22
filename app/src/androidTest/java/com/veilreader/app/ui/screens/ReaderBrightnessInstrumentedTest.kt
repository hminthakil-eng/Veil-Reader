package com.veilreader.app.ui.screens

import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderBrightnessInstrumentedTest {
    @Test
    fun customBrightnessAndSystemReset_updateReaderWindowOnly() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                applyReaderBrightness(activity, 0.42)

                assertEquals(
                    0.42f,
                    activity.window.attributes.screenBrightness,
                    0.001f
                )

                applyReaderBrightness(activity, null)

                assertEquals(
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
                    activity.window.attributes.screenBrightness,
                    0.001f
                )
            }
        }
    }
}
