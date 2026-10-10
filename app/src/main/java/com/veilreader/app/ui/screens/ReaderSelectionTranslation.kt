package com.veilreader.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build

/**
 * Platform-only context tools for Readium's native selection toolbar.
 *
 * No automatic network/clipboard/web-search fallback and no vendor-specific
 * dependency. The user must explicitly request a text transfer. The receiving
 * dictionary/translation app owns its own privacy and offline behavior.
 */
internal enum class ReaderExternalTextAction(
    val platformAction: String,
    val maxSelectionChars: Int
) {
    DEFINE(Intent.ACTION_DEFINE, 256),
    TRANSLATE(Intent.ACTION_TRANSLATE, 8_192)
}

internal object ReaderSelectionExternalText {
    fun intentFor(
        action: ReaderExternalTextAction,
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Intent? {
        if (sdkInt < Build.VERSION_CODES.Q) return null

        val text = quote.trim()
        if (
            text.isBlank() ||
            text.length > action.maxSelectionChars ||
            '\u0000' in text
        ) return null

        return Intent(action.platformAction).apply {
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    fun dispatch(
        action: ReaderExternalTextAction,
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
        launch: (Intent) -> Unit
    ): Boolean {
        val request = intentFor(action, quote, sdkInt) ?: return false
        return try {
            launch(request)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun launch(
        activity: Activity?,
        action: ReaderExternalTextAction,
        quote: String
    ): Boolean {
        val host = activity ?: return false
        return dispatch(action, quote) { intent -> host.startActivity(intent) }
    }
}

/** Kept as a narrow facade so existing translation callers/tests stay stable. */
internal object ReaderSelectionTranslation {
    fun intentFor(
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Intent? = ReaderSelectionExternalText.intentFor(
        ReaderExternalTextAction.TRANSLATE,
        quote,
        sdkInt
    )

    fun dispatch(
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
        launch: (Intent) -> Unit
    ): Boolean = ReaderSelectionExternalText.dispatch(
        ReaderExternalTextAction.TRANSLATE,
        quote,
        sdkInt,
        launch
    )

    fun launch(activity: Activity?, quote: String): Boolean =
        ReaderSelectionExternalText.launch(activity, ReaderExternalTextAction.TRANSLATE, quote)
}

/** Dictionary definition is a separate explicit choice, not a disguised web lookup. */
internal object ReaderSelectionDefinition {
    fun intentFor(
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Intent? = ReaderSelectionExternalText.intentFor(
        ReaderExternalTextAction.DEFINE,
        quote,
        sdkInt
    )

    fun dispatch(
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
        launch: (Intent) -> Unit
    ): Boolean = ReaderSelectionExternalText.dispatch(
        ReaderExternalTextAction.DEFINE,
        quote,
        sdkInt,
        launch
    )

    fun launch(activity: Activity?, quote: String): Boolean =
        ReaderSelectionExternalText.launch(activity, ReaderExternalTextAction.DEFINE, quote)
}
