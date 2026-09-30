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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
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
import com.veilreader.app.R
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.veilContentTextStyle

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

        Canvas(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
                .size(148.dp)
        ) {
            val stroke = Stroke(width = 1.dp.toPx())
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = 0.040f),
                radius = size.minDimension * 0.34f,
                center = center,
                style = stroke
            )
            drawLine(
                VeilPalette.Brass.copy(alpha = 0.045f),
                Offset(center.x, size.height * 0.08f),
                Offset(center.x, size.height * 0.92f),
                stroke.width
            )
            drawLine(
                VeilPalette.Brass.copy(alpha = 0.035f),
                Offset(size.width * 0.18f, center.y),
                Offset(size.width * 0.82f, center.y),
                stroke.width
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = 0.34f),
                            VeilPalette.Ink.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        content()
    }
}

internal fun tabSlideDirection(
    initialOrdinal: Int,
    targetOrdinal: Int,
    rtl: Boolean
): Int {
    val logicalDirection = if (targetOrdinal >= initialOrdinal) 1 else -1
    return if (rtl) -logicalDirection else logicalDirection
}

@Composable
fun VeilAnimatedTabHost(
    selectedTab: VeilTab,
    modifier: Modifier = Modifier,
    content: @Composable (VeilTab) -> Unit
) {
    val reducedMotion = LocalVeilReducedMotion.current
    val layoutDirection = LocalLayoutDirection.current

    AnimatedContent(
        targetState = selectedTab,
        modifier = modifier,
        transitionSpec = {
            if (reducedMotion) {
                (
                    fadeIn(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
                    ) togetherWith (
                    fadeOut(tween(VeilMotion.REDUCED_MOTION_FADE_MS))
                    ) using SizeTransform(clip = false)
            } else {
                val direction = tabSlideDirection(
                    initialOrdinal = initialState.ordinal,
                    targetOrdinal = targetState.ordinal,
                    rtl = layoutDirection == LayoutDirection.Rtl
                )
                (
                    fadeIn(tween(VeilMotion.STANDARD_MS, delayMillis = 12)) +
                        slideInHorizontally(tween(VeilMotion.STANDARD_MS)) { fullWidth ->
                            direction * (fullWidth / 34)
                        }
                    ) togetherWith (
                    fadeOut(tween(VeilMotion.QUICK_MS)) +
                        slideOutHorizontally(tween(VeilMotion.STANDARD_MS)) { fullWidth ->
                            -direction * (fullWidth / 42)
                        }
                    ) using SizeTransform(clip = false)
            }
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
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moreSelected =
        selected == VeilTab.CASTLE ||
            selected == VeilTab.PATH ||
            selected == VeilTab.PROFILE

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.995f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            GrayfogRule(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VeilPrimaryDockItem(
                    label = stringResource(R.string.nav_home),
                    selected = selected == VeilTab.READING,
                    onClick = { onSelect(VeilTab.READING) },
                    modifier = Modifier.weight(1f)
                ) { tint ->
                    VeilTabIcon(VeilTab.READING, tint = tint, modifier = Modifier.size(21.dp))
                }

                VeilPrimaryDockItem(
                    label = stringResource(R.string.nav_library),
                    selected = selected == VeilTab.LIBRARY,
                    onClick = { onSelect(VeilTab.LIBRARY) },
                    modifier = Modifier.weight(1f)
                ) { tint ->
                    VeilTabIcon(VeilTab.LIBRARY, tint = tint, modifier = Modifier.size(21.dp))
                }

                VeilPrimaryDockItem(
                    label = stringResource(R.string.nav_notebook),
                    selected = false,
                    onClick = onOpenArchive,
                    modifier = Modifier.weight(1f)
                ) { tint ->
                    VeilNotebookIcon(tint = tint, modifier = Modifier.size(21.dp))
                }

                VeilPrimaryDockItem(
                    label = stringResource(R.string.nav_more),
                    selected = moreSelected,
                    onClick = { onSelect(VeilTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                ) { tint ->
                    VeilMoreIcon(tint = tint, modifier = Modifier.size(21.dp))
                }
            }
        }
    }
}

@Composable
private fun VeilPrimaryDockItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit
) {
    val foreground = if (selected) {
        VeilPalette.Brass
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(2.dp))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(foreground)
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = veilContentTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.48.sp,
                    fontWeight = FontWeight.Medium
                ),
                label
            ),
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(18.dp)
                .height(1.dp)
                .background(
                    if (selected) VeilPalette.Brass.copy(alpha = 0.86f)
                    else Color.Transparent
                )
        )
    }
}

@Composable
private fun VeilNotebookIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val stroke = Stroke(
            width = 1.45.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val w = size.width
        val h = size.height
        val left = Path().apply {
            moveTo(w * 0.12f, h * 0.22f)
            quadraticTo(w * 0.32f, h * 0.17f, w * 0.50f, h * 0.31f)
            lineTo(w * 0.50f, h * 0.82f)
            quadraticTo(w * 0.31f, h * 0.68f, w * 0.12f, h * 0.73f)
            close()
        }
        val right = Path().apply {
            moveTo(w * 0.88f, h * 0.22f)
            quadraticTo(w * 0.68f, h * 0.17f, w * 0.50f, h * 0.31f)
            lineTo(w * 0.50f, h * 0.82f)
            quadraticTo(w * 0.69f, h * 0.68f, w * 0.88f, h * 0.73f)
            close()
        }
        drawPath(left, tint, style = stroke)
        drawPath(right, tint, style = stroke)
        drawLine(
            tint.copy(alpha = 0.72f),
            Offset(w * 0.50f, h * 0.31f),
            Offset(w * 0.50f, h * 0.82f),
            stroke.width
        )
    }
}

@Composable
private fun VeilMoreIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.075f
        val y = size.height * 0.50f
        drawCircle(tint, radius, Offset(size.width * 0.24f, y))
        drawCircle(tint, radius, Offset(size.width * 0.50f, y))
        drawCircle(tint, radius, Offset(size.width * 0.76f, y))
    }
}

@Composable
private fun veilTabLabel(tab: VeilTab): String =
    when (tab) {
        VeilTab.READING -> stringResource(R.string.nav_reading)
        VeilTab.LIBRARY -> stringResource(R.string.nav_library)
        VeilTab.CASTLE -> stringResource(R.string.nav_castle)
        VeilTab.PATH -> stringResource(R.string.nav_path)
        VeilTab.PROFILE -> stringResource(R.string.nav_profile)
    }
@Composable
private fun VeilDockItem(
    tab: VeilTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = Color.Transparent
    val foreground = if (selected) {
        VeilPalette.Brass
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val tabLabel = veilTabLabel(tab)

    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VeilTabIcon(tab, tint = foreground, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            tabLabel,
            style = veilContentTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.48.sp,
                    fontWeight = FontWeight.Medium
                ),
                tabLabel
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
                .width(18.dp)
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
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.975f),
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            VeilPalette.Brass.copy(alpha = 0.22f)
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
                VeilTab.entries.forEach { tab ->
                    val isSelected = selected == tab
                    val tabLabel = veilTabLabel(tab)
                    val foreground = if (isSelected) {
                        VeilPalette.Brass
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth().heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Transparent)
                            .selectable(selected = isSelected, role = Role.Tab) { onSelect(tab) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VeilTabIcon(tab, foreground, Modifier.size(22.dp))
                        Spacer(Modifier.height(5.dp))
                        Text(
                            tabLabel,
                            style = veilContentTextStyle(
                                MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.36.sp
                                ),
                                tabLabel
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
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.44f)),
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        VeilSigilMark(
            modifier = Modifier.size(34.dp),
            tint = VeilPalette.Brass
        )
    }
}

@Composable
fun VeilSigilMark(
    modifier: Modifier = Modifier,
    tint: Color = VeilPalette.Brass
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = 1.35.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Open book.
        val leftPage = Path().apply {
            moveTo(w * 0.14f, h * 0.55f)
            quadraticTo(w * 0.31f, h * 0.48f, w * 0.50f, h * 0.61f)
            lineTo(w * 0.50f, h * 0.84f)
            quadraticTo(w * 0.31f, h * 0.72f, w * 0.14f, h * 0.77f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(w * 0.86f, h * 0.55f)
            quadraticTo(w * 0.69f, h * 0.48f, w * 0.50f, h * 0.61f)
            lineTo(w * 0.50f, h * 0.84f)
            quadraticTo(w * 0.69f, h * 0.72f, w * 0.86f, h * 0.77f)
            close()
        }
        drawPath(leftPage, tint.copy(alpha = 0.90f), style = stroke)
        drawPath(rightPage, tint.copy(alpha = 0.90f), style = stroke)

        // Eight-point threshold star.
        val cx = w * 0.50f
        val cy = h * 0.29f
        val outer = size.minDimension * 0.15f
        val inner = outer * 0.38f
        val star = Path()
        repeat(16) { index ->
            val radius = if (index % 2 == 0) outer else inner
            val angle = Math.toRadians(-90.0 + index * 22.5)
            val x = cx + kotlin.math.cos(angle).toFloat() * radius
            val y = cy + kotlin.math.sin(angle).toFloat() * radius
            if (index == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, tint.copy(alpha = 0.94f), style = stroke)
        drawCircle(tint, radius = 1.1.dp.toPx(), center = Offset(cx, cy))
    }
}

@Composable
fun VeilLoadingState(
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val displayLabel = label ?: stringResource(R.string.common_opening_archive)
    val brandLabel = stringResource(R.string.library_brand_name)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VeilSigilMark(
            modifier = Modifier.size(58.dp),
            tint = VeilPalette.Brass
        )
        Spacer(Modifier.height(18.dp))
        Text(
            brandLabel,
            style = veilContentTextStyle(
                MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                brandLabel
            ),
            color = VeilPalette.Brass
        )
        Spacer(Modifier.height(6.dp))
        Text(
            displayLabel,
            style = veilContentTextStyle(MaterialTheme.typography.titleMedium, displayLabel),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .width(132.dp)
                .height(2.dp),
            color = VeilPalette.Brass,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
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
