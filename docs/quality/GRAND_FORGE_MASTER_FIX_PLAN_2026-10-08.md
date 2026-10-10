# Veil Reader Grand Forge — Master Fix Plan

Evidence date: 2026-10-08 UTC  
Execution branch: `grand-forge/p0-kindle-reader-quality-20261008`  
Baseline parent: `codex/reader-quality-hardening-20261007`

This is an execution document, not a feature wishlist. A capability is GREEN only when all applicable implementation, integration, reachability, automated-test, physical-device, UX, performance and release gates are satisfied.

## Live repository truth

- `main` is not the canonical Reader development tip.
- Aggregate tracking/review is PR #429. Bounded stacked reviews #430–#435 keep subsystem changes independently reviewable.
- Current Grand Forge work already contains durability hardening for bookmarks/highlights/notes, precise navigation admission, Paper lifecycle fencing, adaptive columns, foreground TTS checkpoint work and a validated multilingual typography corpus.
- Physical Paper quality, audible TTS, TalkBack, rendered RTL, low-memory, battery/thermal and physical-device performance remain unverified and therefore cannot be GREEN.
- Rechecked 2026-10-10: `ReaderNavigationSessionState` and explicit `ReaderNavigationCommitPolicy` are integrated. Search/TOC/reference destinations preserve the reading anchor; lifecycle checkpoints are suppressed while exploring. Complete Android/device acceptance remains outstanding. Earlier unconditional-commit descriptions were stale.

## Status vocabulary

- GREEN: all applicable gates passed.
- YELLOW: implemented/integrated but evidence incomplete.
- RED: user-visible correctness or architecture gap exists.
- BLOCKED: cannot be responsibly promoted because a required dependency/evidence source is unavailable.
- HOLD: intentionally deferred behind higher-priority work.

# P0 execution graph

## P0.1 Local durability and acknowledgement

### GF-P0-DUR-01 — Progress durability
Priority: P0  
Subsystem: Reader persistence  
User impact: accepted reading progress must survive process death.  
Primary files:
- `app/src/main/java/com/veilreader/app/data/LocalLibraryRepository.kt`
- `app/src/main/java/com/veilreader/app/ui/reader/ReaderViewModel.kt`
- `app/src/main/java/com/veilreader/app/ui/reader/ReaderLocatorPolicy.kt`
- crash-checkpoint storage files already used by the Reader
Tests:
- Room runtime instrumentation
- abrupt process-kill probes
- lifecycle close/pause characterization
Acceptance:
- accepted semantic commits are journal/Room durable;
- stale writer epochs cannot overwrite a newer Reader;
- failed checkpoint never emits success;
- close waits for durability.
Current: YELLOW. Automated fault coverage exists; physical-device lifecycle acceptance remains.

### GF-P0-DUR-02 — Bookmark/highlight/note acknowledgement
Primary files:
- `LocalLibraryRepository.kt`
- `data/db/Daos.kt`
- `ui/screens/ReaderScreen.kt`
- `ui/screens/ArchiveScreen.kt`
- `ui/VeilApp.kt`
Tests:
- `AnnotationDurabilityInstrumentedTest.kt`
Acceptance:
- success means Room commit completed;
- failed create/update/delete never creates phantom visible state;
- retry is idempotent;
- failed note edit retains draft.
Current: YELLOW. Source work is present in PRs #431/#434; full device UX/TalkBack still pending.

## P0.2 Publication identity and import transaction

### GF-P0-ID-01 — Stable publication identity
Primary files:
- publication/domain model
- import coordinator
- Room book entity/DAO
- relink/SAF ownership code
Implementation:
- retain SHA-256/content fingerprint as content identity;
- add explicit source identity and publication identifier where available;
- keep the durable Veil publication ID stable across rename/move/relink;
- duplicate import must attach to existing user data, not create a second identity.
Tests:
- rename/relink/re-import;
- concurrent duplicate imports;
- same filename/different content;
- same content/different filename.
Current: YELLOW. Fingerprint duplicate detection exists; full source/relink identity contract must be characterized.

### GF-P0-IMPORT-01 — Transactional import pipeline
Stages:
Acquire → Validate → Parse → Normalize → Fingerprint → Metadata → Index → Library Commit.
Primary files:
- import coordinator/worker
- `LocalLibraryRepository.kt`
- EPUB/PDF parser adapters
- cover extraction/indexing
Acceptance:
- no partially imported library rows or orphan assets;
- stage-specific error;
- safe rollback;
- hostile/oversized content bounds.
Current: YELLOW/RED by stage; complete forensic pass required before GREEN.

## P0.3 Book-open transaction and capability truth

### GF-P0-OPEN-01 — Deterministic open transaction
Primary files:
- `ui/VeilApp.kt`
- Reader open coordinator/view model
- navigator factory
- settings/appearance policy
Acceptance:
resolve identity → validate source → resolve capabilities → restore stable locator → prepare navigator → apply effective appearance → prepare adjacent content → first readable page → overlays.
Metrics:
TTFR (time to first readable page), P50/P95/worst.
Current: YELLOW.

### GF-P0-CAP-01 — PublicationCapabilities
Primary files:
- new shared domain contract
- EPUB/PDF capability adapters
- Reader controls
- appearance policy
Acceptance:
no control is shown/enabled when the active publication cannot support it; no silent no-op.
Current: RED.

## P0.4 ReadingAnchor / exploration navigation

### GF-P0-NAV-01 — Semantic navigation contract
Primary files:
- `ui/reader/ReaderNavigationTransaction.kt`
- new `ReaderNavigationSemantics.kt`
- `ui/reader/ReaderLocatorPolicy.kt`
- `ui/screens/ReaderScreen.kt`
Tests:
- `ReaderNavigationTransactionTest.kt`
- new semantic policy tests.
Implementation order:
1. Add explicit navigation reason and commit policy without changing current behavior.
2. Classify search/TOC/footnote/bookmark/page-preview/reference/listening-position jumps.
3. Track durable `ReadingAnchor` independently from temporary `ExplorationLocator`.
4. Programmatic exploration settlement must not overwrite durable reading progress.
5. First explicit commit action converts exploration into reading progress.
6. Previous-location return is modeled from semantic history, not a generic mutable slot.
Acceptance:
- search/TOC preview cannot silently destroy reading progress;
- close/process-death during exploration restores the durable anchor;
- a real page turn after exploration can deliberately commit;
- history records reason/origin/destination/commit state.
Current: RED → foundation in progress.

### GF-P0-NAV-02 — NavigationHistory
Primary files:
- new domain model/store
- Reader session coordinator
Fields:
origin, destination, reason, timestamp, committed, publication revision.
Current: RED. Must remain local-first; sync not required for P0.

## P0.5 Input ownership and page engines

### GF-P0-INPUT-01 — Deterministic ReaderAction/Input router
Primary files:
- input arbiter/listeners
- `PaperCurlInputListener.kt`
- slide/static listeners
- hardware key controller
- `ReaderScreen.kt`
Acceptance:
one gesture owner; Paper reserves at DOWN/initial ownership boundary; native/Slide/tap cannot race.
Current: YELLOW. Existing arbiter/lifecycle hardening must be device-proven.

### GF-P0-PAPER-01 — Material Paper activation/readiness
Primary files:
- `MaterialPageEngine.kt`
- GPU curl view/renderer
- `PaperCurlState.kt`
- `ReaderAppearancePolicy.kt`
- feature gates
Acceptance:
- no Slide substitution;
- renderer readiness explicit;
- controlled failure;
- fixed-layout unsupported state explicit until leaf/spread model exists.
Current: RED/BLOCKED for release gate.

### GF-P0-PAPER-02 — Paper prewarm/performance
Focus:
snapshot capture, reusable buffers, GL init, shader compilation, texture upload, first-turn hitch, GC, stale generation, context loss.
Tests/benchmarks:
first turn, 100, 500, alternating direction, rotation, background/foreground, low memory, 60/90/120 Hz.
Current: YELLOW source instrumentation; physical tactile/perf evidence BLOCKED.

## P0.6 Adaptive typesetting and RTL

### GF-P0-TYPE-01 — AdaptiveTypesettingPolicy
Primary files:
- `ui/screens/AdaptiveTypesettingPolicy.kt`
- appearance/settings contracts
- Readium preference mapping
Fixtures:
- `qa/fixtures/veil-typesetting-ltr.epub`
- `qa/fixtures/veil-typesetting-rtl.epub`
Acceptance:
extreme font/accessibility settings degrade gracefully; columns/margins/line-height remain readable.
Current: YELLOW.

### GF-P0-TYPE-02 — Script-aware fallback/user-font safety
Primary files:
font repository/import validation, typeface resolver, appearance settings.
Acceptance:
Persian/Arabic, Latin, CJK, Emoji/Symbol fallback; malformed font cannot destabilize Reader.
Current: RED/YELLOW depending path.

### GF-P0-RTL-01 — Semantic RTL progression
Primary files:
navigation action mapping, Paper/Slide/static input, TOC/search/selection/TTS.
Acceptance:
Next/Previous are semantic; physical direction derived from publication progression.
Device matrix:
Persian/Arabic + Paper + 120Hz; mixed Persian-English; large text landscape.
Current: YELLOW source support / device proof missing.

## P0.7 TTS session

### GF-P0-TTS-01 — Independent listening session
Primary files:
- `ui/reader/tts/ReaderTtsSession.kt`
- `ReadiumReaderTtsSession.kt`
- foreground checkpoint controller
- background service/controller
- `ReaderTtsControls.kt`
Acceptance:
play/pause/resume/stop, next/prev sentence, semantic highlight, checkpoint, return to listening position, audio focus/headset controls.
Current: YELLOW foreground; background BLOCKED by release evidence.

### GF-P0-TTS-02 — Speech text pipeline
Pipeline:
Content → SentenceSegmenter → LanguageDetector → PronunciationRules → VoiceRouter → PlaybackQueue.
Features:
URL/page/header filtering, footnote policy, abbreviation/symbol normalization, pronunciation/regex rules.
Current: RED.

### GF-P0-TTS-03 — Multilingual routing
Acceptance:
language spans route to per-language preferred voice with explicit fallback.
Current: RED/YELLOW.

### GF-P0-TTS-04 — Sleep/end-of-chapter/background QA
Current 10/15/30/45/60 minute controls exist; end-of-chapter still required.
Device evidence:
audible playback, Bluetooth, notification, kill/restart, battery/thermal.
Current: YELLOW/BLOCKED.

## P0.8 Selection, annotations, knowledge tools

### GF-P0-SEL-01 — SemanticTextSelection
Primary files:
selection action mode, Reader selection controller, annotation commands.
Consumers:
highlight, note, dictionary, translation, search, copy, TTS.
Current: RED/YELLOW.

### GF-P0-ANN-01 — Reflow-stable annotation identity
Use locator + quote + prefix/suffix + revision where appropriate.
Current: YELLOW; durability improved, reflow robustness requires corpus/device verification.

### GF-P0-TOOLS-01 — Dictionary/Translation provider architecture
Primary files:
new provider interfaces + compact Reader result UI.
Privacy:
network provider explicit; text leaving device disclosed.
Current: RED.

### GF-P0-SEARCH-01 — Publication search
Requirements:
fast initial results, snippets/chapter context, count, next/previous, visible match, session persistence, exploration navigation.
Current: YELLOW/RED.

## P0.9 PDF core

### GF-P0-PDF-01 — Professional PDF core
Verify:
open, progressive render, page nav, zoom/pan, links, selection, search, rotation, locator/persistence, large/password/error/low-memory.
Current: YELLOW.

PDF authored annotation is P1; advanced markup P2.

## P0.10 Accessibility/adaptive layout

### GF-P0-A11Y-01
Primary files:
Reader chrome/tools, Library surfaces, semantics, focus, reduced motion, non-drag alternatives.
Acceptance:
TalkBack logical focus/roles/state, 48dp-equivalent targets where applicable, large fonts, contrast, keyboard alternatives.
Current: YELLOW; physical TalkBack acceptance missing.

### GF-P0-ADAPT-01
Phone portrait/landscape, tablet, foldable, split-screen, resizable window.
Dual-page policy must consider content, viewport, orientation, font/accessibility scale and user preference.
Current: YELLOW.

# P1 execution graph

P1 starts only when its P0 dependencies are stable; P1 work can be researched in parallel but must not displace P0.

1. Cloud semantic sync:
   - entities: progress/bookmark/highlight/note/collection/preference;
   - stable ID/revision/modifiedAt/deviceId/tombstone;
   - conflict policy per entity, not universal LWW.
2. Backup/restore completion:
   - schemaVersion, validation, migration dry-run, preview, merge/replace, staged commit, integrity/encryption option.
3. User fonts:
   - import/validation/metadata/script support/preview/per-language/per-book.
4. Advanced Library:
   - collections, smart collections, series, filters/sort/search at 10k scale.
5. Reading statistics:
   - calm time/session/streak/book summaries; Reader Core emits events only.
6. Reading themes/custom theme.
7. Folder watch / relink workflows.
8. OPDS/catalog adapter architecture.
9. PDF annotations:
   - highlight, underline, strikethrough, note, persistence/hit testing/export/undo as applicable.
10. Architecture extraction:
   - ReaderRoute, ReaderSessionCoordinator, ReaderNavigationCoordinator, ReaderInputRouter, ReaderChrome, Appearance, Selection, TTS, PDF, Paper, ErrorPresenter;
   - LibraryRoute/ViewModel/State/Toolbar/Search/Grid/List/Filters/Collections/Selection/Empty.
   - Characterization tests before every extraction.

# P2 execution graph

HOLD until quality gates:
- Word-Wise-style assistance;
- X-Ray-style contextual layer;
- DjVu;
- Auto Scroll;
- advanced PDF drawing/shape/text markup;
- network neural TTS;
- deeper catalog integrations;
- extreme customization.

# Cross-cutting release gates

For every subsystem keep:
Feature | Implemented | Integrated | Reachable | Unit | Instrumented | Device | UX | Perf | Release | Status.

Mandatory device scenarios:
- 60/90/120Hz;
- portrait/landscape;
- LTR/RTL;
- normal/accessibility-large font;
- touch/TalkBack/keyboard where relevant;
- phone/tablet/foldable simulation;
- RTL + Paper + 120Hz;
- Persian + mixed-English TTS;
- large font + landscape;
- TalkBack + selection;
- foldable + PDF.

Performance evidence:
Cold Start, Library Open, Book Open, First Page, Page Turn, Paper Curl, Search, TTS Start, PDF Render, Cover Grid — P50/P95/worst-case.

# Immediate commit sequence

1. `docs(quality): establish live Grand Forge master fix plan`
2. `refactor(reader): introduce semantic navigation reason and commit policy`
3. `test(reader): characterize navigation semantic policy`
4. Wire reasons at each programmatic jump.
5. Add ReadingAnchor/Exploration state without changing renderer ownership.
6. Characterize close/process-death while exploring.
7. Only then change programmatic settlement from unconditional progress commit to policy-driven commit.
8. Continue Input/Paper readiness and physical-device evidence.
9. Continue typesetting/RTL.
10. Continue TTS pipeline/device evidence.

No feature gate is promoted by this document. No physical-device claim is inferred from emulator CI.
