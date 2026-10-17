package com.veilreader.app.ui.station

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.LocalVeilReducedMotion

/**
 * Input-responsive, layered station sign. This is a Compose 2.5D fallback:
 * perspective is applied to the plaque material only, never to reading text.
 * Real glTF geometry is a separately-gated renderer described in ADR-021.
 *
 * The banner routes through the existing library callback. There is no
 * simulated clock, fake train timetable, navigation repository or gesture layer
 * intercepting the surrounding book list.
 */
@Composable
internal fun VictorianStationHeader(
    brand: String,
    title: String,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = LocalVeilReducedMotion.current
    val highContrast = LocalVeilHighContrast.current
    val mode = stationRenderMode(
        capabilities = StationRendererCapabilities(
            featureEnabled = true,
            reducedMotion = reducedMotion,
            highContrast = highContrast
        ),
        surface = StationSurface.THRESHOLD
    )
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressTilt by animateFloatAsState(
        targetValue = if (isPressed && mode != StationRendererMode.STATIC) 4f else 0f,
        animationSpec = tween(durationMillis = stationInputMotionMillis(mode)),
        label = "station-plaque-tilt"
    )

    val iron = scheme.surface
    val brass = scheme.primary
    val ink = scheme.onSurface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(iron)
            .heightIn(min = 248.dp)
    ) {
        if (!highContrast) {
            Image(
                painter = painterResource(R.drawable.grayfog_threshold_v1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.42f,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            iron.copy(alpha = 0.52f),
                            iron.copy(alpha = 0.64f),
                            iron.copy(alpha = 0.90f)
                        )
                    )
                )
            )
        }
        // Lightweight glass roof, hanging chains and station vanishing lines.
        // Decorative paths are static and do not participate in hit testing.
        Canvas(Modifier.matchParentSize()) {
            val line = brass.copy(alpha = if (highContrast) .5f else .18f)
            val center = size.width * .5f
            for (index in 0..5) {
                val x = size.width * index / 5f
                drawLine(
                    color = line,
                    start = Offset(x, 0f),
                    end = Offset(center, size.height * .60f),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
            for (index in 0..3) {
                val y = size.height * (.07f + index * .15f)
                drawLine(line, Offset(0f, y), Offset(size.width, y), .65.dp.toPx())
            }
            val leftChain = size.width * .15f
            val rightChain = size.width * .85f
            drawLine(line, Offset(leftChain, 0f), Offset(leftChain, size.height * .25f), 1.dp.toPx())
            drawLine(line, Offset(rightChain, 0f), Offset(rightChain, size.height * .25f), 1.dp.toPx())
            drawCircle(
                color = line,
                radius = size.minDimension * .09f,
                center = Offset(center, size.height * .15f),
                style = Stroke(1.dp.toPx())
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(PaddingValues(20.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // The visual plane and text are separate layers so large text never
            // suffers perspective warping or inaccessible clipping.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onOpenLibrary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .graphicsLayer {
                            rotationY = pressTilt
                            shadowElevation = 9.dp.toPx()
                            cameraDistance = 20f * density
                        }
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(scheme.surfaceVariant, scheme.surface)
                            )
                        )
                )
                // Accessible, flat (unwarped) text and affordance.
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.headlineLarge,
                        color = ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.reborn_open_library),
                        style = MaterialTheme.typography.labelMedium,
                        color = brass
                    )
                }
            }
            Text(
                text = title,
                color = ink,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
