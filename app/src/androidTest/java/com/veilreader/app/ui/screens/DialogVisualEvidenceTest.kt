package com.veilreader.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.veilreader.app.exportGrayfogCapture
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.review.GrayfogReviewContent
import com.veilreader.app.ui.review.GrayfogReviewSurface
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilTheme
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Pixel evidence for dialog-backed Grayfog surfaces.
 *
 * These captures intentionally live on a real emulator rather than Robolectric
 * Native Graphics because a native dialog capture can SIGABRT the Gradle worker.
 */
@RunWith(Parameterized::class)
class DialogVisualEvidenceTest(
    private val language: String,
    private val scale: Float,
    private val highContrast: Boolean
) {
    @get:Rule val compose = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} text={1} contrast={2}")
        fun cases(): List<Array<Any>> =
            listOf("en", "fa").flatMap { language ->
                listOf(1f, 2f).flatMap { scale ->
                    listOf(false, true).map { contrast ->
                        arrayOf<Any>(language, scale, contrast)
                    }
                }
            }
    }

    private fun localizedContext(): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
                fontScale = scale
            }
        )
    }

    @Test
    fun dialogSurfacesHaveEmulatorBackedPixelEvidence() {
        val localized = localizedContext()
        val surface = mutableStateOf(GrayfogReviewSurface.BOOK_DETAIL)
        compose.setContent {
            val density = LocalDensity.current
            val activityResults = checkNotNull(LocalActivityResultRegistryOwner.current)
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides activityResults,
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalConfiguration provides localized.resources.configuration,
                LocalLayoutDirection provides if (language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density.density, scale)
            ) {
                VeilTheme(themeMode = AppThemeMode.DARK, highContrastEnabled = highContrast) {
                    CompositionLocalProvider(LocalVeilReducedMotion provides true) {
                        Box(
                            Modifier
                                .width(360.dp)
                                .height(800.dp)
                                .background(VeilPalette.Ink)
                        ) {
                            key(surface.value) {
                                GrayfogReviewContent(
                                    surface = surface.value,
                                    highContrast = highContrast
                                )
                            }
                        }
                    }
                }
            }
        }

        for (target in listOf(
            GrayfogReviewSurface.BOOK_DETAIL,
            GrayfogReviewSurface.RITUAL,
            GrayfogReviewSurface.ERROR
        )) {
            compose.runOnIdle { surface.value = target }
            compose.mainClock.advanceTimeBy(300)
            compose.waitForIdle()
            val image = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
            check(image.width > 0 && image.height > 0)
            val directory = File(
                ApplicationProvider.getApplicationContext<Context>().filesDir,
                "grayfog-review"
            ).apply { mkdirs() }
            val name =
                "dialog-${target.name.lowercase()}-$language-${(scale * 100).toInt()}-" +
                    "${if (highContrast) "contrast" else "standard"}.png"
            val file = File(directory, name)
            file.outputStream().use { output ->
                check(image.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            exportGrayfogCapture(file)
        }
    }
}
