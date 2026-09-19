package com.veilreader.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * Calm world chrome for everything around the actual publication.
 *
 * Motion is intentionally reserved for navigation and state changes. There is no perpetual moving
 * background competing with reading, battery life or accessibility settings.
 */
@Composable
fun VeilWorldBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = content
    )
}

@Composable
fun VeilAnimatedTabHost(
    selectedTab: VeilTab,
    modifier: Modifier = Modifier,
    content: @Composable (VeilTab) -> Unit
) {
    AnimatedContent(
        targetState = selectedTab,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
            (
                fadeIn(tween(VeilMotion.STANDARD_MS, delayMillis = 35)) +
                    slideInHorizontally(tween(VeilMotion.STANDARD_MS)) { fullWidth ->
                        direction * (fullWidth / 14)
                    }
                ) togetherWith (
                fadeOut(tween(VeilMotion.QUICK_MS)) +
                    slideOutHorizontally(tween(VeilMotion.STANDARD_MS)) { fullWidth ->
                        -direction * (fullWidth / 20)
                    }
                ) using SizeTransform(clip = false)
        },
        label = "veil-tab"
    ) { tab ->
        content(tab)
    }
}

@Composable
fun VeilBottomDock(
    selected: VeilTab,
    onSelect: (VeilTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VeilTab.entries.forEach { tab ->
                VeilDockItem(
                    tab = tab,
                    selected = selected == tab,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(2.dp))
            VeilSettingsButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun VeilDockItem(
    tab: VeilTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val foreground = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            VeilTabIcon(tab, tint = foreground, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            veilTabLabel(tab),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.05.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = foreground,
            maxLines = 1
        )
    }
}

@Composable
fun VeilNavigationRail(
    selected: VeilTab,
    onSelect: (VeilTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(104.dp)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = VeilSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VeilBrandMark()
            Spacer(Modifier.height(VeilSpacing.xl))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                VeilTab.entries.forEach { tab ->
                    val isSelected = selected == tab
                    val foreground = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.80f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable(role = Role.Tab) { onSelect(tab) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VeilTabIcon(tab, foreground, Modifier.size(24.dp))
                        Spacer(Modifier.height(5.dp))
                        Text(
                            veilTabLabel(tab),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.1.sp
                            ),
                            color = foreground
                        )
                    }
                }
            }
            Spacer(Modifier.height(VeilSpacing.sm))
            VeilSettingsButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
fun VeilBrandMark(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            Canvas(Modifier.size(34.dp)) {
                val stroke = Stroke(
                    width = 2.15.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val leftPage = Path().apply {
                    moveTo(size.width * .12f, size.height * .22f)
                    quadraticTo(
                        size.width * .34f,
                        size.height * .12f,
                        size.width * .50f,
                        size.height * .34f
                    )
                    lineTo(size.width * .50f, size.height * .82f)
                    quadraticTo(
                        size.width * .31f,
                        size.height * .66f,
                        size.width * .12f,
                        size.height * .72f
                    )
                }
                val rightPage = Path().apply {
                    moveTo(size.width * .88f, size.height * .22f)
                    quadraticTo(
                        size.width * .66f,
                        size.height * .12f,
                        size.width * .50f,
                        size.height * .34f
                    )
                    lineTo(size.width * .50f, size.height * .82f)
                    quadraticTo(
                        size.width * .69f,
                        size.height * .66f,
                        size.width * .88f,
                        size.height * .72f
                    )
                }
                drawPath(leftPage, primary, style = stroke)
                drawPath(rightPage, primary, style = stroke)
                drawArc(
                    color = secondary,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(size.width * .31f, size.height * .18f),
                    size = androidx.compose.ui.geometry.Size(size.width * .38f, size.height * .38f),
                    style = Stroke(width = 1.55.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = secondary,
                    radius = 1.55.dp.toPx(),
                    center = Offset(size.width * .50f, size.height * .29f)
                )
            }
        }
        if (showWordmark) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "VEIL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp
                    )
                )
                Text(
                    "READER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun veilTabLabel(tab: VeilTab): String = stringResource(
    when (tab) {
        VeilTab.READING -> R.string.nav_reading
        VeilTab.LIBRARY -> R.string.nav_library
        VeilTab.CASTLE -> R.string.nav_castle
        VeilTab.PATH -> R.string.nav_path
        VeilTab.PROFILE -> R.string.nav_profile
    }
)

@Composable
private fun VeilSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val settingsLabel = stringResource(R.string.settings)
    Box(
        modifier = modifier
            .semantics { contentDescription = settingsLabel }
            .clip(RoundedCornerShape(19.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(23.dp)) {
            val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(
                color = tint,
                radius = size.minDimension * .20f,
                center = center,
                style = stroke
            )
            repeat(8) { index ->
                val angle = Math.toRadians(index * 45.0)
                val inner = size.minDimension * .34f
                val outer = size.minDimension * .46f
                val sx = center.x + kotlin.math.cos(angle).toFloat() * inner
                val sy = center.y + kotlin.math.sin(angle).toFloat() * inner
                val ex = center.x + kotlin.math.cos(angle).toFloat() * outer
                val ey = center.y + kotlin.math.sin(angle).toFloat() * outer
                drawLine(tint, Offset(sx, sy), Offset(ex, ey), stroke.width, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun VeilTabIcon(
    tab: VeilTab,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val stroke = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val w = size.width
        val h = size.height
        when (tab) {
            VeilTab.READING -> {
                val left = Path().apply {
                    moveTo(w * 0.10f, h * 0.22f)
                    quadraticTo(w * 0.32f, h * 0.14f, w * 0.50f, h * 0.30f)
                    lineTo(w * 0.50f, h * 0.82f)
                    quadraticTo(w * 0.30f, h * 0.66f, w * 0.10f, h * 0.72f)
                    close()
                }
                val right = Path().apply {
                    moveTo(w * 0.90f, h * 0.22f)
                    quadraticTo(w * 0.68f, h * 0.14f, w * 0.50f, h * 0.30f)
                    lineTo(w * 0.50f, h * 0.82f)
                    quadraticTo(w * 0.70f, h * 0.66f, w * 0.90f, h * 0.72f)
                    close()
                }
                drawPath(left, tint, style = stroke)
                drawPath(right, tint, style = stroke)
            }

            VeilTab.LIBRARY -> {
                val gap = w * 0.08f
                val bookW = w * 0.22f
                val top = h * 0.17f
                val bottom = h * 0.83f
                repeat(3) { index ->
                    val lift = if (index == 1) h * 0.05f else 0f
                    val left = w * 0.10f + index * (bookW + gap)
                    val bookHeight = (bottom - top) - lift
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top + lift),
                        size = androidx.compose.ui.geometry.Size(bookW, bookHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx()),
                        style = stroke
                    )
                }
            }

            VeilTab.CASTLE -> {
                val towerTop = h * 0.24f
                val baseTop = h * 0.46f
                drawLine(tint, Offset(w * 0.16f, baseTop), Offset(w * 0.84f, baseTop), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, baseTop), Offset(w * 0.22f, h * 0.84f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.78f, baseTop), Offset(w * 0.78f, h * 0.84f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, h * 0.84f), Offset(w * 0.78f, h * 0.84f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, towerTop), Offset(w * 0.22f, baseTop), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.78f, towerTop), Offset(w * 0.78f, baseTop), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.13f, towerTop), Offset(w * 0.31f, towerTop), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.69f, towerTop), Offset(w * 0.87f, towerTop), stroke.width, StrokeCap.Round)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.43f, h * 0.63f),
                    size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.21f),
                    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
                    style = stroke
                )
            }

            VeilTab.PATH -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.08f)
                    lineTo(w * 0.61f, h * 0.39f)
                    lineTo(w * 0.92f, h * 0.50f)
                    lineTo(w * 0.61f, h * 0.61f)
                    lineTo(w * 0.50f, h * 0.92f)
                    lineTo(w * 0.39f, h * 0.61f)
                    lineTo(w * 0.08f, h * 0.50f)
                    lineTo(w * 0.39f, h * 0.39f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawCircle(tint, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.5f))
            }

            VeilTab.PROFILE -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.20f,
                    center = Offset(w * 0.50f, h * 0.33f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.17f, h * 0.43f),
                    size = androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.49f),
                    style = stroke
                )
            }
        }
    }
}
