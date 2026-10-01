# Arena Review & Execution Plan — Veil Reader

Date: 2026-10-01  
Canonical branch: `alpha/w26-arena-canonical-ui-rebuild-v1`  
Canonical PR: #349  
Audited HEAD: `2a52592991da9e6024f1fe0b9348284784830f3a`

Status: **ARENA-REVIEWED / REBUILD ACTIVE / NOT DEVICE-GREEN**

## 1. Arena verdict

Veil Reader is no longer primarily limited by missing core reading architecture.

The strongest systems are already present and must be preserved:
- EPUB/Readium ownership
- PDF direct manipulation
- durable reading progress and locator history
- notes/highlights/bookmarks
- local import and Room library state
- Paper/Slide/Paged separation
- recovery/navigation transaction logic
- historical memory primitives

The highest remaining product risk is now a combination of:

1. **verification truth** — two red CI gates currently fail in their test/harness contracts before they can prove product quality;
2. **uneven product finish** — Book Detail, Hidden Archive and the World surfaces lag the Reader/data core in authored presentation;
3. **insufficient device evidence** — RTL, TalkBack, 200% text, landscape, tablet/foldable and performance still need real proof;
4. **very large composition files** — Reader and Library are regression hotspots even though their architecture is currently functional;
5. **world-system durability/coverage** — improved, but still less mature than the reading/library trust layer.

Do **not** rebuild Reader or create a parallel architecture.

---

## 2. Current verification truth

### Android CI — GREEN

The latest canonical HEAD passes:
- unit tests
- Android lint
- debug APK compilation
- performance harness compilation
- committed Room schema verification

### Storage Instrumentation — RED, but root cause is verification-contract debt

55 tests ran; 54 passed.

The single failure is:

`ReaderPdfReliabilityInstrumentedTest.importedPdf_exposesFitLayoutAndSurvivesRotation`

The timeout occurs while the test searches for the selected PDF layout node.

The current UI uses Compose `selectable(... role = RadioButton ...)`, which exposes a clickable/checkable node. The test helper currently requires:

`isCheckable && !isClickable`

This conflicts with the actual semantic contract of the control.

Arena classification:
- **Do not treat this as Room/storage failure.**
- First repair the test to target stable semantics/state rather than an invalid clickability assumption.
- Then rerun the gate before changing PDF product code.

### Performance Benchmarks — RED, harness defect before meaningful frame verdict

Baseline/startup profile generation itself succeeded.

Generated:
- `baseline-prof.txt`
- `startup-prof.txt`

The later frame benchmark failed with:

`IllegalStateException: Reader surface accessibility resource is missing from benchmark target`

Root cause in `ReaderFrameBenchmark.kt`:
- it reads `InstrumentationRegistry.getInstrumentation().targetContext`;
- then asks those resources for `reader_surface_label` under `TARGET_PACKAGE`;
- the target context belongs to the benchmark test package, not the app-under-test resource table.

Arena classification:
- this is a benchmark harness/resource lookup bug;
- it is **not evidence of a frame-time regression**;
- fix the lookup/ready-signal contract and rerun before interpreting performance.

---

## 3. Arena strategy tournament

Four directions were considered.

### Strategy A — Visual-first
Continue Book Detail/Castle/Archive polish while leaving red verification gates for later.

Strength:
- fastest visible improvement.

Fatal weakness:
- red gates become increasingly ambiguous as more code changes land.

Verdict: **Rejected as primary strategy.**

### Strategy B — Architecture-first
Split large files, migrate persistence, refactor Reader/Library before further design.

Strength:
- cleaner internals.

Fatal weakness:
- unnecessarily destabilizes the strongest existing systems and delays user-visible convergence.

Verdict: **Rejected.**

### Strategy C — Verification-only
Stop reconstruction until every benchmark/device gate is green.

Strength:
- maximum confidence.

Weakness:
- contradicts the current product directive to finish world-class design before release verification; several current failures are harness defects, not product regressions.

Verdict: **Rejected as exclusive strategy.**

### Strategy D — Verification truth first, then selective flagship reconstruction
Repair invalid gates without expanding release scope, then continue the highest-value visual/product work while preserving Reader/data architecture.

Strength:
- removes false red signals;
- keeps every future regression interpretable;
- advances the product instead of freezing it;
- respects the single-branch contract.

Verdict: **SELECTED.**

---

## 4. Priority map

### Critical now — verification truth

1. PDF layout instrumentation semantic contract.
2. Macrobenchmark app-resource lookup / Reader-ready contract.
3. Rerun only the affected automated gates and classify remaining failures.

Exit:
- test harnesses describe the real UI contract;
- a red gate means a product problem, not a stale assertion.

### Highest product-value work

1. Artifact Chamber / Book Detail final composition.
2. Hidden Archive + Reader Notebook material convergence.
3. Castle / Path / Observatory / Profile convergence.
4. Error/empty/loading/recovery art direction.

### High-value proof work

1. Persian + RTL.
2. 200% text.
3. TalkBack and focus order.
4. landscape/tablet/foldable.
5. missing-cover/long-title/missing-metadata stress cases.
6. real performance/profile evidence.

---

## 5. Execution waves

## W62 — Verification Truth

Goal: make CI signals trustworthy before adding another large semantic wave.

Tasks:
- repair `ReaderPdfReliabilityInstrumentedTest` selected-layout lookup;
- prefer stable semantics/state descriptions over implementation-shape assumptions;
- repair `ReaderFrameBenchmark` app-resource lookup or replace it with a stable app-owned Reader-ready semantic probe;
- keep generated baseline/startup profile path intact;
- remove avoidable compile warnings where trivial and safe;
- rerun Storage Instrumentation and Performance Benchmarks.

Do not:
- rewrite PDF engine;
- redesign Reader;
- change Room/schema;
- weaken assertions merely to make tests green.

Exit:
- both test harnesses reach the intended product action;
- any remaining failure is product-relevant.

---

## W63 — Artifact Chamber Flagship Pass

Goal: make Book Detail one of the app's signature surfaces.

Required composition:
- standing cover remains the visual anchor;
- book-specific aura stays restrained;
- one clear Resume/Open action;
- Current Journey before metadata utility;
- Preserved Memory receives deliberate visual priority;
- Archive History reads as chronology, not card inventory;
- destructive actions are calm and visually separated.

Stress matrix:
- unopened;
- in progress;
- completed;
- reread;
- missing cover;
- missing author;
- long English title;
- long Persian title;
- multiple collections;
- series identity;
- high annotation/history density;
- compact and expanded width.

Exit:
- no generic metadata-sheet impression;
- no nested-card soup;
- correct RTL/large-text behavior at source level;
- policy/model regressions covered where appropriate.

---

## W64 — Living Memory Convergence

Goal: make Hidden Archive and Reader Notebook feel like two views into one memory system.

Unify:
- Notes;
- passages/highlights;
- bookmarks;
- Echoes;
- Time Capsules;
- TOC/search return flows.

Focus:
- one hierarchy language;
- shared passage artifact treatment;
- clear note-only vs quote+note representation;
- exact return-to-passage affordance;
- calm empty/search/no-result states;
- editing/deletion remains unmistakable;
- Reader-side Notebook stays faster and lighter than full Hidden Archive.

Exit:
- no feeling of two separately designed products;
- memory features become a primary differentiation layer.

---

## W65 — World Convergence

Goal: bring the optional World layer up to the finish quality of Threshold/Archive without competing with reading.

Surfaces:
- Castle;
- Castle chambers;
- Path;
- Observatory;
- Profile;
- Treasury;
- Ritual;
- Sanctum.

Rules:
- no reading access gates;
- no fake history;
- no generic account-dashboard Profile;
- no permanent reward noise;
- rare visual effects remain rare;
- world state derives from real durable reading history.

Priority inside the wave:
1. Castle spatial hierarchy;
2. Path progression clarity;
3. Observatory factual memory visualization;
4. Profile information architecture;
5. Treasury/Sanctum/Ritual convergence.

Exit:
- each realm has a distinct role but a shared Veil DNA.

---

## W66 — Edge-State Mastery

Goal: treat every failure/empty/recovery condition as first-class product design.

Cover:
- empty library;
- no search results;
- import failure;
- restricted publication;
- missing file;
- Reader open failure;
- PDF renderer delayed;
- backup/restore failure;
- persistence recovery;
- interrupted navigation;
- missing metadata;
- duplicate import;
- no memory/history yet.

Rules:
- typed user-facing errors;
- one clear next action when actionable;
- technical facts remain honest;
- no mystical wording that hides durability/recovery issues.

---

## W67 — Accessibility & Adaptive Proof

Goal: convert source-level support into evidence.

Matrix:
- English LTR;
- Persian RTL;
- 200% font;
- TalkBack/touch exploration;
- reduced motion;
- portrait phone;
- landscape phone;
- tablet;
- foldable/split window;
- keyboard where applicable.

Critical paths:
- import/open/resume;
- Reader chrome;
- Paged/Scroll;
- Paper/Slide/None;
- Notebook;
- PDF View;
- Book Detail;
- Library search/filter;
- Settings;
- World entry/return.

Exit:
- no clipped essential action;
- focus order is intentional;
- state changes announce correctly;
- 48dp actions remain reachable;
- RTL mirrors direction without changing meaning.

---

## W68 — Performance / Profile / Package Gate

Start only after the major visual reconstruction stabilizes.

Tasks:
- generate and persist baseline profile evidence;
- verify startup profile packaging;
- run startup benchmark;
- run Reader frame benchmark after harness repair;
- inspect memory pressure from Paper/Slide snapshots;
- large-library retrieval checks;
- R8/resource-shrink package verification;
- final APK/AAB proof.

Do not optimize from emulator noise alone.
Use representative-device evidence for final conclusions.

---

## 6. Architecture hygiene

### ReaderScreen
Current size is approximately 5.3k lines.

Action:
- no architecture rewrite;
- extract only stable UI/policy units when doing so lowers regression risk;
- preserve one Reader ownership model.

### LibraryScreen
Current size is approximately 3.9k lines.

Action:
- higher refactor priority than Reader because retrieval, archive memory, detail and dialogs are concentrated together;
- extract compositional sections and pure policies without forking data ownership;
- keep one Room/library source of truth.

---

## 7. Gamification durability

Current state is stronger than the previous audit:
- `flushDurably()` exists;
- lifecycle STOP/DESTROY uses the durability barrier;
- quest/discovery durability tests exist;
- milestone-like writes use synchronous commits in important paths.

Arena decision:
- no emergency migration now;
- keep under observation;
- move canonical progression toward Room/DataStore only as a controlled later durability improvement;
- do not let persistence migration interrupt flagship UI/Reader work unless new loss evidence appears.

---

## 8. Work that must not be reopened without new evidence

- replacing Readium;
- replacing the PDF renderer purely for aesthetics;
- creating another Reader architecture;
- recombining Reading mode with Page turn;
- duplicating Notebook/Archive state;
- reintroducing world tabs into primary reading navigation;
- expanding Manga live sources;
- broad release packaging before design convergence.

---

## 9. Arena scorecard for every future slice

Every slice must be challenged against:

- Correctness — 30%
- Completeness — 25%
- Robustness — 20%
- Specificity — 15%
- Clarity — 10%

Automatic failure:
- data-loss regression;
- navigation ownership regression;
- inaccessible critical control;
- broken RTL meaning;
- fake state/history;
- duplicate architecture;
- Reader regression caused by World/UI work.

---

## 10. Immediate Alpha order

1. W62 repair the two invalid verification contracts.
2. Rerun only Storage Instrumentation + Performance.
3. If a real product defect remains, fix it before visual expansion.
4. W63 Artifact Chamber.
5. W64 Living Memory.
6. W65 World convergence.
7. W66 edge states.
8. W67 accessibility/adaptive proof.
9. W68 performance/profile/package gate.
10. Only then consider PR #349 ready for non-draft review.

## Final Arena rule

**Do not confuse more code with more quality.**

The next phase wins by making verification truthful, concentrating artistic effort on the most visible weak surfaces, and refusing to destabilize the reading/data core that is already ahead of the rest of the product.


---

# 11. Detailed execution decomposition

This section converts W62–W68 into implementation slices. Each slice is intended to be independently reviewable and reversible on the same canonical branch.

## Operating rules for every slice

Before editing:
1. fetch the latest canonical HEAD;
2. inspect the owning source and nearest regression tests;
3. identify the invariant that must not regress;
4. prefer the smallest semantic change that resolves the slice;
5. do not create a new branch for milestone work.

After editing:
1. run source-level consistency checks;
2. add/update tests when behavior or semantics change;
3. confirm localization/RTL implications;
4. record the slice on PR #349;
5. do not declare DEVICE-GREEN without device evidence.

Every slice must preserve:
- one Reader architecture;
- one Library source of truth;
- one page-turn ownership model;
- one Settings implementation;
- one Archive/Notebook data model;
- local/offline ownership;
- exact progress/history truth.

---

# W62 — Verification Truth

Priority: **CRITICAL FIRST**

Purpose: ensure red automation means a real product regression.

## W62.1 — PDF layout semantics contract

Primary files:
- `app/src/androidTest/java/com/veilreader/app/ReaderPdfReliabilityInstrumentedTest.kt`
- `app/src/main/java/com/veilreader/app/ui/screens/PdfZoomControls.kt`

Observed problem:
- `PdfLayoutChoice` uses Compose `selectable(selected, role = RadioButton)`;
- the instrumentation helper looks for a selected layout node that is `isCheckable && !isClickable`;
- this assumes a semantic tree shape that contradicts the interactive control.

Implementation plan:
1. inspect the actual semantics emitted by the current selectable control;
2. change the instrumentation locator to identify the selected PDF layout by stable public semantics:
   - content description;
   - selected/checkable state;
   - state description if useful;
3. avoid relying on `!isClickable`, child-node placement, text-node merging, or specific Compose implementation details;
4. keep both layout choices genuinely interactive for accessibility users;
5. confirm the helper can read both:
   - Paginated;
   - Continuous Scroll;
6. keep the existing test sequence:
   - import PDF;
   - open PDF;
   - reveal chrome;
   - open PDF View;
   - toggle layout;
   - verify changed state;
   - restore original layout;
   - exercise fit width;
   - rotate;
   - verify location/layout continuity.

Do not:
- weaken the test to only click without verifying the selected state;
- remove radio/selectable semantics;
- change product behavior to satisfy a stale test.

Acceptance:
- selected state is verified by stable semantics;
- both choices remain 48dp+;
- TalkBack still receives a meaningful label and selected state;
- layout survives rotation as originally intended.

## W62.2 — Benchmark target-resource contract

Primary file:
- `benchmark/src/main/java/com/veilreader/benchmark/ReaderFrameBenchmark.kt`

Supporting areas:
- Reader-ready semantics in `ReaderScreen.kt`;
- benchmark helper functions if shared.

Observed problem:
- benchmark uses `InstrumentationRegistry.getInstrumentation().targetContext`;
- it then resolves `reader_surface_label` using `TARGET_PACKAGE`;
- the resource table belongs to the benchmark target context rather than the app-under-test package;
- lookup returns zero and the benchmark fails before frame measurement.

Preferred fix order:
1. remove resource-table dependency from the benchmark readiness probe if possible;
2. use a stable app-owned accessibility semantic identifier/value that the benchmark can query directly;
3. if a resource string is required, create/load the app package context explicitly and resolve resources from that context;
4. keep the readiness probe user-observable and deterministic;
5. do not substitute arbitrary sleeps for readiness.

Acceptance:
- benchmark reaches Reader surface;
- `FrameTimingMetric` measurement actually begins;
- no resource lookup exception;
- benchmark still fails if Reader never becomes ready.

## W62.3 — Baseline/startup profile evidence sanity

Primary areas:
- benchmark module;
- baseline profile generation task;
- generated release profiles.

Already observed:
- baseline profile generation succeeded;
- startup profile generation succeeded.

Tasks:
1. verify generated outputs are non-empty;
2. verify rules include real Veil startup/Reader paths;
3. confirm release/benchmark variants consume the profile path expected by Gradle;
4. do not commit generated noise unless the project's profile workflow expects committed artifacts;
5. distinguish:
   - profile generation success;
   - profile packaging success;
   - performance success.

Acceptance:
- evidence categories are not conflated;
- later performance failure cannot erase the fact that profile generation itself succeeded.

## W62.4 — Warning hygiene

Known warnings to review:
- experimental API usage in `PaperCurlInputListener.kt`;
- deprecated framework paint bridge in `PaperCurlDraw.kt`;
- redundant conversion in `SettingsScreen.kt`;
- named-argument parameter mismatch in Manga Room store.

Policy:
- fix only warnings whose remediation is low-risk and behavior-neutral during W62;
- do not churn Reader gesture code merely to remove an experimental warning.

Acceptance:
- trivial warnings removed;
- non-trivial warnings documented, not hidden.

## W62.5 — Verification rerun classification

Run:
- Android CI;
- Storage Instrumentation;
- Performance Benchmarks.

Classification tree:
- GREEN → proceed;
- RED in harness/test plumbing → repair harness;
- RED in product behavior → stop visual expansion and fix product defect;
- emulator-only performance anomaly → collect traces before modifying product.

W62 exit gate:
- automation is trustworthy;
- no known false-red remains;
- any persistent red has a concrete product cause.

---

# W63 — Artifact Chamber Flagship

Priority: **HIGHEST VISIBLE PRODUCT VALUE**

Primary owner:
- `LibraryScreen.kt`

Primary composables:
- `BookDetailDestination`
- `BookDetailArtifactStand`
- `BookDetailIdentity`
- `BookDetailFragments`
- `BookDetailFact`
- metadata/edit/delete dialogs surrounding Book Detail

Supporting files:
- `BookArtifact.kt`
- `BookEntryTransition.kt`
- book-detail/model tests.

## W63.1 — Hero composition

Goal:
Book cover should read as an artifact in a chamber, not a thumbnail in a modal.

Tasks:
- preserve standing-cover treatment;
- tune cover scale separately for compact and expanded widths;
- reduce competing ornament near title/action;
- make book-specific aura subordinate to artwork;
- create visual grounding under cover without heavy shadow soup;
- ensure no publication artwork modification.

Acceptance:
- cover is first visual anchor;
- title is second;
- Resume/Open is third;
- utility metadata does not compete above the fold.

## W63.2 — Identity block

Current owner:
`BookDetailIdentity`.

Tasks:
- define hierarchy:
  1. artifact eyebrow/format;
  2. title;
  3. author;
  4. series/collection identity;
  5. status;
- handle missing title/author without awkward blank geometry;
- permit long Persian and long Latin titles;
- avoid forced uppercase/tracking for Arabic-script text;
- ensure series index does not visually dominate author.

Stress:
- 1-line title;
- 4-line title;
- author missing;
- title missing;
- mixed Persian/English metadata.

## W63.3 — Current Journey

Tasks:
- progress line stays factual;
- progress percentage remains legible but quiet;
- current chapter appears only when meaningful;
- unopened state does not fake a chapter;
- completed state changes narrative wording without losing exact progress history;
- reread cycles remain historical rather than overwriting the first completion.

Primary action rules:
- unopened → Open;
- in progress → Continue;
- completed → Read Again;
- unavailable local file → disabled action + truthful explanation.

## W63.4 — Preserved Memory

Owner:
`BookDetailFragments`.

Tasks:
- distinguish:
  - quote only;
  - note only;
  - quote + note;
- use one artifact language shared later with Hidden Archive;
- show only useful sample memory on Book Detail;
- do not turn the destination into a full notebook;
- provide path into deeper memory view if supported by current navigation.

Acceptance:
- memory adds emotional/history value;
- it does not block fast resume.

## W63.5 — Archive History

Tasks:
- change fact-list feeling into a quiet chronological record;
- group:
  - archived/imported;
  - first opened;
  - reading milestones;
  - completion cycle(s);
  - archive depth;
- keep real dates/history only;
- do not fabricate “insights.”

## W63.6 — Utility/destructive actions

Tasks:
- Favorite and Edit stay secondary;
- Delete becomes spatially separated from normal actions;
- destructive confirmation remains explicit;
- actions preserve large-text stacking behavior.

## W63.7 — Artifact Chamber adaptive matrix

Source-level matrix:
- compact phone;
- large phone;
- landscape;
- tablet/foldable;
- RTL;
- font scale 2.0;
- long title;
- missing cover;
- unavailable file.

Tests:
- extend `BookArtifactTest.kt`;
- add pure composition/policy tests where useful;
- avoid fragile pixel snapshots until device-visual phase.

W63 exit:
- Book Detail feels authored;
- no generic modal/product-card feel;
- no state lies;
- no Reader/data changes.

---

# W64 — Living Memory Convergence

Priority: **CORE DIFFERENTIATION**

Primary files:
- `ArchiveScreen.kt`
- `ReaderNotebook.kt`
- `TimeCapsuleUi.kt`

Supporting:
- Reader return/jump policies;
- highlight/bookmark/note data models;
- historical memory derivation tests.

## W64.1 — Shared memory taxonomy

Define one visual/content vocabulary:
- Note = user-authored thought;
- Passage = preserved source text;
- Bookmark = location marker;
- Echo = later factual resurfacing/relationship;
- Time Capsule = historical temporal artifact;
- Contents/Search = retrieval instruments, not memory objects.

Do not rename basic actions mystically.

## W64.2 — Reader Notebook hierarchy

Reader Notebook must remain quick.

Order:
1. active tab;
2. lightweight search/TOC retrieval;
3. nearest relevant memory;
4. minimal actions;
5. return to reading.

Rules:
- no large ceremonial headers;
- no full Archive density;
- 1 gesture from Reader;
- dismissal and return remain predictable.

## W64.3 — Full Hidden Archive hierarchy

Full Archive may be richer.

Tasks:
- Notes first by default;
- stronger title/section hierarchy;
- counts remain factual;
- deep memory modules appear after retrieval controls;
- empty and filtered-empty states are distinct;
- current book vs all-books context remains obvious.

## W64.4 — Passage artifact component

Create/reuse one semantic presentation for:
- quote;
- note;
- source book;
- chapter/location;
- age/date;
- revisit state.

Do not duplicate separate versions in ReaderNotebook and Archive unless density requires a wrapper around the same core language.

## W64.5 — Return-to-passage

Preserve existing target-aware navigation transaction logic.

Tasks:
- every actionable memory makes destination clear;
- exact-current destination produces no fake jump;
- failed jump shows calm recovery;
- revisit history is recorded only after destination settles.

## W64.6 — Edit/delete ownership

Rules:
- note edit edits the intended record only;
- stale dialogs do not recreate deleted records;
- delete confirmation explains object type;
- note-only record never renders a fake quotation.

## W64.7 — Time Capsule convergence

Tasks:
- retain temporal/history identity;
- reduce one-off component styling;
- connect typography, rules and material to Archive;
- maintain truthful generation from recorded history.

Tests:
- expand Reader Notebook policy tests;
- memory derivation tests;
- return-to-passage regression tests;
- process restoration for open tab/search state where appropriate.

W64 exit:
- Reader Notebook and Hidden Archive clearly belong to one system;
- fast Reader use remains fast;
- historical depth becomes visibly differentiated.

---

# W65 — World Convergence

Priority: **HIGH, AFTER CORE MEMORY**

Primary files:
- `CastleScreen.kt`
- `CastleChamberScreen.kt`
- `PathScreen.kt`
- `ObservatoryScreen.kt`
- `ProfileScreen.kt`
- `WorldLabels.kt`

## W65.1 — Castle spatial hierarchy

Owner:
`CastleScreen`.

Current major primitives:
- `CastleKeep`
- `CastleWorldMap`
- `CastleFloor`
- `CastleChamberNode`
- progression/mutation/ritual inscriptions.

Plan:
- Castle map/space first;
- chamber choices second;
- progression state third;
- ledgers/history later;
- remove equal-weight panel feeling;
- keep ornamental density higher than Archive but below noise threshold.

Accessibility:
- spatial visual map must have a linear semantic equivalent;
- chamber nodes require clear accessible labels/state.

## W65.2 — Path clarity

Owner:
`PathScreen`.

Major primitives:
- `PathIdentityPanel`
- `RankConstellation`
- `PathMasteryRow`
- `RitualPanel`
- `AdvancementCeremonyDialog`.

Plan:
- path identity explains current commitment immediately;
- current rank and next threshold visually obvious;
- mastery axes explain why advancement is/not ready;
- ritual CTA only appears when meaningful;
- dead alternative paths remain absent after commitment;
- ceremony is rare and reduced-motion safe.

## W65.3 — Observatory truth

Owner:
`ObservatoryScreen`.

Plan:
- atlas remains factual;
- clarify what creates a connection;
- selected book becomes anchor;
- related books emphasized without fabricating similarity;
- isolated books are not treated as failure;
- list/index remains available under visual map;
- empty Observatory teaches how factual relationships appear.

Accessibility:
- Canvas cannot be the sole means of selecting/understanding nodes;
- maintain corresponding rows and meaningful semantics.

## W65.4 — Profile / More

Owner:
`ProfileScreen`.

Current issue:
useful data risks reading like a gamification dashboard.

Plan order:
1. identity/dossier;
2. meaningful reading history;
3. path/mastery;
4. Hidden Archive access;
5. reading goal;
6. sigils/discoveries;
7. settings/utility access.

Rules:
- no account-dashboard styling;
- goals remain voluntary;
- no punishment/streak anxiety;
- world data never outranks reading history.

## W65.5 — Treasury / Sanctum / Ritual

Owner:
`CastleChamberScreen.kt` plus Path ritual.

Plan:
- Treasury = collection/display;
- Ritual = transformation event;
- Sanctum = rare endgame/history;
- ensure they do not visually collapse into the same card grid;
- rarity uses domain truth;
- discovered/undiscovered states are persistent historical facts.

## W65.6 — Shared world grammar

Standardize:
- micro-labels;
- registration rules;
- brass usage;
- section boundaries;
- empty/locked/revealed states;
- motion tiers;
- RTL typography.

Do not standardize away realm identity.

Tests:
- pure state derivation;
- adaptive layout policies;
- semantic availability of chamber navigation;
- process restoration of selected chambers;
- historical discovery durability.

W65 exit:
- world feels deliberate, optional and connected to reading;
- no screen looks like a generic RPG dashboard;
- no reading feature is gated.

---

# W66 — Edge-State Mastery

Priority: **HIGH PRODUCT TRUST**

Primary areas:
- `VeilApp.kt`
- Library import flow;
- Reader recovery;
- PDF recovery;
- Archive search;
- Settings backup/restore;
- Manga local import;
- World empty states.

## W66.1 — Typed error taxonomy

Define categories:
- Storage;
- Import;
- Publication restriction;
- Missing file;
- Reader open;
- Renderer delayed/unavailable;
- Navigation recovery;
- Backup/restore;
- Persistence warning;
- Search empty;
- Validation.

Each maps to:
- user-facing title;
- concise body;
- severity;
- primary action;
- optional secondary action;
- technical logging detail kept outside user copy.

## W66.2 — Empty-state grammar

Differentiate:
- true empty;
- filtered empty;
- search no-results;
- no history yet;
- unsupported/unavailable;
- delayed loading.

Avoid using the same generic empty component everywhere.

## W66.3 — Recovery ownership

For every recoverable failure define:
- who owns retry;
- whether state is preserved;
- what happens on Back;
- what is durable before retry.

## W66.4 — Visual treatment

Use:
- restrained oxblood for error;
- brass for warning/recovery;
- Mist/Moon for neutral empty states.

Avoid:
- giant illustrations for routine errors;
- mystical copy that hides the real problem;
- raw exception strings.

Tests:
- error mapping;
- retry state preservation;
- duplicate import truth;
- missing-file paths;
- renderer retry bounds.

W66 exit:
- every common failure has a designed path forward;
- no raw technical text leaks into user UI.

---

# W67 — Accessibility and Adaptive Proof

Priority: **MANDATORY BEFORE RELEASE-GREEN**

## W67.1 — Language/script matrix

Verify:
- English LTR;
- Persian RTL;
- mixed-script title/author;
- Persian numerals/formatting behavior where currently supported.

Surfaces:
- Threshold;
- Library;
- Artifact Chamber;
- Reader;
- Notebook;
- Hidden Archive;
- Settings;
- Profile;
- Castle/Path/Observatory;
- Treasury/Sanctum;
- Manga shell.

## W67.2 — 200% text

Look specifically for:
- clipped tab labels;
- fixed-height buttons;
- horizontal choice rows;
- metadata collision;
- title overlap;
- action overflow;
- toolbar chrome height;
- Castle node labels.

Use existing dense-choice policy rather than ad-hoc per-screen hacks.

## W67.3 — TalkBack

Verify:
- logical traversal;
- no decorative ornament in traversal;
- selected states announced;
- expanded/collapsed Settings sections announced;
- Reader show/hide controls;
- PDF layout/zoom;
- bookmark state;
- Note save/cancel;
- Castle/Observatory alternatives to canvas-only meaning.

## W67.4 — Reduced motion

Audit:
- entry reveals;
- Paper;
- Slide;
- transition overlays;
- ritual/ceremony;
- Castle/Path ambient motion.

Reduced motion should preserve:
- state feedback;
- hierarchy;
- functionality.

It should remove:
- unnecessary displacement;
- long ceremonial delay;
- snapshot-heavy page animation.

## W67.5 — Window classes

Verify:
- compact portrait;
- compact landscape;
- medium tablet;
- expanded tablet/foldable;
- split window/resizing.

Critical invariant:
viewport resize cannot commit false page turns or stretch stale mode snapshots.

## W67.6 — Keyboard/non-touch

Verify where relevant:
- arrows;
- PageUp/PageDown;
- Space/Shift+Space;
- Back/Escape behavior;
- focusable actions on large-screen devices.

W67 exit:
- evidence exists, not merely source intent.

---

# W68 — Performance, Profile and Package Proof

Priority: **FINAL ENGINEERING GATE**

Only execute after W63–W67 semantics/design stabilize.

## W68.1 — Startup

Measure:
- cold startup;
- warm startup where useful;
- first usable Threshold;
- import/open transition separately from app startup.

Interpretation:
- emulator results are trend signals;
- representative hardware is final evidence.

## W68.2 — Reader frame benchmark

After W62 harness repair:
- Paged turn;
- Slide turn;
- Paper turn;
- chrome reveal;
- Appearance open/close;
- PDF zoom/layout if a stable benchmark path exists.

Do not combine all interactions into one opaque score.

## W68.3 — Memory pressure

Inspect:
- Paper current/next snapshots;
- Slide snapshots;
- mode-handoff buffer;
- image viewer bitmap;
- large cover caches;
- PDF native memory.

Test:
- repeated mode switching;
- rotation;
- split-window resize;
- long session.

## W68.4 — Large library

Scenarios:
- 100;
- 1,000+ metadata records if test fixture permits;
- search;
- filter;
- sort;
- shelf grouping;
- cover decode behavior;
- Archive memory computation.

Goal:
keep retrieval immediate and avoid doing world-history work on every scroll item.

## W68.5 — Baseline/startup profile package proof

Verify:
- profile generated;
- profile merged;
- release artifact contains/consumes expected profile;
- no profile generation success is mistaken for runtime improvement without measurement.

## W68.6 — R8/resource shrink

Verify:
- Readium;
- Room;
- reflection-sensitive models;
- PDF;
- serialization;
- navigation restoration;
- baseline profile integration.

## W68.7 — Release artifact proof

Only at explicit build gate:
- APK/AAB;
- install;
- cold launch;
- import;
- EPUB read;
- PDF read;
- process death;
- restore;
- backup/restore;
- package size report;
- ABI/resource inspection as configured.

W68 exit:
- TEST-GREEN;
- DEVICE-GREEN;
- performance evidence accepted;
- package verified.

---

# 12. Cross-wave dependency graph

```
W62 Verification Truth
   ↓
W63 Artifact Chamber ─────┐
   ↓                      │
W64 Living Memory         │
   ↓                      │
W65 World Convergence     │
   ↓                      │
W66 Edge States           │
   └──────────┬───────────┘
              ↓
W67 Accessibility / Adaptive Proof
              ↓
W68 Performance / Profile / Package
              ↓
PR #349 ready-for-review decision
```

Parallelism policy:
- code changes remain serial on the canonical branch;
- research/review can happen in parallel;
- do not land overlapping semantic changes to the same large source file concurrently.

---

# 13. Priority inside each source file

## `LibraryScreen.kt`

High-risk because of size and responsibility concentration.

Refactor only while touching relevant sections.

Preferred extraction order:
1. Artifact Chamber composables;
2. metadata/edit/delete dialogs;
3. library filter/control strip;
4. Archive wings/deep shelf;
5. gallery/list/shelf row primitives.

Do not extract:
- data ownership into a parallel ViewModel;
- duplicated repository state;
- new navigation architecture.

## `ReaderScreen.kt`

Do not proactively refactor for aesthetics.

Extract only when:
- a stable pure policy already has tests;
- a self-contained dialog/control has clear ownership;
- extraction reduces regression surface.

Never separate:
- navigation transaction gate ownership;
- Paper/Slide input arbitration into competing controllers;
- durable locator lifecycle into duplicate paths.

## `ArchiveScreen.kt` and `ReaderNotebook.kt`

Converge visual primitives first, then consider shared extraction.

Do not merge screens; they have different density/usage goals.

---

# 14. Definition of Done by status

## IMPLEMENTED
Code exists and source invariants are preserved.

## SOURCE-VERIFIED
- source reviewed;
- no duplicate architecture;
- localization/RTL considered;
- tests compile logically;
- state truth checked.

## TEST-GREEN
- unit/lint;
- relevant instrumentation;
- relevant benchmark harness reaches intended path.

## DEVICE-GREEN
- representative device/emulator interaction proof;
- RTL/large text/TalkBack where relevant;
- rotation/window behavior checked.

## RELEASE-GREEN
- optimized package verified;
- baseline/startup profile package evidence;
- no release-only regression;
- critical user journeys pass.

No milestone may skip these labels.

---

# 15. Stop-the-line triggers

Immediately stop the current visual wave if new evidence shows:
- lost reading location;
- lost note/highlight/bookmark;
- first real page turn not counted;
- duplicate/phantom progress;
- PDF zoom/layout ownership regression;
- page-turn mode persistence regression;
- stale Fragment reuse;
- broken process recreation;
- migration/storage corruption;
- accessibility blocks core reading;
- profile/build tooling hides a real crash.

Fix the reliability defect first, then resume the planned wave.

---

# 16. Alpha daily execution loop

For each slice:

1. **Inspect**
   - latest HEAD;
   - owning source;
   - nearest tests;
   - latest CI.

2. **Challenge**
   - what can break?
   - what state is being implied?
   - is there an existing subsystem to reuse?
   - does Persian/RTL change the layout?
   - what happens at 200% text?
   - what happens on process/window recreation?

3. **Implement**
   - smallest reversible change;
   - no parallel architecture;
   - no fake data.

4. **Protect**
   - add/update policy regression test where semantics changed;
   - preserve accessibility labels;
   - preserve persisted schema unless explicitly justified.

5. **Review**
   - Arena attack against generic styling, edge cases and duplicate ownership.

6. **Record**
   - commit;
   - PR #349 milestone note;
   - status label: IMPLEMENTED / SOURCE-VERIFIED / TEST-GREEN / DEVICE-GREEN.

---

# 17. Immediate next queue

Exact next sequence from the current HEAD:

### Queue 1 — W62.1
Fix PDF instrumentation selected-layout semantics.

### Queue 2 — W62.2
Fix ReaderFrameBenchmark target resource/readiness lookup.

### Queue 3 — W62.3
Rerun the two failing gates and classify any remaining red.

### Queue 4 — W63.1 + W63.2
Artifact Chamber hero + identity hierarchy.

### Queue 5 — W63.3 + W63.4
Current Journey + Preserved Memory.

### Queue 6 — W63.5 + W63.6
Archive History + utility/destructive action hierarchy.

### Queue 7 — W64
Living Memory convergence.

### Queue 8 — W65
World convergence.

### Queue 9 — W66
Edge-state mastery.

### Queue 10 — W67
Accessibility/adaptive proof.

### Queue 11 — W68
Performance/profile/package verification.

No ordinary work creates a new branch between these queues.
