package com.veilreader.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.theme.VeilPalette
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
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF080A0D),
                        colors.background,
                        Color(0xFF0D1116),
                        colors.surface.copy(alpha = 0.96f)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val brass = VeilPalette.Brass.copy(alpha = 0.075f)
            val stone = colors.outlineVariant.copy(alpha = 0.10f)
            val stroke = 1.dp.toPx()

            // Tall archive pillars.
            drawLine(stone, Offset(w * 0.08f, 0f), Offset(w * 0.08f, h), stroke)
            drawLine(stone, Offset(w * 0.16f, 0f), Offset(w * 0.16f, h), stroke)
            drawLine(stone, Offset(w * 0.84f, 0f), Offset(w * 0.84f, h), stroke)
            drawLine(stone, Offset(w * 0.92f, 0f), Offset(w * 0.92f, h), stroke)

            // Gothic arch silhouette.
            drawArc(
                color = brass,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.14f, -h * 0.10f),
                size = Size(w * 0.72f, h * 0.52f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawArc(
                color = brass.copy(alpha = 0.045f),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.20f, -h * 0.035f),
                size = Size(w * 0.60f, h * 0.40f),
                style = Stroke(width = stroke)
            )

            // Shelves recede into the lower hall.
            listOf(0.64f, 0.73f, 0.82f, 0.91f).forEach { y ->
                drawLine(
                    colors.outlineVariant.copy(alpha = 0.065f),
                    Offset(0f, h * y),
                    Offset(w, h * y),
                    stroke
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-130).dp)
                .size(360.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            VeilPalette.Spirit.copy(alpha = 0.11f),
                            VeilPalette.Spirit.copy(alpha = 0.025f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-130).dp, y = 40.dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFD18B3E).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 130.dp, y = 150.dp)
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            VeilPalette.Brass.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        content()
    }
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
                fadeIn(tween(190, delayMillis = 20)) +
                    slideInHorizontally(tween(220)) { fullWidth ->
                        direction * (fullWidth / 24)
                    }
                ) togetherWith (
                fadeOut(tween(120)) +
                    slideOutHorizontally(tween(180)) { fullWidth ->
                        -direction * (fullWidth / 30)
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
    modifier: Modifier = Modifier
) {
    val primarySelected = if (selected.primary) selected else VeilTab.PROFILE

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.30f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            GrayfogRule(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VeilTab.entries.filter { it.primary }.forEach { tab ->
                    VeilDockItem(
                        tab = tab,
                        selected = primarySelected == tab,
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
    val background = if (selected) {
        VeilPalette.DeepBrass.copy(alpha = 0.48f)
    } else {
        Color.Transparent
    }
    val foreground = if (selected) {
        VeilPalette.Brass
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VeilTabIcon(tab, tint = foreground, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                letterSpacing = 0.25.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        // Reserve the marker space in both states so selection does not move the icon.
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(28.dp)
                .height(1.dp)
                .background(if (selected) VeilPalette.Brass.copy(alpha = 0.86f) else Color.Transparent)
        )
    }
}

@Composable
fun VeilNavigationRail(
    selected: VeilTab,
    onSelect: (VeilTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val primarySelected = if (selected.primary) selected else VeilTab.PROFILE

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.30f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = VeilSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VeilBrandMark()
            Spacer(Modifier.height(VeilSpacing.xl))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                VeilTab.entries.filter { it.primary }.forEach { tab ->
                    val isSelected = primarySelected == tab
                    val foreground = if (isSelected) {
                        VeilPalette.Brass
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth().heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    VeilPalette.DeepBrass.copy(alpha = 0.48f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .selectable(selected = isSelected, role = Role.Tab) { onSelect(tab) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VeilTabIcon(tab, foreground, Modifier.size(24.dp))
                        Spacer(Modifier.height(5.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.1.sp
                            ),
                            color = foreground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VeilBrandMark() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.48f)),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "V",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = VeilPalette.Brass
        )
    }
}

@Composable
private fun GrayfogRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        VeilPalette.Brass.copy(alpha = 0.58f),
                        Color.Transparent
                    )
                )
            )
    )
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

            VeilTab.ARCHIVE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.20f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.62f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    tint,
                    Offset(w * 0.22f, h * 0.38f),
                    Offset(w * 0.78f, h * 0.38f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * 0.28f, h * 0.54f),
                    Offset(w * 0.72f, h * 0.54f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * 0.28f, h * 0.67f),
                    Offset(w * 0.62f, h * 0.67f),
                    stroke.width,
                    StrokeCap.Round
                )
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

