package com.veilreader.app.ui.screens

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.readium.r2.navigator.SelectableNavigator
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ReaderSelectionActionModeTest {
    @Test
    fun translateActionIsLocalizedAndNotDuplicatedWhenToolbarRecreates() = runTest {
        val callback = callback(mutableListOf())
        val mode = TestActionMode()
        callback.onCreateActionMode(mode, mode.menu)
        callback.onCreateActionMode(mode, mode.menu)
        assertEquals(4, mode.menu.size())
        assertEquals("Translate", mode.menu.findItem(0x5654).title.toString())
    }

    @Test
    @Config(sdk = [28])
    fun translateActionIsHiddenBeforePlatformSupport() = runTest {
        val callback = callback(mutableListOf())
        val mode = TestActionMode()
        callback.onCreateActionMode(mode, mode.menu)
        assertEquals(3, mode.menu.size())
        assertNull(mode.menu.findItem(0x5654))
    }

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

    @Test
    fun replacingModeWhileSelectionQueryWaits_cannotClearSuccessor() = runTest {
        val fake = SelectionNavigator(holdQuery = true)
        val callback = callback(mutableListOf(), { fake.navigator })
        val old = TestActionMode()
        val replacement = TestActionMode()
        callback.onCreateActionMode(old, old.menu)
        callback.onActionItemClicked(old, old.menu.findItem(0x5648))
        runCurrent()
        assertEquals(1, fake.queries)
        callback.onCreateActionMode(replacement, replacement.menu)
        fake.pending!!.resume(null)
        runCurrent()
        assertEquals(0, fake.clears)
        assertEquals(1, old.finishes)
        assertEquals(0, replacement.finishes)
    }

    @Test
    fun queuedDismissal_cannotClearReplacementNavigatorOrMode() = runTest {
        val oldNavigator = SelectionNavigator()
        val newNavigator = SelectionNavigator()
        var current = oldNavigator.navigator
        val callback = callback(mutableListOf(), { current })
        val old = TestActionMode()
        val replacement = TestActionMode()
        callback.onCreateActionMode(old, old.menu)
        callback.dismissSelection()
        current = newNavigator.navigator
        callback.onCreateActionMode(replacement, replacement.menu)
        runCurrent()
        assertEquals(0, oldNavigator.clears)
        assertEquals(0, newNavigator.clears)
        assertEquals(1, old.finishes)
        assertEquals(0, replacement.finishes)
    }

    @Test
    fun currentEmptySelectionAction_stillClearsAndFinishesOwnedMode() = runTest {
        val fake = SelectionNavigator()
        val callback = callback(mutableListOf(), { fake.navigator })
        val mode = TestActionMode()
        callback.onCreateActionMode(mode, mode.menu)
        callback.onActionItemClicked(mode, mode.menu.findItem(0x5648))
        runCurrent()
        assertEquals(1, fake.queries)
        assertEquals(1, fake.clears)
        assertEquals(1, mode.finishes)
    }

    private class SelectionNavigator(val holdQuery: Boolean = false) {
        var queries = 0
        var clears = 0
        var pending: Continuation<Any?>? = null
        @Suppress("UNCHECKED_CAST")
        val navigator = Proxy.newProxyInstance(
            SelectableNavigator::class.java.classLoader, arrayOf(SelectableNavigator::class.java)
        ) { _, method, args ->
            when (method.name) {
                "currentSelection" -> {
                    queries++
                    if (holdQuery) {
                        pending = args!!.last() as Continuation<Any?>
                        COROUTINE_SUSPENDED
                    } else null
                }
                "clearSelection" -> { clears++; Unit }
                else -> error("Unexpected navigator access: ${method.name}")
            }
        } as SelectableNavigator
    }

    private fun kotlinx.coroutines.CoroutineScope.callback(states: MutableList<Boolean>, navigator: () -> SelectableNavigator? = { null }) =
        ReaderSelectionActionModeCallback(
            coroutineScope = this,
            navigatorProvider = navigator,
            highlightLabel = "Highlight",
            noteLabel = "Note",
            lookupLabel = "Lookup",
            translateLabel = "Translate",
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
        var finishes = 0
        override fun finish() { finishes++ }
        override fun getMenu(): Menu = items
        override fun getTitle(): CharSequence = ""
        override fun getSubtitle(): CharSequence = ""
        override fun getCustomView(): View? = null
        override fun getMenuInflater(): MenuInflater = MenuInflater(context)
    }
}
