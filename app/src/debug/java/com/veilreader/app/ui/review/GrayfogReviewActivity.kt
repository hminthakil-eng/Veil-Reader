package com.veilreader.app.ui.review

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/** Debug-only capture entrance. It neither loads nor writes the user's archive or preferences. */
class GrayfogReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val surface = runCatching { GrayfogReviewSurface.valueOf(intent.getStringExtra("surface") ?: "THRESHOLD_ACTIVE") }
            .getOrDefault(GrayfogReviewSurface.THRESHOLD_ACTIVE)
        val language = intent.getStringExtra("locale") ?: "en"
        val scale = intent.getFloatExtra("fontScale", 1f).let { if (it.isFinite()) it.coerceIn(1f, 2f) else 1f }
        val config = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            fontScale = scale
        }
        val localized = createConfigurationContext(config)
        val contrast = intent.getBooleanExtra("highContrast", false)
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localized, LocalResources provides localized.resources,
                LocalConfiguration provides config, LocalDensity provides Density(density.density, scale),
                LocalLayoutDirection provides if (config.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL)
                    LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    com.veilreader.app.ui.VeilSystemBars(lightBackground = false)
                    GrayfogReviewContent(surface, contrast)
                }
            }
        }
    }
}
