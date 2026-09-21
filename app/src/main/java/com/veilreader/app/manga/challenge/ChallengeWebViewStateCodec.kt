package com.veilreader.app.manga.challenge

import android.os.Bundle
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

object ChallengeWebViewStateCodec {
    private const val MAX_STATE_BYTES = 256 * 1024

    fun capture(webView: WebView): Bundle? {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SAVE_STATE)) {
            return null
        }
        val out = Bundle()
        return runCatching {
            WebViewCompat.saveState(
                webView,
                out,
                MAX_STATE_BYTES,
                false
            )
            out.takeUnless { it.isEmpty }
        }.getOrNull()
    }

    fun restore(webView: WebView, state: Bundle?): Boolean {
        state ?: return false
        return runCatching {
            webView.restoreState(Bundle(state)) != null
        }.getOrDefault(false)
    }
}