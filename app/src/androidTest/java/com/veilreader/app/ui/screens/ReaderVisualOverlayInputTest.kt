package com.veilreader.app.ui.screens

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderVisualOverlayInputTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nestedNativeOverlaySharesTapAndSwipeWithPublicationOnlyWhenParentShares() {
        val share = mutableStateOf(false)
        val events = mutableListOf<Int>()
        compose.setContent {
            Box(Modifier.size(240.dp).testTag("viewport")) {
                AndroidView(
                    factory = { RecordingView(it, events) },
                    modifier = Modifier.fillMaxSize()
                )
                // Match the nested native GPU host above the separate publication subtree.
                Box(if (share.value) Modifier.fillMaxSize().readerVisualOnlyInput() else Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { View(it).apply { isClickable = false; isFocusable = false } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        val viewport = compose.onNodeWithTag("viewport")
        viewport.performTouchInput { click(center) }
        viewport.performTouchInput { swipe(Offset(width * .8f, height * .8f), Offset(width * .2f, height * .8f)) }
        compose.runOnIdle {
            assertTrue("Negative control must reproduce the blocked publication", events.isEmpty())
            share.value = true
        }
        viewport.performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), events)
            events.clear()
        }
        viewport.performTouchInput { swipe(Offset(width * .8f, height * .8f), Offset(width * .2f, height * .8f)) }
        compose.runOnIdle {
            assertEquals(MotionEvent.ACTION_DOWN, events.first())
            assertTrue(events.contains(MotionEvent.ACTION_MOVE))
            assertEquals(MotionEvent.ACTION_UP, events.last())
        }
    }

    private class RecordingView(context: Context, private val events: MutableList<Int>) : View(context) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
            events += event.actionMasked
            return true
        }
    }
}
