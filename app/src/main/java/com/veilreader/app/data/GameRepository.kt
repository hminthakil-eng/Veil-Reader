package com.veilreader.app.data

import android.content.Context
import android.os.SystemClock
import java.time.LocalTime
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.RitualAftermathRecord
import com.veilreader.app.domain.derivePathMastery
import com.veilreader.app.domain.pathInsightEvidenceTotal
import com.veilreader.app.domain.pathStabilityEvidenceTotal
import com.veilreader.app.domain.validateRitualAftermath
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal fun completionEvidenceFloor(finishedBooks: Int, sealedCycles: Int): Int =
    maxOf(finishedBooks, sealedCycles, 0)

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

    /**
     * Restored/legacy libraries can contain completion history that predates game prefs.
     * Treat durable library evidence as a floor; never revoke historical completion credit when
     * a finished volume is later removed from the local shelf.
     */
    fun syncExistingBookCompletions(finishedBooks: Int, sealedCycles: Int) {
        val evidence = completionEvidenceFloor(finishedBooks, sealedCycles)
        if (evidence > prefs.getInt("booksFinished", 0)) {
            prefs.edit().putInt("booksFinished", evidence).apply()
        }
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
        val longestStreak = maxOf(
            prefs.getInt("longestStreakDays", 0),
            prefs.getInt("streakDays", 0),
            streak
        )
        prefs.edit()
            .putString("lastReadDate", todayString)
            .putInt("streakDays", streak)
            .putInt("longestStreakDays", longestStreak)
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
        val p = buildProfile()
        if (p.minutesRead >= 60) earned.add("first_hour")
        if (prefs.getInt("totalHighlights", 0) >= 10) earned.add("passage_keeper")
        if (prefs.getInt("streakDays", 0) >= 7) earned.add("seven_days")
        if (p.booksFinished >= 10) earned.add("ten_tomes")
        if (p.rankIndex >= 1) earned.add("first_threshold")

        val discoveries = prefs.getStringSet("earnedDiscoveries", emptySet()).orEmpty().toMutableSet()
        if ("seven_days" in earned && p.minutesRead >= 600) discoveries.add("patient_flame")
        if ("passage_keeper" in earned && p.pagesRead >= 1_000) discoveries.add("marginalia_gate")
        if (p.booksFinished >= 10 && p.rankIndex >= 1) discoveries.add("deep_shelf")
        if (p.minutesRead >= 3_000) discoveries.add("long_watch")
        if (earned.size >= 4) discoveries.add("veil_thins")
        if (p.rankIndex >= 3 && earned.size >= 5) discoveries.add("unnamed_chamber")

        prefs.edit()
            .putStringSet("earnedSigils", earned)
            .putStringSet("earnedDiscoveries", discoveries)
            .apply()
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
        val path = SampleData.paths.firstOrNull {
            it.id == prefs.getString("pathId", SampleData.currentPath.id)
        } ?: SampleData.currentPath
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
                if (
                    date != null &&
                    ChronoUnit.DAYS.between(date, LocalDate.now()) in 0L..1L
                ) {
                    prefs.getInt("streakDays", 0)
                } else {
                    0
                }
            } ?: 0,
            pagesRead = pagesRead,
            minutesRead = minutesRead,
            booksFinished = booksFinished,
            path = path,
            rankIndex = rankIndex,
            ritualProgress = ritualProgress,
            ritualTarget = ritualTarget,
            earnedSigils = prefs.getStringSet("earnedSigils", emptySet()).orEmpty().toSet(),
            longestStreakDays = maxOf(
                prefs.getInt("longestStreakDays", 0),
                prefs.getInt("streakDays", 0)
            ),
            earnedDiscoveries = prefs.getStringSet("earnedDiscoveries", emptySet()).orEmpty().toSet(),
            ritualAftermath = ritualAftermath,
            pathMastery = pathMastery
        )
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
}
