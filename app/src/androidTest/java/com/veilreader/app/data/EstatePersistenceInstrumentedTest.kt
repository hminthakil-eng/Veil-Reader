package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.*
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EstatePersistenceInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: VeilDatabase
    private lateinit var settings: SettingsStore

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java).allowMainThreadQueries().build()
        settings = SettingsStore(context)
    }

    @After fun cleanup() {
        database.close()
        context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun claimsBuildsAndDesignSurviveRecreationWithoutDuplicateRewards() {
        val game = GameRepository(context)
        repeat(15) { game.recordReadingMinute() }
        assertTrue(game.claimStoryChapter("first_light"))
        assertTrue(game.buildEstate())
        assertFalse(game.buildEstate())
        assertFalse(game.designEstate("Home", EstatePalette.AMBER, EstateGrounds.FOUNTAIN, EstateSky.DAWN))
        assertTrue(game.designEstate("خانه من", EstatePalette.AMBER, EstateGrounds.LANTERNS, EstateSky.DAWN))
        val expected = game.estate.value
        val restored = GameRepository(context)
        assertEquals(expected, restored.estate.value)
        assertEquals(15, restored.profile.value.minutesRead)
        assertFalse(restored.claimStoryChapter("first_light"))
        assertEquals(5, EstateCampaign.balance(restored.estate.value, 15))
    }

    @Test fun backupRoundTripPreservesEstateAndEveryNewReadingPreference() = runBlocking {
        val library = LocalLibraryRepository(context, database, settings, runLegacyMigration = false)
        val game = GameRepository(context)
        repeat(15) { game.recordReadingMinute() }
        game.claimStoryChapter("first_light")
        game.buildEstate()
        game.designEstate("The Quiet Gate", EstatePalette.WISTERIA, EstateGrounds.LANTERNS, EstateSky.MIST)
        val expectedEstate = game.estate.value
        val expectedAppearance = ReaderAppearance(theme = ReaderTheme.SEPIA, fontScale = 1.3,
            pageTurnStyle = PageTurnStyle.CURL, font = ReaderFont.SANS, justified = true,
            keepScreenOn = true, reduceMotion = true)
        library.saveAppearance(expectedAppearance)
        library.flushWrites()
        val backup = File(context.cacheDir, "estate-roundtrip.zip")
        try {
            val exporter = LibraryExport(context, library)
            exporter.writeBackup(Uri.fromFile(backup))
            context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE).edit().clear().commit()
            library.saveAppearance(ReaderAppearance())
            library.flushWrites()
            exporter.restoreBackup(Uri.fromFile(backup))
            game.refresh()
            assertEquals(expectedEstate, game.estate.value)
            assertEquals(15, game.profile.value.minutesRead)
            assertEquals(expectedAppearance, settings.settings.first().readerAppearance)
            assertFalse(game.claimStoryChapter("first_light"))
        } finally { backup.delete() }
    }

    @Test fun savedAppearanceArrivingAfterStartupReachesTheReaderFlow() = runBlocking {
        val library = LocalLibraryRepository(context, database, settings, runLegacyMigration = false)
        val expected = ReaderAppearance(pageTurnStyle = PageTurnStyle.INSTANT, font = ReaderFont.SERIF)
        settings.saveReaderAppearance(expected)
        assertEquals(expected, withTimeout(5000) { library.appearance.first { it == expected } })
    }

    @Test fun continuousScrollSurvivesSettingsReloadWithDefaultPageStyle() = runBlocking {
        settings.saveReaderAppearance(ReaderAppearance(scroll = true))
        val appearance = SettingsStore(context).settings.first().readerAppearance
        assertTrue(appearance.scroll)
        assertEquals(PageTurnStyle.SLIDE, appearance.pageTurnStyle)
        assertEquals(ReaderFont.ORIGINAL, appearance.font)
        assertFalse(appearance.keepScreenOn)
        assertFalse(appearance.reduceMotion)
    }
}
