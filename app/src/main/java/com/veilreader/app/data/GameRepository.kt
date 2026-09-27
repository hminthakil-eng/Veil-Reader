package com.veilreader.app.data

import android.content.Context
import android.os.SystemClock
import java.time.LocalTime
import com.veilreader.app.domain.ReadingPolicy
import com.veilreader.app.domain.GamificationEngine
import com.veilreader.app.domain.Quest
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.VeiledDiscoveryPolicy
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
        if (!ReadingPolicy.acceptsEvent(buildProfile().path.id, "note")) return
        val credited = prefs.getStringSet("creditedNotes", emptySet()).orEmpty().toMutableSet()
        if (!credited.add(id)) return
        prefs.edit().putStringSet("creditedNotes", credited).apply()
        recordRitualEvent("note")
        publish()
    }

    fun recordReadingMinute() {
        rollDayIfNeeded()
        totalXp += GamificationEngine.XP_PER_MINUTE
        todayMinutes += 1
        recordRitualEvent("minute")
        if (ReadingPolicy.isNight(LocalTime.now().hour)) recordRitualEvent("nightMinute")
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
        prefs.edit()
            .putInt("rankIndex", current.rankIndex + 1)
            .putInt("ritualProgress", 0)
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
        prefs.edit()
            .putString("pathId", pathId)
            .putInt("ritualProgress", 0)
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
        prefs.edit().putString("lastReadDate", todayString).putInt("streakDays", streak).apply()
    }

    private fun rollDayIfNeeded() {
        val today = LocalDate.now().toString()
        if (dayKey == today) return
        dayKey = today
        todayMinutes = 0
        todayPages = 0
        todayHighlights = 0
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

        // Discoveries are one-way memory. A temporary state regression (for example a broken
        // streak) must never reseal something the reader has already uncovered.
        snapshot = buildProfile()
        val earnedDiscoveries = prefs
            .getStringSet("earnedDiscoveries", emptySet())
            .orEmpty()
            .toMutableSet()
        earnedDiscoveries += VeiledDiscoveryPolicy.eligibleIds(
            profile = snapshot,
            highlightCount = prefs.getInt("totalHighlights", 0)
        )
        prefs.edit().putStringSet("earnedDiscoveries", earnedDiscoveries).apply()

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

    private fun buildProfile(): ReaderProfile {
        val (inside, needed) = GamificationEngine.progressInsideLevel(totalXp)
        val path = SampleData.paths.firstOrNull { it.id == prefs.getString("pathId", SampleData.currentPath.id) }
            ?: SampleData.currentPath
        return ReaderProfile(
            level = GamificationEngine.levelFor(totalXp),
            xp = inside,
            xpForNextLevel = needed,
            streakDays = prefs.getString("lastReadDate", null)?.let { raw ->
                val date = runCatching { LocalDate.parse(raw) }.getOrNull()
                if (date != null && ChronoUnit.DAYS.between(date, LocalDate.now()) in 0L..1L) prefs.getInt("streakDays", 0) else 0
            } ?: 0,
            pagesRead = prefs.getInt("pagesRead", 0),
            minutesRead = prefs.getInt("minutesRead", 0),
            booksFinished = prefs.getInt("booksFinished", 0),
            path = path,
            rankIndex = prefs.getInt("rankIndex", 0).coerceIn(0, path.ranks.lastIndex),
            ritualProgress = prefs.getInt("ritualProgress", 0),
            ritualTarget = ReadingPolicy.ritualTarget(path.id, prefs.getInt("rankIndex", 0)),
            earnedSigils = prefs.getStringSet("earnedSigils", emptySet()).orEmpty().toSet(),
            earnedDiscoveries = prefs
                .getStringSet("earnedDiscoveries", emptySet())
                .orEmpty()
                .toSet()
        )
    }

    private fun buildQuests(): List<Quest> {
        val goal = readDailyGoal()
        return listOf(
            Quest("read", "Read for $goal minutes", todayMinutes.coerceAtMost(goal), goal, 90),
            Quest("pages", "Read 15 paced pages", todayPages.coerceAtMost(15), 15, 120),
            Quest("mark", "Mark 3 intriguing passages", todayHighlights.coerceAtMost(3), 3, 75)
        )
    }
}
