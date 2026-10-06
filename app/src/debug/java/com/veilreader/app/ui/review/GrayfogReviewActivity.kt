package com.veilreader.app.ui.review

import android.content.res.Configuration
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.veilreader.app.R
import java.util.Locale

/** Debug-only capture entrance. It neither loads nor writes the user's archive or preferences. */
class GrayfogReviewActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SURFACE = "surface"
        const val EXTRA_LOCALE = "locale"
        const val EXTRA_FONT_SCALE = "fontScale"
        const val EXTRA_HIGH_CONTRAST = "highContrast"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val surface = runCatching {
            GrayfogReviewSurface.valueOf(
                intent.getStringExtra(EXTRA_SURFACE) ?: GrayfogReviewSurface.THRESHOLD_ACTIVE.name
            )
        }.getOrDefault(GrayfogReviewSurface.THRESHOLD_ACTIVE)
        val language = intent.getStringExtra(EXTRA_LOCALE) ?: "en"
        val scale = intent.getFloatExtra(EXTRA_FONT_SCALE, 1f)
            .let { if (it.isFinite()) it.coerceIn(1f, 2f) else 1f }
        val contrast = intent.getBooleanExtra(EXTRA_HIGH_CONTRAST, false)

        // Dialogs are separate platform windows. A CompositionLocal-only density override on the
        // Activity's content does not reliably reach those windows, so the review root itself must
        // own a real configuration context. DialogWrapper inherits this ComposeView context.
        val reviewConfiguration = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            fontScale = scale
        }
        val reviewContext = ContextThemeWrapper(this, R.style.Theme_VeilReader).apply {
            applyOverrideConfiguration(reviewConfiguration)
        }
        val reviewRoot = ComposeView(reviewContext)
        setContentView(reviewRoot)

        reviewRoot.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                com.veilreader.app.ui.VeilSystemBars(lightBackground = false)
                GrayfogReviewContent(surface, contrast)
            }
        }
    }
}
