package com.veilreader.app.ui.screens

import androidx.compose.ui.unit.IntSize
import com.veilreader.app.domain.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderInteractionPolicyDurabilityTest {

    @Test
    fun finalSnapshot_isRejectedWhilePaperOrSlidePreviewOwnsNavigation() {
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = true,
                slidePreviewActive = false,
                previewCancelled = false
            )
        )
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = true,
                previewCancelled = false
            )
        )
    }

    @Test
    fun finalSnapshot_isRejectedAfterEmergencyPreviewCancellationEvenWhenVisualStateIsAlreadyCleared() {
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = true
            )
        )
    }

    @Test
    fun finalSnapshot_isRejectedWhileProgrammaticJumpHasNotSettled() {
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = false,
                programmaticNavigationInFlight = true
            )
        )
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.PDF,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = false,
                programmaticNavigationInFlight = true
            )
        )
    }

    @Test
    fun finalSnapshot_isAllowedOnlyAfterNavigationOwnershipIsSettled() {
        assertTrue(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = false,
                programmaticNavigationInFlight = false
            )
        )
        assertTrue(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.PDF,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = false,
                programmaticNavigationInFlight = false
            )
        )
    }

    @Test
    fun backCancelsTransientPageOwnershipBeforeClosingReader() {
        assertEquals(
            ReaderBackDisposition.SWALLOW,
            readerBackDisposition(
                closeInFlight = true,
                paperPreviewActive = true,
                slidePreviewActive = true
            )
        )
        assertEquals(
            ReaderBackDisposition.CANCEL_PAPER,
            readerBackDisposition(
                closeInFlight = false,
                paperPreviewActive = true,
                slidePreviewActive = true
            )
        )
        assertEquals(
            ReaderBackDisposition.CANCEL_SLIDE,
            readerBackDisposition(
                closeInFlight = false,
                paperPreviewActive = false,
                slidePreviewActive = true
            )
        )
        assertEquals(
            ReaderBackDisposition.CLOSE,
            readerBackDisposition(
                closeInFlight = false,
                paperPreviewActive = false,
                slidePreviewActive = false
            )
        )
    }

    @Test
    fun viewportChangeCancelsOnlyAnActivePreview() {
        val oldSize = IntSize(1080, 2400)
        val newSize = IntSize(2400, 1080)

        assertTrue(
            shouldCancelReaderPreviewForViewportChange(
                previousSize = oldSize,
                newSize = newSize,
                paperPreviewActive = true,
                slidePreviewActive = false
            )
        )
        assertTrue(
            shouldCancelReaderPreviewForViewportChange(
                previousSize = oldSize,
                newSize = newSize,
                paperPreviewActive = false,
                slidePreviewActive = true
            )
        )
        assertFalse(
            shouldCancelReaderPreviewForViewportChange(
                previousSize = oldSize,
                newSize = newSize,
                paperPreviewActive = false,
                slidePreviewActive = false
            )
        )
        assertFalse(
            shouldCancelReaderPreviewForViewportChange(
                previousSize = IntSize.Zero,
                newSize = newSize,
                paperPreviewActive = true,
                slidePreviewActive = false
            )
        )
    }
}
