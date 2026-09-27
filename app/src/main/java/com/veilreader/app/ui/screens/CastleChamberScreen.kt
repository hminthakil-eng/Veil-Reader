package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.VeiledDiscoveryRecord
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.adaptiveClassFor
import com.veilreader.app.ui.theme.castleLayoutPolicyFor
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.currentVeilTemporalPhase
import java.text.DateFormat
import java.util.Date

private data class SigilPresentation(
    val name: String,
    val symbol: String,
    val description: String
)

internal enum class RelicRarity(val label: String) {
    FOUNDATION("FOUNDATION"),
    RESONANT("RESONANT"),
    ASCENDANT("ASCENDANT"),
    SOVEREIGN("SOVEREIGN")
}

internal fun relicRarityFor(relicId: String): RelicRarity =
    when (relicId) {
        "ember_bookmark" -> RelicRarity.FOUNDATION
        "moonlit_lens",
        "brass_quill",
        "ivory_bookplate" -> RelicRarity.RESONANT
        "astral_key" -> RelicRarity.ASCENDANT
        "veil_crown" -> RelicRarity.SOVEREIGN
        else -> RelicRarity.FOUNDATION
    }

private data class RelicPresentation(
    val id: String,
    val name: String,
    val symbol: String,
    val clue: String,
    val rarity: RelicRarity,
    val awakened: (ReaderProfile) -> Boolean
)

private data class BookplatePresentation(
    val name: String,
    val inscription: String,
    val awakened: (ReaderProfile) -> Boolean
)

private val sigils = linkedMapOf(
    "first_hour" to SigilPresentation(
        "Quiet Hour",
        "◷",
        "A full hour spent inside the written world."
    ),
    "passage_keeper" to SigilPresentation(
        "Passage Keeper",
        "✦",
        "Ten passages preserved from the books that changed you."
    ),
    "seven_days" to SigilPresentation(
        "Seven-Day Lantern",
        "◇",
        "A reading flame kept alive for seven days."
    ),
    "ten_tomes" to SigilPresentation(
        "Ten Tomes",
        "▥",
        "Ten completed books now stand in the Grand Library."
    ),
    "first_threshold" to SigilPresentation(
        "First Threshold",
        "✧",
        "The first true advancement along your chosen Path."
    )
)

private val readingRelics = listOf(
    RelicPresentation(
        id = "ember_bookmark",
        name = "Ember Bookmark",
        symbol = "⌇",
        clue = "Return often enough that the page begins to remember you.",
        rarity = relicRarityFor("ember_bookmark"),
        awakened = { it.streakDays >= 3 }
    ),
    RelicPresentation(
        id = "moonlit_lens",
        name = "Moonlit Lens",
        symbol = "◐",
        clue = "Spend three quiet hours beyond the first threshold of attention.",
        rarity = relicRarityFor("moonlit_lens"),
        awakened = { it.minutesRead >= 180 }
    ),
    RelicPresentation(
        id = "brass_quill",
        name = "Brass Quill",
        symbol = "✒",
        clue = "Turn five hundred pages and leave the mechanism warm.",
        rarity = relicRarityFor("brass_quill"),
        awakened = { it.pagesRead >= 500 }
    ),
    RelicPresentation(
        id = "ivory_bookplate",
        name = "Ivory Bookplate",
        symbol = "▤",
        clue = "Complete three volumes and the archive will grant a mark of ownership.",
        rarity = relicRarityFor("ivory_bookplate"),
        awakened = { it.booksFinished >= 3 }
    ),
    RelicPresentation(
        id = "astral_key",
        name = "Astral Key",
        symbol = "⌘",
        clue = "Cross two Path thresholds and listen for the lock that was not there before.",
        rarity = relicRarityFor("astral_key"),
        awakened = { it.rankIndex >= 2 }
    ),
    RelicPresentation(
        id = "veil_crown",
        name = "Veil Crown",
        symbol = "♜",
        clue = "Awaken the five core sigils and reach the final rank of your Path.",
        rarity = relicRarityFor("veil_crown"),
        awakened = {
            it.rankIndex >= it.path.ranks.lastIndex && it.earnedSigils.size >= 5
        }
    )
)

private val bookplates = listOf(
    BookplatePresentation(
        name = "First Binding",
        inscription = "This volume belongs to one who returned.",
        awakened = { it.minutesRead >= 60 }
    ),
    BookplatePresentation(
        name = "Deep Shelf",
        inscription = "A library becomes a place when finished books begin to gather weight.",
        awakened = { it.booksFinished >= 10 }
    ),
    BookplatePresentation(
        name = "Veilbound",
        inscription = "The reader crossed every threshold and carried the archive forward.",
        awakened = { it.rankIndex >= it.path.ranks.lastIndex }
    )
)

@Composable
fun TreasuryScreen(
    profile: ReaderProfile,
    equippedSigil: String?,
    onEquip: (String?) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val equipped = equippedSigil?.let(sigils::get)
    val awakenedRelics = readingRelics.count { it.awakened(profile) }
    val awakenedBookplates = bookplates.count { it.awakened(profile) }
    val treasuryAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val treasuryLayout = castleLayoutPolicyFor(treasuryAdaptiveClass)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.CASTLE,
                seed = profile.earnedSigils.size * 31 + awakenedRelics * 11,
                intensity = 0.90f,
                temporalPhase = currentVeilTemporalPhase()
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            alpha = 0.09f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(500.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(570.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = 0.54f),
                            VeilPalette.Ink
                        )
                    )
                )
        )

    Column(
        Modifier
            .fillMaxSize()
            .widthIn(max = treasuryLayout.contentMaxWidthDp.dp)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = treasuryLayout.horizontalPaddingDp.dp,
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

        ScreenHeader(
            eyebrow = "TREASURY · RELIC VAULT",
            title = "Relics of your reading life",
            subtitle = "Nothing here is bought. Every mark, relic, and bookplate is awakened by reading already stored on this device."
        )

        VeilReveal(delayMillis = 40, distance = 10.dp) {
            TreasuryPedestal(
                equipped = equipped,
                onClear = { onEquip(null) }
            )
        }

        ArchiveChamberHeading(
            eyebrow = "Core constellation",
            title = "Sigils",
            trailing = "${profile.earnedSigils.size.coerceAtMost(sigils.size)}/${sigils.size}"
        )

        sigils.entries.forEachIndexed { index, (id, presentation) ->
            val earned = id in profile.earnedSigils
            VeilReveal(
                delayMillis = 70 + index * 45,
                distance = 7.dp
            ) {
                SigilRelicRow(
                    id = id,
                    presentation = presentation,
                    earned = earned,
                    equipped = equippedSigil == id,
                    onEquip = onEquip
                )
            }
        }

        ArchiveChamberHeading(
            eyebrow = "Hidden cabinet",
            title = "Reading relics",
            trailing = "$awakenedRelics/${readingRelics.size}"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            readingRelics.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEachIndexed { itemIndex, relic ->
                        VeilReveal(
                            delayMillis = 80 + (rowIndex * 2 + itemIndex) * 45,
                            distance = 7.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            RelicCabinetCell(
                                relic = relic,
                                awakened = relic.awakened(profile),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        ArchiveChamberHeading(
            eyebrow = "Inside the cover",
            title = "Bookplates",
            trailing = "$awakenedBookplates/${bookplates.size}"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookplates.forEachIndexed { index, plate ->
                VeilReveal(
                    delayMillis = 70 + index * 55,
                    distance = 7.dp
                ) {
                    BookplateRecord(
                        plate = plate,
                        awakened = plate.awakened(profile)
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun TreasuryPedestal(
    equipped: SigilPresentation?,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 230.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17130E),
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
        TreasuryBackdrop(Modifier.matchParentSize())
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.36f
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeilSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "DISPLAY PEDESTAL",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                color = VeilPalette.Brass
            )

            SigilPedestalSeal(
                symbol = equipped?.symbol,
                modifier = Modifier.size(104.dp)
            )

            if (equipped == null) {
                Text(
                    "No sigil equipped",
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    "Choose an awakened sigil below. It changes only your Castle identity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    equipped.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    equipped.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onClear,
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.36f)),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Clear pedestal", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SigilRelicRow(
    id: String,
    presentation: SigilPresentation,
    earned: Boolean,
    equipped: Boolean,
    onEquip: (String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (earned) {
            VeilPalette.Archive.copy(alpha = 0.78f)
        } else {
            VeilPalette.Ink.copy(alpha = 0.34f)
        },
        border = BorderStroke(
            1.dp,
            if (earned) VeilPalette.Brass.copy(alpha = 0.34f)
            else VeilPalette.BorderDark.copy(alpha = 0.66f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (earned) VeilPalette.Brass.copy(alpha = 0.56f)
                            else VeilPalette.Mist.copy(alpha = 0.18f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (earned) presentation.symbol else "?",
                    fontSize = 21.sp,
                    color = if (earned) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.40f)
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    presentation.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (earned) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.48f)
                )
                Text(
                    if (earned) presentation.description else "The condition remains hidden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = if (earned) 0.82f else 0.46f),
                    maxLines = 2
                )
            }

            if (earned) {
                Button(
                    onClick = { onEquip(id) },
                    enabled = !equipped,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = Color(0xFF17120A),
                        disabledContainerColor = VeilPalette.DeepBrass.copy(alpha = 0.52f),
                        disabledContentColor = VeilPalette.Moon.copy(alpha = 0.68f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        if (equipped) "ON DISPLAY" else "DISPLAY",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RelicCabinetCell(
    relic: RelicPresentation,
    awakened: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                if (awakened) {
                    Brush.verticalGradient(
                        listOf(
                            VeilPalette.DeepBrass.copy(alpha = 0.22f),
                            VeilPalette.Archive.copy(alpha = 0.88f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            VeilPalette.Iron.copy(alpha = 0.28f),
                            VeilPalette.Ink.copy(alpha = 0.72f)
                        )
                    )
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (awakened) VeilPalette.Brass.copy(alpha = 0.34f)
                    else VeilPalette.BorderDark.copy(alpha = 0.70f)
                ),
                MaterialTheme.shapes.extraSmall
            )
            .padding(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                if (awakened) relic.symbol else "◇",
                fontSize = 24.sp,
                color = if (awakened) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.30f)
            )
            Text(
                if (awakened) relic.name else "Uncatalogued relic",
                style = MaterialTheme.typography.titleSmall,
                color = if (awakened) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.48f)
            )
            Text(
                if (awakened) {
                    "${relic.rarity.label} · AWAKENED"
                } else {
                    "RARITY VEILED"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (awakened) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.46f),
                maxLines = 1
            )
            if (!awakened) {
                Text(
                    relic.clue,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.58f),
                    maxLines = 4
                )
            }
        }
    }
}

@Composable
private fun BookplateRecord(
    plate: BookplatePresentation,
    awakened: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                if (awakened) VeilPalette.ReaderPaper.copy(alpha = 0.94f)
                else VeilPalette.Ink.copy(alpha = 0.30f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (awakened) VeilPalette.DeepBrass.copy(alpha = 0.78f)
                    else VeilPalette.BorderDark.copy(alpha = 0.62f)
                ),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (awakened) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "EX LIBRIS · VEIL READER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = Color(0xFF6B5332)
                )
                Text(
                    plate.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2A251F)
                )
                Text(
                    plate.inscription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A4034),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "SEALED BOOKPLATE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                    color = VeilPalette.Mist.copy(alpha = 0.44f)
                )
                Text(
                    "An inscription has not yet appeared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.52f)
                )
            }
        }
    }
}

@Composable
fun SanctumScreen(
    profile: ReaderProfile,
    castleTitle: String,
    availableTitles: List<String>,
    discoveries: List<VeiledDiscoveryRecord> = emptyList(),
    onSelectTitle: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val finalRank = profile.path.ranks.lastIndex
    val rankProgress = if (finalRank == 0) 1f else profile.rankIndex.toFloat() / finalRank.toFloat()
    val sigilProgress = profile.earnedSigils.size.coerceAtMost(5) / 5f
    val sovereignReady = profile.rankIndex >= finalRank && profile.earnedSigils.size >= 5
    val knownDiscoveryCount = discoveries.count { discoveryPresentationFor(it.id) != null }
    val sanctumAdaptiveClass = adaptiveClassFor(
        LocalConfiguration.current.screenWidthDp.toFloat()
    )
    val sanctumLayout = castleLayoutPolicyFor(sanctumAdaptiveClass)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .grayfogAtmosphere(
                realm = VeilRealm.SANCTUM,
                seed = profile.rankIndex * 43 +
                    profile.earnedSigils.size * 13 +
                    knownDiscoveryCount * 17,
                intensity = if (sovereignReady) {
                    1f
                } else {
                    (0.72f + knownDiscoveryCount * 0.025f).coerceAtMost(0.90f)
                },
                temporalPhase = currentVeilTemporalPhase()
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = if (sovereignReady) 0.12f else 0.06f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(520.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(600.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            VeilPalette.Ink.copy(alpha = if (sovereignReady) 0.46f else 0.62f),
                            VeilPalette.Ink
                        )
                    )
                )
        )

    Column(
        Modifier
            .fillMaxSize()
            .widthIn(max = sanctumLayout.contentMaxWidthDp.dp)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = sanctumLayout.horizontalPaddingDp.dp,
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

        ScreenHeader(
            eyebrow = "INNER SANCTUM · DEEPEST RECORD",
            title = castleTitle,
            subtitle = "The Sanctum records thresholds crossed, constellations completed, and titles the Castle considers permanent."
        )

        SanctumSealPanel(
            profile = profile,
            rankProgress = rankProgress,
            sigilProgress = sigilProgress,
            sovereignReady = sovereignReady
        )

        SanctumDiscoveryLedger(discoveries)

        ArchiveChamberHeading(
            eyebrow = "Permanent identity",
            title = "Castle title",
            trailing = "${availableTitles.size} recognized"
        )

        availableTitles.forEachIndexed { index, title ->
            VeilReveal(
                delayMillis = 50 + index * 35,
                distance = 6.dp
            ) {
                OutlinedButton(
                    onClick = { onSelectTitle(title) },
                    enabled = title != castleTitle,
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(
                        1.dp,
                        if (title == castleTitle) {
                            VeilPalette.Brass.copy(alpha = 0.56f)
                        } else {
                            VeilPalette.BorderDark.copy(alpha = 0.72f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            if (title == castleTitle) "ACTIVE TITLE" else "RECOGNIZED TITLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (title == castleTitle) {
                                VeilPalette.Brass
                            } else {
                                VeilPalette.Mist.copy(alpha = 0.62f)
                            }
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        HiddenSanctumRecord(sovereignReady = sovereignReady)
    }
    }
}

@Composable
private fun SanctumDiscoveryLedger(
    discoveries: List<VeiledDiscoveryRecord>
) {
    val recordsById = discoveries.associateBy { it.id }
    val revealedKnown = veiledDiscoveryPresentations.count { it.id in recordsById }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        ArchiveChamberHeading(
            eyebrow = "Permanent ledger",
            title = "Veiled discoveries",
            trailing = "$revealedKnown/${veiledDiscoveryPresentations.size}"
        )
        Text(
            "The Sanctum reads only durable discovery records. Once revealed, a fragment does not vanish when a temporary signal changes.",
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )

        veiledDiscoveryPresentations.forEachIndexed { index, presentation ->
            val record = recordsById[presentation.id]
            VeilReveal(delayMillis = 55 + index * 35, distance = 6.dp) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (record != null) {
                        VeilPalette.DeepBrass.copy(alpha = 0.18f)
                    } else {
                        VeilPalette.Ink.copy(alpha = 0.30f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (record != null) VeilPalette.Brass.copy(alpha = 0.40f)
                        else VeilPalette.BorderDark.copy(alpha = 0.64f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (record != null) VeilPalette.Brass.copy(alpha = 0.58f)
                                        else VeilPalette.Mist.copy(alpha = 0.18f)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (record != null) presentation.symbol else "◇",
                                fontSize = 20.sp,
                                color = if (record != null) VeilPalette.Brass
                                else VeilPalette.Mist.copy(alpha = 0.32f)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                if (record != null) presentation.title
                                else "Sealed discovery ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (record != null) VeilPalette.Moon
                                else VeilPalette.Mist.copy(alpha = 0.46f)
                            )
                            Text(
                                if (record != null) {
                                    record.recordedAtEpochMs?.let { timestamp ->
                                        "PERMANENT · RECORDED ${formatSanctumDate(timestamp)}"
                                    } ?: "PERMANENT · RECORD DATE UNKNOWN"
                                } else {
                                    "CONDITION VEILED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (record != null) VeilPalette.Spirit.copy(alpha = 0.82f)
                                else VeilPalette.Mist.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSanctumDate(epochMs: Long): String =
    if (epochMs <= 0L) {
        "DATE UNKNOWN"
    } else {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs)).uppercase()
    }

@Composable
private fun SanctumSealPanel(
    profile: ReaderProfile,
    rankProgress: Float,
    sigilProgress: Float,
    sovereignReady: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF111017),
                        VeilPalette.Archive,
                        VeilPalette.Ink
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (sovereignReady) VeilPalette.Brass.copy(alpha = 0.62f)
                    else VeilPalette.BorderDark.copy(alpha = 0.82f)
                ),
                MaterialTheme.shapes.small
            )
    ) {
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = if (sovereignReady) 0.46f else 0.24f
        )
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.40f)
            val brass = VeilPalette.Brass
            val radius = size.minDimension * 0.28f

            drawCircle(
                brass.copy(alpha = if (sovereignReady) 0.12f else 0.045f),
                radius,
                center,
                style = Stroke(1.2.dp.toPx())
            )
            drawCircle(
                brass.copy(alpha = if (sovereignReady) 0.08f else 0.030f),
                radius * 0.72f,
                center,
                style = Stroke(1.dp.toPx())
            )

            repeat(6) { index ->
                val angle = Math.toRadians(-90.0 + index * 60.0)
                val r1 = radius * 0.76f
                val r2 = radius * 1.05f
                drawLine(
                    brass.copy(alpha = if (sovereignReady) 0.20f else 0.07f),
                    Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * r1,
                        center.y + kotlin.math.sin(angle).toFloat() * r1
                    ),
                    Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * r2,
                        center.y + kotlin.math.sin(angle).toFloat() * r2
                    ),
                    1.dp.toPx(),
                    StrokeCap.Round
                )
            }
        }

        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (sovereignReady) "THE SEAL IS OPEN" else "THE SIXTH DOOR",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                color = VeilPalette.Brass
            )

            SanctumGlyph(
                awakened = sovereignReady,
                modifier = Modifier.size(98.dp)
            )

            Text(
                "${profile.path.name} · ${profile.rankName}",
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center
            )

            DossierProgressLine(
                label = "PATH COMPLETION",
                progress = rankProgress,
                detail = "${profile.rankIndex + 1}/${profile.path.ranks.size}"
            )
            DossierProgressLine(
                label = "CORE SIGILS",
                progress = sigilProgress,
                detail = "${profile.earnedSigils.size.coerceAtMost(5)}/5"
            )

            Text(
                if (sovereignReady) {
                    "The Castle recognizes a complete Path and a full core constellation. A hidden record has surfaced below."
                } else {
                    "Reach the final Path rank and awaken all five core sigils. The remaining door has no visible handle."
                },
                style = MaterialTheme.typography.bodySmall,
                color = VeilPalette.Mist,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DossierProgressLine(
    label: String,
    progress: Float,
    detail: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.90.sp),
                color = VeilPalette.Mist.copy(alpha = 0.72f)
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = VeilPalette.Brass,
            trackColor = VeilPalette.Moon.copy(alpha = 0.07f),
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun HiddenSanctumRecord(sovereignReady: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (sovereignReady) 164.dp else 112.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(
                Brush.verticalGradient(
                    if (sovereignReady) {
                        listOf(
                            VeilPalette.DeepBrass.copy(alpha = 0.18f),
                            VeilPalette.Archive.copy(alpha = 0.72f),
                            VeilPalette.Ink.copy(alpha = 0.90f)
                        )
                    } else {
                        listOf(
                            VeilPalette.Archive.copy(alpha = 0.34f),
                            VeilPalette.Ink.copy(alpha = 0.78f)
                        )
                    }
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (sovereignReady) VeilPalette.Brass.copy(alpha = 0.42f)
                    else VeilPalette.BorderDark.copy(alpha = 0.62f)
                ),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.84f, size.height * 0.50f)
            repeat(3) { index ->
                drawCircle(
                    color = VeilPalette.Brass.copy(
                        alpha = if (sovereignReady) {
                            0.045f + index * 0.018f
                        } else {
                            0.014f + index * 0.006f
                        }
                    ),
                    radius = size.minDimension * (0.18f + index * 0.09f),
                    center = center,
                    style = Stroke(1.dp.toPx())
                )
            }
            if (sovereignReady) {
                repeat(6) { index ->
                    val angle = Math.toRadians(-90.0 + index * 60.0)
                    val inner = size.minDimension * 0.15f
                    val outer = size.minDimension * 0.34f
                    drawLine(
                        VeilPalette.Brass.copy(alpha = 0.085f),
                        Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * inner,
                            center.y + kotlin.math.sin(angle).toFloat() * inner
                        ),
                        Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * outer,
                            center.y + kotlin.math.sin(angle).toFloat() * outer
                        ),
                        0.8.dp.toPx()
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                if (sovereignReady) {
                    "SOVEREIGN RECORD · THE STAR BETWEEN SHELVES"
                } else {
                    "SOVEREIGN RECORD · SEALED"
                },
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.10.sp),
                color = if (sovereignReady) {
                    VeilPalette.Brass
                } else {
                    VeilPalette.Mist.copy(alpha = 0.42f)
                }
            )
            Text(
                if (sovereignReady) {
                    "There was never a final shelf. Only another threshold hidden behind the act of returning."
                } else {
                    "A permanent inscription is present here, but its condition has not yet been satisfied."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (sovereignReady) {
                    VeilPalette.Moon
                } else {
                    VeilPalette.Mist.copy(alpha = 0.48f)
                }
            )
            Text(
                if (sovereignReady) {
                    "PERMANENT · NON-CONSUMABLE · LOCAL RECORD"
                } else {
                    "NOT YET RECOGNIZED"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (sovereignReady) {
                    VeilPalette.Spirit.copy(alpha = 0.76f)
                } else {
                    VeilPalette.Mist.copy(alpha = 0.36f)
                }
            )
        }
    }
}

@Composable
internal fun ArchiveChamberHeading(
    eyebrow: String,
    title: String,
    trailing: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.25.sp),
                color = VeilPalette.Brass
            )
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon
            )
        }
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Mist
            )
        }
    }
}

@Composable
private fun TreasuryBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.42f)

        drawCircle(
            color = VeilPalette.Brass.copy(alpha = 0.04f),
            radius = size.minDimension * 0.31f,
            center = center,
            style = Stroke(1.dp.toPx())
        )

        repeat(5) { index ->
            val y = h * (0.18f + index * 0.15f)
            drawLine(
                color = VeilPalette.StrongBorderDark.copy(alpha = 0.06f),
                start = Offset(w * 0.08f, y),
                end = Offset(w * 0.92f, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        drawRoundRect(
            color = VeilPalette.DeepBrass.copy(alpha = 0.08f),
            topLeft = Offset(w * 0.34f, h * 0.72f),
            size = Size(w * 0.32f, h * 0.11f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
    }
}

@Composable
private fun SigilPedestalSeal(
    symbol: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val brass = VeilPalette.Brass
            val stroke = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)

            drawCircle(brass.copy(alpha = 0.62f), size.minDimension * 0.43f, center, style = stroke)
            drawCircle(brass.copy(alpha = 0.24f), size.minDimension * 0.31f, center, style = stroke)

            repeat(8) { index ->
                val angle = Math.toRadians(index * 45.0 - 90.0)
                val r1 = size.minDimension * 0.34f
                val r2 = size.minDimension * 0.48f
                drawLine(
                    brass.copy(alpha = 0.36f),
                    Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * r1,
                        center.y + kotlin.math.sin(angle).toFloat() * r1
                    ),
                    Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * r2,
                        center.y + kotlin.math.sin(angle).toFloat() * r2
                    ),
                    stroke.width,
                    StrokeCap.Round
                )
            }
        }

        Text(
            symbol ?: "◇",
            fontSize = 34.sp,
            color = if (symbol == null) VeilPalette.Mist.copy(alpha = 0.44f) else VeilPalette.Brass
        )
    }
}

@Composable
private fun SanctumGlyph(
    awakened: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val tint = if (awakened) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.42f)
        val stroke = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)

        val diamond = Path().apply {
            moveTo(c.x, size.height * 0.08f)
            lineTo(size.width * 0.88f, c.y)
            lineTo(c.x, size.height * 0.92f)
            lineTo(size.width * 0.12f, c.y)
            close()
        }
        drawPath(diamond, tint.copy(alpha = if (awakened) 0.72f else 0.34f), style = stroke)
        drawCircle(tint.copy(alpha = if (awakened) 0.70f else 0.28f), size.minDimension * 0.18f, c, style = stroke)
        drawCircle(tint.copy(alpha = if (awakened) 0.92f else 0.42f), 2.4.dp.toPx(), c)
    }
}
