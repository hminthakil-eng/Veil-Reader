package com.veilreader.app

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.ui.reader.ReaderHardwareButtonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderHardwareKeysInstrumentedTest {
    @Test
    fun activityOwnsMappedPressWithoutChangingVolumeAndRejectsStaleDisposal() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val streams = listOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM)
                val before = streams.map(audio::getStreamVolume)
                var calls = 0
                activity.installReaderHardwareKeyHandler("old") { error("Stale handler") }
                activity.installReaderHardwareKeyHandler("new") {
                    if (it.phase == ReaderHardwareButtonPhase.DOWN) calls += 1
                    true
                }
                activity.clearReaderHardwareKeyHandler("old")
                val now = SystemClock.uptimeMillis()
                assertTrue(activity.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0)))
                activity.clearReaderHardwareKeyHandler("new")
                assertTrue(activity.dispatchKeyEvent(KeyEvent(now, now + 20L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 0)))
                assertEquals(1, calls)
                assertEquals(before, streams.map(audio::getStreamVolume))
            }
        }
    }
}
