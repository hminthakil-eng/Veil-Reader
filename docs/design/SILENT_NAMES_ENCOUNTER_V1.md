# Silent Names: encounter policy and authored content

Related: #314 (Gamification + World Projection), #312 (Alpha Recovery), #316 (capability gate).

Status: reviewed implementation boundary, **not a playable or durable feature yet**.
Source baseline: `16342664ecfe8cab59e58381e51c61476233bb08`,
`alpha/cathedral-visual-breakthrough-v1`, inspected 2026-09-28.

## Why this slice

The book-source design proposed an optional solo RPG around reading. Current Alpha already owns
six Paths, ritual mastery, discovery chains and a world projection. Its rank is the only room
unlock authority. The design proposal must not become a second XP/rank implementation.

This patch supplies one bounded, pure encounter policy, six authored outcomes, a fixed cosmetic
reward intent and English/Persian copy. It leaves GameRepository, existing MysteryChainPolicy,
world mutation derivation, the Room schema, rewards, reading sessions and navigation untouched.
It is deliberately not yet called by the app. The next slice must integrate storage and UI
together before exposing an entry point; no in-memory-only playable screen should ship.

## Corrections to the earlier proposal

| Earlier design assumption | Inspected Alpha contract | Decision for integration |
|---|---|---|
| Six newly named roles | oracle, dreamwalker, archivist, vanguard, nocturne, artificer already exist | Reuse these IDs; no renaming/migration in this patch |
| Time-only XP and new rank thresholds | Existing XP, ritual, mastery, rank ownership | Do not implement the proposed replacement economy |
| Free switching of paths at every rank | choosePath currently allows changes only at rank zero | Preserve policy; any later change needs an explicit progression migration |
| Every world mutation permanent | WorldMutationLedger derives some entries from removable source data | New earned receipt must be permanent; do not label existing projections permanent |
| Game off stops collecting game evidence | Quiet-mode behavior is a separate existing product contract | Do not alter collection or quiet-mode semantics here |

## The encounter

A window in the Hall has lost its maker's name. Three traces offer three approaches:

| Choice | Direct outcome | Alternate outcome |
|---|---|---|
| Examine the seal | Recover the inscription | Find a workshop trail |
| Follow the light | Cross the lantern bridge | Find the lower passage |
| Speak to the clockwork keeper | Hear testimony | Shelter a nameless lantern |

All six outcomes finish this episode and offer the same Lantern of Remembrance. The lantern is
a story collectible, not reading evidence, rank, XP, room access or a claim about book contents.
No book text, title, character or plot is extracted. The fiction and labels are original.

Oracle/Archivist specialize in the seal; Dreamwalker/Vanguard in the light;
Nocturne/Artificer in the keeper (listening to or understanding a clockwork witness).
Each existing path receives exactly one +3 approach; other approaches use +1.
Unknown future path IDs retain +1 and can complete all choices. This is a small fixed first
episode, not a new player-stat allocation system.

Dice: d20 + modifier >= 13. Specialty odds are 55%, ordinary odds 45%.
Advantage uses the higher of two independent dice: 79.75% or 69.75% respectively.
No special critical-success/critical-failure rule. The initial UI should use a single die;
advantage support is tested for later authored situations and must not be silently granted.
Story mode uses no dice and takes the direct branch for the chosen approach.

## Receipt and storage boundary

`SilentNamesReceipt` retains encounter ID, content version, path ID at the time of choice,
choice, mode, dice, outcome, reward ID and timestamp. A supplied valid receipt wins over a
retried command, even if the new command changes the path, choice, die or timestamp.

`needsCommit` is only an intent. Pure unit tests cannot prove disk durability or concurrent
exactly-once awarding. Integration MUST:

1. Use the existing GameRepository ownership boundary. Inspect backup/restore before selecting
   the durable representation; do not add a parallel game repository or disconnected cache.
2. Read by unique encounter ID inside the same serialized transaction that saves the choice.
3. If a receipt exists, retain it and display its result. Unsupported/corrupt records must be
   retained and reported; do not catch the validation error and retry with `existing = null`.
4. Generate dice only after the transactional lookup establishes this is a new encounter.
5. Atomically commit receipt + unique reward. The receipt and reward must not be separate
   asynchronous SharedPreferences `apply()` calls. A local boolean is not an award ledger.
6. Display the seal/reward only after successful persistence. On a failed write retain the
   pending choice for retry, do not advance UI as though it succeeded.
7. Preserve original receipt on revisit, deletion of a book and path changes. No replay reward.
8. Include receipts in versioned explicit backup/restore, retain unknown future versions and
   test migration/restore before enabling UI. No cloud or implicit backup dependency.

This version intentionally contains no serialization implementation, migration, persistence
adapter, inventory grant or random generator. These are open integration gates, not finished work.
If the storage engine cannot atomically save the receipt and reward, model the cosmetic ownership
as derived from the single committed receipt rather than making two fallible writes.

## Integration surface and acceptance

Entry belongs in the Great Hall, never on the Reader. This self-contained introductory story
must not unlock any rank-gated room; do not change SampleData.rooms or canAdvanceRank.
Default entry must respect existing progression visibility settings. Dismissal returns to the
Hall; returning to a book must use the current app navigation path and preserve its locator.

Bind the enum choices/outcomes explicitly to the new resource strings; avoid reflection-based
resource lookup. Show mode and odds before a dice choice. The mode can change only before the
receipt is committed. Narration and seal are text-first; animation/sound are optional, skippable,
and absent in reduced-motion/sound-off settings. All controls need 48dp touch targets and
TalkBack semantics; verify Persian RTL, large fonts and small screens before visual acceptance.

| Capability | Before | After this patch | Evidence |
|---|---|---|---|
| Reader settings/locator/EPUB/PDF | Existing Alpha behavior | No call sites or files touched | Changed-file scope |
| Rank and room authority | GameRepository + rank | Unchanged | Policy has no XP/rank/room outputs |
| Existing six Paths | Existing IDs | Referenced, not replaced | Unit test against actual SampleData |
| Discovery mystery chains | Existing derived clues | Unchanged | Separate original episode; no discovery award |
| Playable solo encounter | Not supplied by this patch's baseline inspection | Still not exposed | UI integration pending |
| Replay-safe rule resolution | New | Existing valid receipt wins | Unit tests, not a disk test |
| Durable single award | Not implemented here | Pending | Needs concurrent/process-death/backup tests |

## Verification

Local results on 2026-09-28: 8 encounter JUnit tests passed; the existing ReadingPolicy harness
passed 39 checks. Both new XML files parsed successfully with 19 matching resource keys.
Android resource compilation, lint, APK assembly and device execution were not run here.

Locally compile the actual policy and its tests with Kotlin 2.3.21, JDK 17 and JUnit 4.13.2,
alongside unmodified baseline Models, PathMastery and SampleData. No substitute model stubs.
Tests cover all six paths, every die face, all 400 advantage pairs per path/choice, story mode,
exact odds, validation, unknown path compatibility and a reconstructed receipt retried with
different inputs. This does not replace the full Android Gradle suite.

Required integration gates still pending: :app:testDebugUnitTest, :app:lintDebug,
:app:assembleDebug, Android storage/migration tests after persistence is added,
process death at each write boundary, concurrent repeated taps, backup round-trip,
TalkBack/RTL/reduced-motion screenshots on the same APK. Do not call this feature GREEN
or claim the user can play it until these boundaries are implemented and verified.

## Source inspiration

The supplied Circle of Inevitability chapter 25 (EPUB/text/ch027.xhtml) informed specialization;
Lord of the Mysteries chapter 1080 (EPUB/text/ch1096.xhtml) informed role-consistent action.
No source characters, dialogue, plot revelations or copied rank ladder are included here.
The d20 comparison/advantage structure is informed by official D&D rules:
https://www.dndbeyond.com/sources/dnd/br-2024/playing-the-game
https://www.dndbeyond.com/srd
The implementation and episode prose are original; this patch does not import SRD text or assets.
