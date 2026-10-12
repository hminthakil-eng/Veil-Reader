package com.veilreader.app.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.veilreader.app.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuantityResourcesTest {
    @Test
    fun `English reading counts use singular only for one`() {
        val resources = resources("en")
        for (count in listOf(0, 1, 2)) {
            val noun = if (count == 1) "session" else "sessions"
            assertEquals("$count $noun", resources.getQuantityString(
                R.plurals.book_detail_session_count, count, count
            ))
        }
        assertEquals("1 chapter · 4 MB sources · 2 MB page cache", resources.getQuantityString(
            R.plurals.manga_hub_storage_summary, 1, 1, "4 MB", "2 MB"
        ))
        assertEquals("Preserve 1 passage that reveals hidden structure", resources.getQuantityString(
            R.plurals.threshold_quest_oracle, 1, 1
        ))
    }

    @Test
    fun `Persian reading counts keep translated nouns and localized numbers`() {
        val resources = resources("fa")
        for (count in listOf(0, 1, 2)) {
            assertEquals(String.format(Locale("fa"), "%d نشست", count),
                resources.getQuantityString(R.plurals.book_detail_session_count, count, count))
        }
        assertEquals(String.format(Locale("fa"), "%d جلد به سکوت رفته است", 1),
            resources.getQuantityString(R.plurals.library_quiet_volume_count, 1, 1))
        assertEquals(String.format(Locale("fa"), "%d جلد به سکوت رفته‌اند", 2),
            resources.getQuantityString(R.plurals.library_quiet_volume_count, 2, 2))
    }

    @Test
    fun `restore notice selects each independent count rather than sharing a plural category`() {
        val resources = resources("en")
        val books = resources.getQuantityString(R.plurals.notice_restored_book_count, 1, 1)
        val highlights = resources.getQuantityString(R.plurals.notice_restored_highlight_count, 2, 2)
        assertEquals("Restored 1 book and 2 highlights. Reopen Veil Reader to load them.",
            resources.getString(R.string.notice_restore_success, books, highlights))

        val volumes = resources.getQuantityString(R.plurals.observatory_volume_count, 2, 2)
        val links = resources.getQuantityString(R.plurals.observatory_link_count, 1, 1)
        assertEquals("Memory Atlas with 2 volumes and 1 recorded link",
            resources.getString(R.string.observatory_canvas_semantics, volumes, links))
    }

    @Test
    fun `manga notice preserves singular duplicate verb and book title argument order`() {
        val resources = resources("en")
        val added = resources.getQuantityString(R.plurals.notice_added_chapter_count, 2, 2)
        val duplicate = resources.getQuantityString(R.plurals.notice_duplicate_chapter_count, 1, 1)
        assertEquals("Added 2 chapters to Grayfog; 1 was already present.",
            resources.getString(R.string.notice_manga_chapters_added_with_duplicates,
                added, duplicate, "Grayfog"))
    }

    @Test
    fun `chapter position cannot select the page count plural`() {
        val resources = resources("en")
        assertEquals("Position 7 · Ch. 2 · 1 page · 4 MB", resources.getQuantityString(
            R.plurals.manga_chapter_metadata, 1, 7, "2", 1, "4 MB"
        ))
    }

    @Test
    fun `large catalog count remains intact after quantity selector conversion`() {
        val count = Int.MAX_VALUE.toLong() + 100L
        assertEquals("$count chapters · 4 MB sources · 2 MB page cache",
            resources("en").getQuantityString(R.plurals.manga_hub_storage_summary,
                resourceQuantity(count), count, "4 MB", "2 MB"))
    }

    private fun resources(language: String): Resources {
        val context = RuntimeEnvironment.getApplication()
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(Locale(language)))
        return context.createConfigurationContext(configuration).resources
    }
}
