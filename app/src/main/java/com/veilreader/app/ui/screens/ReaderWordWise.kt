package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import java.util.Locale

/**
 * Small, fully offline, opt-in vocabulary aid.
 *
 * Operates on the exact Readium user selection; never parses or rewrites publisher HTML,
 * reaches the network, stores selected text, or touches the semantic reading locator.
 * No made-up definitions are shown for an unknown word.
 */
internal data class ReaderWordWiseDefinition(
    val lemma: String,
    val meaningFa: String,
    val meaningEn: String
)

internal object ReaderWordWise {
    private val definitions: Map<String, ReaderWordWiseDefinition> = listOf(
        ReaderWordWiseDefinition("crimson", "سرخِ تیره یا خونین", "Deep red"),
        ReaderWordWiseDefinition("hallucination", "توهم؛ ادراک بدون محرک واقعی", "A perception without an external stimulus"),
        ReaderWordWiseDefinition("revolver", "هفت‌تیر؛ نوعی سلاح کمری با خشاب گردان", "A handgun with a rotating cylinder"),
        ReaderWordWiseDefinition("mysticism", "عرفان و تجربه‌های معنوی", "Spiritual or mystical beliefs and practices"),
        ReaderWordWiseDefinition("cogitation", "تفکر یا اندیشهٔ عمیق", "Deep thought"),
        ReaderWordWiseDefinition("divination", "فال‌گیری یا پیش‌گویی", "An attempt to foresee events through omens or rituals"),
        ReaderWordWiseDefinition("counterclockwise", "خلاف جهت حرکت عقربه‌های ساعت", "In the opposite direction to clock hands"),
        ReaderWordWiseDefinition("unfathomable", "درک‌ناپذیر؛ بسیار ژرف و پیچیده", "Impossible or very difficult to understand"),
        ReaderWordWiseDefinition("parchment", "پوست‌نوشته", "Prepared animal skin used as writing material"),
        ReaderWordWiseDefinition("marionette", "عروسک خیمه‌شب‌بازیِ نخ‌دار", "A puppet controlled by strings"),
        ReaderWordWiseDefinition("cathedral", "کلیسای جامع", "The principal church of a diocese"),
        ReaderWordWiseDefinition("incantation", "ورد یا عبارت آیینی", "Words chanted in a ritual or spell"),
        ReaderWordWiseDefinition("beyonder", "آن‌سوتر؛ فراتر از چیزی", "Something or someone beyond a boundary"),
        ReaderWordWiseDefinition("sefirah", "واژه‌ای برگرفته از اصطلاحات عرفانی سفیروت", "A term associated with the mystical sefirot")
    ).associateBy { it.lemma }

    /** A single English word only. Other scripts or passages fall back to system Lookup by choice. */
    fun find(selection: String): ReaderWordWiseDefinition? {
        val normalized = selection.trim()
            .trim { it == '"' || it == '\'' || it == '“' || it == '”' ||
                it == '(' || it == ')' || it == '!' || it == '?' ||
                it == '.' || it == ',' || it == ';' || it == ':' }
            .lowercase(Locale.ROOT)
        if (normalized.length !in 3..40 || normalized.any { it !in 'a'..'z' && it != '-' }) {
            return null
        }
        return definitions[normalized]
    }
}

@Composable
internal fun ReaderWordWiseDialog(
    selectedText: String,
    onDismiss: () -> Unit,
    onSystemLookup: (String) -> Unit
) {
    val definition = remember(selectedText) { ReaderWordWise.find(selectedText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.reader_word_wise_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = selectedText.take(120),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = definition?.meaningFa
                        ?: stringResource(R.string.reader_word_wise_unknown),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (definition != null) {
                    Text(
                        text = definition.meaningEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.reader_word_wise_offline_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reader_word_wise_close))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSystemLookup(selectedText) }) {
                Text(stringResource(R.string.reader_selection_lookup))
            }
        }
    )
}
