# Cathedral W1 — World Kernel Preflight

Status: PRE-MERGE / BLOCKED ON W0 RUNTIME ACCEPTANCE  
Tracking: #302  
Parent: #299  
Base: alpha/cathedral-convergence-w0

## Purpose

W1 turns already-durable reading facts into an immutable world history that later realms may project.
It does not create a second source of truth for books, Reader state, progression, or annotations.

The World Event Ledger is append-only:
- exact duplicate ids collapse,
- conflicting reuse of an id is surfaced and never overwrites the retained fact,
- ordering is deterministic by occurredAtEpochMs then id,
- unknown future event kinds survive the domain layer,
- events contain facts only, never personality diagnosis, motivation claims, or invented lore.

## Canonical event vocabulary

1. FirstBookImported
2. BookOpened
3. ReadingSessionCompleted
4. PassageMarked
5. NoteCreated
6. PassageRevisited
7. BookCompleted
8. CycleCompleted
9. RereadCompleted
10. DiscoveryEarned
11. RankAdvanced
12. ReturnedAfterSilence
13. LongHistoryReached

## Current durable-source map

| Event | Existing factual source / future emission edge | W1 status |
| --- | --- | --- |
| FirstBookImported | LocalLibraryRepository successful book insert | emitter deferred |
| BookOpened | LocalLibraryRepository.markOpened after durable timestamp write | emitter deferred |
| ReadingSessionCompleted | ReadingSessionSnapshot with endedAtEpochMs after lifecycle flush | emitter deferred |
| PassageMarked | successful Highlight persistence | emitter deferred |
| NoteCreated | highlight note blank -> non-blank after durable write | emitter deferred |
| PassageRevisited | successful PassageVisit persistence | emitter deferred |
| BookCompleted | saveProgress first completion edge | emitter deferred |
| CycleCompleted | ReadingCycle durable completion transaction | emitter deferred |
| RereadCompleted | durable ReadingCycle with cycleIndex > 1 | emitter deferred |
| DiscoveryEarned | GameRepository newly-earned discovery set | transaction boundary unresolved |
| RankAdvanced | GameRepository accepted rank transition | transaction boundary unresolved |
| ReturnedAfterSilence | durable session history gap | exact emission policy deferred |
| LongHistoryReached | no canonical threshold defined yet | product definition required |

## Cross-store blocker discovered in preflight

Reading history is Room-owned while progression/discovery state is currently partly SharedPreferences-owned.

Creating a Room world_events table immediately would make DiscoveryEarned and RankAdvanced cross-store
writes. A process death between SharedPreferences and Room could leave either:
- state advanced with no historical event, or
- an event claiming advancement that did not persist.

W1 therefore does not add Room schema version 3 yet and does not hook repository writes before W0 passes.

Accepted resolution paths must preserve one owner and factual timestamps. Candidate directions:
1. converge progression/discovery persistence into Room before event emission, or
2. introduce a rigorously recoverable transaction/outbox design that cannot fabricate timestamps.

The decision must be made before W1 persistence lands.

## Realm contract

World-facing realms may read ledger projections only. They never mutate reading history, progression,
or the ledger directly, and inactivity never removes or downgrades history.

Sanctuary is stronger: it has no decorative world-state access at all.

This keeps Reader calm while allowing Threshold, Great Hall, Living Mirror, Archive, Observatory,
Ritual, Treasury, and Sanctum to manifest historical facts outside the book.

## Gates before persistence

- W0 exact-head unit/lint/build gate GREEN.
- W0 commit-traceable APK produced.
- Existing Reader restore/reliability behavior remains GREEN.
- Persistence ownership choice documented.
- Migration + backup/restore behavior designed together.
- World event ids and emitter idempotency specified per event kind.
- No event threshold invented where the Cathedral blueprint is silent.

## Next implementation slice after W0 GREEN

W1a:
- choose persistence owner,
- define world_events schema and migration,
- add DAO/Flow,
- add backup/restore support,
- implement deterministic emitter ids,
- hook Room-owned factual events first,
- instrument process-death/idempotency tests.

W1b:
- solve GameRepository transaction boundary,
- add DiscoveryEarned and RankAdvanced,
- define ReturnedAfterSilence and LongHistoryReached only from approved factual policies.

W1c:
- expose read-only WorldHistory projection for Great Hall W2.
