package com.veilreader.app.ui.screens

import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class PaperCoveredSourceTest {
    private fun view() = View(ApplicationProvider.getApplicationContext()).apply {
        layout(0, 0, 120, 240)
        isClickable = true
        isFocusable = true
    }

    @Test fun `covered source still receives the native gesture and restores drawing on cancellation`() {
        val source = view()
        var received = false
        source.setOnTouchListener { _, event ->
            received = event.actionMasked == MotionEvent.ACTION_DOWN
            true
        }
        val parent = FrameLayout(source.context).apply {
            addView(source, FrameLayout.LayoutParams(120, 240))
            measure(View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY))
            layout(0, 0, 120, 240)
        }
        val lease = PaperCoveredSource(source)
        assertEquals(0f, source.alpha, 0f)
        assertEquals(View.VISIBLE, source.visibility)
        assertTrue(source.isFocusable)
        assertEquals(120, source.width)
        assertEquals(240, source.height)
        val down = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        try { assertTrue(parent.dispatchTouchEvent(down)) } finally { down.recycle() }
        assertTrue("Opacity must not steal native touch ownership", received)
        lease.close()
        lease.close()
        assertEquals(1f, source.alpha, 0f)
    }

    @Test fun `existing reader fade belongs to its original owner`() {
        val source = view().apply { alpha = 0.6f }
        val lease = PaperCoveredSource(source)
        assertEquals(0.6f, source.alpha, 0f)
        lease.close()
        assertEquals(0.6f, source.alpha, 0f)
    }

    @Test fun `release does not overwrite another owner changing opacity`() {
        val source = view()
        val lease = PaperCoveredSource(source)
        source.alpha = 0.4f
        lease.close()
        assertEquals(0.4f, source.alpha, 0f)
    }
}
