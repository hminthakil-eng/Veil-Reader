package com.veilreader.app.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Lightweight visual fixture for reviewing Veil identity decisions without touching production
 * screens. Keep this intentionally small; it is a design-system lab, not a demo app.
 */
@Composable
private fun VeilThemeLabContent() {
    Surface {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "THE LIBRARY BEYOND THE VEIL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "A quiet archive of impossible worlds.",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Archive speaks. Threshold reveals. Sanctuary falls silent.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(VeilRadius.Control),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ARCHIVE", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Catalogued, precise, mysterious.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(VeilRadius.Control),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("SANCTUARY", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Content first. Identity almost disappears.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Preview(name = "Veil Identity — Light", showBackground = true, widthDp = 412)
@Composable
private fun VeilThemeLabLightPreview() {
    VeilTheme { VeilThemeLabContent() }
}

@Preview(
    name = "Veil Identity — Dark",
    showBackground = true,
    widthDp = 412,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun VeilThemeLabDarkPreview() {
    VeilTheme { VeilThemeLabContent() }
}

@Preview(name = "Veil Identity — RTL", showBackground = true, widthDp = 412)
@Composable
private fun VeilThemeLabRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        VeilTheme { VeilThemeLabContent() }
    }
}
