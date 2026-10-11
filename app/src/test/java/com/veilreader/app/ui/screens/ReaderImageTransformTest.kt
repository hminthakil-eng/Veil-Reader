package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderImageTransformTest {
    @Test
    fun `wide image has no vertical pan until it exceeds portrait viewport`() {
        val result = clampReaderImageTransform(ReaderImageTransform(2f, 900f, 900f), 1000, 2000, 2000, 1000)
        assertEquals(ReaderImageTransform(2f, 500f, 0f), result)
    }

    @Test
    fun `tall image has no horizontal pan until it exceeds landscape viewport`() {
        val result = clampReaderImageTransform(ReaderImageTransform(2f, -900f, -900f), 2000, 1000, 1000, 2000)
        assertEquals(ReaderImageTransform(2f, 0f, -500f), result)
    }

    @Test
    fun `pinch holds content under off centre centroid`() {
        val result = transformReaderImage(ReaderImageTransform(), 1000, 1000, 1000, 1000, 750f, 600f, 2f)
        assertEquals(ReaderImageTransform(2f, -250f, -100f), result)
        // The same source point remains at x=750, y=600 after scaling about centre.
        assertEquals(750f, 500f + 250f * result.scale + result.panX, 0.001f)
        assertEquals(600f, 500f + 100f * result.scale + result.panY, 0.001f)
    }

    @Test
    fun `zoom limit uses effective ratio and still permits dragging`() {
        val result = transformReaderImage(ReaderImageTransform(5f, 10f, 20f), 1000, 1000, 1000, 1000, 750f, 600f, 2f, 30f, -40f)
        assertEquals(ReaderImageTransform(5f, 40f, -20f), result)
    }

    @Test
    fun `zoom back to fit clears translation`() {
        val result = transformReaderImage(ReaderImageTransform(2f, 200f, -200f), 1000, 1000, 1000, 1000, 750f, 600f, 0.2f)
        assertEquals(ReaderImageTransform(), result)
    }

    @Test
    fun `resize clamps existing translation using new image fit`() {
        val result = clampReaderImageTransform(ReaderImageTransform(2f, 500f, 0f), 2000, 1000, 2000, 1000)
        assertEquals(ReaderImageTransform(2f, 500f, 0f), result)
        val portrait = clampReaderImageTransform(result, 500, 1000, 2000, 1000)
        assertEquals(ReaderImageTransform(2f, 250f, 0f), portrait)
    }

    @Test
    fun `double tap zooms toward touched illustration detail`() {
        val result = toggleReaderImageDoubleTap(
            ReaderImageTransform(), 1000, 1000, 1000, 1000, 700f, 550f
        )
        assertEquals(ReaderImageTransform(2.5f, -300f, -75f), result)
        assertEquals(700f, 500f + 200f * result.scale + result.panX, 0.001f)
        assertEquals(550f, 500f + 50f * result.scale + result.panY, 0.001f)
    }

    @Test
    fun `second double tap restores fit without stale pan`() {
        val enlarged = toggleReaderImageDoubleTap(
            ReaderImageTransform(), 1000, 1000, 1000, 1000, 700f, 550f
        )
        val restored = toggleReaderImageDoubleTap(
            enlarged, 1000, 1000, 1000, 1000, 200f, 200f
        )
        assertEquals(ReaderImageTransform(), restored)
    }

    @Test
    fun `double tap with unresolved viewport never invents a pan`() {
        val result = toggleReaderImageDoubleTap(
            ReaderImageTransform(2f, 80f, 70f), 0, 1000, 1000, 1000, 200f, 200f
        )
        assertEquals(ReaderImageTransform(2f), result)
    }

    @Test
    fun `double tap wide illustration respects true letterboxed bounds`() {
        val result = toggleReaderImageDoubleTap(
            ReaderImageTransform(), 1000, 1000, 2000, 1000, 800f, 500f
        )
        assertEquals(2.5f, result.scale, 0.001f)
        assertEquals(-450f, result.panX, 0.001f)
        assertEquals(0f, result.panY, 0.001f)
    }

    @Test
    fun `invalid dimensions and non finite input cannot poison transform`() {
        assertEquals(ReaderImageTransform(), clampReaderImageTransform(ReaderImageTransform(Float.NaN, Float.NaN, Float.POSITIVE_INFINITY), 1000, 1000, 1000, 1000))
        assertEquals(ReaderImageTransform(2f), clampReaderImageTransform(ReaderImageTransform(2f, 20f, 30f), 0, 1000, 1000, 1000))
        assertEquals(ReaderImageTransform(2f), transformReaderImage(ReaderImageTransform(2f), 1000, 1000, 1000, 1000, Float.NaN, Float.NaN, Float.NaN))
    }
}
