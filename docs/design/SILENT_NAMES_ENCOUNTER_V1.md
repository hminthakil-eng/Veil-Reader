# Silent Names: encounter policy and authored content

Related: #314 (Gamification + World Projection), #312 (Alpha Recovery), #316 (capability gate).

Status: runtime integration implemented on the G1 branch; **Android build/device verification is still blocked by runner availability**.
Source baseline: `16342664ecfe8cab59e58381e51c61476233bb08`,
`alpha/cathedral-visual-breakthrough-v1`, inspected 2026-09-28.

## Why this slice

The book-source design proposed an optional solo RPG around reading. Current Alpha already owns
six Paths, ritual mastery, discovery chains and a world projection. Its rank is the only room
unlock authority. The design proposal must not become a second XP/rank implementation.

This branch now supplies one bounded encounter policy, six authored outcomes, a fixed cosmetic
reward, English/Persian copy, durable receipt storage through the existing GameRepository, a
restorable Great Hall destination and an optional Hall story portal. Existing MysteryChainPolicy,
world mutation derivation, the Room schema, reading sessions, rank authority and Reader navigation
remain untouched. The encounter is never hosted inside Reader/Sanctuary.

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

`needsCommit` remains a pure-policy intent. The runtime integration resolves it as follows:

1. The existing GameRepository is the only owner; no parallel game repository/cache was added.
2. A process-wide lock reads the unique receipt before generating a die or timestamp.
3. A new resolution is serialized into one versioned JSON string under
   `encounter:silent_names_window:receipt`.
4. That one value is written with synchronous `SharedPreferences.commit()` on
   `Dispatchers.IO`; the UI receives success only after the committed value is re-read and
   validated.
5. The Lantern of Remembrance is **derived from the valid receipt**. There is no second inventory
   write, so receipt/reward cannot split into two fallible persistence operations.
6. Existing valid receipts win on every revisit. Unsupported/corrupt/future receipts fail closed
   and remain untouched instead of being reset into another reward.
7. LibraryExport already serializes every value in `veil_game_v1` into `gamePreferences` and
   restores that preference set synchronously, so the encounter receipt enters explicit local
   backup/restore without a backup-schema bump.
8. Story-mode and dice-mode sealing run off the main thread. The initial UI rolls exactly one d20;
   advantage support remains policy-only for later authored situations.

The remaining durability evidence is runtime verification: Android process death around the write,
explicit backup round-trip, and a real-device repeated-tap stress pass. The branch includes a
Robolectric repository test for replay, two-repository same-process races, fail-closed future
records and story mode, but hosted CI has not executed those tests yet.

## Integration surface and acceptance

Entry belongs in the Great Hall, never on the Reader. This self-contained introductory story
must not unlock any rank-gated room; do not change SampleData.rooms or canAdvanceRank.
The entry is a separate optional Story Portal rather than a ninth rank-gated artifact, preserving
the eight canonical Great Hall artifact destinations and the existing room-unlock authority.
The chamber route is stored in SavedState and restores after recreation. Dismissal returns to the
Hall; no Reader locator is created, cleared or modified by this route.

The UI binds every choice and outcome explicitly to resources, shows Story/Dice mode before a
choice, shows the direct-route odds in Dice mode, and disables mode/choices while a commit is in
flight. Outcome, sealed state and Lantern are shown only after persistence returns successfully.
Unsupported stored records render a fail-closed notice with no reroll path. The screen contains no
nonessential motion or sound, all primary controls use at least 48dp touch height, and the copy has
English/Persian resources. TalkBack, Persian RTL, large-font and small-screen device acceptance is
still pending.

| Capability | Before | After this patch | Evidence |
|---|---|---|---|
| Reader settings/locator/EPUB/PDF | Existing Alpha behavior | No call sites or files touched | Changed-file scope |
| Rank and room authority | GameRepository + rank | Unchanged | Policy has no XP/rank/room outputs |
| Existing six Paths | Existing IDs | Referenced, not replaced | Unit test against actual SampleData |
| Discovery mystery chains | Existing derived clues | Unchanged | Separate original episode; no discovery award |
| Playable solo encounter | Not supplied by baseline | Great Hall Story Portal + restorable chamber wired | Source inspection; device acceptance pending |
| Replay-safe rule resolution | New | Existing valid receipt wins before RNG/time generation | Policy tests + repository tests authored |
| Durable single award | Not implemented in initial policy | One committed receipt; reward derived from it | Source inspection; runtime process-death/backup round-trip pending |

## Verification

Earlier local results on 2026-09-28: 8 pure encounter JUnit tests passed and the existing
ReadingPolicy harness passed 39 checks. The runtime slice subsequently added repository,
navigation and Great Hall policy tests plus UI/resource changes, but those new tests have **not**
been executed by Android CI yet. Current GitHub-hosted Android CI, Storage Instrumentation and
Performance jobs terminate before step 1 with no job steps/log stream, so their red status is
runner/infrastructure evidence rather than a Gradle/test failure.

Locally compile the actual policy and its tests with Kotlin 2.3.21, JDK 17 and JUnit 4.13.2,
alongside unmodified baseline Models, PathMastery and SampleData. No substitute model stubs.
Tests cover all six paths, every die face, all 400 advantage pairs per path/choice, story mode,
exact odds, validation, unknown path compatibility and a reconstructed receipt retried with
different inputs. This does not replace the full Android Gradle suite.

Required verification gates still pending: `:app:testDebugUnitTest`, `:app:lintDebug`,
`:app:assembleDebug`, process death around the receipt commit, explicit backup round-trip,
concurrent repeated taps on Android, and TalkBack/RTL/large-font/small-screen screenshots on the
same APK. Do not call this feature GREEN until those gates run successfully.

## Source inspiration

The supplied Circle of Inevitability chapter 25 (EPUB/text/ch027.xhtml) informed specialization;
Lord of the Mysteries chapter 1080 (EPUB/text/ch1096.xhtml) informed role-consistent action.
No source characters, dialogue, plot revelations or copied rank ladder are included here.
The d20 comparison/advantage structure is informed by official D&D rules:
https://www.dndbeyond.com/sources/dnd/br-2024/playing-the-game
https://www.dndbeyond.com/srd
The implementation and episode prose are original; this patch does not import SRD text or assets.
