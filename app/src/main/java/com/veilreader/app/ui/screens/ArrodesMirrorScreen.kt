package com.veilreader.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ArrodesFragment
import com.veilreader.app.domain.ArrodesFragmentKind
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.deriveArrodesFragments
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere

@Composable
fun ArrodesMirrorScreen(
    books: List<Book>,
    highlights: List<Highlight>,
    passageVisits: List<PassageVisit> = emptyList(),
    onOpenSource: (ArrodesFragment) -> Unit,
    onClose: () -> Unit
) {
    val fragments = remember(books, highlights, passageVisits) {
        deriveArrodesFragments(
            highlights = highlights,
            books = books,
            passageVisits = passageVisits
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var manifested by rememberSaveable { mutableStateOf(false) }
    val reducedMotion = LocalVeilReducedMotion.current
    val highContrast = LocalVeilHighContrast.current
    val safeIndex = if (fragments.isEmpty()) 0 else index % fragments.size
    val fragment = fragments.getOrNull(safeIndex)

    val revealAlpha by animateFloatAsState(
        targetValue = if (manifested && fragment != null) 1f else 0.06f,
        animationSpec = if (reducedMotion) snap() else tween(VeilMotion.SPATIAL_MS),
        label = "arrodes-fragment-alpha"
    )

    fun summonNext() {
        if (fragments.isEmpty()) return
        index = if (!manifested) safeIndex else (safeIndex + 1) % fragments.size
        manifested = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.THRESHOLD,
                seed = fragments.size * 41 + safeIndex,
                intensity = if (highContrast) 0.72f else 0.92f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = VeilSpacing.lg,
                    vertical = VeilSpacing.lg
                ),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        stringResource(R.string.mirror_eyebrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.mirror_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.mirror_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onClose) {
                    Text(stringResource(R.string.mirror_close))
                }
            }

            if (fragment == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        Modifier.padding(VeilSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        Text(
                            stringResource(R.string.mirror_empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.mirror_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val actionDescription = if (manifested) {
                    stringResource(
                        R.string.mirror_content_description_open,
                        fragment.book.title
                    )
                } else {
                    stringResource(R.string.mirror_content_description_awaken)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 430.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF080A0D),
                                    Color(0xFF11151B),
                                    Color(0xFF050608)
                                )
                            )
                        )
                        .semantics { contentDescription = actionDescription }
                        .clickable(role = Role.Button) {
                            if (manifested) onOpenSource(fragment)
                            else manifested = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val brass = if (highContrast) {
                            Color(0xFFFFD98A)
                        } else {
                            Color(0xFFD2A65F)
                        }
                        drawOval(
                            color = brass.copy(alpha = if (highContrast) 0.94f else 0.72f),
                            topLeft = Offset(size.width * 0.035f, size.height * 0.025f),
                            size = Size(size.width * 0.93f, size.height * 0.95f),
                            style = Stroke(width = if (highContrast) 5f else 3f)
                        )
                        drawOval(
                            color = Color.White.copy(alpha = if (highContrast) 0.20f else 0.08f),
                            topLeft = Offset(size.width * 0.10f, size.height * 0.08f),
                            size = Size(size.width * 0.80f, size.height * 0.84f),
                            style = Stroke(width = 1.5f)
                        )
                        if (manifested && !reducedMotion) {
                            repeat(96) { particle ->
                                val x = ((particle * 37 + safeIndex * 13) % 101) / 100f
                                val y = ((particle * 61 + safeIndex * 17) % 103) / 102f
                                val centrality = 1f - kotlin.math.abs(y - 0.5f) * 1.6f
                                drawCircle(
                                    color = brass.copy(
                                        alpha = 0.05f + centrality.coerceAtLeast(0f) * 0.14f
                                    ),
                                    radius = 1.2f + (particle % 3) * 0.55f,
                                    center = Offset(
                                        x = size.width * (0.13f + x * 0.74f),
                                        y = size.height * (0.16f + y * 0.68f)
                                    )
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 42.dp)
                            .graphicsLayer(alpha = revealAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                    ) {
                        Text(
                            text = if (fragment.kind == ArrodesFragmentKind.NOTE) {
                                stringResource(R.string.mirror_fragment_note)
                            } else {
                                stringResource(R.string.mirror_fragment_highlight)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass
                        )
                        Text(
                            text = "“${fragment.text}”",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fragment.book.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            stringResource(R.string.mirror_open_source_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (!manifested) {
                        Text(
                            stringResource(R.string.mirror_idle_hint),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 38.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
                ) {
                    Button(
                        onClick = ::summonNext,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            stringResource(
                                if (manifested) R.string.mirror_next else R.string.mirror_summon
                            )
                        )
                    }
                    if (manifested) {
                        OutlinedButton(
                            onClick = { onOpenSource(fragment) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.mirror_return_to_source))
                        }
                    }
                }
            }

            Spacer(Modifier.size(VeilSpacing.sm))
        }
    }
}
