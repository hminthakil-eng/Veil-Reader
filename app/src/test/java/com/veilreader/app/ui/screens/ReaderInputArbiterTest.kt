package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderInputArbiterTest {

    @Test
    fun `directional edge taps belong only to paginated EPUB slide mode`() {
        assertTrue(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = false,
                pageTurnStyle = PageTurnStyle.PAPER
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.EPUB,
                scroll = true,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.PDF,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )

        assertFalse(
            shouldUseDirectionalTapNavigation(
                format = BookFormat.COMIC,
                scroll = false,
                pageTurnStyle = PageTurnStyle.SLIDE
            )
        )
    }
}
