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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.ui.theme.VeilPalette

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
    val systemBrightnessLabel = stringResource(R.string.reader_system_brightness)
    val readingBrightnessLabel = stringResource(R.string.settings_brightness)
    val formatPercent = rememberVeilPercentFormatter()
    var draft by remember(customBrightness) {
        mutableFloatStateOf((customBrightness ?: 0.5).toFloat())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            stringResource(R.string.settings_brightness),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = VeilPalette.Brass
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    stringResource(R.string.reader_system_brightness),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    if (customBrightness == null) {
                        stringResource(R.string.reader_system_brightness_description)
                    } else {
                        stringResource(R.string.reader_custom_brightness_description)
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
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VeilPalette.Moon,
                    checkedTrackColor = VeilPalette.DeepBrass,
                    checkedBorderColor = VeilPalette.Brass,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = systemBrightnessLabel
                }
            )
        }

        if (customBrightness != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_brightness),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatPercent(draft),
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Brass
                )
            }

            Slider(
                value = draft,
                onValueChange = {
                    draft = it
                    onChange(appearance.withScreenBrightness(it.toDouble()))
                },
                valueRange = 0.05f..1f,
                modifier = Modifier.semantics {
                    contentDescription = readingBrightnessLabel
                }
            )
        }
    }
}
