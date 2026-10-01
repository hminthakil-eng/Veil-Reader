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
