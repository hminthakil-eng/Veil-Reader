package com.veilreader.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.LivingMirrorNote
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.ReadingCycleRecord
import com.veilreader.app.domain.deriveLivingMirrorNotes
import com.veilreader.app.ui.BrassRule
import com.veilreader.app.ui.VeilEyebrowText
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.hallSharedBoundsKey
import com.veilreader.app.ui.rememberVeilTouchExplorationEnabled
import com.veilreader.app.ui.veilSharedBounds
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotionClass
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.narrativeArchitectureField
import com.veilreader.app.ui.theme.motionBudgetFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class LivingMirrorMode { MIRROR, INDEX }

private const val MIRROR_SURFACE_NODE_LIMIT = 28

internal fun selectLivingMirrorSurfaceNotes(
    notes: List<LivingMirrorNote>,
    matches: Set<String>,
    queryActive: Boolean,
    maxNodes: Int = MIRROR_SURFACE_NODE_LIMIT
): List<LivingMirrorNote> {
    if (maxNodes <= 0 || notes.isEmpty()) return emptyList()
    if (!queryActive) return notes.take(maxNodes)

    val matched = notes.filter { it.highlightId in matches }
    val unrelated = notes.filterNot { it.highlightId in matches }
    return (matched + unrelated).take(maxNodes)
}

@Composable
fun LivingMirrorScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    passageVisits: List<PassageVisit>,
    readingCycles: List<ReadingCycleRecord>,
    onOpenPassage: (Book, String) -> Unit,
    onClose: () -> Unit
) {
    val now = remember(books, highlights, passageVisits, readingCycles) { System.currentTimeMillis() }
    val allNotes = remember(books, highlights, passageVisits, readingCycles, now) {
        deriveLivingMirrorNotes(
            books = books,
            highlights = highlights,
            passageVisits = passageVisits,
            readingCycles = readingCycles,
            nowEpochMs = now
        )
    }
    val booksById = remember(books) { books.associateBy { it.id } }

    val touchExplorationEnabled = rememberVeilTouchExplorationEnabled()

    var mode by rememberSaveable { mutableStateOf(LivingMirrorMode.MIRROR) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveMode =
        if (touchExplorationEnabled) LivingMirrorMode.INDEX else mode

    val normalizedQuery = query.trim().lowercase()
    val filtered = remember(allNotes, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            allNotes
        } else {
            allNotes.filter { note ->
                sequenceOf(note.bookTitle, note.bookAuthor, note.quote, note.note)
                    .any { it.lowercase().contains(normalizedQuery) }
            }
        }
    }
    val selected = allNotes.firstOrNull { it.highlightId == selectedId }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.ARCHIVE,
                seed = allNotes.size * 37 + filtered.size,
                intensity = 0.94f,
                temporalPhase = currentVeilTemporalPhase()
            )
            .narrativeArchitectureField(
                realm = VeilRealm.ARCHIVE,
                seed = allNotes.size * 41,
                intensity = 0.88f
            )
    ) {
        val compactHeight = maxHeight < 760.dp
        val compactWidth = maxWidth < 380.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 980.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            LivingMirrorMasthead(
                noteCount = allNotes.size,
                compact = compactHeight || compactWidth,
                onClose = onClose
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                singleLine = true,
                label = { Text("Summon notes") },
                placeholder = { Text("Book, author, quote, or note") },
                shape = MaterialTheme.shapes.extraSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VeilPalette.Brass,
                    unfocusedBorderColor = VeilPalette.BorderDark,
                    focusedContainerColor = VeilPalette.Ink.copy(alpha = 0.76f),
                    unfocusedContainerColor = VeilPalette.Ink.copy(alpha = 0.58f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MirrorModeButton(
                    label = "Mirror",
                    selected = effectiveMode == LivingMirrorMode.MIRROR,
                    enabled = !touchExplorationEnabled,
                    onClick = { mode = LivingMirrorMode.MIRROR },
                    modifier = Modifier.weight(1f)
                )
                MirrorModeButton(
                    label = "Index",
                    selected = effectiveMode == LivingMirrorMode.INDEX,
                    onClick = { mode = LivingMirrorMode.INDEX },
                    modifier = Modifier.weight(1f)
                )
            }

            if (touchExplorationEnabled && allNotes.isNotEmpty()) {
                Text(
                    "TalkBack uses Index presentation so every note is available in reading order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
            }

            when {
                allNotes.isEmpty() -> LivingMirrorEmptyState(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                effectiveMode == LivingMirrorMode.MIRROR -> LivingMirrorSurface(
                    notes = allNotes,
                    query = normalizedQuery,
                    matches = filtered.mapTo(hashSetOf()) { it.highlightId },
                    onSelect = { selectedId = it.highlightId },
                    compact = compactHeight,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                else -> LivingMirrorIndex(
                    notes = filtered,
                    onSelect = { selectedId = it.highlightId },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }

    selected?.let { note ->
        LivingMirrorNoteDialog(
            note = note,
            onDismiss = { selectedId = null },
            onReturnToPassage = {
                booksById[note.bookId]?.let { book ->
                    onOpenPassage(book, note.locatorJson)
                }
                selectedId = null
            }
        )
    }
}

@Composable
private fun LivingMirrorMasthead(
    noteCount: Int,
    compact: Boolean,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .veilSharedBounds(hallSharedBoundsKey("mirror"))
            .fillMaxWidth()
            .heightIn(min = if (compact) 184.dp else 238.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111820), Color(0xFF0A0E14), VeilPalette.Ink)
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Spirit.copy(alpha = 0.58f)),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        GrayfogOrnamentFrame(Modifier.matchParentSize(), strength = 0.72f)
        VeilRealmEmblem(
            realm = VeilRealm.ARCHIVE,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = VeilSpacing.lg)
                .size(150.dp),
            tint = VeilPalette.Spirit.copy(alpha = 0.26f)
        )
        TextButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .heightIn(min = 48.dp)
        ) {
            Text("Great Hall")
        }
        VeilEyebrowText(
            text = "$noteCount PRESERVED NOTES",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(VeilSpacing.md),
            color = VeilPalette.Spirit,
            trackingSp = 1.1f
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            VeilEyebrowText(
                text = "LIVING MIRROR · MEMORY SURFACE",
                color = VeilPalette.Brass,
                trackingSp = 1.35f
            )
            Text(
                "The Living Mirror",
                style = MaterialTheme.typography.displaySmall,
                color = VeilPalette.Moon
            )
            if (!compact) {
                Text(
                    "Notes arrange by book, age, revisit history, and reading cycle. Nothing here is inferred.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist.copy(alpha = 0.84f),
                    modifier = Modifier.widthIn(max = 620.dp)
                )
            }
            BrassRule(Modifier.width(156.dp), strong = true)
        }
    }
}

@Composable
private fun MirrorModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilPalette.DeepBrass.copy(alpha = 0.74f),
                contentColor = VeilPalette.Moon
            ),
            border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.58f))
        ) { Text(label) }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, VeilPalette.BorderDark)
        ) { Text(label) }
    }
}

@Composable
private fun LivingMirrorSurface(
    notes: List<LivingMirrorNote>,
    query: String,
    matches: Set<String>,
    onSelect: (LivingMirrorNote) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalVeilReducedMotion.current

    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = if (compact) 320.dp else 470.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF26333B).copy(alpha = 0.72f),
                        Color(0xFF121A21).copy(alpha = 0.92f),
                        Color(0xFF070A0F)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Spirit.copy(alpha = 0.42f)),
                MaterialTheme.shapes.extraSmall
            )
            .semantics {
                contentDescription =
                    "Living Mirror. ${notes.size} notes arranged spatially by factual reading history."
            }
    ) {
        val minimumSurfaceHeight = if (compact) 320f else 470f
        val surfaceHeight = maxHeight.value.coerceIn(minimumSurfaceHeight, 900f)
        val width = maxWidth.value.coerceAtLeast(280f)
        val visibleNotes = remember(notes, matches, query) {
            selectLivingMirrorSurfaceNotes(
                notes = notes,
                matches = matches,
                queryActive = query.isNotBlank()
            )
        }
        val visibleMatchCount =
            if (query.isBlank()) 0 else visibleNotes.count { it.highlightId in matches }

        Canvas(Modifier.matchParentSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            repeat(5) { index ->
                drawCircle(
                    color = VeilPalette.Spirit.copy(alpha = 0.055f - index * 0.007f),
                    center = center,
                    radius = size.minDimension * (0.16f + index * 0.095f),
                    style = Stroke((1.0f - index * 0.10f).dp.toPx())
                )
            }
            repeat(7) { index ->
                val y = size.height * (0.12f + index * 0.12f)
                drawLine(
                    color = VeilPalette.Mist.copy(alpha = 0.025f),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, y),
                    strokeWidth = 0.7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        visibleNotes.forEach { note ->
            val matched = query.isBlank() || note.highlightId in matches
            val targetAlpha = if (matched) 0.96f else 0.12f
            val targetScale = if (matched && query.isNotBlank()) 1.07f else 1f
            val alpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = if (reducedMotion) snap() else tween(motionBudgetFor(VeilMotionClass.MATERIAL).targetDurationMs),
                label = "mirror-node-alpha"
            )
            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = if (reducedMotion) {
                    snap()
                } else {
                    tween(motionBudgetFor(VeilMotionClass.MATERIAL).targetDurationMs)
                },
                label = "mirror-node-scale"
            )
            val x = ((width - 116f) * note.clusterX).coerceIn(0f, width - 116f)
            val y = ((surfaceHeight - 92f) * note.clusterY).coerceIn(0f, surfaceHeight - 92f)

            LivingMirrorNode(
                note = note,
                onClick = { onSelect(note) },
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .width(116.dp)
                    .scale(scale),
                alpha = alpha
            )
        }

        if (query.isNotBlank()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = VeilPalette.Ink.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, VeilPalette.Spirit.copy(alpha = 0.38f))
            ) {
                Text(
                    if (matches.size > visibleMatchCount) {
                        "${visibleMatchCount} of ${matches.size} summoned on surface · open Index for all"
                    } else {
                        "${matches.size} summoned · unrelated notes recede"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Moon
                )
            }
        }
    }
}

@Composable
private fun LivingMirrorNode(
    note: LivingMirrorNote,
    onClick: () -> Unit,
    modifier: Modifier,
    alpha: Float
) {
    Box(
        modifier = modifier
            .semantics {
                contentDescription =
                    "Note from ${note.bookTitle}. ${note.revisitCount} revisits. Reading cycle ${note.cycleIndex}."
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            repeat(note.ringCount) { index ->
                drawCircle(
                    color = VeilPalette.Spirit.copy(alpha = alpha * (0.26f - index * 0.045f)),
                    center = center,
                    radius = size.minDimension * (0.38f + index * 0.08f),
                    style = Stroke(0.8.dp.toPx())
                )
            }
        }
        Surface(
            modifier = Modifier.width(94.dp).heightIn(min = 68.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = VeilPalette.Archive.copy(alpha = alpha * (0.72f + note.proximity * 0.18f)),
            border = BorderStroke(
                1.dp,
                VeilPalette.Spirit.copy(alpha = alpha * (0.30f + note.proximity * 0.34f))
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    note.bookTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = VeilPalette.Moon.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    note.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = alpha * 0.90f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LivingMirrorIndex(
    notes: List<LivingMirrorNote>,
    onSelect: (LivingMirrorNote) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = VeilSpacing.xl)
    ) {
        items(notes, key = { it.highlightId }) { note ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onSelect(note) },
                shape = MaterialTheme.shapes.extraSmall,
                color = VeilPalette.Archive.copy(alpha = 0.70f),
                border = BorderStroke(1.dp, VeilPalette.BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(VeilSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            note.bookTitle,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            color = VeilPalette.Moon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Depth ${(note.depth * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Spirit
                        )
                    }
                    Text(
                        note.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeilPalette.Mist,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${note.revisitCount} revisits · cycle ${note.cycleIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LivingMirrorEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF1D2931), Color(0xFF0A0E13), VeilPalette.Ink)
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Spirit.copy(alpha = 0.30f)),
                MaterialTheme.shapes.extraSmall
            ),
        contentAlignment = Alignment.Center
    ) {
        VeilRealmEmblem(
            realm = VeilRealm.ARCHIVE,
            modifier = Modifier.size(190.dp),
            tint = VeilPalette.Spirit.copy(alpha = 0.12f)
        )
        Column(
            modifier = Modifier.widthIn(max = 460.dp).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VeilEyebrowText(
                text = "THE MIRROR IS STILL",
                color = VeilPalette.Spirit,
                trackingSp = 1.35f
            )
            Text(
                "Write the first note",
                style = MaterialTheme.typography.headlineMedium,
                color = VeilPalette.Moon
            )
            Text(
                "Notes appear here only after you preserve a passage and write something of your own.",
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LivingMirrorNoteDialog(
    note: LivingMirrorNote,
    onDismiss: () -> Unit,
    onReturnToPassage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        containerColor = VeilPalette.Archive,
        tonalElevation = 0.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VeilEyebrowText(
                    text = "MIRROR FRAGMENT · CYCLE ${note.cycleIndex}",
                    color = VeilPalette.Spirit,
                    trackingSp = 1.1f
                )
                Text(
                    note.bookTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = VeilPalette.Moon
                )
                Text(
                    note.bookAuthor,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "“${note.quote}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Moon.copy(alpha = 0.82f)
                )
                BrassRule(Modifier.fillMaxWidth())
                Text(
                    note.note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    buildString {
                        append("Recorded ").append(formatMirrorDate(note.recordedAtEpochMs))
                        append(" · ").append(note.revisitCount).append(" revisits")
                        note.lastRevisitedAtEpochMs?.let {
                            append(" · last returned ").append(formatMirrorDate(it))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist
                )
            }
        },
        confirmButton = {
            Button(onClick = onReturnToPassage, shape = MaterialTheme.shapes.extraSmall) {
                Text("Return to passage")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatMirrorDate(epochMs: Long): String =
    runCatching {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
    }.getOrDefault("Recorded")
