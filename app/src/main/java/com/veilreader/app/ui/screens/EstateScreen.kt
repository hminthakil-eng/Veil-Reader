package com.veilreader.app.ui.screens

import android.animation.ValueAnimator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.*

/** The estate is a home between reading sessions; no campaign UI is added to the book. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstateScreen(
    profile: ReaderProfile,
    estate: EstateState,
    onBuild: () -> Boolean,
    onClaim: (String) -> Boolean,
    onDesign: (EstateState) -> Boolean,
    chambers: @Composable () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("Home") }
    var designing by rememberSaveable { mutableStateOf(false) }
    var visitorId by rememberSaveable { mutableStateOf<String?>(null) }
    var loreId by rememberSaveable { mutableStateOf<String?>(null) }
    var showChambers by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val minutes = profile.minutesRead
    val balance = EstateCampaign.balance(estate, minutes)
    val stage = EstateCampaign.stages[estate.stage]
    val next = EstateCampaign.nextStage(estate)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item("header") {
            ScreenHeader("Your Castle", estate.name, "A home built from the books you live in.")
        }
        item("scene") {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column {
                    EstateScene(estate, reduceMotion = !ValueAnimator.areAnimatorsEnabled())
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stage.name, style = MaterialTheme.typography.headlineMedium)
                        Text(stage.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EstateStat("$balance", "lorestones")
                            EstateStat(timeRead(minutes), "spent reading")
                            EstateStat("${estate.stage + 1} / ${EstateCampaign.stages.size}", "home stages")
                        }
                    }
                }
            }
        }
        item("tabs") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Home", "Quests", "Residents", "Lore").forEach { label ->
                    FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        if (message != null) item("message") {
            Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = .12f), shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message.orEmpty(), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { message = null }) { Text("Dismiss") }
                }
            }
        }
        when (tab) {
            "Home" -> {
                item("construction") {
                    EstatePanel {
                        if (next != null) {
                            EstateHeading("Your next chapter")
                            Text(next.name, style = MaterialTheme.typography.headlineSmall)
                            Text(next.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val progress = (minutes.toFloat() / next.minutes).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Text("${timeRead(minutes.coerceAtMost(next.minutes))} / ${timeRead(next.minutes)} reading · ${next.cost} lorestones", style = MaterialTheme.typography.bodyMedium)
                            val ready = minutes >= next.minutes && balance >= next.cost
                            Button(onClick = {
                                message = if (onBuild()) "Welcome home. Your ${next.name.lowercase()} is ready." else "Keep reading to gather the remaining materials."
                            }, enabled = ready, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Text(if (ready) "Build ${next.name.lowercase()} · ${next.cost}" else "${(next.minutes - minutes).coerceAtLeast(0)} reading minutes to go")
                            }
                            Text("One active reading minute earns one lorestone. Story chapters give a little extra. No countdowns, no decay.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            EstateHeading("You built a sanctuary")
                            Text("The citadel is yours. Keep discovering stories, visit your residents, and make it feel like home.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { designing = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Design your home") }
                    }
                }
                item("next-quest") {
                    val chapter = EstateCampaign.chapters.firstOrNull { it.id !in estate.claimedChapters }
                    if (chapter != null) {
                        EstatePanel {
                            EstateHeading("A story is waiting")
                            Text(chapter.title, style = MaterialTheme.typography.titleLarge)
                            Text(chapter.request, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { tab = "Quests" }) { Text("Visit the quest journal") }
                        }
                    }
                }
                item("chambers-toggle") {
                    OutlinedButton(onClick = { showChambers = !showChambers }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(if (showChambers) "Close Path chambers" else "Explore Path chambers")
                    }
                }
                if (showChambers) item("chambers") { chambers() }
            }
            "Quests" -> {
                item("quest-intro") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EstateHeading("The Lantern Road")
                        Text("${estate.claimedChapters.size} of ${EstateCampaign.chapters.size} chapters discovered. Read any EPUB or PDF at your own pace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(EstateCampaign.chapters, key = { it.id }) { chapter ->
                    val claimed = chapter.id in estate.claimedChapters
                    val ready = EstateCampaign.chapterReady(estate, minutes, chapter)
                    val index = EstateCampaign.chapters.indexOf(chapter)
                    val previousDone = index == 0 || EstateCampaign.chapters[index - 1].id in estate.claimedChapters
                    EstatePanel {
                        Text("CHAPTER ${index + 1} · ${EstateCampaign.residents.first { it.id == chapter.npcId }.name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(chapter.title, style = MaterialTheme.typography.titleLarge)
                        Text(chapter.request, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!claimed) {
                            LinearProgressIndicator(progress = { (minutes.toFloat() / chapter.minutes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            Text("${timeRead(minutes.coerceAtMost(chapter.minutes))} / ${timeRead(chapter.minutes)} · +${chapter.reward} lorestones", style = MaterialTheme.typography.bodyMedium)
                        }
                        when {
                            claimed -> TextButton(onClick = { loreId = chapter.id }) { Text("Completed · read the story") }
                            ready -> Button(onClick = { if (onClaim(chapter.id)) loreId = chapter.id }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Complete chapter · +${chapter.reward}") }
                            !previousDone -> Text("Continue the previous chapter first", style = MaterialTheme.typography.labelLarge)
                            estate.stage < chapter.stage -> Text("Build the ${EstateCampaign.stages[chapter.stage].name.lowercase()} to continue", style = MaterialTheme.typography.labelLarge)
                            else -> Text("Progress saves automatically as you read", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            "Residents" -> {
                item("residents-intro") { EstateHeading("People make a place") }
                items(EstateCampaign.residents, key = { it.id }) { resident ->
                    val unlocked = estate.stage >= resident.stage
                    EstatePanel {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ResidentPortrait(resident.id, resident.name, unlocked)
                            Column(Modifier.weight(1f)) {
                                Text(resident.name, style = MaterialTheme.typography.titleLarge)
                                Text(resident.role, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (unlocked) {
                            Text(resident.greeting, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { visitorId = resident.id }) { Text("Talk with ${resident.name}") }
                        } else {
                            Text("Arrives at the ${EstateCampaign.stages[resident.stage].name.lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            "Lore" -> {
                item("lore-intro") {
                    EstateHeading("The valley remembers")
                    Text("Original stories from the Lantern Road. New entries appear after you complete a chapter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val unlocked = EstateCampaign.chapters.filter { it.id in estate.claimedChapters }
                if (unlocked.isEmpty()) item("empty-lore") {
                    EstatePanel {
                        Text("Your first story begins with a borrowed lamp.", style = MaterialTheme.typography.titleLarge)
                        Text("Read for five active minutes, then complete A light in the rain in your quest journal.")
                        TextButton(onClick = { tab = "Quests" }) { Text("Open quest journal") }
                    }
                }
                items(unlocked, key = { it.id }) { chapter ->
                    EstatePanel {
                        Text(chapter.loreTitle, style = MaterialTheme.typography.titleLarge)
                        Text(chapter.lore, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (designing) {
        ModalBottomSheet(onDismissRequest = { designing = false }) {
            EstateDesigner(estate, onSave = { draft -> if (onDesign(draft)) designing = false })
        }
    }
    val resident = EstateCampaign.residents.firstOrNull { it.id == visitorId && estate.stage >= it.stage }
    if (resident != null) {
        var subject by remember(resident.id) { mutableStateOf("greeting") }
        ModalBottomSheet(onDismissRequest = { visitorId = null }) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ResidentPortrait(resident.id, resident.name, true)
                    Column { Text(resident.name, style = MaterialTheme.typography.headlineMedium); Text(resident.role, color = MaterialTheme.colorScheme.secondary) }
                }
                Text(EstateCampaign.dialogue(resident, estate, subject), style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = { subject = "home" }, modifier = Modifier.fillMaxWidth()) { Text("Tell me about our home") }
                OutlinedButton(onClick = { subject = "mystery" }, modifier = Modifier.fillMaxWidth()) { Text("What do you know of the valley?") }
                TextButton(onClick = { visitorId = null; tab = "Quests" }, modifier = Modifier.fillMaxWidth()) { Text("Look at the quest journal") }
                Button(onClick = { visitorId = null }, modifier = Modifier.fillMaxWidth()) { Text("Until next time") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    EstateCampaign.chapters.firstOrNull { it.id == loreId && it.id in estate.claimedChapters }?.let { chapter ->
        AlertDialog(onDismissRequest = { loreId = null }, title = { Text(chapter.loreTitle) },
            text = { Text(chapter.lore, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { loreId = null }) { Text("Keep this story") } })
    }
}

@Composable
private fun EstateDesigner(estate: EstateState, onSave: (EstateState) -> Unit) {
    var name by rememberSaveable { mutableStateOf(estate.name) }
    var palette by remember { mutableStateOf(estate.palette) }
    var grounds by remember { mutableStateOf(estate.grounds) }
    var sky by remember { mutableStateOf(estate.sky) }
    val draft = estate.copy(name = name, palette = palette, grounds = grounds, sky = sky)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EstateHeading("Make it yours")
        EstateScene(draft, Modifier.clip(MaterialTheme.shapes.large), !ValueAnimator.areAnimatorsEnabled())
        OutlinedTextField(value = name, onValueChange = { name = it.take(32) }, label = { Text("Home name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Stone & timber", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EstatePalette.entries.forEach { value -> FilterChip(palette == value, { palette = value }, label = { Text(value.label) }, modifier = Modifier.heightIn(min = 48.dp)) }
        }
        Text("The grounds", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EstateGrounds.entries.forEach { value ->
                val unlocked = EstateCampaign.groundsUnlocked(value, estate.stage)
                FilterChip(grounds == value, { grounds = value }, enabled = unlocked, label = { Text(value.label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Text("Lanterns unlock at the cottage; the fountain at the manor. All unlocked designs are free to change.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Time & atmosphere", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EstateSky.entries.forEach { value -> FilterChip(sky == value, { sky = value }, label = { Text(value.label) }, modifier = Modifier.heightIn(min = 48.dp)) }
        }
        Button(onClick = { onSave(draft) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save design") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResidentPortrait(id: String, name: String, unlocked: Boolean) {
    val color = when (id) { "mara" -> Color(0xFFD8BD7C); "oren" -> Color(0xFFC9A69B); "sable" -> Color(0xFFA1BACF); "iona" -> Color(0xFFA4C7AF); else -> Color(0xFFC1A9D1) }
    Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = color.copy(alpha = if (unlocked) .18f else .06f), border = BorderStroke(1.dp, color.copy(alpha = .3f))) {
        Box(contentAlignment = Alignment.Center) { Text(name.take(1), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun EstatePanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
private fun EstateHeading(text: String) { Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge) }
@Composable
private fun EstateStat(value: String, label: String) {
    Column { Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary); Text(label, style = MaterialTheme.typography.bodySmall) }
}
private fun timeRead(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
