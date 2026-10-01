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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ArrodesFragment
import com.veilreader.app.domain.ArrodesFragmentKind
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.PassageVisit
import com.veilreader.app.domain.deriveArrodesFragments
import com.veilreader.app.domain.orderArrodesFragmentsForSession
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
import kotlinx.coroutines.delay

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
    val sessionSeed = rememberSaveable {
        (System.currentTimeMillis() xor (fragments.size.toLong() shl 32)).toInt()
    }
    val sessionFragments = remember(fragments, sessionSeed) {
        orderArrodesFragmentsForSession(fragments, sessionSeed)
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var manifested by rememberSaveable { mutableStateOf(false) }
    var transitionToken by remember { mutableIntStateOf(0) }
    var transitioning by remember { mutableStateOf(false) }
    var contentVisible by remember(manifested) { mutableStateOf(manifested) }
    val reducedMotion = LocalVeilReducedMotion.current
    val highContrast = LocalVeilHighContrast.current
    val safeIndex = if (sessionFragments.isEmpty()) 0 else index % sessionFragments.size
    val fragment = sessionFragments.getOrNull(safeIndex)

    val revealAlpha by animateFloatAsState(
        targetValue = if (manifested && contentVisible && fragment != null) 1f else 0.04f,
        animationSpec = if (reducedMotion) snap() else tween(VeilMotion.SPATIAL_MS),
        label = "arrodes-fragment-alpha"
    )
    val coalescence by animateFloatAsState(
        targetValue = if (manifested && contentVisible && fragment != null) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(VeilMotion.SPATIAL_MS),
        label = "arrodes-glyph-coalescence"
    )

    LaunchedEffect(transitionToken, sessionFragments, reducedMotion) {
        if (transitionToken == 0) return@LaunchedEffect
        if (!reducedMotion) {
            delay((VeilMotion.SPATIAL_MS / 2L).coerceAtLeast(1L))
        }
        if (sessionFragments.isNotEmpty()) {
            index = (index + 1) % sessionFragments.size
        }
        contentVisible = true
        transitioning = false
    }

    fun summonNext() {
        if (sessionFragments.isEmpty() || transitioning) return
        if (!manifested) {
            manifested = true
            contentVisible = true
            return
        }
        if (reducedMotion) {
            index = (safeIndex + 1) % sessionFragments.size
            contentVisible = true
        } else {
            transitioning = true
            contentVisible = false
            transitionToken += 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.THRESHOLD,
                seed = sessionFragments.size * 41 + safeIndex,
                intensity = if (highContrast) 0.72f else 0.92f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
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
                    VeilMicroLabel(
                        text = stringResource(R.string.mirror_eyebrow),
                        color = if (highContrast) MaterialTheme.colorScheme.primary else VeilPalette.Brass,
                        strong = true
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
                        .clickable(
                            enabled = !transitioning,
                            role = Role.Button
                        ) {
                            if (manifested && contentVisible) {
                                onOpenSource(fragment)
                            } else {
                                manifested = true
                                contentVisible = true
                            }
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
                            val fragmentSeed = fragment.id.hashCode()
                            val characterCount = fragment.text.length.coerceAtLeast(1)
                            val lineCount = ((characterCount + 27) / 28).coerceIn(2, 7)
                            val columns = 22
                            repeat(132) { particle ->
                                val scatterX =
                                    ((particle * 37 + fragmentSeed * 13) and 0x7fffffff) % 1009 / 1008f
                                val scatterY =
                                    ((particle * 61 + fragmentSeed * 17) and 0x7fffffff) % 1013 / 1012f
                                val targetColumn =
                                    ((particle * 11 + fragmentSeed) and 0x7fffffff) % columns
                                val targetLine =
                                    ((particle * 7 + fragmentSeed) and 0x7fffffff) % lineCount
                                val targetX = 0.20f +
                                    (targetColumn / (columns - 1f)) * 0.60f
                                val targetY = if (lineCount == 1) {
                                    0.50f
                                } else {
                                    0.40f + (targetLine / (lineCount - 1f)) * 0.20f
                                }
                                val scatteredX = 0.11f + scatterX * 0.78f
                                val scatteredY = 0.14f + scatterY * 0.72f
                                val x = scatteredX + (targetX - scatteredX) * coalescence
                                val y = scatteredY + (targetY - scatteredY) * coalescence
                                val ashBias = 1f - kotlin.math.abs(scatterY - 0.5f) * 1.45f
                                drawCircle(
                                    color = brass.copy(
                                        alpha = (
                                            0.035f +
                                                ashBias.coerceAtLeast(0f) * 0.08f +
                                                coalescence * 0.10f
                                            ).coerceAtMost(0.22f)
                                    ),
                                    radius = 0.9f + (particle % 4) * 0.48f,
                                    center = Offset(
                                        x = size.width * x,
                                        y = size.height * y
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
                        val passageStyle = when {
                            fragment.text.length <= 160 -> MaterialTheme.typography.titleLarge
                            fragment.text.length <= 320 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.bodyLarge
                        }
                        Text(
                            text = stringResource(
                                R.string.mirror_fragment_quote,
                                fragment.text
                            ),
                            style = passageStyle,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
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
                        enabled = !transitioning,
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
                            enabled = contentVisible && !transitioning,
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
