package com.veilreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.data.SampleData
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing

/**
 * The Castle is a living map, not a dashboard.
 * Every chamber still routes to an existing useful Veil Reader surface.
 */
@Composable
fun CastleScreen(
    profile: ReaderProfile,
    onAdvanceRank: () -> Unit,
    onOpenRoom: (String) -> Unit
) {
    val canAdvance = GamificationEngine.canAdvanceRank(profile)
    val awakenedRooms = SampleData.rooms.count { profile.rankIndex >= it.unlockRankIndex }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
    ) {
        ScreenHeader(
            eyebrow = "CASTLE · LIVING ARCHIVE",
            title = "The Keep Remembers",
            subtitle = "Every finished volume leaves a mark. Chambers awaken as your Path deepens."
        )

        CastleKeep(
            profile = profile,
            canAdvance = canAdvance,
            awakenedRooms = awakenedRooms,
            totalRooms = SampleData.rooms.size,
            onAdvanceRank = onAdvanceRank
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "THE INNER KEEP",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.55.sp),
                color = VeilPalette.Brass
            )
            Text(
                "Awakened Chambers",
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon
            )
            Text(
                "Follow the central stair. Open rooms are usable now; sealed rooms reveal the rank that awakens them.",
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist
            )
        }

        CastleWorldMap(
            rankIndex = profile.rankIndex,
            onOpenRoom = onOpenRoom
        )

        BrassRule(Modifier.fillMaxWidth())

        Text(
            "Nothing in the Castle is sold or time-gated. It grows from reading progress already stored on this device.",
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun CastleKeep(
    profile: ReaderProfile,
    canAdvance: Boolean,
    awakenedRooms: Int,
    totalRooms: Int,
    onAdvanceRank: () -> Unit
) {
    val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(1)
    val targetProgress = (profile.rankIndex.toFloat() / finalRank).coerceIn(0f, 1f)
    val castleProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(650),
        label = "castle-tier-progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17140F),
                        VeilPalette.Archive.copy(alpha = 0.98f),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                MaterialTheme.shapes.small
            )
    ) {
        CastleKeepBackdrop(
            modifier = Modifier.matchParentSize(),
            rankIndex = profile.rankIndex,
            rankCount = profile.path.ranks.size
        )

        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(VeilPalette.Ink.copy(alpha = 0.52f))
                        .border(
                            BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.48f)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CastleCrest(
                        modifier = Modifier.size(34.dp),
                        tint = VeilPalette.Brass
                    )
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "KEEP TIER ${profile.rankIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.30.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        profile.rankName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = VeilPalette.Moon
                    )
                    Text(
                        "${profile.path.name} · ${profile.booksFinished} finished ${if (profile.booksFinished == 1) "volume" else "volumes"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }

                Text(
                    "${profile.rankIndex + 1}/${profile.path.ranks.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = VeilPalette.Brass
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AWAKENED CHAMBERS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.20.sp),
                    color = VeilPalette.Mist.copy(alpha = 0.72f)
                )
                Text(
                    "$awakenedRooms/${totalRooms.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Brass
                )
            }

            LinearProgressIndicator(
                progress = { castleProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = VeilPalette.Brass,
                trackColor = VeilPalette.Moon.copy(alpha = 0.10f),
                drawStopIndicator = {}
            )

            if (canAdvance) {
                Button(
                    onClick = onAdvanceRank,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 42.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Perform advancement",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CastleKeepBackdrop(
    rankIndex: Int,
    rankCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val brass = VeilPalette.Brass
        val stone = VeilPalette.StrongBorderDark
        val baseY = h * 0.86f
        val towerBottom = h * 0.78f
        val glow = ((rankIndex + 1f) / rankCount.coerceAtLeast(1)).coerceIn(0f, 1f)

        drawRect(
            color = Color(0xFF07090C).copy(alpha = 0.54f),
            topLeft = Offset(w * 0.17f, h * 0.38f),
            size = Size(w * 0.66f, h * 0.44f)
        )
        drawRect(
            color = Color(0xFF07090C).copy(alpha = 0.66f),
            topLeft = Offset(w * 0.10f, h * 0.48f),
            size = Size(w * 0.16f, h * 0.34f)
        )
        drawRect(
            color = Color(0xFF07090C).copy(alpha = 0.66f),
            topLeft = Offset(w * 0.74f, h * 0.48f),
            size = Size(w * 0.16f, h * 0.34f)
        )

        val roof = Path().apply {
            moveTo(w * 0.30f, h * 0.38f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.70f, h * 0.38f)
        }
        drawPath(
            roof,
            color = brass.copy(alpha = 0.20f + glow * 0.12f),
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        listOf(0.18f, 0.34f, 0.50f, 0.66f, 0.82f).forEachIndexed { index, x ->
            drawLine(
                stone.copy(alpha = if (index == 2) 0.18f else 0.10f),
                Offset(w * x, h * 0.34f),
                Offset(w * x, towerBottom),
                1.dp.toPx()
            )
        }

        val windows = 7
        repeat(windows) { index ->
            val row = index / 3
            val col = index % 3
            val x = w * (0.38f + col * 0.12f)
            val y = h * (0.48f + row * 0.105f)
            val lit = index <= rankIndex
            drawRoundRect(
                color = if (lit) {
                    brass.copy(alpha = 0.15f + glow * 0.13f)
                } else {
                    stone.copy(alpha = 0.08f)
                },
                topLeft = Offset(x, y),
                size = Size(w * 0.035f, h * 0.050f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        drawLine(
            brass.copy(alpha = 0.18f),
            Offset(w * 0.08f, baseY),
            Offset(w * 0.92f, baseY),
            1.dp.toPx()
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    VeilPalette.Ink.copy(alpha = 0.56f)
                ),
                startY = h * 0.60f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.58f),
            size = Size(w, h * 0.42f)
        )
    }
}

@Composable
private fun CastleWorldMap(
    rankIndex: Int,
    onOpenRoom: (String) -> Unit
) {
    val rooms = SampleData.rooms
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090C10),
                        VeilPalette.Archive.copy(alpha = 0.98f),
                        Color(0xFF0B1016),
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.28f)),
                MaterialTheme.shapes.small
            )
    ) {
        CastleArchitectureBackdrop(
            modifier = Modifier.matchParentSize(),
            rankIndex = rankIndex,
            roomCount = rooms.size
        )

        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(tween(420)) + slideInVertically(tween(460)) { it / 18 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                CastleGateLabel(
                    title = "CROWN",
                    subtitle = "The upper halls"
                )

                rooms.forEachIndexed { index, room ->
                    VeilReveal(
                        delayMillis = 90 + index * 70,
                        distance = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CastleFloor(
                            floor = rooms.size - index,
                            id = room.id,
                            name = room.name,
                            purpose = room.purpose,
                            unlockRank = room.unlockRankIndex,
                            unlocked = rankIndex >= room.unlockRankIndex,
                            roomOnLeft = index % 2 == 0,
                            isLast = index == rooms.lastIndex,
                            onOpenRoom = onOpenRoom
                        )
                    }
                }

                CastleGateLabel(
                    title = "FOUNDATION",
                    subtitle = "The first stone remembers"
                )
            }
        }
    }
}

@Composable
private fun CastleArchitectureBackdrop(
    rankIndex: Int,
    roomCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f

        drawLine(
            VeilPalette.StrongBorderDark.copy(alpha = 0.18f),
            Offset(w * 0.08f, 0f),
            Offset(w * 0.17f, h),
            1.dp.toPx()
        )
        drawLine(
            VeilPalette.StrongBorderDark.copy(alpha = 0.18f),
            Offset(w * 0.92f, 0f),
            Offset(w * 0.83f, h),
            1.dp.toPx()
        )

        drawLine(
            VeilPalette.Brass.copy(alpha = 0.14f),
            Offset(centerX, 26.dp.toPx()),
            Offset(centerX, h - 26.dp.toPx()),
            1.dp.toPx()
        )

        repeat(3) { index ->
            val inset = 0.12f + index * 0.055f
            drawArc(
                color = VeilPalette.Brass.copy(alpha = 0.045f + index * 0.012f),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(w * inset, h * (0.03f + index * 0.045f)),
                size = Size(w * (1f - inset * 2f), h * (0.34f + index * 0.03f)),
                style = Stroke(1.dp.toPx())
            )
        }

        val floors = roomCount.coerceAtLeast(1)
        repeat(floors + 1) { index ->
            val y = h * ((index + 1f) / (floors + 2f))
            drawLine(
                VeilPalette.StrongBorderDark.copy(alpha = 0.09f),
                Offset(w * 0.12f, y),
                Offset(w * 0.88f, y),
                1.dp.toPx()
            )
        }

        drawArc(
            color = VeilPalette.Brass.copy(alpha = 0.14f),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, -h * 0.02f),
            size = Size(w * 0.60f, h * 0.24f),
            style = Stroke(1.1.dp.toPx())
        )

        val unlockedFraction =
            ((rankIndex + 1f) / roomCount.coerceAtLeast(1)).coerceIn(0f, 1f)
        val glowHeight = h * unlockedFraction * 0.42f
        drawLine(
            VeilPalette.Brass.copy(alpha = 0.34f),
            Offset(centerX, h - 34.dp.toPx()),
            Offset(centerX, h - 34.dp.toPx() - glowHeight),
            2.dp.toPx(),
            StrokeCap.Round
        )
    }
}

@Composable
private fun CastleFloor(
    floor: Int,
    id: String,
    name: String,
    purpose: String,
    unlockRank: Int,
    unlocked: Boolean,
    roomOnLeft: Boolean,
    isLast: Boolean,
    onOpenRoom: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (unlocked) 18.dp else 14.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) VeilPalette.Brass
                        else VeilPalette.BorderDark
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (unlocked) VeilPalette.Moon.copy(alpha = 0.42f)
                            else VeilPalette.StrongBorderDark.copy(alpha = 0.54f)
                        ),
                        CircleShape
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (roomOnLeft) {
                    CastleChamberNode(
                        id = id,
                        name = name,
                        purpose = purpose,
                        unlockRank = unlockRank,
                        unlocked = unlocked,
                        onOpenRoom = onOpenRoom,
                        modifier = Modifier.weight(1f)
                    )
                    CastleBridge(unlocked = unlocked)
                    FloorInscription(
                        floor = floor,
                        unlocked = unlocked,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    FloorInscription(
                        floor = floor,
                        unlocked = unlocked,
                        modifier = Modifier.weight(1f)
                    )
                    CastleBridge(unlocked = unlocked)
                    CastleChamberNode(
                        id = id,
                        name = name,
                        purpose = purpose,
                        unlockRank = unlockRank,
                        unlocked = unlocked,
                        onOpenRoom = onOpenRoom,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (!isLast) {
            Box(
                Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(
                        if (unlocked) {
                            VeilPalette.Brass.copy(alpha = 0.24f)
                        } else {
                            VeilPalette.BorderDark.copy(alpha = 0.62f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun CastleBridge(unlocked: Boolean) {
    Box(
        Modifier
            .width(20.dp)
            .height(1.dp)
            .background(
                if (unlocked) VeilPalette.Brass.copy(alpha = 0.42f)
                else VeilPalette.BorderDark.copy(alpha = 0.62f)
            )
    )
}

@Composable
private fun FloorInscription(
    floor: Int,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "FLOOR ${floor.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = if (unlocked) {
                VeilPalette.Brass.copy(alpha = 0.78f)
            } else {
                VeilPalette.Mist.copy(alpha = 0.50f)
            }
        )
        Text(
            if (unlocked) "AWAKENED" else "SILENT",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) {
                VeilPalette.Moon.copy(alpha = 0.62f)
            } else {
                VeilPalette.Mist.copy(alpha = 0.42f)
            }
        )
    }
}

@Composable
private fun CastleChamberNode(
    id: String,
    name: String,
    purpose: String,
    unlockRank: Int,
    unlocked: Boolean,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val edge = if (unlocked) {
        VeilPalette.Brass.copy(alpha = 0.60f)
    } else {
        VeilPalette.BorderDark.copy(alpha = 0.86f)
    }

    Column(
        modifier = modifier
            .heightIn(min = 116.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.verticalGradient(
                    if (unlocked) {
                        listOf(
                            VeilPalette.DeepBrass.copy(alpha = 0.22f),
                            VeilPalette.RaisedIron.copy(alpha = 0.30f),
                            VeilPalette.Archive.copy(alpha = 0.93f)
                        )
                    } else {
                        listOf(
                            VeilPalette.Iron.copy(alpha = 0.34f),
                            VeilPalette.Ink.copy(alpha = 0.88f)
                        )
                    }
                )
            )
            .border(BorderStroke(1.dp, edge), MaterialTheme.shapes.extraSmall)
            .clickable(enabled = unlocked) { onOpenRoom(id) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .width(50.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(
                    if (unlocked) VeilPalette.DeepBrass.copy(alpha = 0.38f)
                    else VeilPalette.Ink.copy(alpha = 0.74f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (unlocked) VeilPalette.Brass.copy(alpha = 0.46f)
                        else VeilPalette.BorderDark
                    ),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            CastleRoomIcon(
                id = id,
                unlocked = unlocked,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.58f)
        )

        Text(
            if (unlocked) purpose else "Awakens at rank ${unlockRank + 1}",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) VeilPalette.Mist else VeilPalette.Mist.copy(alpha = 0.46f)
        )

        Spacer(Modifier.height(5.dp))

        Text(
            if (unlocked) "ENTER" else "SEALED",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
            color = if (unlocked) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.44f)
        )
    }
}

@Composable
private fun CastleGateLabel(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
            color = VeilPalette.Brass.copy(alpha = 0.80f)
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.58f)
        )
    }
}

@Composable
private fun CastleCrest(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val stroke = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val top = h * .22f
        val mid = h * .46f
        drawLine(tint, Offset(w * .16f, mid), Offset(w * .84f, mid), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .22f, mid), Offset(w * .22f, h * .84f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .78f, mid), Offset(w * .78f, h * .84f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .22f, h * .84f), Offset(w * .78f, h * .84f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .22f, top), Offset(w * .22f, mid), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .78f, top), Offset(w * .78f, mid), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .14f, top), Offset(w * .31f, top), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * .69f, top), Offset(w * .86f, top), stroke.width, StrokeCap.Round)
        drawRoundRect(
            tint,
            topLeft = Offset(w * .43f, h * .64f),
            size = Size(w * .14f, h * .20f),
            cornerRadius = CornerRadius(w * .07f),
            style = stroke
        )
    }
}

@Composable
private fun CastleRoomIcon(
    id: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (unlocked) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.44f)

    Canvas(modifier) {
        val stroke = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        when (id) {
            "library" -> {
                repeat(3) { index ->
                    val left = w * (.12f + index * .28f)
                    drawRoundRect(
                        tint,
                        topLeft = Offset(left, h * .18f + if (index == 1) h * .06f else 0f),
                        size = Size(w * .20f, h * .64f - if (index == 1) h * .06f else 0f),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = stroke
                    )
                }
            }
            "ritual" -> {
                val p = Path().apply {
                    moveTo(w * .50f, h * .08f)
                    lineTo(w * .62f, h * .38f)
                    lineTo(w * .92f, h * .50f)
                    lineTo(w * .62f, h * .62f)
                    lineTo(w * .50f, h * .92f)
                    lineTo(w * .38f, h * .62f)
                    lineTo(w * .08f, h * .50f)
                    lineTo(w * .38f, h * .38f)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            "observatory" -> {
                drawCircle(tint, w * .30f, Offset(w * .48f, h * .48f), style = stroke)
                drawCircle(tint, w * .08f, Offset(w * .48f, h * .48f))
                drawLine(
                    tint,
                    Offset(w * .66f, h * .70f),
                    Offset(w * .86f, h * .88f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
            "archive" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * .14f, h * .26f),
                    size = Size(w * .72f, h * .56f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    tint,
                    Offset(w * .14f, h * .42f),
                    Offset(w * .86f, h * .42f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .40f, h * .56f),
                    Offset(w * .60f, h * .56f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
            "treasury" -> {
                val p = Path().apply {
                    moveTo(w * .50f, h * .10f)
                    lineTo(w * .86f, h * .40f)
                    lineTo(w * .66f, h * .88f)
                    lineTo(w * .34f, h * .88f)
                    lineTo(w * .14f, h * .40f)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawLine(
                    tint,
                    Offset(w * .14f, h * .40f),
                    Offset(w * .86f, h * .40f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
            "sanctum" -> {
                drawCircle(tint, w * .29f, Offset(w * .50f, h * .50f), style = stroke)
                drawCircle(tint, w * .10f, Offset(w * .50f, h * .50f), style = stroke)
                drawLine(
                    tint,
                    Offset(w * .50f, h * .06f),
                    Offset(w * .50f, h * .20f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .50f, h * .80f),
                    Offset(w * .50f, h * .94f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .06f, h * .50f),
                    Offset(w * .20f, h * .50f),
                    stroke.width,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    Offset(w * .80f, h * .50f),
                    Offset(w * .94f, h * .50f),
                    stroke.width,
                    StrokeCap.Round
                )
            }
        }
    }
}
