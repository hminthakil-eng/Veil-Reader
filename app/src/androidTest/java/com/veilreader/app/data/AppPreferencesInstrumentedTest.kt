package com.veilreader.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.AppPreferences
import com.veilreader.app.domain.AppTheme
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPreferencesInstrumentedTest {
    @Test fun quietReadingStillFundsTheEstateAndPreferencesSurviveRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val original = store.settings.first().appPreferences
        val gamePrefs = context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
        gamePrefs.edit().clear().commit()
        try {
            val expected = AppPreferences(AppTheme.LIGHT, gameVisible = false, onboardingCompleted = true)
            store.saveAppPreferences(expected)
            val game = GameRepository(context)
            repeat(15) { game.recordReadingMinute() }
            assertTrue(game.buildEstate())
            assertEquals(15, GameRepository(context).profile.value.minutesRead)
            assertEquals(expected, SettingsStore(context).settings.first().appPreferences)
        } finally {
            store.saveAppPreferences(original)
            gamePrefs.edit().clear().commit()
        }
    }

    @Test fun olderBackupRetainsAppChoicesAndLateSettingsReachTheUiFlow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java).allowMainThreadQueries().build()
        val store = SettingsStore(context)
        val original = store.settings.first().appPreferences
        val backup = File(context.cacheDir, "preferences-new.zip")
        val legacy = File(context.cacheDir, "preferences-old.zip")
        try {
            val library = LocalLibraryRepository(context, database, store, runLegacyMigration = false)
            val exporter = LibraryExport(context, library)
            exporter.writeBackup(Uri.fromFile(backup))
            ZipFile(backup).use { input ->
                ZipOutputStream(legacy.outputStream()).use { output ->
                    input.entries().asSequence().forEach { entry ->
                        output.putNextEntry(ZipEntry(entry.name))
                        val bytes = input.getInputStream(entry).use { it.readBytes() }
                        if (entry.name == "manifest.json") {
                            val manifest = JSONObject(bytes.toString(Charsets.UTF_8))
                            manifest.getJSONObject("library").remove("appPreferences")
                            output.write(manifest.toString().toByteArray())
                        } else output.write(bytes)
                        output.closeEntry()
                    }
                }
            }
            val expected = AppPreferences(AppTheme.DARK, gameVisible = false, onboardingCompleted = true)
            store.saveAppPreferences(expected)
            assertEquals(expected, withTimeout(5000) { library.appPreferences.first { it == expected } })
            exporter.restoreBackup(Uri.fromFile(legacy))
            assertEquals(expected, store.settings.first().appPreferences)
        } finally {
            store.saveAppPreferences(original)
            backup.delete(); legacy.delete(); database.close()
        }
    }
}
