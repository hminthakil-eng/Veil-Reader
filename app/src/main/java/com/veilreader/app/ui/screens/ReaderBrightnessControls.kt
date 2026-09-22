package com.veilreader.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.ReaderAppearance

@Composable
internal fun ReaderBrightnessEffect(
    activity: Activity,
    screenBrightness: Double?
) {
    val originalBrightness = remember(activity) {
        activity.window.attributes.screenBrightness
    }

    LaunchedEffect(activity, screenBrightness) {
        applyReaderBrightness(activity, screenBrightness)
    }

    DisposableEffect(activity) {
        onDispose {
            val attributes = activity.window.attributes
            attributes.screenBrightness = originalBrightness
            activity.window.attributes = attributes
        }
    }
}

internal fun applyReaderBrightness(activity: Activity, value: Double?) {
    val attributes = activity.window.attributes
    attributes.screenBrightness = normalizedReaderBrightness(value)
        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    activity.window.attributes = attributes
}

internal fun normalizedReaderBrightness(value: Double?): Float? =
    value
        ?.takeIf { it.isFinite() }
        ?.coerceIn(0.05, 1.0)
        ?.toFloat()

@Composable
internal fun ReaderBrightnessControls(
    appearance: ReaderAppearance,
    onChange: (ReaderAppearance) -> Unit,
    modifier: Modifier = Modifier
) {
    val customBrightness = appearance.screenBrightness
    var draft by remember(customBrightness) {
        mutableFloatStateOf((customBrightness ?: 0.5).toFloat())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Use system brightness", fontWeight = FontWeight.SemiBold)
                Text(
                    if (customBrightness == null) {
                        "Reader follows the device brightness."
                    } else {
                        "Reader uses its own brightness and restores the previous level when you leave."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = customBrightness == null,
                onCheckedChange = { useSystem ->
                    onChange(
                        appearance.withScreenBrightness(
                            if (useSystem) null else draft.toDouble()
                        )
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = "Use system brightness"
                }
            )
        }

        if (customBrightness != null) {
            Text(
                "Reading brightness · ${(draft * 100).toInt()}%",
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = draft,
                onValueChange = {
                    draft = it
                    onChange(appearance.withScreenBrightness(it.toDouble()))
                },
                valueRange = 0.05f..1f,
                modifier = Modifier.semantics {
                    contentDescription = "Reading brightness"
                }
            )
        }
    }
}
