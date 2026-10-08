package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.ui.theme.ReaderVisualGeometry
import com.veilreader.app.ui.theme.ReaderVisualOpacity
import com.veilreader.app.ui.theme.VeilPalette

/** Presentation-only actions used by Reader chrome. */
internal enum class ReaderAction { BACK, NOTEBOOK, BOOKMARK, FOCUS, APPEARANCE, ZOOM }

@Composable
internal fun ReaderChromeButton(
    action: ReaderAction,
    accessibilityLabel: String,
    tint: Color = VeilPalette.Brass,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(ReaderVisualGeometry.TouchTarget)
            .semantics { contentDescription = accessibilityLabel }
    ) {
        ReaderActionIcon(
            action = action,
            modifier = Modifier.size(ReaderVisualGeometry.ChromeIcon),
            tint = tint
        )
    }
}

@Composable
internal fun ReaderControl(
    action: ReaderAction,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = VeilPalette.Brass,
    foreground: Color = VeilPalette.Moon,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(
            minWidth = 0.dp,
            minHeight = ReaderVisualGeometry.ReaderControlMinHeight
        ),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = foreground,
            disabledContentColor = foreground.copy(alpha = ReaderVisualOpacity.Disabled)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ReaderActionIcon(
                action = action,
                modifier = Modifier.size(
                    if (action == ReaderAction.APPEARANCE) {
                        ReaderVisualGeometry.AppearanceGlyph
                    } else {
                        ReaderVisualGeometry.StandardToolIcon
                    }
                ),
                tint = if (enabled) accent
                else foreground.copy(alpha = ReaderVisualOpacity.Disabled)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    foreground.copy(alpha = ReaderVisualOpacity.EnabledSecondary)
                } else {
                    foreground.copy(alpha = ReaderVisualOpacity.Disabled)
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReaderActionIcon(action: ReaderAction, modifier: Modifier, tint: Color) {
    if (action == ReaderAction.APPEARANCE) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Aa", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = tint)
        }
        return
    }

    Canvas(modifier) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        when (action) {
            ReaderAction.BACK -> {
                val tipX = if (layoutDirection == LayoutDirection.Rtl) w * .66f else w * .34f
                val tailX = if (layoutDirection == LayoutDirection.Rtl) w * .28f else w * .72f
                drawLine(tint, Offset(tailX, h * .20f), Offset(tipX, h * .50f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(tipX, h * .50f), Offset(tailX, h * .80f), stroke.width, StrokeCap.Round)
            }
            ReaderAction.NOTEBOOK -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .18f, h * .14f),
                    size = Size(w * .64f, h * .72f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(tint, Offset(w * .34f, h * .34f), Offset(w * .68f, h * .34f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .34f, h * .50f), Offset(w * .68f, h * .50f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .34f, h * .66f), Offset(w * .58f, h * .66f), stroke.width, StrokeCap.Round)
            }
            ReaderAction.BOOKMARK -> {
                val path = Path().apply {
                    moveTo(w * .28f, h * .12f)
                    lineTo(w * .72f, h * .12f)
                    lineTo(w * .72f, h * .86f)
                    lineTo(w * .50f, h * .69f)
                    lineTo(w * .28f, h * .86f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            ReaderAction.FOCUS -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .14f, h * .22f),
                    size = Size(w * .72f, h * .56f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = stroke
                )
                drawLine(tint, Offset(w * .18f, h * .50f), Offset(w * .82f, h * .50f), stroke.width, StrokeCap.Round)
            }
            ReaderAction.APPEARANCE -> Unit
            ReaderAction.ZOOM -> {
                drawCircle(
                    color = tint,
                    radius = w * .22f,
                    center = Offset(w * .43f, h * .40f),
                    style = stroke
                )
                drawLine(tint, Offset(w * .58f, h * .56f), Offset(w * .80f, h * .80f), stroke.width, StrokeCap.Round)
            }
        }
    }
}
