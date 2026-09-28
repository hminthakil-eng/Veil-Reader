package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.MemoryAtlas
import com.veilreader.app.domain.MemoryAtlasEdge
import com.veilreader.app.domain.MemoryRelationKind
import com.veilreader.app.domain.PathArchitecturalMotif
import com.veilreader.app.domain.PathWorldSignature
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.derivePathWorldSignature
import com.veilreader.app.domain.buildMemoryAtlas
import com.veilreader.app.ui.VeilMastheadMetaRow
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.hallSharedBoundsKey
import com.veilreader.app.ui.veilSharedBounds
import com.veilreader.app.ui.books.bookArtifactState
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.castleLayoutPolicyFor
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.narrativeArchitectureField
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

internal fun observatoryPathSignature(profile: ReaderProfile?): PathWorldSignature? {
    val value = profile ?: return null
    val ritualTarget = value.ritualTarget.coerceAtLeast(1)
    val ritualCharge =
        (value.ritualProgress.coerceAtLeast(0).toFloat() / ritualTarget.toFloat())
            .coerceIn(0f, 1f)
    return derivePathWorldSignature(
        pathId = value.path.id,
        rankIndex = value.rankIndex,
        rankCount = value.path.ranks.size,
        ritualCharge = ritualCharge
    )
}

@Composable
fun ObservatoryScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    readingSessions: List<ReadingSessionSnapshot>,
    profile: ReaderProfile? = null,
    onOpenBook: (Book) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val atlas = remember(books, highlights, readingSessions) {
        buildMemoryAtlas(
            books = books,
            highlights = highlights,
            sessions = readingSessions
        )
    }
    val pathSignature = remember(profile) {
        observatoryPathSignature(profile)
    }
    var selectedBookId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(atlas.nodes) {
        if (atlas.nodes.none { it.book.id == selectedBookId }) {
            selectedBookId = atlas.nodes.firstOrNull()?.book?.id
        }
    }

    val selectedNode = atlas.nodes.firstOrNull { it.book.id == selectedBookId }
    val connections = selectedNode?.let { atlas.connectionsFor(it.book.id) }.orEmpty()
    val booksById = remember(atlas.nodes) { atlas.nodes.associateBy { it.book.id } }
    val observatoryAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val observatoryLayout = castleLayoutPolicyFor(observatoryAdaptiveClass)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.CASTLE,
                seed =
                    atlas.nodes.size * 17 +
                        atlas.edges.size * 7 +
                        (pathSignature?.motif?.ordinal ?: 0) * 101,
                intensity = (
                    0.88f + (pathSignature?.strength ?: 0f) * 0.10f
                    ).coerceIn(0.88f, 0.98f),
                temporalPhase = currentVeilTemporalPhase()
            )
            .narrativeArchitectureField(
                realm = VeilRealm.CASTLE,
                seed =
                    atlas.nodes.size * 23 +
                        atlas.edges.size * 11 +
                        (pathSignature?.motif?.ordinal ?: 0) * 131,
                intensity = (
                    0.78f + (pathSignature?.strength ?: 0f) * 0.12f
                    ).coerceIn(0.78f, 0.90f)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = 0.30f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(720.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(780.dp)
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.04f),
                        0.38f to Color.Transparent,
                        0.72f to VeilPalette.Ink.copy(alpha = 0.62f),
                        1f to VeilPalette.Ink
                    )
                )
        )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = observatoryLayout.contentMaxWidthDp.dp)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = observatoryLayout.horizontalPaddingDp.dp,
                vertical = VeilSpacing.lg
            ),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg)
    ) {
        OutlinedButton(
            onClick = onClose,
            shape = MaterialTheme.shapes.extraSmall,
            border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.80f)),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(VeilBackLabel("Castle"), style = MaterialTheme.typography.labelMedium)
        }

        ObservatoryGrandMasthead(
            volumeCount = atlas.nodes.size,
            linkCount = atlas.edges.size,
            pathSignature = pathSignature
        )

        ObservatoryAtlasPanel(
            atlas = atlas,
            selectedBookId = selectedBookId,
            pathSignature = pathSignature,
            panelHeightDp = observatoryLayout.observatoryHeightDp,
            onSelectBook = { selectedBookId = it }
        )

        Text(
            buildString {
                append(atlas.nodes.size).append(" volumes")
                append(" · ").append(atlas.edges.size).append(" recorded links")
                if (atlas.isolatedCount > 0) {
                    append(" · ").append(atlas.isolatedCount).append(" solitary")
                }
            },
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
            color = VeilPalette.Mist.copy(alpha = 0.66f)
        )

        selectedNode?.let { node ->
            ObservatorySelection(
                node = node,
                connections = connections,
                booksById = booksById,
                onOpenBook = onOpenBook
            )
        } ?: ObservatoryEmptyState()

        if (atlas.nodes.isNotEmpty()) {
            ArchiveChamberHeading(
                eyebrow = "Constellation index",
                title = "Volumes in the atlas",
                trailing = "${atlas.nodes.size.coerceAtMost(24)}/24"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                atlas.nodes.forEach { node ->
                    ObservatoryBookRow(
                        node = node,
                        selected = node.book.id == selectedBookId,
                        onClick = { selectedBookId = node.book.id }
                    )
                }
            }
        }

        Text(
            "Atlas links are recorded relations, not AI claims: shared author, series, collection, or repeated vocabulary inside passages you explicitly preserved.",
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.64f)
        )
    }
    }
}

@Composable
private fun ObservatoryGrandMasthead(
    volumeCount: Int,
    linkCount: Int,
    pathSignature: PathWorldSignature?
) {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .veilSharedBounds(hallSharedBoundsKey("observatory"))
            .fillMaxWidth()
            .heightIn(min = if (fontScale > 1.35f) 342.dp else 270.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.62f)),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.08f),
                        0.40f to Color.Transparent,
                        1f to VeilPalette.Ink.copy(alpha = 0.97f)
                    )
                )
        )
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.90f
        )
        VeilRealmEmblem(
            realm = VeilRealm.WORLD,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = VeilSpacing.lg)
                .size(144.dp),
            tint = VeilPalette.Spirit.copy(alpha = 0.24f)
        )
        VeilMastheadMetaRow(
            primary = "OBSERVATORY · MEMORY ATLAS",
            secondary = "$volumeCount VOLUMES · $linkCount LINKS",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(VeilSpacing.md)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "The Sky of Memory",
                style = MaterialTheme.typography.displaySmall,
                color = VeilPalette.Moon
            )
            Text(
                "Constellations built only from durable reading history and passages you chose to preserve.",
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Moon.copy(alpha = 0.82f),
                modifier = Modifier.widthIn(max = 600.dp)
            )
            pathSignature?.let { signature ->
                Text(
                    "PATH LENS · ${signature.motif.name} · ${signature.inscription}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.72.sp),
                    color = VeilPalette.Spirit.copy(
                        alpha = 0.62f + signature.strength * 0.22f
                    ),
                    modifier = Modifier.widthIn(max = 640.dp)
                )
            }
            BrassRule(Modifier.width(158.dp), strong = true)
        }
    }
}

@Composable
private fun ObservatoryAtlasPanel(
    atlas: MemoryAtlas,
    selectedBookId: String?,
    pathSignature: PathWorldSignature?,
    panelHeightDp: Float,
    onSelectBook: (String) -> Unit
) {
    val relatedBookIds = remember(atlas.edges, selectedBookId) {
        if (selectedBookId == null) {
            emptySet()
        } else {
            buildSet {
                atlas.edges.forEach { edge ->
                    when (selectedBookId) {
                        edge.fromBookId -> add(edge.toBookId)
                        edge.toBookId -> add(edge.fromBookId)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF17202A),
                        VeilPalette.Archive,
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
                MaterialTheme.shapes.small
            )
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    contentDescription =
                        "Memory Atlas with ${atlas.nodes.size} volumes and ${atlas.edges.size} recorded links"
                }
                .pointerInput(atlas.nodes, selectedBookId) {
                    detectTapGestures { tap ->
                        if (atlas.nodes.isEmpty()) return@detectTapGestures
                        val nearest = atlas.nodes.mapIndexed { index, node ->
                            val point = atlasNodeOffset(
                                index = index,
                                total = atlas.nodes.size,
                                width = size.width.toFloat(),
                                height = size.height.toFloat()
                            )
                            Triple(
                                node.book.id,
                                hypot(tap.x - point.x, tap.y - point.y),
                                point
                            )
                        }.minByOrNull { it.second }

                        if (nearest != null && nearest.second <= 30.dp.toPx()) {
                            onSelectBook(nearest.first)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            pathSignature?.let { signature ->
                drawObservatoryPathLens(signature, center)
            }
            listOf(0.19f, 0.31f, 0.43f).forEach { fraction ->
                drawCircle(
                    color = VeilPalette.Brass.copy(alpha = 0.07f),
                    radius = size.minDimension * fraction,
                    center = center,
                    style = Stroke(0.8.dp.toPx())
                )
            }

            repeat(8) { index ->
                val angle = (index * 45.0 - 90.0) * PI / 180.0
                val end = Offset(
                    center.x + cos(angle).toFloat() * size.width * 0.43f,
                    center.y + sin(angle).toFloat() * size.height * 0.40f
                )
                drawLine(
                    color = VeilPalette.Brass.copy(alpha = 0.035f),
                    start = center,
                    end = end,
                    strokeWidth = 0.7.dp.toPx()
                )
            }

            val points = atlas.nodes.mapIndexed { index, _ ->
                atlasNodeOffset(index, atlas.nodes.size, size.width, size.height)
            }
            val indexById = atlas.nodes.mapIndexed { index, node -> node.book.id to index }.toMap()

            atlas.edges.forEach { edge ->
                val fromIndex = indexById[edge.fromBookId] ?: return@forEach
                val toIndex = indexById[edge.toBookId] ?: return@forEach
                val selectedEdge =
                    edge.fromBookId == selectedBookId || edge.toBookId == selectedBookId
                drawLine(
                    color = VeilPalette.Brass.copy(
                        alpha = if (selectedEdge) {
                            0.20f + edge.strength * 0.025f
                        } else {
                            0.022f + edge.strength * 0.008f
                        }
                    ),
                    start = points[fromIndex],
                    end = points[toIndex],
                    strokeWidth = if (selectedEdge) 1.3.dp.toPx() else 0.7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            atlas.nodes.forEachIndexed { index, node ->
                val point = points[index]
                val selected = node.book.id == selectedBookId
                val tint = when {
                    node.book.finished -> VeilPalette.Brass
                    node.book.progress > 0f -> VeilPalette.Spirit
                    else -> VeilPalette.Mist
                }
                val radius = (
                    4.0f +
                        node.engagementScore.coerceIn(0f, 7f) * 0.62f +
                        node.connectionCount.coerceAtMost(5) * 0.34f
                    ).dp.toPx()

                if (selected) {
                    drawCircle(
                        color = VeilPalette.Brass.copy(alpha = 0.68f),
                        radius = radius + 6.dp.toPx(),
                        center = point,
                        style = Stroke(1.2.dp.toPx())
                    )
                    drawCircle(
                        color = tint.copy(alpha = 0.12f),
                        radius = radius + 11.dp.toPx(),
                        center = point
                    )
                }

                val nodeAlpha = when {
                    selected -> 1f
                    selectedBookId == null -> 0.82f
                    node.book.id in relatedBookIds -> 0.86f
                    else -> 0.28f
                }

                drawCircle(
                    color = tint.copy(alpha = nodeAlpha),
                    radius = radius,
                    center = point
                )
                drawCircle(
                    color = VeilPalette.Ink.copy(alpha = 0.72f),
                    radius = (radius * 0.34f).coerceAtLeast(1.6.dp.toPx()),
                    center = point
                )
            }
        }

        if (atlas.nodes.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "THE SKY IS EMPTY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = VeilPalette.Brass
                )
                Text(
                    "Import volumes and preserve passages; the Observatory will map only relations your archive can support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist
                )
            }
        }
    }
}

private fun DrawScope.drawObservatoryPathLens(
    signature: PathWorldSignature,
    center: Offset
) {
    val strength = signature.strength.coerceIn(0f, 1f)
    val alpha = (0.025f + strength * 0.075f).coerceAtMost(0.10f)
    val brass = VeilPalette.Brass.copy(alpha = alpha)
    val spirit = VeilPalette.Spirit.copy(alpha = alpha * 0.82f)
    val w = size.width
    val h = size.height
    val count = signature.ornamentCount.coerceIn(2, 9)

    when (signature.motif) {
        PathArchitecturalMotif.CIPHER -> {
            repeat(count.coerceAtMost(6)) { index ->
                val radius = size.minDimension * (0.08f + index * 0.045f)
                drawArc(
                    color = if (index % 2 == 0) brass else spirit,
                    startAngle = 18f + index * 21f,
                    sweepAngle = 214f - index * 9f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke((0.55f + index * 0.06f).dp.toPx())
                )
            }
        }
        PathArchitecturalMotif.DREAM -> {
            repeat(count.coerceAtMost(6)) { index ->
                val rx = w * (0.18f + index * 0.028f)
                val ry = h * (0.08f + index * 0.018f)
                drawOval(
                    color = spirit.copy(alpha = alpha * (0.92f - index * 0.08f)),
                    topLeft = Offset(center.x - rx, center.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(0.65.dp.toPx())
                )
            }
        }
        PathArchitecturalMotif.ARCHIVE -> {
            repeat(count.coerceAtMost(8)) { index ->
                val x = w * (0.25f + index * 0.07f)
                drawLine(brass.copy(alpha = alpha * 0.78f), Offset(x, h * 0.18f), Offset(x, h * 0.82f), 0.55.dp.toPx())
            }
            repeat(4) { index ->
                val y = h * (0.30f + index * 0.13f)
                drawLine(spirit.copy(alpha = alpha * 0.58f), Offset(w * 0.20f, y), Offset(w * 0.80f, y), 0.50.dp.toPx())
            }
        }
        PathArchitecturalMotif.VANGUARD -> {
            repeat(count.coerceAtMost(6)) { index ->
                val spread = w * (0.09f + index * 0.028f)
                val y = h * (0.26f + index * 0.07f)
                drawLine(brass, Offset(center.x - spread, y), Offset(center.x, y + h * 0.055f), 0.72.dp.toPx())
                drawLine(brass, Offset(center.x + spread, y), Offset(center.x, y + h * 0.055f), 0.72.dp.toPx())
            }
        }
        PathArchitecturalMotif.NOCTURNE -> {
            val radius = size.minDimension * (0.22f + strength * 0.04f)
            drawCircle(spirit, radius, center, style = Stroke(0.85.dp.toPx()))
            drawCircle(
                color = VeilPalette.Ink.copy(alpha = 0.38f + strength * 0.22f),
                radius = radius * 0.88f,
                center = center + Offset(radius * 0.24f, -radius * 0.05f)
            )
        }
        PathArchitecturalMotif.ARTIFICE -> {
            val inner = size.minDimension * 0.18f
            val outer = size.minDimension * 0.25f
            drawCircle(brass, inner, center, style = Stroke(0.78.dp.toPx()))
            repeat(count.coerceAtLeast(6)) { index ->
                val angle = (index.toDouble() / count.coerceAtLeast(1)) * PI * 2.0
                val unit = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(
                    if (index % 2 == 0) brass else spirit,
                    center + unit * inner,
                    center + unit * outer,
                    0.65.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun ObservatorySelection(
    node: com.veilreader.app.domain.MemoryAtlasNode,
    connections: List<MemoryAtlasEdge>,
    booksById: Map<String, com.veilreader.app.domain.MemoryAtlasNode>,
    onOpenBook: (Book) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Archive.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.76f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(
                    title = node.book.title,
                    subtitle = node.book.author,
                    imagePath = node.book.coverCachePath,
                    artifact = bookArtifactState(node.book),
                    modifier = Modifier
                        .width(62.dp)
                        .height(92.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "SELECTED CONSTELLATION",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.92.sp),
                        color = VeilPalette.Brass
                    )
                    Text(
                        node.book.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${node.connectionCount} recorded ${if (node.connectionCount == 1) "link" else "links"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }
            }

            if (connections.isEmpty()) {
                Text(
                    "This volume currently stands alone. Veil will not fabricate a relation merely to make the constellation denser.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist.copy(alpha = 0.78f)
                )
            } else {
                connections.take(6).forEach { edge ->
                    val otherId =
                        if (edge.fromBookId == node.book.id) edge.toBookId else edge.fromBookId
                    val other = booksById[otherId]?.book ?: return@forEach
                    ObservatoryConnectionRow(edge, other)
                }
            }

            Button(
                onClick = { onOpenBook(node.book) },
                enabled = node.book.isImported,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeilPalette.Brass,
                    contentColor = Color(0xFF17120A)
                ),
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
            ) {
                Text(if (node.book.isImported) "Enter volume" else "Source unavailable")
            }
        }
    }
}

@Composable
private fun ObservatoryConnectionRow(edge: MemoryAtlasEdge, other: Book) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.60f)),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                other.title,
                style = MaterialTheme.typography.titleSmall,
                color = VeilPalette.Moon,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "LINK ${edge.strength}/10",
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
        }
        Text(
            edge.reasons
                .sortedBy { it.ordinal }
                .joinToString(" · ") { relationLabel(it) },
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.66f)
        )
        if (MemoryRelationKind.PASSAGE_PATTERN in edge.reasons && edge.sharedPassageTerms.isNotEmpty()) {
            Text(
                "Shared preserved terms · " + edge.sharedPassageTerms.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist.copy(alpha = 0.52f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ObservatoryBookRow(
    node: com.veilreader.app.domain.MemoryAtlasNode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) {
            VeilPalette.DeepBrass.copy(alpha = 0.30f)
        } else {
            VeilPalette.Ink.copy(alpha = 0.26f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) VeilPalette.Brass.copy(alpha = 0.64f)
            else VeilPalette.BorderDark.copy(alpha = 0.58f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                node.book.title,
                style = MaterialTheme.typography.titleSmall,
                color = VeilPalette.Moon,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${node.connectionCount} ${if (node.connectionCount == 1) "LINK" else "LINKS"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.58f)
            )
        }
    }
}

@Composable
private fun ObservatoryEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Ink.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, VeilPalette.BorderDark.copy(alpha = 0.58f)),
        tonalElevation = 0.dp
    ) {
        Text(
            "The Observatory needs at least one volume before it can draw a private atlas.",
            modifier = Modifier.padding(VeilSpacing.lg),
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )
    }
}

private fun relationLabel(kind: MemoryRelationKind): String = when (kind) {
    MemoryRelationKind.AUTHOR -> "AUTHOR"
    MemoryRelationKind.SERIES -> "SERIES"
    MemoryRelationKind.COLLECTION -> "COLLECTION"
    MemoryRelationKind.PASSAGE_PATTERN -> "PRESERVED WORD PATTERN"
}

private fun atlasNodeOffset(
    index: Int,
    total: Int,
    width: Float,
    height: Float
): Offset {
    if (total <= 1) return Offset(width / 2f, height / 2f)
    val normalized = sqrt((index + 1f) / (total + 1f))
    val angle = index * 2.399963229728653 + 0.47
    val radiusX = width * 0.37f * normalized
    val radiusY = height * 0.34f * normalized
    return Offset(
        x = width / 2f + cos(angle).toFloat() * radiusX,
        y = height / 2f + sin(angle).toFloat() * radiusY
    )
}
