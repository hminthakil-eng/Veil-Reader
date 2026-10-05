# Veil Reader — Reforge 100 Master Modernization Plan

Date: 2026-10-05  
Owner: Veil Reader Grand Forge + Arena  
Canonical execution branch: `grand-forge/arena-app-hardening-v1`  
Product-owner baseline: **10/100 RED**  
Target: **92+/100 before “world-class” claims; 100 is an ongoing perfection bar, not a marketing label.**

---

## 0. Purpose

Veil Reader already contains many capabilities, but feature count has outrun product finish. The current problem is not lack of ambition; it is incomplete composition, inconsistent detail quality, half-finished behavior, insufficient device proof, and too many surfaces that technically exist without reaching commercial usability.

This plan converts the project from “feature accumulation” to **product completion**.

No feature is GREEN because code exists. No screen is GREEN because it looks good in one screenshot. No subsystem is GREEN because unit tests pass in isolation.

The program goal is:

**10 → 40:** repair layout, hierarchy, broken interactions and incomplete flows.  
**40 → 65:** close functional holes, edge cases and resilience.  
**65 → 80:** polish typography, motion, responsiveness, Persian/RTL and power-user ergonomics.  
**80 → 92:** physical-device tuning, tactile quality, visual consistency and performance proof.  
**92 → 100:** obsessive refinement, optical tuning and elimination of remaining friction.

---

# Engineering and design adoption rules

1. **Stable-first.** Stable Android/AndroidX releases are the production default. Alpha/beta APIs may enter only as isolated pilots behind capability gates or feature flags.
2. **Newest is not automatically best.** Upgrade only when compatibility, tests, performance and migration costs are understood.
3. **No stable-core rewrite for fashion.** Readium, durable Reader state, Room ownership and exact locator semantics remain unless evidence proves a defect.
4. **No parallel architecture.** One canonical Reader, one canonical progress model, one navigation ownership model per interaction.
5. **Local/offline-first remains the product contract.** Cloud is optional, never required for normal reading.
6. **Content first.** Atmosphere may frame content but may never delay the primary reading/retrieval task.
7. **Accessibility is a release gate.** 48dp interaction targets, 200% text, TalkBack, Switch Access, contrast, reduced motion and keyboard navigation are not post-release polish.
8. **Persian/RTL is first-class.** Never shrink Persian text to solve layout. Adapt composition instead.
9. **Feature truthfulness.** Never expose a control whose renderer/backend cannot reliably fulfill it.
10. **Every new dependency needs a reason, license check, security check, size cost and removal path.**
11. **Paid tooling is optional.** The product must not depend on a paid design/research service to progress.
12. **Evidence is tied to a SHA.** Screenshots, metrics and test claims must state the executable revision.

---

# Definition of Done — applies to every feature and screen

A task is not DONE until all applicable items are proven:

- Happy path works end-to-end.
- Invalid input and boundary cases fail calmly.
- No dead control, fake state, placeholder action or unreachable capability remains.
- State survives rotation/window change where appropriate.
- Critical state survives backgrounding/process recreation.
- Loading, empty, error, retry and cancel states are designed.
- Persian, RTL and mixed-script content are tested.
- 200% text retains all essential functionality.
- TalkBack / Switch Access semantics and focus order are correct.
- 48dp minimum interactive targets are preserved.
- Compact portrait, landscape, tablet/foldable and resizable-window behavior is verified where applicable.
- Performance and memory are within budgets.
- Regression tests cover ownership/correctness.
- Visual evidence is attached to the exact executable SHA.
- Physical-device evidence exists for tactile/rendering claims.
- The feature has one obvious removal/revert path if it fails release gates.

---

# Current technology position — 2026-10-05

Current Veil stack already has a strong foundation:

- Android Gradle Plugin 9.4.0
- Room 2.8.5
- Baseline Profile plugin 1.5.0
- Material3 1.4.0
- Material3 Adaptive 1.3.0
- Readium Kotlin Toolkit 3.4.0
- DataStore 1.2.1
- Coil 3.6.3
- R8 + resource shrinking
- Macrobenchmark / benchmark module
- Edge-to-edge enabled
- minSdk 26

Immediate technology findings:

- Compose UI/Foundation 1.10.6 should be upgraded and qualified against stable 1.12.1.
- Android 17 / API 37 is still a preview/beta target. Keep an API 37 compatibility lane, but public release targeting should stay on the latest final production SDK until Android 17 becomes final.
- Navigation3 1.2.0 is a stable candidate for shell/list-detail modernization, but Reader session ownership must not be migrated merely for novelty.
- WorkManager 2.12.0 is a strong candidate for persistent offline sync queues.
- Media3 1.11.x is appropriate for background TTS/media-session ownership if that path is adopted.
- AndroidX PDF 1.0.0-beta01 is promising for PDF annotation/editing/OCR/two-page features, but remains a pilot candidate rather than a production replacement for current Readium/Pdfium.
- Readium 3.4.0 is current and should remain the canonical publication engine.
- Kotlin 2.4.x must not be adopted until KSP/Room/toolchain compatibility is proven; the current green Kotlin/KSP pair is more valuable than a speculative version bump.

---

# R0 — Product truth and toolchain foundation — 12 tasks

### R0.01 — Freeze uncontrolled feature expansion
New feature work requires explicit justification while completion debt is RED.

### R0.02 — Build executable feature inventory
Classify every visible capability:
- Works
- Partial
- Broken
- Unreachable
- Duplicate
- Debug-only
- Planned-only

### R0.03 — Install universal GREEN contract
Use the Definition of Done above in PRs and audits.

### R0.04 — Separate release SDK from Android 17 compatibility lane
Keep API 37 testing isolated until final platform SDK; release configuration must target a final publishable API.

### R0.05 — Upgrade Compose stable line
Dedicated reversible branch:
- UI 1.10.6 → 1.12.1
- Foundation 1.10.6 → 1.12.1
- tooling/test artifacts aligned
- full Reader/Grayfog regression
- capture before/after rendering differences

### R0.06 — Keep current stable core dependencies
Do not churn AGP 9.4.0, Room 2.8.5, Material3 1.4.0, Adaptive 1.3.0, Readium 3.4.0 without a concrete reason.

### R0.07 — Hold Kotlin upgrade behind compatibility proof
Prove KSP/Room compatibility before 2.4.x adoption.

### R0.08 — Lint warning triage
Classify every warning:
- real defect
- cleanup
- intentional
- tooling false-positive
No blanket suppressions.

### R0.09 — Enable strict debug diagnostics
Add bounded StrictMode policies for disk/network/main-thread misuse and leaked closables.

### R0.10 — Dependency / license / SBOM gate
Track direct/transitive licenses, vulnerabilities, package size and origin.

### R0.11 — Build SHA-linked quality dashboard
Show:
- current head
- unit/instrumentation status
- lint
- profile packaging
- performance
- screenshot revision
- physical-device state

### R0.12 — Create broken-feature kill switch policy
Any unstable optional feature must be independently disableable without corrupting user state.

---

# R1 — Architecture and maintainability — 14 tasks

### R1.01 — Decompose ReaderScreen
`ReaderScreen.kt` is ~6.6k lines. Split by ownership, not arbitrary file size:
- session lifecycle
- input arbitration
- chrome
- appearance
- navigation transaction
- selection/notes
- TTS
- PDF controls
- material page presentation
Preserve the canonical state machine.

### R1.02 — Decompose LibraryScreen
`LibraryScreen.kt` is ~4.3k lines. Separate:
- query/retrieval model
- Gallery
- Shelves
- Index
- Book Detail
- metadata edit
- import/recovery
- archive-derived utilities

### R1.03 — Decompose VeilApp shell/router
Separate shell/navigation orchestration from screen content and data bootstrapping.

### R1.04 — Decompose Settings by domain
Reader / Accessibility / Sensory / Library / World / Diagnostics.

### R1.05 — Decompose Castle by spatial layer
Map, chamber, progression inscription, environment mutation and ritual surfaces.

### R1.06 — Immutable UI state contracts
Use explicit immutable screen state where mutable composition-local state is currently duplicated.

### R1.07 — Unidirectional data flow
State down, user intent up; avoid screens mutating shared data from multiple side channels.

### R1.08 — Put state at the lowest correct owner
Remove duplicate remembered mirrors unless needed for draft/transaction semantics.

### R1.09 — Standard async ownership token
Every async result tied to Reader/book/session/window generation must prove it still owns the target before applying.

### R1.10 — Cancellation correctness
Never swallow `CancellationException`; audit all `runCatching`, retry and backfill paths.

### R1.11 — Replace fragile one-shot event plumbing
Use state or durable event records where process recreation matters.

### R1.12 — Navigation3 pilot for shell only
Evaluate Navigation3 1.2.0 for:
- shell back stack
- settings/archive destinations
- list/detail
- adaptive layouts
Do not migrate Reader session ownership unless equivalence is proven.

### R1.13 — Predictive Back contract
Every modal, destination, Reader preview and edit state must have explicit predictive-back behavior.

### R1.14 — Deep-link/import restoration model
External EPUB/PDF open must survive interrupted import/open and process recreation without duplicate books.

---

# R2 — Design system and visual foundation — 15 tasks

### R2.01 — Rebuild spacing rhythm
Adopt a disciplined 4dp micro / 8dp macro grid with documented optical exceptions.

### R2.02 — Semantic design tokens
Replace repeated raw dimensions/alphas with role-based tokens where repetition has product meaning.

### R2.03 — Realm density budgets
Lock:
Sanctuary < Threshold < Archive < Castle/Ritual.

### R2.04 — Content-first composition law
Every first viewport must justify each non-content element.

### R2.05 — Optical Latin typography pass
Size, leading, measure, weight, tracking, baseline and hierarchy.

### R2.06 — Optical Persian typography pass
Independent Persian line-height, hierarchy, baseline and density tuning.

### R2.07 — Mixed-script typography
English book titles in Persian UI and Persian titles in English UI must remain optically balanced.

### R2.08 — Contrast contract
Meet at least WCAG AA:
- normal text 4.5:1
- large text 3:1
Use stronger product-specific targets for critical text.

### R2.09 — 48dp interaction floor
Audit every icon/text action, including debug and dense Index controls.

### R2.10 — State language
Define visual behavior for:
default / pressed / selected / focused / disabled / loading / error / success.

### R2.11 — Motion grammar
Centralize:
- durations
- easing
- distance
- interruption
- reduced-motion equivalents

### R2.12 — Haptic grammar
Define semantic levels for navigation, mark, note, page lift/turn, advancement, error and confirmation.

### R2.13 — Surface grammar
Document when to use:
- open field
- ruled record
- parchment artifact
- cool instrument plate
- chamber
- modal
Stop component-style leakage across realms.

### R2.14 — Ban decorative UI without function
No universal rounded cards, gratuitous borders, mystical labels for ordinary utility, or ornament that delays reading.

### R2.15 — Living component catalogue
Capture every shared component in English/Persian, normal/high contrast, 100/200% text.

---

# R3 — Threshold, navigation and shell — 11 tasks

### R3.01 — Continue Reading becomes the primary returning-user action
Current book must enter the first scan path immediately.

### R3.02 — Condense recurring Threshold narrative
Large cinematic copy appears only when it carries state meaning.

### R3.03 — Reserve full cinematic entrance
Use full entrance treatment for:
- first launch
- empty library
- major milestone
- meaningful world transition

### R3.04 — Reduce hero vertical budget
Preserve atmosphere without pushing current book below the fold.

### R3.05 — Faster recent-book scanning
Improve cover/title rhythm and reduce dead vertical space.

### R3.06 — Navigation optical pass
English/Persian/200% text, selected/inactive contrast and icon alignment.

### R3.07 — Essential labels never truncate ambiguously
Reflow or use accessible alternatives.

### R3.08 — Predictive back across shell
Settings, Archive, chambers and modal destinations.

### R3.09 — Edge-to-edge/system-bar ownership
Each realm owns light/dark icon appearance and safe insets deliberately.

### R3.10 — One-hand reach audit
Primary recurring mobile actions should stay in comfortable thumb zones.

### R3.11 — Shell state restoration
Selected tab, open overlay/destination and relevant query state survive recreation.

---

# R4 — Library / Archive retrieval rebuild — 17 tasks

### R4.01 — First book appears much earlier
The first viewport should contain actual library content whenever possible.

### R4.02 — Collapse secondary facets
Collection/Series/Sort become compact or disclosed when inactive.

### R4.03 — Reduce Search visual mass
Search must be easy to hit but not visually dominate the library.

### R4.04 — Reduce Reading State visual mass
Convert large panel treatment to compact retrieval instrument.

### R4.05 — Build compact retrieval rail
Search state + active filters + mode + sort in a coherent hierarchy.

### R4.06 — Preserve three real modes
Gallery / Shelves / Index remain genuinely different products.

### R4.07 — Use Index as density benchmark
Do not over-compress Gallery, but close the current excessive gap.

### R4.08 — Gallery record refinement
Reduce record height, duplicate fallback titles and dead space.

### R4.09 — Shelves spatial continuity
Books should share a real datum/shelf rather than isolated mini cards.

### R4.10 — Persist query/filter/mode/scroll
Restoration must be deterministic after navigation and process recreation.

### R4.11 — Search suggestions/history only if useful
No UI clutter merely because the feature is common.

### R4.12 — Clear/reset is always obvious and reversible
Never trap users in hidden filtered states.

### R4.13 — Separate bulk mode
Multi-select/archive actions must not pollute ordinary browsing.

### R4.14 — Counts only when informative
Avoid repeated numbers that create visual noise.

### R4.15 — Missing/corrupt publication recovery
Relink, inspect, remove, retry.

### R4.16 — Large-library performance
Benchmark 1k and 10k local items for query latency, scrolling and memory.

### R4.17 — Full state matrix
Empty / no-result / loading / storage failure / permission loss / keyboard and mouse traversal.

---

# R5 — Book detail, import and format ingestion — 15 tasks

### R5.01 — One dominant Read/Continue action
All secondary actions visually subordinate.

### R5.02 — Long metadata resilience
Very long title, author, series, collection and mixed-script combinations.

### R5.03 — Missing metadata design
Unknown author/series/cover must look intentional, not broken.

### R5.04 — Provenance and metadata edit
Clearly distinguish publication metadata from user edits.

### R5.05 — Low-storage import recovery
Predict, surface and recover cleanly.

### R5.06 — Import progress / cancel / rollback
No ghost book after failed import.

### R5.07 — Duplicate-resolution flow
Explain same-content detection and let user choose safe outcomes.

### R5.08 — Relink missing file
Repair moved/deleted source when possible.

### R5.09 — Tier A native formats
EPUB and PDF remain canonical highest-quality paths.

### R5.10 — Tier B comics
CBZ/image publication remains a distinct image-reader path.

### R5.11 — Evaluate Readium audiobook support
Pilot only after text Reader quality is stable.

### R5.12 — Tier C normalized text ingestion
TXT / HTML / Markdown may convert into an internal publication representation.

### R5.13 — License-gate legacy format support
MOBI/AZW/FB2/DjVu support requires explicit library/license/security evaluation before embedding third-party parsers.

### R5.14 — OPDS after local core
Do not add catalog browsing before local retrieval is excellent.

### R5.15 — Android open/share matrix
SAF, file manager, messaging attachment, browser download, external `ACTION_VIEW`, repeated-open and revoked-permission cases.

---

# R6 — Reader core and page engines — 21 tasks

### R6.01 — Physical acceptance matrix for all four modes
Paper / Slide / Paged / Scroll.

### R6.02 — Paper release-velocity physics
Actual release velocity influences completion/cancel behavior appropriately.

### R6.03 — Zero native-slide leakage under Paper
One gesture, one owner.

### R6.04 — Zero page-turn overlay in Scroll
Continuous flow remains renderer-owned.

### R6.05 — PDF independence
No EPUB page effect can mutate PDF behavior.

### R6.06 — First-turn reliability
Cold/opened/recreated session first turn must never fail silently.

### R6.07 — EGL/context recreation
Repeated loss/recreation cannot strand Paper in INITIALIZING.

### R6.08 — Paper material tuning
Contact shadow, crease, backside, translucency, thickness and light response.

### R6.09 — Reduce texture fatigue
Hours-long reading is more important than screenshot drama.

### R6.10 — Appearance ownership
Clear global defaults vs per-book overrides.

### R6.11 — Truthful live preview
Preview and committed renderer state converge before close.

### R6.12 — Keep Quick actually quick
Theme + size + reading mode + only essential controls.

### R6.13 — Advanced power controls
Retain expert typography without exposing unsupported settings.

### R6.14 — User font import
Validate font files, isolate malformed input, per-book/global mapping, safe fallback and Persian shaping tests.

### R6.15 — E-Ink mode
Static paged, high contrast, minimal animation, low-refresh invalidation.

### R6.16 — Auto-scroll
Touch pauses, adjustable speed, reduced-motion policy and persistence.

### R6.17 — Pinch text scaling
EPUB only; never conflict with PDF/fixed-layout zoom.

### R6.18 — Optional edge gestures
Brightness/font size only if gesture arbitration proves safe.

### R6.19 — Reading ETA
Use rolling personal pace with uncertainty, not false precision.

### R6.20 — Chapter page estimates
Only if stable for the current appearance profile; invalidate correctly after reflow.

### R6.21 — Hardware input
Keyboard, mouse, page keys, volume-key policy and accessibility arbitration.

---

# R7 — Selection, notes, search and knowledge layer — 16 tasks

### R7.01 — Reliable native long-press selection
No overlay hack that steals Readium/WebView selection.

### R7.02 — Stable annotation locators
Highlight/note must return to exact content after restart/reflow where possible.

### R7.03 — Annotation styles
Color plus non-color semantic distinction for accessibility.

### R7.04 — Atomic note editor
Draft survives process death and commits exactly once.

### R7.05 — Exact return from Notebook
Never approximate unless clearly stated.

### R7.06 — Streamed/debounced book search
Responsive cancellation and bounded result rendering.

### R7.07 — Global metadata search index
Evaluate Room FTS for title/author/series/collection/note/highlight metadata.

### R7.08 — Layered dictionary sheet
Offline/installed dictionary first where available.

### R7.09 — Optional translation
Explicit provider, privacy disclosure and no mandatory network dependency.

### R7.10 — Optional Wikipedia/web knowledge
Separate from dictionary and book-local search.

### R7.11 — Book-local search action
Selection can search within current publication.

### R7.12 — Future local X-Ray-like entity layer
Only after reliable source extraction; no hallucinated facts.

### R7.13 — Annotation export
Markdown / HTML / JSON.

### R7.14 — Provenance
Export book/title/location/quote metadata truthfully.

### R7.15 — Simplify Hidden Archive taxonomy
Power features should not require understanding internal lore categories.

### R7.16 — Keyboard/a11y shortcuts
Selection actions, search results and notebook must work without touch.

---

# R8 — PDF workspace modernization — 13 tasks

### R8.01 — Keep current Readium/Pdfium production owner
Do not replace a working PDF reader prematurely.

### R8.02 — AndroidX PDF isolated pilot
Evaluate 1.0 beta branch separately.

### R8.03 — API/minSdk capability gate
Current Veil minSdk is 26; PDF pilot must not silently break 26–27.

### R8.04 — Evaluate EditablePdfViewerFragment + AndroidX Ink
Real pen/highlighter/eraser only if persistence and performance are acceptable.

### R8.05 — Real PDF annotation persistence
Undo/redo and saved-document round trip.

### R8.06 — Form filling pilot
Only if stable and useful.

### R8.07 — Two-page tablet/fold layout
Use only where page geometry and window posture justify it.

### R8.08 — OCR scanned PDF pilot
Search/select/read scanned documents where OCR quality permits.

### R8.09 — Annotation list
Exact page/location navigation.

### R8.10 — PDF navigation aids
Page label, thumbnails, outline/TOC and scrubber as supported.

### R8.11 — Keyboard/mouse PDF controls
Desktop-window and tablet productivity.

### R8.12 — A/B benchmark
Current Pdfium vs AndroidX PDF memory, startup, scroll, zoom, search, links, annotations.

### R8.13 — Migration gate
No default-engine switch until parity, migration and rollback are proven.

---

# R9 — TTS and accessibility — 15 tasks

### R9.01 — Complete EPUB TTS controls
Play / pause / previous / next / stop.

### R9.02 — Voice, rate and pitch
Live preview and persistent settings.

### R9.03 — Read-along highlighting
Sentence/word where backend timing permits.

### R9.04 — Background resume
Reading should continue predictably when allowed.

### R9.05 — Media3 MediaSessionService pilot
Use for proper media controls/notification if background TTS is adopted.

### R9.06 — Sleep timer and audio focus
Correct interruption behavior.

### R9.07 — PDF TTS only with reliable extraction
Never pretend scanned/image-only PDF is readable without OCR.

### R9.08 — Focus Guide / Reading Line refinement
Keep reader-owned, non-touch-stealing overlay.

### R9.09 — Automated Compose accessibility checks
Run in CI where supported.

### R9.10 — Manual TalkBack matrix
Every primary surface and modal.

### R9.11 — Switch Access matrix
Focus grouping and reachability.

### R9.12 — 200% text contract
No lost function and no unnecessary two-dimensional scrolling.

### R9.13 — High contrast / color-independent semantics
Meaning cannot rely only on brass/red/green.

### R9.14 — RTL progression independence
Book progression direction follows publication semantics, not blindly the app locale.

### R9.15 — Reduced Motion completeness
Every meaningful transition has an equivalent, not merely a faster animation.

---

# R10 — Sync, backup and data resilience — 15 tasks

### R10.01 — Room remains immediate local authority
UI never waits for network to read current state.

### R10.02 — Optional account/sync
Normal reading remains account-free.

### R10.03 — Operation log
Idempotent event IDs for syncable mutations.

### R10.04 — Tombstones
Deletion sync cannot resurrect removed notes/bookshelves.

### R10.05 — Sync entity streams independently
Progress, bookmarks, highlights, notes, settings/history have different conflict rules.

### R10.06 — Preserve meaningful history
Do not reduce progress/history to one destructive “last write wins” blob.

### R10.07 — Deterministic conflict rules
Document per entity.

### R10.08 — WorkManager 2.12 queue
Persistent retries, constraints and backoff.

### R10.09 — Quiet sync status
Never block reading with a spinner for background sync.

### R10.10 — Protect credentials and sensitive payloads
Platform-backed key material and minimal token storage.

### R10.11 — Do not upload publication files by default
Metadata/progress sync is separate from book-file cloud storage.

### R10.12 — User-owned backup/export
Portable, versioned and inspectable.

### R10.13 — Restore preview
Show what will change before destructive replacement.

### R10.14 — Migration suite
Every schema/export version has upgrade tests.

### R10.15 — Adversarial sync tests
Airplane mode, flaky network, duplicate devices, clock skew, simultaneous edits and reinstall.

---

# R11 — Castle, world systems and gamification — 14 tasks

### R11.01 — Castle must read as architecture
Not a dashboard wearing arches.

### R11.02 — Reduce repeated chamber-panel grammar
More environment, fewer obvious containers.

### R11.03 — Strong floor/portal continuity
Spatial transitions must visually connect.

### R11.04 — Reading history mutates environment
Only real user actions create visual world changes.

### R11.05 — Earned world changes persist
Achievements do not vanish because a derived view changed.

### R11.06 — Never interrupt reading with rewards
Progression reveals after natural boundaries.

### R11.07 — Gamification durability equals reading durability
No weaker persistence path.

### R11.08 — Reading calendar/year statistics
Useful reflection, not vanity analytics.

### R11.09 — Truthful insights
No fabricated precision or invented personality judgments.

### R11.10 — Observatory evidence
Relationships must be source-derived and explainable.

### R11.11 — Ritual is short, skippable and reduced-motion safe
Ceremony never traps the user.

### R11.12 — Sanctum rarity through restraint
Rarity is not “more glow.”

### R11.13 — Accessible map semantics
Every room/edge/status has a meaningful nonvisual representation.

### R11.14 — Tablet/fold spatial composition
Use extra width to create world structure, not stretched phone panels.

---

# R12 — Performance, memory, battery and release proof — 19 tasks

### R12.01 — Keep benchmark tooling current
Use Benchmark 1.5.x stable line and verify harness behavior after upgrades.

### R12.02 — Startup macrobenchmarks
Cold / warm:
- Home
- Library
- direct-open book

### R12.03 — Frame timing benchmarks
Library scroll, Reader turn, PDF zoom/scroll, Castle navigation.

### R12.04 — Gate P95/P99 frame overrun
Do not hide bad tails behind average FPS.

### R12.05 — Generate Baseline + Startup Profiles
Real critical journeys, not synthetic no-op profiles.

### R12.06 — Package profile verification
Prove profile exists in release APK/AAB.

### R12.07 — Report fully drawn
Mark meaningful first usable content.

### R12.08 — Compose recomposition audit
After 1.12 upgrade, identify hot invalidation and unstable parameters.

### R12.09 — Cover/art downsampling
Decode to actual presentation bounds.

### R12.10 — Bitmap memory budgets
Track large cover, PDF and manga bitmap usage, especially under Android 17 testing.

### R12.11 — GPU page-buffer budget
Bound Paper snapshots/textures across repeated turns and context recreation.

### R12.12 — Leak/StrictMode pass
Activities, Fragment navigators, AccessibilityNodeInfo, bitmaps, streams and TTS engines.

### R12.13 — Battery/background audit
No unnecessary wakeups, polling or long-lived work.

### R12.14 — R8/resource-shrink release validation
No reflection/resource breakage.

### R12.15 — AAB/APK package inspection
Size, ABI, resources, native libs and profiles.

### R12.16 — Representative hardware matrix
At minimum:
- Pixel/reference Android
- Samsung flagship
- lower/mid device
- tablet or foldable

### R12.17 — 60/90/120Hz acceptance
Paper/Slide frame pacing and touch response.

### R12.18 — Android 17 compatibility lane
Run preview-specific behavior checks without forcing preview SDK onto public release prematurely.

### R12.19 — Release evidence bundle
No RC without:
- exact SHA
- CI results
- instrumentation
- screenshots
- physical-device evidence
- performance
- package/profile proof
- known-risk list

---

# Canonical execution order

The implementation order is intentionally not “R0 to R12 then stop.” It is organized into product waves:

## Wave 1 — Stop the bleeding
R0.01–R0.12  
R1.01–R1.10  
R2.01–R2.15

Exit: modern stable toolchain, explicit quality contract, decomposable codebase, consistent design primitives.

## Wave 2 — Make the visible product usable
R3 all  
R4 all  
R5.01–R5.08

Exit: Threshold, navigation, Library and Book Detail feel like a finished commercial app.

## Wave 3 — Make reading excellent
R6 all  
R7.01–R7.06  
R9.01–R9.03, R9.08–R9.15

Exit: Reader is physically pleasant, mode ownership is flawless, appearance and accessibility are excellent.

## Wave 4 — Power-reader parity
R7.07–R7.16  
R5.09–R5.15  
R9.04–R9.07

Exit: search, lookup, export, formats and TTS no longer force serious readers into another app for common tasks.

## Wave 5 — PDF professionalization
R8 all

Exit: PDF path is competitive without sacrificing current reliability.

## Wave 6 — Resilient ecosystem
R10 all

Exit: optional sync/backup is genuinely offline-first and safe.

## Wave 7 — Veil-specific moat
R11 all

Exit: Castle/Archive/world systems are an earned product advantage rather than decorative complexity.

## Wave 8 — Release perfection
R12 all plus a full cross-feature regression.

Exit: 92+ candidate, eligible for product-owner visual/tactile acceptance.

---

# First 20 implementation tickets

To begin one-by-one, the initial sequence is:

1. R0.02 executable feature inventory.
2. R0.03 universal GREEN contract in repo/PR template.
3. R0.04 release SDK vs Android 17 compatibility split.
4. R0.05 Compose 1.12.1 qualification branch.
5. R0.08 lint warning triage.
6. R0.09 StrictMode debug diagnostics.
7. R1.01 ReaderScreen ownership-preserving decomposition.
8. R1.02 LibraryScreen decomposition.
9. R2.01 spacing rhythm.
10. R2.02 semantic design tokens.
11. R2.04 content-first composition law.
12. R2.05–R2.07 Latin/Persian/mixed-script optical typography.
13. R3.01 Continue Reading first-action correction.
14. R3.02 recurring Threshold copy condensation.
15. R4.01 first-book viewport correction.
16. R4.02 secondary-facet disclosure.
17. R4.03 Search visual-mass reduction.
18. R4.04 Reading-State visual-mass reduction.
19. R4.05 compact retrieval rail.
20. R4.08 Gallery record/fallback-cover refinement.

These are chosen because they attack the user-visible 10/100 problem before adding more novelty.

---

# Research basis

Official/current sources used for this plan include:

- Android Developers — Android Studio / AGP / Jetpack release notes
- Android Developers — Android 17 setup and behavior changes
- Google Play target API requirements
- Android Developers — Compose / Material3 / Adaptive / Navigation3
- Android Developers — app architecture and offline-first data guidance
- Android Developers — accessibility in Compose
- Android Developers — Macrobenchmark and Baseline Profiles
- Android Developers — Android vitals / performance / memory
- AndroidX PDF release notes
- AndroidX WorkManager and Media3 release notes/guides
- Readium Kotlin Toolkit 3.4 release documentation
- W3C WCAG 2.2
- Apple Design Principles and Human Interface Guidelines
- Apple Books current reader controls
- Kindle accessibility/reading customization documentation
- Kobo reading-menu guidance
- Google Play Books selection/annotation guidance

Commercial products are used as behavior references only. No closed-source proprietary implementation is assumed or copied.

---

# Final program rule

**The target is not “more features.” The target is that every feature we keep earns its place, works completely, survives real-world failure, and feels intentionally designed.**

Grand Forge should prefer:
- deleting friction over adding ornament,
- finishing one capability over creating three partial ones,
- real device proof over source confidence,
- direct user language over lore for utility actions,
- reversible pilots over architecture churn,
- calm reading over visual spectacle.

Only after the existing product becomes excellent should feature expansion accelerate again.
