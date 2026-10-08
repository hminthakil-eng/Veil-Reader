package com.veilreader.app.ui.screens

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ReaderSelectionActionModeTest {
    @Test
    fun currentModeDestruction_releasesSelectionOwnershipExactlyOnce() = runTest {
        val states = mutableListOf<Boolean>()
        val callback = callback(states)
        val mode = TestActionMode()
        callback.onCreateActionMode(mode, mode.menu)
        callback.onDestroyActionMode(mode)
        callback.onDestroyActionMode(mode)
        assertEquals(listOf(true, false), states)
    }

    @Test
    fun replacedModeDestruction_cannotReleaseNewSelectionOwnership() = runTest {
        val states = mutableListOf<Boolean>()
        val callback = callback(states)
        val oldMode = TestActionMode()
        val newMode = TestActionMode()
        callback.onCreateActionMode(oldMode, oldMode.menu)
        callback.onCreateActionMode(newMode, newMode.menu)
        callback.onDestroyActionMode(oldMode)
        assertEquals(listOf(true, true), states)
        callback.onDestroyActionMode(newMode)
        assertEquals(listOf(true, true, false), states)
    }

    @Test
    fun unownedModeDestruction_doesNotChangeReaderInputOwnership() = runTest {
        val states = mutableListOf<Boolean>()
        callback(states).onDestroyActionMode(TestActionMode())
        assertEquals(emptyList<Boolean>(), states)
    }

    private fun kotlinx.coroutines.CoroutineScope.callback(states: MutableList<Boolean>) =
        ReaderSelectionActionModeCallback(
            coroutineScope = this,
            navigatorProvider = { null },
            highlightLabel = "Highlight",
            noteLabel = "Note",
            lookupLabel = "Lookup",
            onModeChanged = { states += it },
            onAction = { _, _, _ -> }
        )

    private class TestActionMode : ActionMode() {
        private val context: Context = RuntimeEnvironment.getApplication()
        private val items = PopupMenu(context, View(context)).menu
        override fun setTitle(title: CharSequence?) = Unit
        override fun setTitle(resId: Int) = Unit
        override fun setSubtitle(subtitle: CharSequence?) = Unit
        override fun setSubtitle(resId: Int) = Unit
        override fun setCustomView(view: View?) = Unit
        override fun invalidate() = Unit
        override fun finish() = Unit
        override fun getMenu(): Menu = items
        override fun getTitle(): CharSequence = ""
        override fun getSubtitle(): CharSequence = ""
        override fun getCustomView(): View? = null
        override fun getMenuInflater(): MenuInflater = MenuInflater(context)
    }
}
