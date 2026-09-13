package com.veilreader.app.ui.screens

import com.veilreader.app.ui.theme.LocalVeilMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import com.veilreader.app.domain.EstateState
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.ui.theme.VeilSpacing

/** A progression world that stays useful: every room maps to a real reader feature. */
@Composable
fun CastleScreen(
    profile: ReaderProfile,
    estate: EstateState,
    onBuild: () -> Boolean,
    onClaim: (String) -> Boolean,
    onDesign: (EstateState) -> Boolean,
    onAdvanceRank: () -> Unit,
    onOpenRoom: (String) -> Unit
) {
    EstateScreen(profile, estate, onBuild, onClaim, onDesign) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Path chambers", style = MaterialTheme.typography.titleLarge)
            Text("Your reading Path unlocks these chambers independently of your home. Your library and notes are always available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (GamificationEngine.canAdvanceRank(profile)) {
                Button(onClick = onAdvanceRank, modifier = Modifier.fillMaxWidth()) { Text("Advance your Path rank") }
            }
            CastleMap(rankIndex = profile.rankIndex, onOpenRoom = onOpenRoom)
        }
    }
}

@Composable
private fun CastleMap(rankIndex: Int, onOpenRoom: (String) -> Unit) {
    val motion = LocalVeilMotion.current
    val rooms = SampleData.rooms
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)),
                MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.xl)
    ) {
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(tween(motion.duration(360))) + expandVertically(tween(motion.duration(420)), expandFrom = Alignment.Top)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (rowIndex in 0 until 3) {
                    val left = rooms[rowIndex * 2]
                    val right = rooms[rowIndex * 2 + 1]

                    RoomPair(
                        leftId = left.id,
                        leftName = left.name,
                        leftPurpose = left.purpose,
                        leftUnlockRank = left.unlockRankIndex,
                        rightId = right.id,
                        rightName = right.name,
                        rightPurpose = right.purpose,
                        rightUnlockRank = right.unlockRankIndex,
                        rankIndex = rankIndex,
                        onOpenRoom = onOpenRoom
                    )

                    if (rowIndex < 2) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.64f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomPair(
    leftId: String,
    leftName: String,
    leftPurpose: String,
    leftUnlockRank: Int,
    rightId: String,
    rightName: String,
    rightPurpose: String,
    rightUnlockRank: Int,
    rankIndex: Int,
    onOpenRoom: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CastleRoomNode(
            id = leftId,
            name = leftName,
            purpose = leftPurpose,
            unlockRank = leftUnlockRank,
            unlocked = rankIndex >= leftUnlockRank,
            onOpenRoom = onOpenRoom,
            modifier = Modifier.weight(1f)
        )

        Box(
            Modifier
                .width(18.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.64f))
        )

        CastleRoomNode(
            id = rightId,
            name = rightName,
            purpose = rightPurpose,
            unlockRank = rightUnlockRank,
            unlocked = rankIndex >= rightUnlockRank,
            onOpenRoom = onOpenRoom,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CastleRoomNode(
    id: String,
    name: String,
    purpose: String,
    unlockRank: Int,
    unlocked: Boolean,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    val edge = if (unlocked) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.50f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
    }
    val fill = if (unlocked) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)
            )
        )
    }

    Column(
        modifier = modifier
            .heightIn(min = 154.dp)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, edge), shape)
            .clickable(enabled = unlocked) { onOpenRoom(id) }
            .padding(horizontal = VeilSpacing.sm, vertical = VeilSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
            contentAlignment = Alignment.Center
        ) {
            CastleRoomIcon(
                id = id,
                unlocked = unlocked,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(VeilSpacing.xs))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (unlocked) purpose else "Unlocks at rank ${unlockRank + 1}",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(5.dp))
        Surface(
            shape = CircleShape,
            color = if (unlocked) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
            }
        ) {
            Text(
                if (unlocked) "OPEN" else "SEALED",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
            )
        }
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
private fun CastleRoomIcon(id: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    val tint = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
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
                drawLine(tint, Offset(w * .66f, h * .70f), Offset(w * .86f, h * .88f), stroke.width, StrokeCap.Round)
            }
            "archive" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * .14f, h * .26f),
                    size = Size(w * .72f, h * .56f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(tint, Offset(w * .14f, h * .42f), Offset(w * .86f, h * .42f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .40f, h * .56f), Offset(w * .60f, h * .56f), stroke.width, StrokeCap.Round)
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
                drawLine(tint, Offset(w * .14f, h * .40f), Offset(w * .86f, h * .40f), stroke.width, StrokeCap.Round)
            }
            "sanctum" -> {
                drawCircle(tint, w * .29f, Offset(w * .50f, h * .50f), style = stroke)
                drawCircle(tint, w * .10f, Offset(w * .50f, h * .50f), style = stroke)
                drawLine(tint, Offset(w * .50f, h * .06f), Offset(w * .50f, h * .20f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .50f, h * .80f), Offset(w * .50f, h * .94f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .06f, h * .50f), Offset(w * .20f, h * .50f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * .80f, h * .50f), Offset(w * .94f, h * .50f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

