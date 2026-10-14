package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.data.AvatarAppearanceStore
import com.veilreader.app.data.WorldStageStore
import com.veilreader.app.domain.RagsToRichesAvatar
import com.veilreader.app.domain.RagsToRichesStage
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.deriveReadingHomeProgression
import com.veilreader.app.domain.previewRagsScene
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette

/** Draft choices remain in the saved instance state on rotation, not the durable game save. */
private val avatarDraftSaver = listSaver<RagsToRichesAvatar, Int>(
    save = { listOf(it.hairStyle, it.hairColor, it.skinTone, it.outfitStyle, it.accentColor) },
    restore = { values ->
        if (values.size != 5) null else RagsToRichesAvatar(
            hairStyle = values[0], hairColor = values[1], skinTone = values[2],
            outfitStyle = values[3], accentColor = values[4]
        ).safeForPreview()
    }
)

/**
 * First navigable avatar-creator vertical slice: persisted built-in cosmetics and real world tier.
 * Portrait is a lightweight Canvas fallback while a real 3D runtime is being benchmarked.
 */
@Composable
fun PremiumAvatarCreatorScreen(profile: ReaderProfile, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { AvatarAppearanceStore(context) }
    var savedAvatar by remember(store) { mutableStateOf(store.read()) }
    var avatar by rememberSaveable(store, stateSaver = avatarDraftSaver) {
        mutableStateOf(savedAvatar)
    }
    var notice by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val requestClose: () -> Unit = {
        if (avatar != savedAvatar) confirmDiscard = true else onClose()
    }
    BackHandler(enabled = !confirmDiscard, onBack = requestClose)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.avatar_discard_title)) },
            text = { Text(stringResource(R.string.avatar_discard_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onClose() }) {
                    Text(stringResource(R.string.avatar_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.avatar_keep_editing))
                }
            }
        )
    }

    val world = remember(profile.minutesRead, profile.booksFinished, avatar, context) {
        previewRagsScene(profile.minutesRead, profile.booksFinished, avatar,
            highestEarnedStage = WorldStageStore(context).read())
    }
    val progress = remember(profile.minutesRead, profile.booksFinished) {
        deriveReadingHomeProgression(profile.minutesRead, profile.booksFinished)
    }
    val stageLabel = when (world.stage) {
        RagsToRichesStage.WANDERER_CAMP -> R.string.premium_world_camp
        RagsToRichesStage.APPRENTICE_HUT -> R.string.premium_world_hut
        RagsToRichesStage.SCHOLAR_HOUSE -> R.string.premium_world_house
        RagsToRichesStage.KEEPER_CASTLE -> R.string.premium_world_castle
    }
    val hairOptions = listOf(R.string.avatar_hair_short, R.string.avatar_hair_long,
        R.string.avatar_hair_side, R.string.avatar_hair_braid)
    val hairColors = listOf(R.string.avatar_dark, R.string.avatar_silver,
        R.string.avatar_auburn, R.string.avatar_gold)
    val skinTones = listOf(R.string.avatar_skin_one, R.string.avatar_skin_two,
        R.string.avatar_skin_three, R.string.avatar_skin_four)
    val outfits = listOf(R.string.avatar_outfit_traveler, R.string.avatar_outfit_scholar,
        R.string.avatar_outfit_noble, R.string.avatar_outfit_keeper)
    val accents = listOf(R.string.avatar_accent_brass, R.string.avatar_accent_teal,
        R.string.avatar_accent_crimson, R.string.avatar_accent_cream)

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(VeilMaterials.RealmBackground, VeilMaterials.Depth, VeilMaterials.RealmBackground)
            )
        ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 730.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = requestClose, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.avatar_back), color = VeilPalette.Brass)
                }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.avatar_beta_2d), color = VeilPalette.Mist,
                    style = MaterialTheme.typography.labelSmall)
            }
            Text(stringResource(R.string.avatar_creator_heading),
                style = MaterialTheme.typography.headlineMedium, color = VeilPalette.Moon,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.avatar_creator_description),
                color = VeilPalette.Mist, style = MaterialTheme.typography.bodyMedium)

            Box(
                Modifier.fillMaxWidth().height(290.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(VeilMaterials.Surface)
                    .border(1.dp, VeilMaterials.Frame.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                PremiumAvatarPortrait(
                    hairStyle = avatar.hairStyle, hairColor = avatar.hairColor,
                    skinTone = avatar.skinTone, outfitStyle = avatar.outfitStyle,
                    accentColor = avatar.accentColor,
                    modifier = Modifier.fillMaxWidth().height(286.dp)
                )
            }

            AvatarOptionRow(stringResource(R.string.avatar_hair_style), hairOptions,
                avatar.hairStyle) { avatar = avatar.copy(hairStyle = it); notice = null }
            AvatarOptionRow(stringResource(R.string.avatar_hair_color), hairColors,
                avatar.hairColor) { avatar = avatar.copy(hairColor = it); notice = null }
            AvatarOptionRow(stringResource(R.string.avatar_skin_tone), skinTones,
                avatar.skinTone) { avatar = avatar.copy(skinTone = it); notice = null }
            AvatarOptionRow(stringResource(R.string.avatar_outfit), outfits,
                avatar.outfitStyle) { avatar = avatar.copy(outfitStyle = it); notice = null }
            AvatarOptionRow(stringResource(R.string.avatar_accent), accents,
                avatar.accentColor) { avatar = avatar.copy(accentColor = it); notice = null }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.avatar_world_tier, stringResource(stageLabel)),
                    color = VeilPalette.Brass, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.avatar_real_minutes, progress.activeMinutes),
                    color = VeilPalette.Mist, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { progress.stageProgress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = VeilPalette.Brass,
                    trackColor = VeilPalette.Mist.copy(alpha = 0.24f)
                )
            }

            Button(
                onClick = {
                    if (store.save(avatar)) {
                        savedAvatar = avatar.safeForPreview()
                        notice = R.string.avatar_saved
                    } else {
                        notice = R.string.avatar_save_failed
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VeilPalette.Brass,
                    contentColor = VeilPalette.Ink)
            ) { Text(stringResource(R.string.avatar_save)) }

            notice?.let { message ->
                Text(stringResource(message), color = VeilPalette.Moon,
                    style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = requestClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.avatar_exit))
            }
        }
    }
}

@Composable
private fun AvatarOptionRow(
    title: String,
    labels: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = VeilPalette.Moon)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { index, label ->
                FilterChip(selected = selected == index, onClick = { onSelect(index) },
                    label = { Text(stringResource(label)) })
            }
        }
    }
}
