package com.veilreader.app.ui.screens

import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderConsoleVisibleModeTest {
    @Test
    fun `unreleased paper never shows active when engine unavailable`() {
        val appearance = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.PAPER
        )
        assertEquals(
            ReaderNavigationMode.PAGED,
            readerConsoleVisibleMode(appearance, paperAvailable = false)
        )
        // The visual label must not silently overwrite the saved Paper choice.
        assertEquals(PageTurnStyle.PAPER, appearance.pageTurnStyle)
    }

    @Test
    fun `enabled paper is visibly distinct from slide`() {
        assertEquals(
            ReaderNavigationMode.PAPER_CURL,
            readerConsoleVisibleMode(
                ReaderAppearance(pageTurnStyle = PageTurnStyle.PAPER),
                paperAvailable = true
            )
        )
        assertEquals(
            ReaderNavigationMode.SLIDE,
            readerConsoleVisibleMode(
                ReaderAppearance(pageTurnStyle = PageTurnStyle.SLIDE),
                paperAvailable = false
            )
        )
    }

    @Test
    fun `scroll has priority over an unavailable stored paper style`() {
        assertEquals(
            ReaderNavigationMode.SCROLL,
            readerConsoleVisibleMode(
                ReaderAppearance(scroll = true, pageTurnStyle = PageTurnStyle.PAPER),
                paperAvailable = false
            )
        )
    }

    @Test
    fun `static paged mode remains independently selected`() {
        assertEquals(
            ReaderNavigationMode.PAGED,
            readerConsoleVisibleMode(
                ReaderAppearance(pageTurnStyle = PageTurnStyle.NONE),
                paperAvailable = true
            )
        )
    }
}
