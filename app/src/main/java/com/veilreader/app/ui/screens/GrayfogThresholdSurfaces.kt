package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Quiet Grayfog surface language for the experimental Reborn Home.
 *
 * Decorative ink lines are intentionally static and inaccessible. Layout and text
 * grow naturally at large font scales; no fixed heights or fake book metadata.
 * The approved app theme, including its high-contrast variants, owns all colors.
 */
@Composable
internal fun GrayfogThresholdHeader(
    brand: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                val accent = scheme.outline.copy(alpha = 0.24f)
                val center = Offset(size.width * .88f, size.height * .22f)
                val radius = size.minDimension * .42f
                drawCircle(accent, radius, center, style = Stroke(width = 1.dp.toPx()))
                drawCircle(accent.copy(alpha = .46f), radius * .78f, center, style = Stroke(width = 1.dp.toPx()))
                drawLine(
                    color = accent,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = brand,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Box(
                    Modifier
                        .width(44.dp)
                        .height(2.dp)
                        .background(scheme.primary)
                )
            }
        }
    }
}

@Composable
internal fun GrayfogSectionHeading(
    text: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(scheme.outlineVariant)
        )
    }
}
