package com.veilreader.app.data

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DurablePreferencesTest {
    @Test
    fun `restore barrier returns success only after one atomic synchronous commit`() {
        val writes = mutableListOf<String>()
        val preferences = preferences(commitResult = true, writes)

        val succeeded = preferences.commitDurably {
            clear()
            putInt("rankIndex", 2)
            putString("pathId", "oracle")
        }

        assertTrue(succeeded)
        assertEquals(listOf("edit", "clear", "putInt", "putString", "commit"), writes)
    }

    @Test
    fun `failed disk commit reaches rollback caller and emits a diagnostic`() {
        val writes = mutableListOf<String>()
        val preferences = preferences(commitResult = false, writes)
        ShadowLog.clear()

        assertFalse(preferences.commitDurably { putInt("todayMinutes", 5) })

        assertEquals(listOf("edit", "putInt", "commit"), writes)
        assertTrue(ShadowLog.getLogsForTag("VeilPreferences").any {
            it.msg == "Synchronous preference write failed"
        })
    }

    private fun preferences(commitResult: Boolean, writes: MutableList<String>): SharedPreferences {
        val editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { proxy, method, _ ->
            writes += method.name
            when (method.name) {
                "commit" -> commitResult
                "apply" -> error("The durable barrier must not schedule an asynchronous write")
                else -> proxy
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, _ ->
            check(method.name == "edit")
            writes += method.name
            editor
        } as SharedPreferences
    }
}
