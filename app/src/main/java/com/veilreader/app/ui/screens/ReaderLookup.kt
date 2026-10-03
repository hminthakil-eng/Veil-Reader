package com.veilreader.app.ui.screens

import android.app.Activity
import android.content.Intent

internal fun readerLookupIntent(text: String): Intent? {
    val cleaned = text.trim()
    if (cleaned.isEmpty()) return null

    return Intent(Intent.ACTION_PROCESS_TEXT)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_PROCESS_TEXT, cleaned)
        .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
}

internal fun launchReaderLookup(
    activity: Activity,
    text: String,
    chooserTitle: String
): Boolean {
    val intent = readerLookupIntent(text) ?: return false
    val chooser = Intent.createChooser(intent, chooserTitle)
    return runCatching {
        activity.startActivity(chooser)
    }.isSuccess
}
