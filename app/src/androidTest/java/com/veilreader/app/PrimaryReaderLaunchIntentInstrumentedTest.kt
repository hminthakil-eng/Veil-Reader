package com.veilreader.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prevent a debug-only visual lab from intercepting ordinary package launches.
 *
 * SAF/PDF end-to-end tests use getLaunchIntentForPackage(), not an explicit
 * MainActivity class. A second debug MAIN/LAUNCHER activity can send those
 * tests (and real users) into fictional Paper fixtures instead of Library.
 */
@RunWith(AndroidJUnit4::class)
class PrimaryReaderLaunchIntentInstrumentedTest {

    @Test
    fun packageLaunchResolvesToActualReaderActivity() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val launch = target.packageManager.getLaunchIntentForPackage(target.packageName)
        assertNotNull("Primary Veil Reader launch intent is missing", launch)
        assertEquals(
            "Debug visual experiments must never hijack the Veil Reader launcher",
            ComponentName(target, MainActivity::class.java),
            launch!!.component
        )
    }

    @Test
    fun onlyPrimaryReaderAppearsAsLauncherTarget() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val launchQuery = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(target.packageName)
        @Suppress("DEPRECATION")
        val launchTargets = target.packageManager
            .queryIntentActivities(launchQuery, 0)
            .map { it.activityInfo.name }
            .toSet()

        assertEquals(
            "No debug-only Activity may advertise MAIN/LAUNCHER",
            setOf(MainActivity::class.java.name),
            launchTargets
        )
    }

    @Test
    fun debugCurlDeepLinkResolvesOnlyToSyntheticLab() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val debugView = Intent(Intent.ACTION_VIEW, Uri.parse("veilpaperlab://curl"))
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setPackage(target.packageName)
        @Suppress("DEPRECATION")
        val targets = target.packageManager.queryIntentActivities(debugView, 0)
            .map { it.activityInfo.name }
            .toSet()
        assertEquals(
            "Curl deep link must resolve only to the synthetic debug lab, never MainActivity",
            setOf("com.veilreader.app.debug.ExternalCurlArenaActivity"),
            targets
        )
    }

    @Test
    fun curlLabStillExistsForExplicitDebugQa() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val lab = ComponentName(
            target.packageName,
            "com.veilreader.app.debug.ExternalCurlArenaActivity"
        )
        @Suppress("DEPRECATION")
        val info = target.packageManager.getActivityInfo(lab, 0)
        assertEquals(lab.className, info.name)
        assertEquals("Explicit debug lab entry must remain available", true, info.exported)
    }
}
