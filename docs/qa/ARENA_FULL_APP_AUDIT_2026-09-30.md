# Arena Full-App Audit — Veil Reader
Date: 2026-09-30
Branch audited: `alpha/arena-rebuild-v2`
Evaluation owner: Alpha / Arena
Build policy for this pass: source audit only; no APK/build claim

## Executive status

Arena reviewed the complete current app surface and its supporting architecture:
- 74 production source files
- 40 unit-test files
- 12 instrumented/device-test files
- Reader / EPUB / PDF
- Library / Grayfog Archive
- Threshold / Home
- Hidden Archive / Notebook
- Settings
- Castle / Path / Observatory / Profile
- persistence / import / backup / migration
- gamification / memory systems
- RTL / accessibility architecture
- baseline-profile / macrobenchmark infrastructure

### Current verdict
- Confirmed source-level P0 data-loss blocker: **none found in this pass**
- Verification blocker: **present** — rebuild branch has not yet been compiled/device-validated by policy
- P1 product/reliability debt: **present**
- P2 polish/depth debt: **present**

No screen is DEVICE-GREEN from this audit alone.

---

## Arena gate

Arena rubric:
- Correctness 30
- Completeness 25
- Specificity 15
- Robustness 20
- Clarity 10

Fatal functional, accessibility, storage or migration regression fails a slice regardless of visual score.

### Audit grades by subsystem

| Subsystem | Status | Arena view | Main reason |
| --- | --- | --- | --- |
| Reader persistence | KEEP / VERIFY | Strong | final locator snapshot + durable close barrier + serialized storage queue |
| EPUB engine | KEEP | Strong | Readium 3.4.0, custom paper curl, directional arbitration |
| PDF engine | KEEP / REFINE | Strong | native paged/scroll + direct zoom; layout/turn cross-contamination fixed on rebuild branch |
| Library data | KEEP | Strong | Room source of truth, transactions, migrations, import fingerprinting |
| Threshold | REFINE | Strong base | scene composition being rebuilt toward locked reference |
| Grayfog Library | REFINE | Strong base | retrieval-first hierarchy repaired; world layers retained |
| Hidden Archive | REFINE | Strong | notes/highlights/bookmarks/echoes/capsules already substantial |
| Book Detail | REBUILD-IN-PROGRESS | Medium/strong | promoted from bottom sheet to full-screen narrative destination |
| Settings | REBUILD-IN-PROGRESS | Medium/strong | progressive disclosure added; device interaction proof pending |
| Castle / Path / Observatory | REFINE | Strong systems, uneven finish | localization and UI test coverage lag the rest of product |
| Profile / More | REFINE | Medium | useful systems, localization and information architecture still uneven |
| Gamification persistence | REFINE P1 | Medium | direct SharedPreferences + apply() weaker than Library durability model |
| Localization / RTL | REFINE P1 | Uneven | RTL architecture exists; user-facing hardcoded English remains |
| Accessibility | KEEP / VERIFY | Good architecture | RTL/script policies and 48dp intent exist; screen-level device proof incomplete |
| Performance | VERIFY | Infrastructure ready | macrobenchmark + baseline profile module exist; evidence generation remains |
| Release packaging | VERIFY | Infrastructure ready | R8/resource shrink/profile plugins present; package/profile proof pending |

---

# P0 — blockers

## P0-V1 — Rebuild branch compile/device verification is intentionally pending
Type: Verification blocker, not a confirmed source defect.

The branch now contains navigation, Reader movement-model, Library hierarchy, Book Detail and Settings changes. Source structure was checked during edits, but no build/device run was produced because the current product directive is to avoid builds until the design reaches world-class quality.

Exit evidence later:
1. compile
2. unit tests
3. instrumented reliability suite
4. emulator/physical-device visual pass
5. accessibility pass
6. package/profile verification

---

# P1 — high priority

## P1-01 — Localization coverage is incomplete
Status: OPEN

RTL support is structurally enabled and Persian resources exist, but multiple production surfaces still emit direct English user-facing strings.

Confirmed examples:
- `VeilApp.kt`: import/restore/storage/path/sigil/title error messages and "Return"
- `ReaderNotebook.kt`: search labels, empty states, Return/Remove/Delete, note dialogs
- `PathScreen.kt`: "Advance", "Not yet"
- `ProfileScreen.kt`: current-duration labels and "Open Hidden Archive"
- smaller direct labels remain in Reader/PDF controls

Why it matters:
- Persian/Arabic is a first-class product requirement.
- mixed-language error/utility states break the premium world illusion.
- error states are part of the design system, not exceptions.

Required fix:
- move every user-visible literal into resources
- add Persian equivalents
- run a source lint rule/test that fails on new direct UI literals except deliberate symbols such as `Aa`, `+`, `−`

## P1-02 — Gamification durability is weaker than reading durability
Status: OPEN

Library/progress/history use Room plus a serialized durable write queue. Gamification still uses `SharedPreferences` and repeated `apply()`.

Risk:
- abrupt process death can lose the newest XP/quest/path/sigil/title update even when book progress is already durable.
- this creates cross-system inconsistency: reading record says the action happened while progression may not.

Required direction:
- move canonical game progression to DataStore or Room, preferably into the same durability philosophy as reading state
- preserve current public GameRepository API
- add process-death/durability tests

## P1-03 — World surfaces lack proportional UI regression coverage
Status: OPEN

Existing tests are strong around:
- Reader durability / locator policy
- process restoration
- paper curl geometry
- Reader input arbitration
- PDF preference mapping
- memory/domain derivation
- threshold/library model behavior

Coverage is comparatively weak for:
- CastleScreen
- CastleChamberScreen
- PathScreen
- ObservatoryScreen
- ProfileScreen
- ArchiveScreen composition
- SettingsScreen composition/interactions
- sensory behavior

Required fix:
- add stable pure policy/model tests where possible
- add Compose/device interaction checks for major destinations
- add screenshot/golden or semantic-layout evidence for locked visual surfaces when build verification resumes

## P1-04 — Baseline/startup profile evidence remains a release gate
Status: OPEN / RELEASE

Infrastructure exists:
- baseline profile plugin
- benchmark module
- generator
- startup benchmark
- reader frame benchmark
- release minification/resource shrink

Still required:
- generated baseline/startup profile files
- package proof that profiles are consumed
- startup and reader frame evidence on representative hardware

Do not redesign Reader to solve this; finish the profiling pipeline.

## P1-05 — Error-state design is inconsistent with the product world
Status: OPEN

Several errors are currently assembled as raw English strings in `VeilApp`. Beyond localization, their voice and action hierarchy are inconsistent.

Required:
- typed app errors / localized resource mapping
- distinguish recoverable, storage, import, position-migration and progression errors
- every error needs one clear next action when actionable
- do not hide technical durability failures behind mystical language

---

# P2 — refinement

## P2-01 — Realm density must remain intentionally unequal
Keep:
- Reader: minimal
- Threshold: medium
- Archive/Castle: rich
- Ritual/Sanctum: rare ceremonial density

Audit rule:
Do not allow the shared component system to flatten realms into one card language.

## P2-02 — Library advanced systems should remain subordinate to retrieval
The rebuild corrected the primary order to:
header -> search -> shelf filters -> controls -> books -> memory/deep shelf/wings/overview.

Keep this invariant.

## P2-03 — Primary mobile navigation should stay reading-first
Current rebuild mobile dock:
- Home
- Library
- Notebook
- More

Castle remains reachable from Threshold; Path remains reachable through Castle/world flow.

Do not restore a five-item permanent world-system dock unless evidence proves it improves reading retrieval.

## P2-04 — Book Detail should remain a destination, not regress to a generic sheet
The rebuild promotes Book Detail to a full-screen narrative surface.

Required later proof:
- mobile portrait
- large text
- landscape/tablet
- long title/author
- RTL
- missing cover
- finished/re-read state
- metadata edit round-trip

## P2-05 — Settings progressive disclosure needs interaction proof
The rebuild changes Settings from an always-expanded dump into collapsible architectural sections.

Verify later:
- header tap only toggles the section
- nested sliders/switches do not collapse the parent
- state survives rotation where appropriate
- TalkBack exposes expansion state clearly

---

# Confirmed KEEP items

## Reading durability
- `ReaderCloseDurability.awaitDurableReaderClose()` keeps route state alive until repository durability completes.
- lifecycle paths take `FINAL_SNAPSHOT` before pause/stop/destroy.
- progress/session writes are coalesced but can be immediately flushed.
- `flushWrites()` provides a queue barrier.
- completion progress, session snapshot and sealed cycle are transactionally coupled.

## Locator/page-turn correctness
- observations and commits are separated.
- committed locators are deduplicated without swallowing a later real commit.
- page pacing is rebased on resume.
- regression tests cover first real turn after a pacing baseline.

## Import/storage
- selected documents are copied into app-private storage.
- SHA-256 content fingerprints prevent duplicate import records.
- cover cache is derived and replaceable.
- restricted publications fail explicitly.
- Room is the structured library source of truth.

## PDF
- direct zoom controls operate on the actual PDFView.
- PDF paged vs continuous layout is supported.
- rebuild fix prevents changing PDF layout from erasing the saved EPUB page-turn effect.

## Release optimization
- release minification enabled
- resource shrinking enabled
- baseline profile plugin installed
- profile installer included
- macrobenchmark module exists

## RTL/adaptive
- manifest supports RTL.
- Arabic-script grouping includes Persian, Arabic, Urdu, Pashto and Sorani.
- navigation-direction tests cover RTL mirroring.
- adaptive navigation policy has unit coverage.

---

# Rebuild changes already under Arena control

1. moved rebuild base to the advanced Grayfog branch instead of obsolete integration RC
2. installed Arena on `alpha/arena-rebuild-v2`
3. rebuilt compact mobile dock around Home / Library / Notebook / More
4. fused Threshold art and resume folio more tightly
5. made Library retrieval-first
6. converted shelf status controls from oversized cards to compact filters
7. promoted Book Detail from bottom sheet to full-screen destination
8. changed Settings to progressive disclosure
9. made Hidden Archive default to Notes and elevated its title hierarchy
10. separated Reader layout mode from page-turn effect:
   - Paged / Scroll
   - Paper / Slide / None
11. preserved page-turn effect while temporarily using Scroll
12. preserved EPUB page-turn effect when changing PDF layout
13. localized new rebuild navigation / movement controls
14. retained advanced memory systems instead of recreating them

---

# Full-app next repair order

## Wave A — correctness and coherence
1. finish localization sweep across all production surfaces
2. typed/localized app error model
3. move gamification persistence to a durable store
4. add missing world/settings/archive regression tests

## Wave B — visual rebuild completion
1. Book Detail final composition
2. Hidden Archive / Notebook material hierarchy
3. Settings row/icon rhythm
4. Castle / Path / Observatory visual harmonization
5. Profile / More information architecture
6. empty/error/loading state art direction

## Wave C — interaction polish
1. motion/reduced-motion consistency
2. haptic/audio event audit
3. large-font / TalkBack / RTL pass
4. foldable/tablet/landscape pass
5. long-title / long-author / missing-metadata stress cases

## Wave D — verification (only when design is ready)
1. build
2. unit suite
3. instrumented suite
4. startup/profile generation
5. macrobench
6. device visual proof
7. release package verification

---

# Arena conclusion

Veil Reader is not an empty prototype and should not be rebuilt from zero.

The advanced Grayfog branch already contains a serious reading engine, durable local library, historical memory, paper curl, PDF direct manipulation, Archive/Notebook, adaptive layout, RTL groundwork, Castle/Path systems and performance infrastructure.

The highest-value strategy is selective reconstruction:
- KEEP the reading/data engines.
- REFINE the world/UI systems.
- REBUILD presentation hierarchy where the reference demands it.
- FIX localization and gamification durability before calling the product complete.
- VERIFY only after the visual rebuild stabilizes.

Current state: **SOURCE-AUDITED / REBUILD ACTIVE / NOT DEVICE-GREEN**
