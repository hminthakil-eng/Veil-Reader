package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import fi.harism.curl.CurlPage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativePaperTextureOwnershipTest {
    @Test fun nativeSizeUploadsKeepInkAndNeverRecycleTheBorrowedReadiumSource() {
        val source = Bitmap.createBitmap(3, 5, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        source.setPixel(1, 2, Color.BLACK)
        val owned = requireNotNull(source.copy(Bitmap.Config.ARGB_8888, false))
        val page = CurlPage()
        page.setExactSizeTextures(true)
        page.setTexture(owned, CurlPage.SIDE_BOTH)
        val coordinates = RectF()
        val texture = page.getTexture(coordinates, CurlPage.SIDE_FRONT)
        assertSame(owned, texture)
        assertEquals(RectF(0f, 0f, 1f, 1f), coordinates)
        assertEquals(Color.BLACK, texture.getPixel(1, 2))
        assertFalse(page.hasBackTexture())
        page.recycle()
        assertTrue(owned.isRecycled)
        assertFalse(source.isRecycled)
        assertEquals(Color.BLACK, source.getPixel(1, 2))
        source.recycle()
    }

    @Test fun olderContextsKeepCorrectPaddingCoordinatesAndTransparentBorders() {
        val source = Bitmap.createBitmap(3, 5, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        source.setPixel(1, 2, Color.BLACK)
        val page = CurlPage()
        page.setTexture(source, CurlPage.SIDE_BOTH)
        val coordinates = RectF()
        val texture = page.getTexture(coordinates, CurlPage.SIDE_FRONT)
        assertNotSame(source, texture)
        assertEquals(4, texture.width)
        assertEquals(8, texture.height)
        assertEquals(RectF(0f, 0f, 0.75f, 0.625f), coordinates)
        assertEquals(Color.BLACK, texture.getPixel(1, 2))
        assertEquals(0, Color.alpha(texture.getPixel(3, 7)))
        assertFalse(source.isRecycled)
        texture.recycle()
        page.recycle()
    }
}
