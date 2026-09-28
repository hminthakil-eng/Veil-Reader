package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.StoryRelicRecord
import com.veilreader.app.domain.VeiledDiscoveryRecord
import com.veilreader.app.domain.mysteryChainSnapshot
import com.veilreader.app.domain.WorldMutationKind
import com.veilreader.app.domain.WorldMutationLedger
import com.veilreader.app.domain.WorldMutationRealm
import com.veilreader.app.ui.VeilMastheadMetaRow
import com.veilreader.app.ui.VeilRealmEmblem
import com.veilreader.app.ui.hallSharedBoundsKey
import com.veilreader.app.ui.veilSharedBounds
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
    @StringRes val nameRes: Int,
    val symbol: String,
    @StringRes val descriptionRes: Int
)

internal enum class RelicRarity {
    FOUNDATION,
    RESONANT,
    ASCENDANT,
    SOVEREIGN
}

@StringRes
private fun relicRarityLabelRes(rarity: RelicRarity): Int =
    when (rarity) {
        RelicRarity.FOUNDATION -> R.string.relic_rarity_foundation
        RelicRarity.RESONANT -> R.string.relic_rarity_resonant
        RelicRarity.ASCENDANT -> R.string.relic_rarity_ascendant
        RelicRarity.SOVEREIGN -> R.string.relic_rarity_sovereign
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
    @StringRes val nameRes: Int,
    val symbol: String,
    @StringRes val clueRes: Int,
    val rarity: RelicRarity
)

internal enum class RelicProvenance {
    READING_EVIDENCE,
    LEGACY_PROFILE,
    SOVEREIGN_COMPOSITE,
    SEALED
}

internal data class RelicUnlockState(
    val awakened: Boolean,
    val provenance: RelicProvenance,
    val evidenceCount: Int,
    val target: Int,
    @StringRes val evidenceLabelRes: Int
)

private fun mutationEvidenceCount(
    ledger: WorldMutationLedger,
    kind: WorldMutationKind
): Int =
    ledger.entries
        .filter { it.kind == kind }
        .maxOfOrNull { it.evidenceCount }
        ?: 0

internal fun relicUnlockState(
    relicId: String,
    profile: ReaderProfile,
    ledger: WorldMutationLedger
): RelicUnlockState {
    fun evidenceBacked(
        kind: WorldMutationKind,
        target: Int,
        @StringRes labelRes: Int,
        legacyAwakened: Boolean
    ): RelicUnlockState {
        val count = mutationEvidenceCount(ledger, kind)
        return when {
            count >= target -> RelicUnlockState(
                awakened = true,
                provenance = RelicProvenance.READING_EVIDENCE,
                evidenceCount = count,
                target = target,
                evidenceLabelRes = labelRes
            )
            legacyAwakened -> RelicUnlockState(
                awakened = true,
                provenance = RelicProvenance.LEGACY_PROFILE,
                evidenceCount = count,
                target = target,
                evidenceLabelRes = labelRes
            )
            else -> RelicUnlockState(
                awakened = false,
                provenance = RelicProvenance.SEALED,
                evidenceCount = count,
                target = target,
                evidenceLabelRes = labelRes
            )
        }
    }

    return when (relicId) {
        "ember_bookmark" -> evidenceBacked(
            kind = WorldMutationKind.FOUNDATION_WEIGHT,
            target = 3,
            labelRes = R.string.relic_evidence_sessions,
            legacyAwakened = profile.streakDays >= 3
        )
        "moonlit_lens" -> evidenceBacked(
            kind = WorldMutationKind.CONSTELLATION_WEB,
            target = 3,
            labelRes = R.string.relic_evidence_atlas_links,
            legacyAwakened = profile.minutesRead >= 180
        )
        "brass_quill" -> evidenceBacked(
            kind = WorldMutationKind.SCRIPTORIUM_LIGHT,
            target = 5,
            labelRes = R.string.relic_evidence_annotations,
            legacyAwakened = profile.pagesRead >= 500
        )
        "ivory_bookplate" -> evidenceBacked(
            kind = WorldMutationKind.COMPLETION_ALCOVES,
            target = 3,
            labelRes = R.string.relic_evidence_completed,
            legacyAwakened = profile.booksFinished >= 3
        )
        "astral_key" -> evidenceBacked(
            kind = WorldMutationKind.PATH_ASCENSION,
            target = 2,
            labelRes = R.string.relic_evidence_path,
            legacyAwakened = profile.rankIndex >= 2
        )
        "veil_crown" -> {
            val finalRank = profile.path.ranks.lastIndex.coerceAtLeast(0)
            val ready =
                profile.rankIndex >= finalRank &&
                    profile.earnedSigils.size >= 5
            RelicUnlockState(
                awakened = ready,
                provenance = if (ready) {
                    RelicProvenance.SOVEREIGN_COMPOSITE
                } else {
                    RelicProvenance.SEALED
                },
                evidenceCount =
                    profile.earnedSigils.size.coerceAtMost(5) +
                        if (profile.rankIndex >= finalRank) 1 else 0,
                target = 6,
                evidenceLabelRes = R.string.relic_evidence_sovereign
            )
        }
        else -> RelicUnlockState(
            awakened = false,
            provenance = RelicProvenance.SEALED,
            evidenceCount = 0,
            target = 1,
            evidenceLabelRes = R.string.relic_evidence_unknown
        )
    }
}

private data class BookplatePresentation(
    @StringRes val nameRes: Int,
    @StringRes val inscriptionRes: Int,
    val awakened: (ReaderProfile) -> Boolean
)

private val sigils = linkedMapOf(
    "first_hour" to SigilPresentation(
        R.string.sigil_quiet_hour_name,
        "◷",
        R.string.sigil_quiet_hour_desc
    ),
    "passage_keeper" to SigilPresentation(
        R.string.sigil_passage_keeper_name,
        "✦",
        R.string.sigil_passage_keeper_desc
    ),
    "seven_days" to SigilPresentation(
        R.string.sigil_seven_day_name,
        "◇",
        R.string.sigil_seven_day_desc
    ),
    "ten_tomes" to SigilPresentation(
        R.string.sigil_ten_tomes_name,
        "▥",
        R.string.sigil_ten_tomes_desc
    ),
    "first_threshold" to SigilPresentation(
        R.string.sigil_first_threshold_name,
        "✧",
        R.string.sigil_first_threshold_desc
    )
)

private val readingRelics = listOf(
    RelicPresentation(
        id = "ember_bookmark",
        nameRes = R.string.relic_ember_bookmark_name,
        symbol = "⌇",
        clueRes = R.string.relic_ember_bookmark_clue,
        rarity = relicRarityFor("ember_bookmark")
    ),
    RelicPresentation(
        id = "moonlit_lens",
        nameRes = R.string.relic_moonlit_lens_name,
        symbol = "◐",
        clueRes = R.string.relic_moonlit_lens_clue,
        rarity = relicRarityFor("moonlit_lens")
    ),
    RelicPresentation(
        id = "brass_quill",
        nameRes = R.string.relic_brass_quill_name,
        symbol = "✒",
        clueRes = R.string.relic_brass_quill_clue,
        rarity = relicRarityFor("brass_quill")
    ),
    RelicPresentation(
        id = "ivory_bookplate",
        nameRes = R.string.relic_ivory_bookplate_name,
        symbol = "▤",
        clueRes = R.string.relic_ivory_bookplate_clue,
        rarity = relicRarityFor("ivory_bookplate")
    ),
    RelicPresentation(
        id = "astral_key",
        nameRes = R.string.relic_astral_key_name,
        symbol = "⌘",
        clueRes = R.string.relic_astral_key_clue,
        rarity = relicRarityFor("astral_key")
    ),
    RelicPresentation(
        id = "veil_crown",
        nameRes = R.string.relic_veil_crown_name,
        symbol = "♜",
        clueRes = R.string.relic_veil_crown_clue,
        rarity = relicRarityFor("veil_crown")
    )
)

private val bookplates = listOf(
    BookplatePresentation(
        nameRes = R.string.bookplate_first_binding_name,
        inscriptionRes = R.string.bookplate_first_binding_text,
        awakened = { it.minutesRead >= 60 }
    ),
    BookplatePresentation(
        nameRes = R.string.bookplate_deep_shelf_name,
        inscriptionRes = R.string.bookplate_deep_shelf_text,
        awakened = { it.booksFinished >= 10 }
    ),
    BookplatePresentation(
        nameRes = R.string.bookplate_veilbound_name,
        inscriptionRes = R.string.bookplate_veilbound_text,
        awakened = { it.rankIndex >= it.path.ranks.lastIndex }
    )
)

@Composable
fun TreasuryScreen(
    profile: ReaderProfile,
    equippedSigil: String?,
    mutationLedger: WorldMutationLedger = WorldMutationLedger.EMPTY,
    storyRelics: List<StoryRelicRecord> = emptyList(),
    onEquip: (String?) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val equipped = equippedSigil?.let(sigils::get)
    val relicStates = readingRelics.associate { relic ->
        relic.id to relicUnlockState(
            relicId = relic.id,
            profile = profile,
            ledger = mutationLedger
        )
    }
    val awakenedRelics = relicStates.values.count { it.awakened }
    val storyRelicDisplays = storyRelicDisplayModels(storyRelics)
    val storyRelicCount = storyRelicDisplays.size
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
                seed =
                    profile.earnedSigils.size * 31 +
                        awakenedRelics * 11 +
                        storyRelicDisplays.sumOf { display ->
                            com.veilreader.app.domain.StoryRelicCatalog
                                .definitionFor(display.record.relicId)
                                ?.atmosphereSeedSalt
                                ?: 0
                        },
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
            alpha = 0.32f,
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
                        0.72f to VeilPalette.Ink.copy(alpha = 0.64f),
                        1f to VeilPalette.Ink
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

        CastleChamberGrandMasthead(
            realm = VeilRealm.CASTLE,
            sharedKey = hallSharedBoundsKey("treasury"),
            eyebrow = stringResource(R.string.treasury_eyebrow),
            title = stringResource(R.string.treasury_title),
            subtitle = stringResource(R.string.treasury_subtitle),
            trailing = buildList {
                add(stringResource(R.string.treasury_summary_reading, awakenedRelics))
                if (storyRelicCount > 0) {
                    add(stringResource(R.string.treasury_summary_story, storyRelicCount))
                }
                add(stringResource(R.string.treasury_summary_bookplates, awakenedBookplates))
            }.joinToString(" · ")
        )

        WorldMutationEcho(
            ledger = mutationLedger,
            realm = WorldMutationRealm.TREASURY,
            eyebrow = stringResource(R.string.treasury_consequence_eyebrow),
            title = stringResource(R.string.treasury_consequence_title)
        )

        if (storyRelicDisplays.isNotEmpty()) {
            ArchiveChamberHeading(
                eyebrow = stringResource(R.string.story_relic_section_eyebrow),
                title = stringResource(R.string.story_relic_section_title),
                trailing = storyRelicCount.toString()
            )
            Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
                storyRelicDisplays.forEachIndexed { index, display ->
                    VeilReveal(
                        delayMillis = 30 + index * 40,
                        distance = 8.dp
                    ) {
                        StoryRelicTreasuryCard(display)
                    }
                }
            }
        }

        VeilReveal(delayMillis = 40, distance = 10.dp) {
            TreasuryPedestal(
                equipped = equipped,
                onClear = { onEquip(null) }
            )
        }

        ArchiveChamberHeading(
            eyebrow = stringResource(R.string.treasury_sigils_eyebrow),
            title = stringResource(R.string.treasury_sigils_title),
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
            eyebrow = stringResource(R.string.treasury_relics_eyebrow),
            title = stringResource(R.string.treasury_relics_title),
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
                                unlockState = requireNotNull(relicStates[relic.id]),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        ArchiveChamberHeading(
            eyebrow = stringResource(R.string.treasury_bookplates_eyebrow),
            title = stringResource(R.string.treasury_bookplates_title),
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
private fun CastleChamberGrandMasthead(
    realm: VeilRealm,
    sharedKey: String,
    eyebrow: String,
    title: String,
    subtitle: String,
    trailing: String
) {
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .veilSharedBounds(sharedKey)
            .fillMaxWidth()
            .heightIn(min = if (fontScale > 1.35f) 342.dp else 270.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.64f)),
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Image(
            painter = painterResource(R.drawable.grayfog_threshold_v1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.08f),
                        0.42f to Color.Transparent,
                        1f to VeilPalette.Ink.copy(alpha = 0.97f)
                    )
                )
        )
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.90f
        )
        VeilRealmEmblem(
            realm = realm,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = VeilSpacing.lg)
                .size(142.dp),
            tint = (
                if (realm == VeilRealm.SANCTUM) VeilPalette.Spirit else VeilPalette.Brass
                ).copy(alpha = 0.22f)
        )
        VeilMastheadMetaRow(
            primary = eyebrow,
            secondary = trailing,
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
                title,
                style = MaterialTheme.typography.displaySmall,
                color = VeilPalette.Moon
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Moon.copy(alpha = 0.82f),
                modifier = Modifier.widthIn(max = 600.dp)
            )
            BrassRule(Modifier.width(158.dp), strong = true)
        }
    }
}

@Composable
private fun StoryRelicTreasuryCard(
    display: StoryRelicDisplayModel
) {
    val record = display.record
    val compactLayout =
        LocalConfiguration.current.screenWidthDp < 420 ||
            LocalDensity.current.fontScale > 1.25f
    val recorded = formatSanctumDate(record.recordedAtEpochMs)
    val modeLabel = stringResource(storyRelicModeRes(record.resolutionModeId))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = VeilPalette.RaisedIron.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.58f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        if (compactLayout) {
            Column(
                modifier = Modifier.padding(VeilSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                StoryRelicGlyphArtwork(
                    glyph = display.presentation.glyph,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(116.dp)
                )
                StoryRelicCopy(
                    display = display,
                    modeLabel = modeLabel,
                    recorded = recorded,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Box {
                StoryRelicGlyphArtwork(
                    glyph = display.presentation.glyph,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = VeilSpacing.lg)
                        .size(142.dp)
                )
                StoryRelicCopy(
                    display = display,
                    modeLabel = modeLabel,
                    recorded = recorded,
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .padding(VeilSpacing.lg)
                )
            }
        }
    }
}

@Composable
private fun StoryRelicCopy(
    display: StoryRelicDisplayModel,
    modeLabel: String,
    recorded: String,
    modifier: Modifier = Modifier
) {
    val record = display.record
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            stringResource(R.string.story_relic_section_eyebrow),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.20.sp),
            color = VeilPalette.Brass
        )
        Text(
            stringResource(display.presentation.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = VeilPalette.Moon
        )
        Text(
            stringResource(display.presentation.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )
        Text(
            display.routeVariantRes?.let { stringResource(it) }
                ?: stringResource(R.string.story_relic_unknown_variant),
            style = MaterialTheme.typography.bodySmall,
            color = VeilPalette.Moon.copy(alpha = 0.82f)
        )
        BrassRule(Modifier.width(112.dp))
        Text(
            stringResource(R.string.story_relic_provenance),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Spirit.copy(alpha = 0.84f)
        )
        Text(
            stringResource(
                R.string.story_relic_path,
                storyRelicPathLabel(record.pathIdAtAcquisition)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.72f)
        )
        Text(
            "$modeLabel · " + stringResource(
                R.string.story_relic_recorded,
                recorded
            ),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Mist.copy(alpha = 0.62f)
        )
    }
}

@Composable
private fun StoryRelicGlyphArtwork(
    glyph: StoryRelicGlyph,
    modifier: Modifier = Modifier
) {
    when (glyph) {
        StoryRelicGlyph.LANTERN -> LanternStoryRelicGlyph(modifier)
    }
}

@Composable
private fun LanternStoryRelicGlyph(
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val brass = VeilPalette.Brass
        val spirit = VeilPalette.Spirit
        val strong = 1.35.dp.toPx()
        val thin = 0.8.dp.toPx()

        drawCircle(
            brass.copy(alpha = 0.08f),
            size.minDimension * 0.46f,
            center
        )
        drawCircle(
            brass.copy(alpha = 0.42f),
            size.minDimension * 0.36f,
            center,
            style = Stroke(strong)
        )
        drawLine(
            brass.copy(alpha = 0.72f),
            Offset(center.x, size.height * 0.16f),
            Offset(center.x, size.height * 0.78f),
            strong,
            StrokeCap.Round
        )
        drawArc(
            brass.copy(alpha = 0.86f),
            startAngle = 202f,
            sweepAngle = 136f,
            useCenter = false,
            topLeft = Offset(size.width * 0.28f, size.height * 0.24f),
            size = Size(size.width * 0.44f, size.height * 0.50f),
            style = Stroke(strong)
        )
        drawCircle(
            spirit.copy(alpha = 0.52f),
            size.minDimension * 0.105f,
            Offset(center.x, size.height * 0.63f)
        )
        drawCircle(
            brass,
            2.2.dp.toPx(),
            Offset(center.x, size.height * 0.63f)
        )
        repeat(4) { index ->
            val y = size.height * (0.31f + index * 0.12f)
            drawLine(
                brass.copy(alpha = 0.16f),
                Offset(size.width * 0.20f, y),
                Offset(size.width * 0.80f, y),
                thin
            )
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
                stringResource(R.string.treasury_pedestal),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                color = VeilPalette.Brass
            )

            SigilPedestalSeal(
                symbol = equipped?.symbol,
                modifier = Modifier.size(104.dp)
            )

            if (equipped == null) {
                Text(
                    stringResource(R.string.treasury_no_sigil),
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(R.string.treasury_no_sigil_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    stringResource(equipped.nameRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = VeilPalette.Moon
                )
                Text(
                    stringResource(equipped.descriptionRes),
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
                    Text(stringResource(R.string.treasury_clear_pedestal), style = MaterialTheme.typography.labelMedium)
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
                    stringResource(presentation.nameRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (earned) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.48f)
                )
                Text(
                    if (earned) {
                        stringResource(presentation.descriptionRes)
                    } else {
                        stringResource(R.string.relic_condition_hidden)
                    },
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
                        stringResource(
                            if (equipped) R.string.relic_on_display else R.string.relic_display
                        ),
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
    unlockState: RelicUnlockState,
    modifier: Modifier = Modifier
) {
    val awakened = unlockState.awakened
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
                if (awakened) {
                    stringResource(relic.nameRes)
                } else {
                    stringResource(R.string.relic_uncatalogued)
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (awakened) VeilPalette.Moon else VeilPalette.Mist.copy(alpha = 0.48f)
            )
            Text(
                if (awakened) {
                    stringResource(
                        R.string.relic_awakened,
                        stringResource(relicRarityLabelRes(relic.rarity))
                    )
                } else {
                    stringResource(R.string.relic_rarity_veiled)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (awakened) VeilPalette.Brass else VeilPalette.Mist.copy(alpha = 0.46f),
                maxLines = 1
            )
            if (awakened) {
                Text(
                    when (unlockState.provenance) {
                        RelicProvenance.READING_EVIDENCE ->
                            stringResource(
                                R.string.relic_provenance_evidence,
                                unlockState.evidenceCount,
                                stringResource(unlockState.evidenceLabelRes)
                            )
                        RelicProvenance.LEGACY_PROFILE ->
                            stringResource(R.string.relic_provenance_legacy)
                        RelicProvenance.SOVEREIGN_COMPOSITE ->
                            stringResource(R.string.relic_provenance_sovereign)
                        RelicProvenance.SEALED ->
                            stringResource(R.string.relic_provenance_sealed)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Spirit.copy(alpha = 0.72f),
                    maxLines = 2
                )
            } else {
                Text(
                    stringResource(relic.clueRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist.copy(alpha = 0.58f),
                    maxLines = 4
                )
                Text(
                    stringResource(
                        R.string.relic_evidence_progress,
                        unlockState.evidenceCount,
                        unlockState.target,
                        stringResource(unlockState.evidenceLabelRes)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Mist.copy(alpha = 0.42f),
                    maxLines = 2
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
                    stringResource(R.string.bookplate_ex_libris),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = Color(0xFF6B5332)
                )
                Text(
                    stringResource(plate.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2A251F)
                )
                Text(
                    stringResource(plate.inscriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A4034),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(R.string.bookplate_sealed),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                    color = VeilPalette.Mist.copy(alpha = 0.44f)
                )
                Text(
                    stringResource(R.string.bookplate_missing),
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
    mutationLedger: WorldMutationLedger = WorldMutationLedger.EMPTY,
    highlightCount: Int = 0,
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
            alpha = if (sovereignReady) 0.36f else 0.28f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(740.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(800.dp)
                .background(
                    Brush.verticalGradient(
                        0f to VeilPalette.Ink.copy(alpha = 0.04f),
                        0.40f to Color.Transparent,
                        0.74f to VeilPalette.Ink.copy(alpha = if (sovereignReady) 0.54f else 0.68f),
                        1f to VeilPalette.Ink
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

        CastleChamberGrandMasthead(
            realm = VeilRealm.SANCTUM,
            sharedKey = hallSharedBoundsKey("sanctum"),
            eyebrow = stringResource(R.string.sanctum_eyebrow),
            title = localizedCastleTitle(castleTitle, profile),
            subtitle = stringResource(R.string.sanctum_subtitle),
            trailing = if (sovereignReady) {
                stringResource(R.string.sanctum_sovereign_seal)
            } else {
                stringResource(R.string.sanctum_discovery_count, knownDiscoveryCount)
            }
        )

        WorldMutationEcho(
            ledger = mutationLedger,
            realm = WorldMutationRealm.SANCTUM,
            durableOnly = true,
            eyebrow = stringResource(R.string.sanctum_consequence_eyebrow),
            title = stringResource(R.string.sanctum_consequence_title)
        )

        SanctumSealPanel(
            profile = profile,
            rankProgress = rankProgress,
            sigilProgress = sigilProgress,
            sovereignReady = sovereignReady
        )

        SanctumDiscoveryLedger(
            profile = profile,
            highlightCount = highlightCount,
            discoveries = discoveries
        )

        ArchiveChamberHeading(
            eyebrow = stringResource(R.string.sanctum_identity_eyebrow),
            title = stringResource(R.string.sanctum_identity_title),
            trailing = stringResource(R.string.sanctum_titles_recognized, availableTitles.size)
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
                            stringResource(
                                if (title == castleTitle) {
                                    R.string.sanctum_title_active
                                } else {
                                    R.string.sanctum_title_recognized
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (title == castleTitle) {
                                VeilPalette.Brass
                            } else {
                                VeilPalette.Mist.copy(alpha = 0.62f)
                            }
                        )
                        Text(
                            localizedCastleTitle(title, profile),
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
    profile: ReaderProfile,
    highlightCount: Int,
    discoveries: List<VeiledDiscoveryRecord>
) {
    val recordsById = discoveries.associateBy { it.id }
    val revealedKnown = veiledDiscoveryPresentations.count { it.id in recordsById }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        ArchiveChamberHeading(
            eyebrow = stringResource(R.string.sanctum_discoveries_eyebrow),
            title = stringResource(R.string.sanctum_discoveries_title),
            trailing = "$revealedKnown/${veiledDiscoveryPresentations.size}"
        )
        Text(
            stringResource(R.string.sanctum_discoveries_body),
            style = MaterialTheme.typography.bodyMedium,
            color = VeilPalette.Mist
        )

        veiledDiscoveryPresentations.forEachIndexed { index, presentation ->
            val record = recordsById[presentation.id]
            val chain = mysteryChainSnapshot(
                id = presentation.id,
                profile = profile,
                highlightCount = highlightCount
            )
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
                                if (record != null) {
                                    stringResource(presentation.titleRes)
                                } else {
                                    stringResource(R.string.sanctum_discovery_sealed, index + 1)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (record != null) VeilPalette.Moon
                                else VeilPalette.Mist.copy(alpha = 0.46f)
                            )
                            Text(
                                if (record != null) {
                                    record.recordedAtEpochMs?.let { timestamp ->
                                        stringResource(
                                            R.string.sanctum_discovery_recorded,
                                            formatSanctumDate(timestamp)
                                        )
                                    } ?: stringResource(R.string.sanctum_discovery_date_unknown)
                                } else {
                                    stringResource(
                                        R.string.sanctum_discovery_fragment,
                                        (chain?.visibleFragmentIndex ?: 0) + 1
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (record != null) VeilPalette.Spirit.copy(alpha = 0.82f)
                                else VeilPalette.Mist.copy(alpha = 0.46f)
                            )
                            if (record == null && chain != null) {
                                Text(
                                    localizedDiscoveryFragment(
                                        presentation = presentation,
                                        fragmentIndex = chain.visibleFragmentIndex,
                                        fallback = chain.visibleClue
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeilPalette.Mist.copy(alpha = 0.66f)
                                )
                            }
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
    val pathPresentation = localizedPathPresentation(profile.path, profile.rankIndex)
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
                stringResource(
                    if (sovereignReady) R.string.sanctum_seal_open
                    else R.string.sanctum_sixth_door
                ),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                color = VeilPalette.Brass
            )

            SanctumGlyph(
                awakened = sovereignReady,
                modifier = Modifier.size(98.dp)
            )

            Text(
                "${pathPresentation.name} · ${pathPresentation.rankName}",
                style = MaterialTheme.typography.titleLarge,
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center
            )

            DossierProgressLine(
                label = stringResource(R.string.sanctum_path_completion),
                progress = rankProgress,
                detail = "${profile.rankIndex + 1}/${profile.path.ranks.size}"
            )
            DossierProgressLine(
                label = stringResource(R.string.sanctum_core_sigils),
                progress = sigilProgress,
                detail = "${profile.earnedSigils.size.coerceAtMost(5)}/5"
            )

            Text(
                if (sovereignReady) {
                    stringResource(R.string.sanctum_ready_body)
                } else {
                    stringResource(R.string.sanctum_locked_body)
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
                    stringResource(R.string.sanctum_record_open_title)
                } else {
                    stringResource(R.string.sanctum_record_sealed_title)
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
                    stringResource(R.string.sanctum_record_open_body)
                } else {
                    stringResource(R.string.sanctum_record_sealed_body)
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
                    stringResource(R.string.sanctum_record_open_meta)
                } else {
                    stringResource(R.string.sanctum_record_sealed_meta)
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
