package com.veilreader.app.ui.reader.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Live publication stays still and accessible. One short opacity settle, no capture, translation,
 * curl, scale or reveal. The fine outside edge is materially distinct even with motion disabled. */
@Composable
internal fun MaterialReducedMotionSurface(
    config: MaterialTurnConfiguration, mirror: Boolean, serial: Int, edgeColor: Color,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(serial) {
        if (serial > 0) {
            alpha.snapTo(.24f)
            alpha.animateTo(0f, tween(70))
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val x = if (mirror) 1.dp.toPx() else size.width - 1.dp.toPx()
        drawLine(edgeColor.copy(alpha = .07f + alpha.value), Offset(x, 0f),
            Offset(x, size.height), config.material.thicknessDp.dp.toPx())
    }
}
