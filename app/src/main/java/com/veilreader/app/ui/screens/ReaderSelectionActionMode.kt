package com.veilreader.app.ui.screens

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.shared.publication.Locator

/** Actions that belong beside the selected passage, not in the persistent reader chrome. */
internal enum class ReaderSelectionAction {
    HIGHLIGHT,
    NOTE,
    LOOKUP
}

/**
 * Adds Veil's annotation actions to Android's native EPUB text-selection toolbar.
 *
 * Readium owns selection handles and the underlying WebView action mode. This callback only adds
 * actions that operate on the current Readium selection, keeping selection behavior native and
 * avoiding a second trip to the reader toolbar.
 */
internal class ReaderSelectionActionModeCallback(
    private val coroutineScope: CoroutineScope,
    private val navigatorProvider: () -> SelectableNavigator?,
    private val highlightLabel: String,
    private val noteLabel: String,
    private val lookupLabel: String,
    private val onModeChanged: (Boolean) -> Unit = {},
    private val onAction: suspend (ReaderSelectionAction, Locator, String) -> Unit
) : BaseActionModeCallback() {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        onModeChanged(true)
        if (menu.findItem(ACTION_HIGHLIGHT) == null) {
            menu.add(Menu.NONE, ACTION_HIGHLIGHT, 0, highlightLabel)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (menu.findItem(ACTION_NOTE) == null) {
            menu.add(Menu.NONE, ACTION_NOTE, 1, noteLabel)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (menu.findItem(ACTION_LOOKUP) == null) {
            menu.add(Menu.NONE, ACTION_LOOKUP, 2, lookupLabel)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val action = when (item.itemId) {
            ACTION_HIGHLIGHT -> ReaderSelectionAction.HIGHLIGHT
            ACTION_NOTE -> ReaderSelectionAction.NOTE
            ACTION_LOOKUP -> ReaderSelectionAction.LOOKUP
            else -> return false
        }
        val navigator = navigatorProvider() ?: return false

        coroutineScope.launch {
            // Capture first because finishing ActionMode can clear the WebView selection
            // immediately on some devices. Once captured, dismiss native selection chrome before
            // durable annotation work so a storage flush never leaves the toolbar hanging.
            val selection = navigator.currentSelection()
            val quote = selection?.locator?.text?.highlight.orEmpty().trim()
            try {
                navigator.clearSelection()
            } finally {
                mode.finish()
            }
            if (selection == null || quote.isBlank()) return@launch
            onAction(action, selection.locator, quote)
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        onModeChanged(false)
    }

    private companion object {
        // App-local IDs; they only need to be stable for the lifetime of the action mode.
        const val ACTION_HIGHLIGHT = 0x5648
        const val ACTION_NOTE = 0x564E
        const val ACTION_LOOKUP = 0x564C
    }
}
