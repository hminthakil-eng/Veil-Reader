package com.veilreader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.WorldMutationLedger
import com.veilreader.app.domain.WorldMutationRealm
import com.veilreader.app.ui.theme.VeilPalette

/**
 * Shared presentation for the same mutation ledger across Castle chambers.
 * Data ownership stays in the domain ledger; realms only choose which entries to reveal.
 */
@Composable
internal fun WorldMutationEcho(
    ledger: WorldMutationLedger,
    realm: WorldMutationRealm,
    modifier: Modifier = Modifier,
    durableOnly: Boolean = false,
    limit: Int = 3,
    eyebrow: String = "WORLD CONSEQUENCE",
    title: String = "Reading mutations"
) {
    val entries = ledger.forRealm(realm)
        .asSequence()
        .filter { !durableOnly || it.durable }
        .take(limit.coerceAtLeast(0))
        .toList()
    if (entries.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Brass.copy(alpha = 0.82f)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = VeilPalette.Moon
        )

        entries.forEach { mutation ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(if (mutation.durable) 5.dp else 4.dp)
                        .background(
                            if (mutation.durable) VeilPalette.Brass else VeilPalette.Spirit,
                            CircleShape
                        )
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        mutation.titleFor(realm),
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Moon
                    )
                    Text(
                        mutation.inscriptionFor(realm),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist.copy(alpha = 0.72f)
                    )
                }
                Text(
                    mutation.evidenceCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.62f)
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VeilPalette.Brass.copy(alpha = 0.16f))
        )
    }
}
