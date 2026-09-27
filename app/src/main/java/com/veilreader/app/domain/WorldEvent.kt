package com.veilreader.app.domain

private val WORLD_EVENT_KIND_PATTERN = Regex("[A-Za-z][A-Za-z0-9]{1,63}")
private val WORLD_EVENT_FACT_KEY_PATTERN = Regex("[a-z][A-Za-z0-9]{0,63}")

/**
 * Stable wire-level event kind.
 *
 * This is intentionally not an enum: future app versions may restore or encounter event kinds
 * unknown to an older build, and the ledger must preserve those facts instead of deleting them.
 */
data class WorldEventKind private constructor(val wireName: String) {
    companion object {
        val FIRST_BOOK_IMPORTED = WorldEventKind("FirstBookImported")
        val BOOK_OPENED = WorldEventKind("BookOpened")
        val READING_SESSION_COMPLETED = WorldEventKind("ReadingSessionCompleted")
        val PASSAGE_MARKED = WorldEventKind("PassageMarked")
        val NOTE_CREATED = WorldEventKind("NoteCreated")
        val PASSAGE_REVISITED = WorldEventKind("PassageRevisited")
        val BOOK_COMPLETED = WorldEventKind("BookCompleted")
        val CYCLE_COMPLETED = WorldEventKind("CycleCompleted")
        val REREAD_COMPLETED = WorldEventKind("RereadCompleted")
        val DISCOVERY_EARNED = WorldEventKind("DiscoveryEarned")
        val RANK_ADVANCED = WorldEventKind("RankAdvanced")
        val RETURNED_AFTER_SILENCE = WorldEventKind("ReturnedAfterSilence")
        val LONG_HISTORY_REACHED = WorldEventKind("LongHistoryReached")

        val canonical: List<WorldEventKind> = listOf(
            FIRST_BOOK_IMPORTED,
            BOOK_OPENED,
            READING_SESSION_COMPLETED,
            PASSAGE_MARKED,
            NOTE_CREATED,
            PASSAGE_REVISITED,
            BOOK_COMPLETED,
            CYCLE_COMPLETED,
            REREAD_COMPLETED,
            DISCOVERY_EARNED,
            RANK_ADVANCED,
            RETURNED_AFTER_SILENCE,
            LONG_HISTORY_REACHED
        )

        private val knownByName = canonical.associateBy { it.wireName }

        fun of(wireName: String): WorldEventKind {
            val clean = wireName.trim()
            require(clean == wireName && WORLD_EVENT_KIND_PATTERN.matches(clean)) {
                "World event kind must be a stable PascalCase wire name."
            }
            return knownByName[clean] ?: WorldEventKind(clean)
        }
    }
}

/**
 * One factual, immutable item in Veil's world history.
 *
 * [facts] contains only source-owned identifiers or measurements needed to render later world
 * projections. It must never contain inferred personality, motivation, or invented lore.
 */
data class WorldEvent(
    val id: String,
    val kind: WorldEventKind,
    val occurredAtEpochMs: Long,
    val subjectId: String? = null,
    val facts: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank() && id == id.trim()) { "World event id must be non-blank and stable." }
        require(occurredAtEpochMs > 0L) { "World event time must be a positive epoch millisecond." }
        require(subjectId == null || (subjectId.isNotBlank() && subjectId == subjectId.trim())) {
            "World event subject id must be null or a stable non-blank identifier."
        }
        facts.forEach { (key, value) ->
            require(WORLD_EVENT_FACT_KEY_PATTERN.matches(key)) {
                "World event fact keys must be stable lowerCamelCase names."
            }
            require(value.isNotBlank() && value == value.trim()) {
                "World event fact values must be non-blank and canonicalized by the emitter."
            }
        }
    }
}

data class WorldEventConflict(
    val id: String,
    val retained: WorldEvent,
    val rejected: WorldEvent
)

data class WorldEventMergeResult(
    val events: List<WorldEvent>,
    val conflicts: List<WorldEventConflict>
) {
    val isClean: Boolean get() = conflicts.isEmpty()
}

/**
 * Append-only merge policy.
 *
 * Exact duplicate ids collapse. A reused id with different facts is never overwritten: the first
 * observed fact is retained and a conflict is surfaced for explicit repair.
 */
fun mergeWorldEvents(
    existing: Iterable<WorldEvent>,
    incoming: Iterable<WorldEvent>
): WorldEventMergeResult {
    val byId = linkedMapOf<String, WorldEvent>()
    val conflicts = mutableListOf<WorldEventConflict>()

    fun ingest(event: WorldEvent) {
        val prior = byId[event.id]
        when {
            prior == null -> byId[event.id] = event
            prior == event -> Unit
            else -> conflicts += WorldEventConflict(
                id = event.id,
                retained = prior,
                rejected = event
            )
        }
    }

    existing.forEach(::ingest)
    incoming.forEach(::ingest)

    return WorldEventMergeResult(
        events = byId.values.sortedWith(
            compareBy<WorldEvent> { it.occurredAtEpochMs }
                .thenBy { it.id }
        ),
        conflicts = conflicts.toList()
    )
}
