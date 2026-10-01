package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.MemoryAtlas
import com.veilreader.app.domain.MemoryAtlasEdge
import com.veilreader.app.domain.MemoryRelationKind
import com.veilreader.app.domain.ReadingSessionSnapshot
import com.veilreader.app.domain.buildMemoryAtlas
import com.veilreader.app.ui.books.bookArtifactState as canonicalBookArtifactState
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.castleLayoutPolicyFor
import com.veilreader.app.ui.theme.grayfogAtmosphere
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ObservatoryScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    readingSessions: List<ReadingSessionSnapshot>,
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
                seed = atlas.nodes.size * 17 + atlas.edges.size * 7,
                intensity = 0.92f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = observatoryLayout.contentMaxWidthDp.dp)
            .fillMaxSize()
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
            Text(
                VeilBackLabel(stringResource(R.string.profile_stat_castle)),
                style = MaterialTheme.typography.labelMedium
            )
        }

        ScreenHeader(
            eyebrow = stringResource(R.string.observatory_header_eyebrow),
            title = stringResource(R.string.observatory_header_title),
            subtitle = stringResource(R.string.observatory_header_body)
        )

        ObservatoryAtlasPanel(
            atlas = atlas,
            selectedBookId = selectedBookId,
            panelHeightDp = observatoryLayout.observatoryHeightDp,
            onSelectBook = { selectedBookId = it }
        )

        Text(
            if (atlas.isolatedCount > 0) {
                stringResource(
                    R.string.observatory_summary_with_solitary,
                    atlas.nodes.size,
                    atlas.edges.size,
                    atlas.isolatedCount
                )
            } else {
                stringResource(
                    R.string.observatory_summary,
                    atlas.nodes.size,
                    atlas.edges.size
                )
            },
            style = MaterialTheme.typography.labelSmall,
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
            ObservatorySectionHeading(
                eyebrow = stringResource(R.string.observatory_index_eyebrow),
                title = stringResource(R.string.observatory_index_title),
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
            stringResource(R.string.observatory_links_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Mist.copy(alpha = 0.64f)
        )
    }
    }
}

@Composable
private fun ObservatoryAtlasPanel(
    atlas: MemoryAtlas,
    selectedBookId: String?,
    panelHeightDp: Float,
    onSelectBook: (String) -> Unit
) {
    val atlasDescription = stringResource(
        R.string.observatory_canvas_semantics,
        atlas.nodes.size,
        atlas.edges.size
    )
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
                    contentDescription = atlasDescription
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
                VeilMicroLabel(
                    text = stringResource(R.string.observatory_sky_empty),
                    strong = true
                )
                Text(
                    stringResource(R.string.observatory_sky_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeilPalette.Mist
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
                    artifact = canonicalBookArtifactState(node.book),
                    modifier = Modifier
                        .width(62.dp)
                        .height(92.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.observatory_selected_constellation)
                    )
                    Text(
                        node.book.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = VeilPalette.Moon,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(
                            if (node.connectionCount == 1) R.string.observatory_recorded_link_one
                            else R.string.observatory_recorded_links_many,
                            node.connectionCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeilPalette.Mist
                    )
                }
            }

            if (connections.isEmpty()) {
                Text(
                    stringResource(R.string.observatory_standalone_body),
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
                Text(
                    stringResource(
                        if (node.book.isImported) R.string.observatory_enter_volume
                        else R.string.observatory_source_unavailable
                    )
                )
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
                stringResource(R.string.observatory_link_strength, edge.strength),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
        }
        Text(
            relationLabelsText(edge.reasons),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.66f)
        )
        if (MemoryRelationKind.PASSAGE_PATTERN in edge.reasons && edge.sharedPassageTerms.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.observatory_shared_terms,
                    edge.sharedPassageTerms.joinToString(" · ")
                ),
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
                stringResource(
                    if (node.connectionCount == 1) R.string.observatory_link_count_one
                    else R.string.observatory_link_count_many,
                    node.connectionCount
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.58f)
            )
        }
    }
}

@Composable
private fun ObservatorySectionHeading(
    eyebrow: String,
    title: String,
    trailing: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VeilMicroLabel(
            text = eyebrow,
            strong = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon
            )
            trailing?.let { value ->
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.62f)
                )
            }
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
            stringResource(R.string.observatory_needs_volume),
            modifier = Modifier.padding(VeilSpacing.lg),
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )
    }
}

@Composable
private fun relationLabelsText(reasons: Set<MemoryRelationKind>): String {
    val author = stringResource(R.string.observatory_relation_author)
    val series = stringResource(R.string.observatory_relation_series)
    val collection = stringResource(R.string.observatory_relation_collection)
    val passagePattern = stringResource(R.string.observatory_relation_passage_pattern)
    return reasons
        .sortedBy { it.ordinal }
        .joinToString(" · ") { kind ->
            when (kind) {
                MemoryRelationKind.AUTHOR -> author
                MemoryRelationKind.SERIES -> series
                MemoryRelationKind.COLLECTION -> collection
                MemoryRelationKind.PASSAGE_PATTERN -> passagePattern
            }
        }
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
