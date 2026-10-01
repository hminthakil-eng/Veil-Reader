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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.ui.navigation.VeilTab
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
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
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) colors.primary else VeilPalette.Brass
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (highContrast) {
                        listOf(
                            colors.background,
                            colors.background,
                            colors.surfaceVariant,
                            colors.surface
                        )
                    } else {
                        listOf(
                            Color(0xFF080A0D),
                            colors.background,
                            Color(0xFF0D1116),
                            colors.surface.copy(alpha = 0.96f)
                        )
                    }
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val brass = accent.copy(alpha = if (highContrast) 0.24f else 0.075f)
            val stone = colors.outlineVariant.copy(alpha = if (highContrast) 0.30f else 0.10f)
            val stroke = (if (highContrast) 1.5.dp else 1.dp).toPx()

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
                            VeilPalette.Spirit.copy(alpha = if (highContrast) 0.045f else 0.11f),
                            VeilPalette.Spirit.copy(alpha = if (highContrast) 0.012f else 0.025f),
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
                            Color(0xFFD18B3E).copy(alpha = if (highContrast) 0.035f else 0.10f),
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
                            accent.copy(alpha = if (highContrast) 0.035f else 0.08f),
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
                color = accent.copy(alpha = if (highContrast) 0.12f else 0.040f),
                radius = size.minDimension * 0.34f,
                center = center,
                style = stroke
            )
            drawLine(
                accent.copy(alpha = if (highContrast) 0.13f else 0.045f),
                Offset(center.x, size.height * 0.08f),
                Offset(center.x, size.height * 0.92f),
                stroke.width
            )
            drawLine(
                accent.copy(alpha = if (highContrast) 0.10f else 0.035f),
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
                            colors.background.copy(alpha = 0.34f),
                            colors.background.copy(alpha = 0.72f)
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
    modifier: Modifier = Modifier
) {
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
    val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 3.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        VeilPalette.Iron.copy(alpha = 0.98f),
                        VeilPalette.Archive.copy(alpha = 0.995f),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(
                    if (highContrast) 1.5.dp else 1.dp,
                    accent.copy(alpha = if (highContrast) 0.76f else 0.34f)
                ),
                shape
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.32f),
                            accent.copy(alpha = 0.76f),
                            accent.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
    val foreground = if (selected) {
        VeilPalette.Moon
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
    }

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.12f),
                            accent.copy(alpha = 0.035f),
                            Color.Transparent
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp)
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(28.dp)
                    .height(1.dp)
                    .background(accent.copy(alpha = if (highContrast) 1f else 0.88f))
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VeilTabIcon(
                tab,
                tint = if (selected) accent else foreground,
                modifier = Modifier.size(if (selected) 22.dp else 20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                veilTabLabel(tab),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 9.2.sp,
                    letterSpacing = 0.34.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = foreground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VeilNavigationRail(
    selected: VeilTab,
    onSelect: (VeilTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.975f),
        shadowElevation = 0.dp,
        border = BorderStroke(
            if (highContrast) 1.5.dp else 1.dp,
            accent.copy(alpha = if (highContrast) 0.72f else 0.22f)
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
                    val foreground = if (isSelected) {
                        accent
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
                            veilTabLabel(tab),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 9.5.sp,
                                letterSpacing = 0.36.sp
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
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
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
                BorderStroke(
                    if (highContrast) 1.5.dp else 1.dp,
                    accent.copy(alpha = if (highContrast) 0.82f else 0.44f)
                ),
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        VeilSigilMark(
            modifier = Modifier.size(34.dp),
            tint = accent
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
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
    val displayLabel = label ?: stringResource(R.string.notice_loading_open)
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VeilSigilMark(
            modifier = Modifier.size(58.dp),
            tint = accent
        )
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Spacer(Modifier.height(6.dp))
        Text(
            displayLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .width(132.dp)
                .height(2.dp),
            color = accent,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = if (highContrast) 0.52f else 0.24f
            )
        )
    }
}

@Composable
private fun GrayfogRule(modifier: Modifier = Modifier) {
    val highContrast = LocalVeilHighContrast.current
    val accent = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
    Box(
        modifier = modifier
            .height(if (highContrast) 2.dp else 1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        accent.copy(alpha = if (highContrast) 0.88f else 0.58f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun veilTabLabel(tab: VeilTab): String =
    stringResource(
        when (tab) {
            VeilTab.READING -> R.string.nav_reading
            VeilTab.LIBRARY -> R.string.nav_library
            VeilTab.CASTLE -> R.string.nav_castle
            VeilTab.PATH -> R.string.nav_path
            VeilTab.PROFILE -> R.string.nav_profile
        }
    )

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

