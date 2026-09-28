package com.veilreader.app.ui.screens

import com.veilreader.app.R
import com.veilreader.app.domain.SilentNamesEncounter
import com.veilreader.app.domain.SilentNamesOutcome
import com.veilreader.app.domain.StoryRelicCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StoryRelicPresentationTest {
    @Test
    fun `every domain story relic definition has a Treasury presentation`() {
        assertEquals(
            StoryRelicCatalog.knownDefinitions().map { it.id }.toSet(),
            knownStoryRelicPresentationIds()
        )
    }

    @Test
    fun `Lantern presentation maps every authored Silent Names outcome`() {
        val presentation = assertNotNull(
            storyRelicPresentationFor(SilentNamesEncounter.REWARD_ID)
        )

        assertEquals(
            SilentNamesOutcome.entries.map { it.name }.toSet(),
            presentation.routeVariantRes.keys
        )
    }

    @Test
    fun `Path provenance uses canonical name with safe future fallback`() {
        assertEquals("Oracle", storyRelicPathLabel("oracle"))
        assertEquals("Future path", storyRelicPathLabel("future_path"))
    }

    @Test
    fun `unknown resolution mode has a dedicated compact label`() {
        assertEquals(R.string.story_relic_unknown_mode, storyRelicModeRes("FUTURE_MODE"))
    }
}
