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
import androidx.compose.ui.graphics.Path
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
import com.veilreader.app.domain.WorldMutationLedger
import com.veilreader.app.domain.WorldMutationRealm
import com.veilreader.app.domain.deriveLivingMirrorNotes
import com.veilreader.app.ui.VeilEyebrowText
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.hallSharedBoundsKey
import com.veilreader.app.ui.rememberVeilTouchExplorationEnabled
import com.veilreader.app.ui.veilSharedBounds
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.LocalVeilQualityTier
import com.veilreader.app.ui.theme.VeilQualityTier
import com.veilreader.app.ui.theme.CathedralMotionClass
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

internal data class LivingMirrorPresentationPolicy(
    val maxSurfaceNodes: Int,
    val animateMaterial: Boolean
)

internal fun livingMirrorPresentationPolicy(
    qualityTier: VeilQualityTier,
    reducedMotion: Boolean
): LivingMirrorPresentationPolicy =
    LivingMirrorPresentationPolicy(
        maxSurfaceNodes = when (qualityTier) {
            VeilQualityTier.FULL -> MIRROR_SURFACE_NODE_LIMIT
            VeilQualityTier.BALANCED -> 20
            VeilQualityTier.ESSENTIAL -> 12
        },
        animateMaterial = !reducedMotion && qualityTier != VeilQualityTier.ESSENTIAL
    )

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

internal fun livingMirrorFogAlpha(
    noteCount: Int,
    queryActive: Boolean
): Float {
    val density = (noteCount.coerceIn(0, 40) / 40f)
    val resting = 0.28f - density * 0.10f
    return (if (queryActive) resting * 0.42f else resting)
        .coerceIn(0.07f, 0.30f)
}

@Composable
fun LivingMirrorScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    passageVisits: List<PassageVisit>,
    readingCycles: List<ReadingCycleRecord>,
    mutationLedger: WorldMutationLedger = WorldMutationLedger.EMPTY,
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

            WorldMutationEcho(
                ledger = mutationLedger,
                realm = WorldMutationRealm.MIRROR,
                limit = 1,
                eyebrow = "MIRROR CONSEQUENCE",
                title = "The glass remembers"
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
            LivingMirrorBrassRule(Modifier.width(156.dp), strong = true)
        }
    }
}


@Composable
private fun LivingMirrorBrassRule(
    modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        VeilPalette.Brass.copy(alpha = if (strong) 0.92f else 0.58f),
                        Color.Transparent
                    )
                )
            )
    )
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
    val qualityTier = LocalVeilQualityTier.current
    val presentation = livingMirrorPresentationPolicy(
        qualityTier = qualityTier,
        reducedMotion = reducedMotion
    )
    val fogAlpha by animateFloatAsState(
        targetValue = livingMirrorFogAlpha(
            noteCount = notes.size,
            queryActive = query.isNotBlank()
        ),
        animationSpec = if (!presentation.animateMaterial) {
            snap()
        } else {
            tween(motionBudgetFor(CathedralMotionClass.MATERIAL).targetDurationMs)
        },
        label = "living-mirror-fog"
    )

    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = if (compact) 320.dp else 470.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17130F),
                        Color(0xFF090C10),
                        Color(0xFF05070A)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.58f)),
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
                queryActive = query.isNotBlank(),
                maxNodes = presentation.maxSurfaceNodes
            )
        }
        val visibleMatchCount =
            if (query.isBlank()) 0 else visibleNotes.count { it.highlightId in matches }

        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val insetX = w * 0.055f
            val insetY = h * 0.035f
            val mirrorLeft = insetX
            val mirrorTop = insetY
            val mirrorWidth = w - insetX * 2f
            val mirrorHeight = h - insetY * 2f
            val corner = 34.dp.toPx()

            // Heavy old frame: iron body, brass lip, then the cold glass surface.
            drawRoundRect(
                color = Color(0xFF16120E),
                topLeft = androidx.compose.ui.geometry.Offset(mirrorLeft, mirrorTop),
                size = androidx.compose.ui.geometry.Size(mirrorWidth, mirrorHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner),
            )
            drawRoundRect(
                color = VeilPalette.Brass.copy(alpha = 0.54f),
                topLeft = androidx.compose.ui.geometry.Offset(mirrorLeft, mirrorTop),
                size = androidx.compose.ui.geometry.Size(mirrorWidth, mirrorHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner),
                style = Stroke(2.1.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF725C34).copy(alpha = 0.42f),
                topLeft = androidx.compose.ui.geometry.Offset(
                    mirrorLeft + 6.dp.toPx(),
                    mirrorTop + 6.dp.toPx()
                ),
                size = androidx.compose.ui.geometry.Size(
                    mirrorWidth - 12.dp.toPx(),
                    mirrorHeight - 12.dp.toPx()
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner - 5.dp.toPx()),
                style = Stroke(0.8.dp.toPx())
            )

            val glassLeft = mirrorLeft + 12.dp.toPx()
            val glassTop = mirrorTop + 12.dp.toPx()
            val glassWidth = mirrorWidth - 24.dp.toPx()
            val glassHeight = mirrorHeight - 24.dp.toPx()

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF31434A).copy(alpha = 0.78f),
                        Color(0xFF152128).copy(alpha = 0.94f),
                        Color(0xFF070A0D)
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        glassLeft + glassWidth * 0.34f,
                        glassTop + glassHeight * 0.28f
                    ),
                    radius = size.minDimension * 0.70f
                ),
                topLeft = androidx.compose.ui.geometry.Offset(glassLeft, glassTop),
                size = androidx.compose.ui.geometry.Size(glassWidth, glassHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner - 9.dp.toPx())
            )

            // Uneven silvering and a narrow reflection make it read as glass, not a panel.
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.18f to VeilPalette.Spirit.copy(alpha = 0.025f),
                        0.31f to VeilPalette.Moon.copy(alpha = 0.085f),
                        0.38f to Color.Transparent,
                        1.00f to Color.Transparent
                    ),
                    startX = glassLeft,
                    endX = glassLeft + glassWidth
                ),
                topLeft = androidx.compose.ui.geometry.Offset(glassLeft, glassTop),
                size = androidx.compose.ui.geometry.Size(glassWidth, glassHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner - 9.dp.toPx())
            )

            val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
            repeat(4) { index ->
                drawCircle(
                    color = VeilPalette.Spirit.copy(
                        alpha = 0.040f - index * 0.006f
                    ),
                    center = center,
                    radius = size.minDimension * (0.15f + index * 0.105f),
                    style = Stroke((0.9f - index * 0.10f).dp.toPx())
                )
            }

            // Fog is strongest while the mirror rests. Summoning clears it without changing data.
            repeat(5) { index ->
                val y = glassTop + glassHeight * (0.12f + index * 0.18f)
                val drift = if (index % 2 == 0) 0.05f else -0.04f
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VeilPalette.Mist.copy(
                                alpha = fogAlpha * (0.22f - index * 0.018f)
                            ),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            glassLeft + glassWidth * (0.48f + drift),
                            y
                        ),
                        radius = glassWidth * 0.46f
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        glassLeft + glassWidth * 0.08f,
                        y - glassHeight * 0.09f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        glassWidth * 0.84f,
                        glassHeight * 0.18f
                    )
                )
            }

            // A small crown and foot make the frame feel like an object in the Hall.
            val crown = Path().apply {
                moveTo(w * 0.43f, mirrorTop + 2.dp.toPx())
                lineTo(w * 0.50f, mirrorTop - 10.dp.toPx())
                lineTo(w * 0.57f, mirrorTop + 2.dp.toPx())
            }
            drawPath(
                crown,
                color = VeilPalette.Brass.copy(alpha = 0.52f),
                style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawLine(
                color = VeilPalette.Brass.copy(alpha = 0.38f),
                start = androidx.compose.ui.geometry.Offset(w * 0.38f, h - 8.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(w * 0.62f, h - 8.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        visibleNotes.forEach { note ->
            val matched = query.isBlank() || note.highlightId in matches
            val targetAlpha = if (matched) 0.96f else 0.12f
            val targetScale = if (matched && query.isNotBlank()) 1.07f else 1f
            val alpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = if (reducedMotion) snap() else tween(motionBudgetFor(CathedralMotionClass.MATERIAL).targetDurationMs),
                label = "mirror-node-alpha"
            )
            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = if (!presentation.animateMaterial) {
                    snap()
                } else {
                    tween(motionBudgetFor(CathedralMotionClass.MATERIAL).targetDurationMs)
                },
                label = "mirror-node-scale"
            )
            val horizontalInset = if (compact) 22f else 42f
            val verticalInset = if (compact) 26f else 42f
            val usableWidth = (width - horizontalInset * 2f - 116f).coerceAtLeast(1f)
            val usableHeight = (surfaceHeight - verticalInset * 2f - 92f).coerceAtLeast(1f)
            val x = horizontalInset + usableWidth * note.clusterX.coerceIn(0f, 1f)
            val y = verticalInset + usableHeight * note.clusterY.coerceIn(0f, 1f)

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
            .heightIn(min = 92.dp)
            .semantics {
                contentDescription =
                    "Note from ${note.bookTitle}. ${note.revisitCount} revisits. Reading cycle ${note.cycleIndex}."
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val proximity = note.proximity.coerceIn(0f, 1f)
            val glow = (0.12f + proximity * 0.22f) * alpha

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VeilPalette.Spirit.copy(alpha = glow),
                        VeilPalette.Spirit.copy(alpha = glow * 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.58f
                ),
                radius = size.minDimension * 0.58f,
                center = center
            )

            repeat(note.ringCount.coerceAtMost(4)) { index ->
                drawOval(
                    color = VeilPalette.Spirit.copy(
                        alpha = alpha * (0.25f - index * 0.045f)
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        size.width * (0.18f - index * 0.025f),
                        size.height * (0.17f - index * 0.018f)
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * (0.64f + index * 0.05f),
                        size.height * (0.58f + index * 0.036f)
                    ),
                    style = Stroke((0.72f + index * 0.08f).dp.toPx())
                )
            }

            drawLine(
                color = VeilPalette.Moon.copy(alpha = alpha * 0.18f),
                start = androidx.compose.ui.geometry.Offset(
                    center.x - size.width * 0.18f,
                    size.height * 0.31f
                ),
                end = androidx.compose.ui.geometry.Offset(
                    center.x + size.width * 0.10f,
                    size.height * 0.22f
                ),
                strokeWidth = 0.62.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(
                color = VeilPalette.Brass.copy(alpha = alpha * (0.54f + proximity * 0.20f)),
                radius = 2.1.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(center.x, size.height * 0.79f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                note.bookTitle,
                style = MaterialTheme.typography.labelMedium,
                color = VeilPalette.Moon.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                note.note,
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = alpha * 0.76f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
                    .semantics {
                        contentDescription =
                            "Note from ${note.bookTitle}. ${note.note}. " +
                                "${note.revisitCount} revisits. Reading cycle ${note.cycleIndex}. " +
                                "Open note and passage options."
                    }
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
                LivingMirrorBrassRule(Modifier.fillMaxWidth())
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
