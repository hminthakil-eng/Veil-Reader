package com.veilreader.app.ui.screens

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderLookupTest {
    @Test
    fun `blank selection has no lookup intent`() {
        assertNull(readerLookupIntent("   \n\t  "))
    }

    @Test
    fun `lookup intent trims selection and keeps it read only`() {
        val intent = requireNotNull(
            readerLookupIntent("  selected phrase  ")
        )

        assertEquals(Intent.ACTION_PROCESS_TEXT, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(
            "selected phrase",
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        )
        assertTrue(
            intent.getBooleanExtra(
                Intent.EXTRA_PROCESS_TEXT_READONLY,
                false
            )
        )
    }

    @Test
    fun `lookup preserves internal whitespace and unicode`() {
        val intent = requireNotNull(
            readerLookupIntent("  معنا  و  meaning  ")
        )

        assertEquals(
            "معنا  و  meaning",
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        )
    }
}
