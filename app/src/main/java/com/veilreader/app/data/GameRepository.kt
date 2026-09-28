package com.veilreader.app.data

import android.content.Context
import android.os.SystemClock
import java.time.LocalTime
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.RitualAftermathRecord
import com.veilreader.app.domain.VeiledDiscoveryPolicy
import com.veilreader.app.domain.derivePathMastery
import com.veilreader.app.domain.pathInsightEvidenceTotal
import com.veilreader.app.domain.pathStabilityEvidenceTotal
import com.veilreader.app.domain.validateRitualAftermath
import com.veilreader.app.domain.VeiledDiscoveryRecord
import com.veilreader.app.domain.SilentNamesChoice
import com.veilreader.app.domain.SilentNamesDice
import com.veilreader.app.domain.SilentNamesEncounter
import com.veilreader.app.domain.SilentNamesMode
import com.veilreader.app.domain.SilentNamesOutcome
import com.veilreader.app.domain.SilentNamesReceipt
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.random.Random
import org.json.JSONObject

data class SilentNamesCommitResult(
    val receipt: SilentNamesReceipt,
    val newlyCommitted: Boolean
)

/** Persistent, offline-first reading progression.
 *
 * XP is supportive feedback only. Path rank advancement still requires ritual progress, preserving
 * the important rule that advancement cannot be farmed with raw points alone.
 */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
    private val pageGate = ReadingPolicy.PageGate()
    private var totalXp = prefs.getInt("totalXp", 0)
    private var todayMinutes = prefs.getInt("todayMinutes", 0)
    private var todayPages = prefs.getInt("todayPages", 0)
    private var todayHighlights = prefs.getInt("todayHighlights", 0)
    private var todayNotes = prefs.getInt("todayNotes", 0)
    private var todayNightMinutes = prefs.getInt("todayNightMinutes", 0)
    private var dayKey = prefs.getString("dayKey", "") ?: ""

    private val _profile = MutableStateFlow(buildProfile())
    val profile: StateFlow<ReaderProfile> = _profile

    private val _quests = MutableStateFlow(buildQuests())
    val quests: StateFlow<List<Quest>> = _quests

    private val _dailyGoalMinutes = MutableStateFlow(readDailyGoal())
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes

    private val _equippedSigil = MutableStateFlow(prefs.getString("equippedSigil", null))
    val equippedSigil: StateFlow<String?> = _equippedSigil

    private val _castleTitle = MutableStateFlow(prefs.getString("castleTitle", "Reader of the Veil") ?: "Reader of the Veil")
    val castleTitle: StateFlow<String> = _castleTitle

    init {
        // Old builds counted every highlight for every Path. Preserve Oracle progress only.
        if (prefs.getInt("ritualVersion", 1) < 2) {
            val editor = prefs.edit().putInt("ritualVersion", 2)
            if (prefs.getString("pathId", "oracle") != "oracle") editor.putInt("ritualProgress", 0)
            editor.apply()
        }
        if (prefs.getInt("pathMasteryVersion", 0) < 1) {
            val path = SampleData.paths.firstOrNull {
                it.id == prefs.getString("pathId", SampleData.currentPath.id)
            } ?: SampleData.currentPath
            val rank = prefs.getInt("rankIndex", 0).coerceIn(0, path.ranks.lastIndex)
            val legacyProgress = prefs.getInt("ritualProgress", 0).coerceAtLeast(0)
            val legacyTarget = ReadingPolicy.ritualTarget(path.id, rank).coerceAtLeast(1)
            val editor = prefs.edit()
                .putInt("pathMasteryVersion", 1)
                .putInt("pathMasteryInsightBaseline", 0)
                .putInt("pathMasteryStabilityBaseline", 0)
            if (legacyProgress >= legacyTarget && rank < path.ranks.lastIndex) {
                editor
                    .putString("pathMasteryGrandfatherPath", path.id)
                    .putInt("pathMasteryGrandfatherRank", rank)
            }
            editor.apply()
        }
        rollDayIfNeeded()
        publish()
    }

    fun syncExistingHighlights(count: Int) {
        if (count > prefs.getInt("totalHighlights", 0)) prefs.edit().putInt("totalHighlights", count).apply()
        publish()
    }

    fun refresh() { rollDayIfNeeded(); publish() }
    fun rebasePagePacing() { pageGate.rebase(SystemClock.elapsedRealtime()) }
    fun pauseReading() { pageGate.pause() }

    fun setDailyGoal(minutes: Int) {
        rollDayIfNeeded()
        val safe = minutes.coerceIn(5, 180)
        prefs.edit().putInt("dailyGoalMinutes", safe).apply()
        _dailyGoalMinutes.value = safe
        // A lower goal can make today's reading quest complete immediately. Award it now instead
        // of waiting for another reading event, while claimedQuestIds still prevents duplicates.
        awardCompletedQuestRewards()
        persistCounters()
        publish()
    }

    /** Equip an already-earned sigil in the Treasury. Pass null to clear the display slot. */
    fun equipSigil(sigilId: String?): Boolean {
        if (sigilId != null && sigilId !in buildProfile().earnedSigils) return false
        if (sigilId == null) prefs.edit().remove("equippedSigil").apply()
        else prefs.edit().putString("equippedSigil", sigilId).apply()
        _equippedSigil.value = sigilId
        return true
    }

    /**
     * Presentation metadata projected from the canonical earnedDiscoveries set.
     * Known IDs keep catalog order; unknown future IDs are preserved after them for downgrade safety.
     */
    fun discoveryRecords(earnedIds: Set<String>): List<VeiledDiscoveryRecord> {
        if (earnedIds.isEmpty()) return emptyList()
        val known = VeiledDiscoveryPolicy.orderedIds.filter { it in earnedIds }
        val unknown = (earnedIds - VeiledDiscoveryPolicy.orderedIds.toSet()).sorted()
        return (known + unknown).map { id ->
            VeiledDiscoveryRecord(
                id = id,
                recordedAtEpochMs = prefs
                    .getLong("discoveryRecordedAt:$id", 0L)
                    .takeIf { it > 0L }
            )
        }
    }

    /** Titles are earned from durable milestones and selected in the Inner Sanctum. */
    fun availableCastleTitles(): List<String> {
        val p = buildProfile()
        return buildList {
            add("Reader of the Veil")
            if ("first_threshold" in p.earnedSigils) add("Threshold Walker")
            if ("first_hour" in p.earnedSigils) add("Keeper of the Quiet Hour")
            if ("passage_keeper" in p.earnedSigils) add("Warden of Passages")
            if ("seven_days" in p.earnedSigils) add("Lantern of Seven Nights")
            if ("ten_tomes" in p.earnedSigils) add("Keeper of Ten Tomes")
            if (p.rankIndex >= p.path.ranks.lastIndex) add("Veilbound ${p.path.ranks.last()}")
            if (p.rankIndex >= p.path.ranks.lastIndex && p.earnedSigils.size >= 5) add("Sovereign of the Living Library")
        }.distinct()
    }

    fun selectCastleTitle(title: String): Boolean {
        if (title !in availableCastleTitles()) return false
        prefs.edit().putString("castleTitle", title).apply()
        _castleTitle.value = title
        return true
    }

    /**
     * Returns the durable encounter receipt, or null when the episode has never been sealed.
     *
     * A corrupt/unsupported stored receipt fails closed and is never silently replaced. This is
     * intentional: the raw record may belong to a newer app version and LibraryExport will retain it.
     */
    fun silentNamesReceipt(): SilentNamesReceipt? = synchronized(SILENT_NAMES_LOCK) {
        readSilentNamesReceiptLocked()
    }

    /** The cosmetic lantern is derived from the one committed receipt; there is no second award ledger. */
    fun ownsSilentNamesReward(): Boolean =
        silentNamesReceipt()?.rewardId == SilentNamesEncounter.REWARD_ID

    /**
     * Resolves and durably seals Silent Names exactly once per process.
     *
     * The existing receipt is checked under a process-wide lock before any die or timestamp is
     * generated. The whole receipt is encoded into one SharedPreferences value and committed
     * synchronously. A revisit therefore cannot reroll, change Path/choice, or duplicate a reward.
     */
    suspend fun sealSilentNamesEncounter(
        choice: SilentNamesChoice,
        mode: SilentNamesMode,
        nextD20: () -> Int = { Random.nextInt(1, 21) },
        nowEpochMs: () -> Long = { System.currentTimeMillis().coerceAtLeast(1L) }
    ): SilentNamesCommitResult = withContext(Dispatchers.IO) {
        synchronized(SILENT_NAMES_LOCK) {
            readSilentNamesReceiptLocked()?.let {
                return@synchronized SilentNamesCommitResult(it, newlyCommitted = false)
            }

            val pathId = buildProfile().path.id
            val dice = when (mode) {
                SilentNamesMode.DICE -> SilentNamesDice(nextD20())
                SilentNamesMode.STORY -> null
            }
            val candidate = SilentNamesEncounter.resolve(
                pathId = pathId,
                choice = choice,
                mode = mode,
                dice = dice,
                recordedAtEpochMs = nowEpochMs(),
                existing = null
            ).receipt

            val committed = prefs.edit()
                .putString(SILENT_NAMES_RECEIPT_KEY, encodeSilentNamesReceipt(candidate))
                .commit()
            check(committed) { "Could not commit Silent Names encounter receipt" }

            val restored = checkNotNull(readSilentNamesReceiptLocked()) {
                "Silent Names receipt was not readable after commit"
            }
            check(restored == candidate) { "Silent Names receipt changed during commit" }
            SilentNamesCommitResult(restored, newlyCommitted = true)
        }
    }

    private fun recordRitualEvent(event: String) {
        val profile = buildProfile()
        if (!ReadingPolicy.acceptsEvent(profile.path.id, event)) return
        prefs.edit().putInt("ritualProgress", (profile.ritualProgress + 1).coerceAtMost(profile.ritualTarget)).apply()
    }

    fun recordNote(id: String, note: String) {
        if (!ReadingPolicy.qualifiesNote(note)) return
        rollDayIfNeeded()
        val credited = prefs.getStringSet("creditedNotes", emptySet()).orEmpty().toMutableSet()
        if (!credited.add(id)) return
        prefs.edit().putStringSet("creditedNotes", credited).apply()
        todayNotes += 1
        recordRitualEvent("note")
        awardCompletedQuestRewards()
        persistCounters()
        publish()
    }

    fun recordReadingMinute() {
        rollDayIfNeeded()
        totalXp += GamificationEngine.XP_PER_MINUTE
        todayMinutes += 1
        recordRitualEvent("minute")
        if (ReadingPolicy.isNight(LocalTime.now().hour)) {
            todayNightMinutes += 1
            recordRitualEvent("nightMinute")
        }
        val previousMinutes = prefs.getInt("minutesRead", 0)
        prefs.edit().putInt("minutesRead", previousMinutes + 1).apply()
        touchReadingDay()
        awardCompletedQuestRewards()
        persistCounters()
        publish()
    }

    /** Returns true only when the paced-page policy accepts this navigation event. */
    fun recordPageTurn(locationKey: String): Boolean {
        rollDayIfNeeded()
        if (!pageGate.visit(locationKey, SystemClock.elapsedRealtime(), todayPages, todayMinutes)) return false
        recordRitualEvent("page")
        totalXp += GamificationEngine.XP_PER_PAGE
        todayPages += 1
        prefs.edit().putInt("pagesRead", prefs.getInt("pagesRead", 0) + 1).apply()
        touchReadingDay()
        awardCompletedQuestRewards()
        persistCounters()
        publish()
        return true
    }

    fun recordHighlight() {
        rollDayIfNeeded()
        todayHighlights += 1
        prefs.edit().putInt("totalHighlights", prefs.getInt("totalHighlights", 0) + 1).apply()
        recordRitualEvent("highlight")
        touchReadingDay()
        awardCompletedQuestRewards()
        persistCounters()
        publish()
    }

    fun recordBookFinished() {
        totalXp += GamificationEngine.BOOK_FINISH_BONUS
        prefs.edit().putInt("booksFinished", prefs.getInt("booksFinished", 0) + 1).apply()
        persistCounters()
        publish()
    }

    fun advanceRank(expectedPathId: String? = null, expectedRankIndex: Int? = null): Boolean {
        val current = buildProfile()
        if (!GamificationEngine.canAdvanceRank(
                current,
                expectedPathId ?: current.path.id,
                expectedRankIndex ?: current.rankIndex
            )) return false
        val nextRankIndex = current.rankIndex + 1
        val sealedAt = System.currentTimeMillis().coerceAtLeast(1L)
        val evidenceTotals = currentMasteryEvidenceTotals(current.path.id)
        // Rank and its descriptive ritual seal are committed in one editor transaction.
        // Rank remains authoritative; the seal can be discarded safely if metadata is corrupt.
        prefs.edit()
            .putInt("rankIndex", nextRankIndex)
            .putInt("ritualProgress", 0)
            .putString("ritualAftermathPathId", current.path.id)
            .putInt("ritualAftermathFromRank", current.rankIndex)
            .putInt("ritualAftermathToRank", nextRankIndex)
            .putLong("ritualAftermathSealedAt", sealedAt)
            .putInt("pathMasteryInsightBaseline", evidenceTotals.first)
            .putInt("pathMasteryStabilityBaseline", evidenceTotals.second)
            .remove("pathMasteryGrandfatherPath")
            .remove("pathMasteryGrandfatherRank")
            .apply()
        publish()
        return true
    }

    /** Change Paths only before the first rank advancement. Ritual progress resets on attunement. */
    fun choosePath(pathId: String): Boolean {
        val currentRank = prefs.getInt("rankIndex", 0)
        if (currentRank > 0) return false
        if (prefs.getString("pathId", "oracle") == pathId) return true
        if (SampleData.paths.none { it.id == pathId }) return false
        val evidenceTotals = currentMasteryEvidenceTotals(pathId)
        prefs.edit()
            .putString("pathId", pathId)
            .putInt("ritualProgress", 0)
            .putInt("pathMasteryInsightBaseline", evidenceTotals.first)
            .putInt("pathMasteryStabilityBaseline", 0)
            .remove("pathMasteryGrandfatherPath")
            .remove("pathMasteryGrandfatherRank")
            .apply()
        publish()
        return true
    }

    private fun touchReadingDay() {
        val today = LocalDate.now()
        val todayString = today.toString()
        val last = prefs.getString("lastReadDate", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (last == today) return

        val streak = when {
            last == null -> 1
            ChronoUnit.DAYS.between(last, today) == 1L -> prefs.getInt("streakDays", 0) + 1
            else -> 1
        }
        val readingDaysTotal = prefs.getInt("readingDaysTotal", 0).coerceAtLeast(0) + 1
        prefs.edit()
            .putString("lastReadDate", todayString)
            .putInt("streakDays", streak)
            .putInt("readingDaysTotal", readingDaysTotal)
            .apply()
    }

    private fun rollDayIfNeeded() {
        val today = LocalDate.now().toString()
        if (dayKey == today) return
        dayKey = today
        todayMinutes = 0
        todayPages = 0
        todayHighlights = 0
        todayNotes = 0
        todayNightMinutes = 0
        prefs.edit().remove("claimedQuestIds").apply()
        persistCounters()
    }

    private fun readDailyGoal(): Int = prefs.getInt("dailyGoalMinutes", 20).coerceIn(5, 180)

    private fun awardCompletedQuestRewards() {
        val claimed = prefs.getStringSet("claimedQuestIds", emptySet()).orEmpty().toMutableSet()
        val newlyCompleted = buildQuests().filter { it.progress >= it.target && it.id !in claimed }
        if (newlyCompleted.isEmpty()) return
        totalXp += newlyCompleted.sumOf { it.xpReward }
        claimed += newlyCompleted.map { it.id }
        prefs.edit().putStringSet("claimedQuestIds", claimed).apply()
    }

    private fun persistCounters() {
        prefs.edit()
            .putInt("totalXp", totalXp)
            .putString("dayKey", dayKey)
            .putInt("todayMinutes", todayMinutes)
            .putInt("todayPages", todayPages)
            .putInt("todayHighlights", todayHighlights)
            .putInt("todayNotes", todayNotes)
            .putInt("todayNightMinutes", todayNightMinutes)
            .apply()
    }

    private fun publish() {
        val earned = prefs.getStringSet("earnedSigils", emptySet()).orEmpty().toMutableSet()
        var snapshot = buildProfile()

        if (snapshot.minutesRead >= 60) earned.add("first_hour")
        if (prefs.getInt("totalHighlights", 0) >= 10) earned.add("passage_keeper")
        if (prefs.getInt("streakDays", 0) >= 7) earned.add("seven_days")
        if (snapshot.booksFinished >= 10) earned.add("ten_tomes")
        if (snapshot.rankIndex >= 1) earned.add("first_threshold")
        prefs.edit().putStringSet("earnedSigils", earned).apply()

        // Discoveries have one canonical ID owner: ReaderProfile. Timestamp metadata lives
        // beside that same merge-only set; there is no second discovery state machine.
        snapshot = buildProfile()
        val existingDiscoveries = prefs
            .getStringSet("earnedDiscoveries", emptySet())
            .orEmpty()
            .toSet()
        val earnedDiscoveries = VeiledDiscoveryPolicy.mergeEarned(
            existingIds = existingDiscoveries,
            profile = snapshot,
            highlightCount = prefs.getInt("totalHighlights", 0)
        )
        if (earnedDiscoveries != existingDiscoveries) {
            val newlyRecorded = earnedDiscoveries - existingDiscoveries
            val now = System.currentTimeMillis().coerceAtLeast(1L)
            val editor = prefs.edit().putStringSet("earnedDiscoveries", earnedDiscoveries)
            newlyRecorded.forEach { id ->
                val key = "discoveryRecordedAt:$id"
                if (!prefs.contains(key)) editor.putLong(key, now)
            }
            editor.apply()
        }

        _profile.value = buildProfile()
        _quests.value = buildQuests()
        _dailyGoalMinutes.value = readDailyGoal()

        // A title is never allowed to point at a milestone the current profile cannot own.
        val allowedTitles = availableCastleTitles()
        val selectedTitle = prefs.getString("castleTitle", "Reader of the Veil") ?: "Reader of the Veil"
        if (selectedTitle !in allowedTitles) {
            prefs.edit().putString("castleTitle", "Reader of the Veil").apply()
            _castleTitle.value = "Reader of the Veil"
        } else {
            _castleTitle.value = selectedTitle
        }
        _equippedSigil.value = prefs.getString("equippedSigil", null)
    }

    private fun currentMasteryEvidenceTotals(pathId: String): Pair<Int, Int> {
        val pagesRead = prefs.getInt("pagesRead", 0).coerceAtLeast(0)
        val minutesRead = prefs.getInt("minutesRead", 0).coerceAtLeast(0)
        val booksFinished = prefs.getInt("booksFinished", 0).coerceAtLeast(0)
        val insight = pathInsightEvidenceTotal(
            pathId = pathId,
            totalHighlights = prefs.getInt("totalHighlights", 0),
            substantialNotes = prefs.getStringSet("creditedNotes", emptySet()).orEmpty().size,
            pagesRead = pagesRead,
            booksFinished = booksFinished
        )
        val stability = pathStabilityEvidenceTotal(
            minutesRead = minutesRead,
            booksFinished = booksFinished,
            readingDays = prefs.getInt("readingDaysTotal", 0)
        )
        return insight to stability
    }

    private fun buildProfile(): ReaderProfile {
        val (inside, needed) = GamificationEngine.progressInsideLevel(totalXp)
        val path = SampleData.paths.firstOrNull { it.id == prefs.getString("pathId", SampleData.currentPath.id) }
            ?: SampleData.currentPath
        val rankIndex = prefs.getInt("rankIndex", 0).coerceIn(0, path.ranks.lastIndex)
        val ritualAftermath = validateRitualAftermath(
            record = RitualAftermathRecord(
                pathId = prefs.getString("ritualAftermathPathId", "") ?: "",
                fromRankIndex = prefs.getInt("ritualAftermathFromRank", -1),
                toRankIndex = prefs.getInt("ritualAftermathToRank", -1),
                sealedAtEpochMs = prefs.getLong("ritualAftermathSealedAt", 0L)
            ),
            currentPathId = path.id,
            currentRankIndex = rankIndex,
            rankCount = path.ranks.size
        )
        val ritualProgress = prefs.getInt("ritualProgress", 0).coerceAtLeast(0)
        val ritualTarget = ReadingPolicy.ritualTarget(path.id, rankIndex).coerceAtLeast(1)
        val pagesRead = prefs.getInt("pagesRead", 0).coerceAtLeast(0)
        val minutesRead = prefs.getInt("minutesRead", 0).coerceAtLeast(0)
        val booksFinished = prefs.getInt("booksFinished", 0).coerceAtLeast(0)
        val derivedMastery = derivePathMastery(
            pathId = path.id,
            rankIndex = rankIndex,
            embodimentValue = ritualProgress,
            embodimentTarget = ritualTarget,
            totalHighlights = prefs.getInt("totalHighlights", 0),
            substantialNotes = prefs.getStringSet("creditedNotes", emptySet()).orEmpty().size,
            pagesRead = pagesRead,
            minutesRead = minutesRead,
            booksFinished = booksFinished,
            readingDays = prefs.getInt("readingDaysTotal", 0),
            insightBaseline = prefs.getInt("pathMasteryInsightBaseline", 0),
            stabilityBaseline = prefs.getInt("pathMasteryStabilityBaseline", 0)
        )
        val grandfathered =
            prefs.getString("pathMasteryGrandfatherPath", null) == path.id &&
                prefs.getInt("pathMasteryGrandfatherRank", -1) == rankIndex &&
                ritualProgress >= ritualTarget
        val pathMastery = if (grandfathered) {
            derivedMastery.copy(
                insight = derivedMastery.insight.copy(
                    value = maxOf(derivedMastery.insight.value, derivedMastery.insight.target)
                ),
                stability = derivedMastery.stability.copy(
                    value = maxOf(derivedMastery.stability.value, derivedMastery.stability.target)
                ),
                dissonance = 0
            )
        } else {
            derivedMastery
        }
        return ReaderProfile(
            level = GamificationEngine.levelFor(totalXp),
            xp = inside,
            xpForNextLevel = needed,
            streakDays = prefs.getString("lastReadDate", null)?.let { raw ->
                val date = runCatching { LocalDate.parse(raw) }.getOrNull()
                if (date != null && ChronoUnit.DAYS.between(date, LocalDate.now()) in 0L..1L) prefs.getInt("streakDays", 0) else 0
            } ?: 0,
            pagesRead = pagesRead,
            minutesRead = minutesRead,
            booksFinished = booksFinished,
            path = path,
            rankIndex = rankIndex,
            ritualProgress = ritualProgress,
            ritualTarget = ritualTarget,
            earnedSigils = prefs.getStringSet("earnedSigils", emptySet()).orEmpty().toSet(),
            earnedDiscoveries = prefs
                .getStringSet("earnedDiscoveries", emptySet())
                .orEmpty()
                .toSet(),
            ritualAftermath = ritualAftermath,
            pathMastery = pathMastery
        )
    }

    private fun readSilentNamesReceiptLocked(): SilentNamesReceipt? {
        val raw = prefs.getString(SILENT_NAMES_RECEIPT_KEY, null) ?: return null
        return runCatching { decodeSilentNamesReceipt(raw) }
            .getOrElse { error ->
                throw IllegalStateException(
                    "Stored Silent Names receipt is unsupported or corrupt; retaining it unchanged.",
                    error
                )
            }
    }

    private fun encodeSilentNamesReceipt(receipt: SilentNamesReceipt): String = JSONObject().apply {
        put("encounterId", receipt.encounterId)
        put("contentVersion", receipt.contentVersion)
        put("pathId", receipt.pathId)
        put("choice", receipt.choice.name)
        put("mode", receipt.mode.name)
        put("diceFirst", receipt.dice?.first ?: JSONObject.NULL)
        put("diceSecond", receipt.dice?.second ?: JSONObject.NULL)
        put("outcome", receipt.outcome.name)
        put("rewardId", receipt.rewardId)
        put("recordedAtEpochMs", receipt.recordedAtEpochMs)
    }.toString()

    private fun decodeSilentNamesReceipt(raw: String): SilentNamesReceipt {
        val json = JSONObject(raw)
        val mode = SilentNamesMode.valueOf(json.getString("mode"))
        val dice = when (mode) {
            SilentNamesMode.STORY -> {
                require(json.isNull("diceFirst") && json.isNull("diceSecond"))
                null
            }
            SilentNamesMode.DICE -> SilentNamesDice(
                first = json.getInt("diceFirst"),
                second = if (json.isNull("diceSecond")) null else json.getInt("diceSecond")
            )
        }
        val receipt = SilentNamesReceipt(
            encounterId = json.getString("encounterId"),
            contentVersion = json.getInt("contentVersion"),
            pathId = json.getString("pathId"),
            choice = SilentNamesChoice.valueOf(json.getString("choice")),
            mode = mode,
            dice = dice,
            outcome = SilentNamesOutcome.valueOf(json.getString("outcome")),
            rewardId = json.getString("rewardId"),
            recordedAtEpochMs = json.getLong("recordedAtEpochMs")
        )
        require(SilentNamesEncounter.isValid(receipt))
        return receipt
    }

    private fun buildQuests(): List<Quest> {
        val goal = readDailyGoal()
        val pathId = prefs.getString("pathId", SampleData.currentPath.id)
            ?: SampleData.currentPath.id
        return listOf(
            Quest(
                "read",
                "Keep a quiet reading session for $goal minutes",
                todayMinutes.coerceAtLeast(0).coerceAtMost(goal),
                goal,
                90
            ),
            Quest(
                "pages",
                "Turn 15 paced pages without rushing",
                todayPages.coerceAtLeast(0).coerceAtMost(15),
                15,
                120
            ),
            GamificationEngine.pathDirective(
                pathId = pathId,
                todayMinutes = todayMinutes,
                todayPages = todayPages,
                todayHighlights = todayHighlights,
                todayNotes = todayNotes,
                todayNightMinutes = todayNightMinutes
            )
        )
    }
    private companion object {
        const val SILENT_NAMES_RECEIPT_KEY = "encounter:${SilentNamesEncounter.ID}:receipt"
        val SILENT_NAMES_LOCK = Any()
    }

}
