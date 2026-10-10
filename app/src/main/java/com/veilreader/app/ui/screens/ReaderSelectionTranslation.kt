package com.veilreader.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build

/**
 * Minimal external translation boundary for text selected through Readium.
 *
 * No network requests, history, clipboard writes, translation SDKs or provider-specific
 * dependencies originate here. A transfer happens only when the reader explicitly taps
 * Translate in Android's selection toolbar. Android chooses the installed handler.
 *
 * This is deliberately separate from the existing LOOKUP behavior so a missing translation
 * provider never silently turns into a web search or changes another reading action.
 */
internal object ReaderSelectionTranslation {
    private const val MAX_SELECTION_CHARS = 8_192

    /**
     * Build an Android platform request, not a vendor-specific intent. API 29 introduced
     * ACTION_TRANSLATE and ACTION_DEFINE; earlier devices must fail visibly instead
     * of advertising an action which cannot be handled by the platform contract.
     */
    fun intentFor(
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Intent? {
        if (sdkInt < Build.VERSION_CODES.Q) return null
        val text = quote.trim()
        if (text.isBlank() || text.length > MAX_SELECTION_CHARS || '\u0000' in text) return null

        return Intent(Intent.ACTION_TRANSLATE).apply {
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    /**
     * An injected launcher permits verifying error-handling without package-manager
     * visibility assumptions. A failed dispatch is reported to Reader chrome; no
     * implicit online service, browser or clipboard fallback is attempted.
     */
    fun dispatch(
        quote: String,
        sdkInt: Int = Build.VERSION.SDK_INT,
        launch: (Intent) -> Unit
    ): Boolean {
        val intent = intentFor(quote, sdkInt) ?: return false
        return try {
            launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun launch(activity: Activity?, quote: String): Boolean {
        val host = activity ?: return false
        return dispatch(quote) { intent -> host.startActivity(intent) }
    }
}
