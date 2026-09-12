package com.veilreader.app.data

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.ui.reader.PageCurlView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageCurlInstrumentedTest {
    @Test fun failedNavigationDoesNotLeaveSnapshotCoveringReader() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val source = View(context).apply { layout(0, 0, 400, 600); setBackgroundColor(android.graphics.Color.WHITE) }
            val overlay = PageCurlView(context).apply { layout(0, 0, 400, 600) }
            assertFalse(overlay.turn(source, "last-page", true) { false })
            assertFalse(overlay.isTurning)
            assertEquals(View.INVISIBLE, overlay.visibility)
            overlay.release()
        }
    }

    @Test fun rapidInputsNavigateOnlyOnceAndDisposalReleasesTheTurn() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val source = View(context).apply { layout(0, 0, 400, 600) }
            val overlay = PageCurlView(context).apply { layout(0, 0, 400, 600) }
            var turns = 0
            overlay.turn(source, "first-page", true) { turns++; true }
            overlay.turn(source, "first-page", true) { turns++; true }
            assertEquals(1, turns)
            assertTrue(overlay.isTurning)
            overlay.release()
            assertFalse(overlay.isTurning)
            assertEquals(View.INVISIBLE, overlay.visibility)
        }
    }
}
