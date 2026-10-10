package com.veilreader.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ReaderSelectionTranslationTest {
    @Test
    fun textIsTrimmedAndSentOnlyThroughThePlatformTranslateContract() {
        val actual = ReaderSelectionTranslation.intentFor("  سلام \uD83D\uDCD6\n ")!!
        assertEquals(Intent.ACTION_TRANSLATE, actual.action)
        assertEquals("سلام \uD83D\uDCD6", actual.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(actual.data)
        assertNull(actual.component)
        assertNull(actual.`package`)
    }

    @Test
    fun actionIsNotClaimedOnPreQDevices() {
        assertNull(ReaderSelectionTranslation.intentFor("word", sdkInt = 28))
        var launched = false
        assertFalse(ReaderSelectionTranslation.dispatch("word", sdkInt = 28) {
            launched = true
        })
        assertFalse(launched)
    }

    @Test
    fun emptyAndUnsafeAndOversizeSelectionsDoNotLeaveTheReader() {
        for (quote in listOf("", "   \t", "first\u0000second", "a".repeat(8_193))) {
            var launchCount = 0
            assertFalse(ReaderSelectionTranslation.dispatch(quote, sdkInt = 35) {
                launchCount++
            })
            assertEquals(0, launchCount)
        }
        assertEquals(8_192, ReaderSelectionTranslation.intentFor("a".repeat(8_192))!!
            .getStringExtra(Intent.EXTRA_TEXT)!!.length)
    }

    @Test
    fun explicitTapUsesOneIntentAndDoesNotLaunchHiddenFallbacks() {
        val captured = mutableListOf<Intent>()
        val sent = ReaderSelectionTranslation.dispatch(" astronomy ", sdkInt = 35) {
            captured += it
        }
        assertTrue(sent)
        assertEquals(1, captured.size)
        assertEquals("astronomy", captured.single().getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun unavailableOrForbiddenProvidersFailClosed() {
        for (error in listOf(ActivityNotFoundException(), SecurityException())) {
            var attempts = 0
            assertFalse(ReaderSelectionTranslation.dispatch("selected", sdkInt = 35) {
                attempts++
                throw error
            })
            assertEquals(1, attempts)
        }
    }
}
