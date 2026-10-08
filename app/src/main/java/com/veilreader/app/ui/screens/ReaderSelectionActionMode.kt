package com.veilreader.app.ui.screens

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.CancellationException
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
 * A fresh Note action must contain note text before it can commit. Existing highlights may save an
 * empty note deliberately, which removes only their annotation while preserving the highlight.
 */
internal fun canSavePendingSelectionNote(
    isNewNote: Boolean,
    note: String
): Boolean =
    !isNewNote || note.isNotBlank()

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

    private var activeMode: ActionMode? = null

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        activeMode = mode
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
            // immediately on some devices. Cleanup is unconditional: renderer disposal or a
            // selection-query failure must never leave Android's native toolbar orphaned.
            val selection = try {
                navigator.currentSelection()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                try {
                    navigator.clearSelection()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The navigator can disappear while ActionMode is closing. The toolbar still
                    // belongs to this callback and must be dismissed.
                } finally {
                    mode.finish()
                }
            }
            val quote = selection?.locator?.text?.highlight.orEmpty().trim()
            if (selection == null || quote.isBlank()) return@launch
            onAction(action, selection.locator, quote)
        }

        return true
    }

    /**
     * Reader-owned overlays must not leave Android's native selection toolbar floating above them.
     * Clear Readium's selection first, then finish the exact ActionMode owned by this callback.
     */
    fun dismissSelection() {
        val mode = activeMode
        coroutineScope.launch {
            try {
                navigatorProvider()?.clearSelection()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Renderer teardown can race an overlay opening. The ActionMode is still ours.
            } finally {
                mode?.finish()
            }
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // Android may destroy a replaced toolbar after its successor has acquired selection.
        // Only the current owner can release the Reader's selection/input reservation.
        if (activeMode !== mode) return
        activeMode = null
        onModeChanged(false)
    }

    private companion object {
        // App-local IDs; they only need to be stable for the lifetime of the action mode.
        const val ACTION_HIGHLIGHT = 0x5648
        const val ACTION_NOTE = 0x564E
        const val ACTION_LOOKUP = 0x564C
    }
}
