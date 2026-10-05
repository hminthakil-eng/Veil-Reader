# Veil Reader — Grayfog UI/UX Arena Review and Next-Step Ruling

Date: 2026-10-04
Reviewed product/UI head: `438a967a074484aec373a8edc15eeca467ffaa13`
PR: #371 — `design/codex-grayfog-masterpiece-v1`
Canonical convergence branch: `integration/page-engine-canonical-v1`

## Arena ruling

**UI/UX STATUS: SOURCE-GREEN / NATIVE-LAYOUT-GREEN / NOT DEVICE-GREEN / NOT VISUALLY FROZEN YET**

**NEXT STEP: one bounded UI correctness hardening pass, then freeze Grayfog shell and proceed with canonical GPU Paper convergence.**

Do not start another broad visual redesign now.

Do not merge PR #371 directly into the old reader integration line. The canonical convergence branch is already based on the exact current Grayfog head, so the current UI work is preserved without replaying a large UI merge.

---

## Repository truth reviewed

PR #371 currently contains:
- 9 commits;
- 88 changed files;
- 29,122 additions;
- 1,393 deletions;
- current head `438a967a074484aec373a8edc15eeca467ffaa13`;
- Draft / mergeable.

Current checked-in verification evidence reports:
- 713 JVM tests;
- 127 suites;
- 0 failures;
- 0 errors;
- 0 skips;
- 35 design-system / constitution / art-direction tests;
- 39 reading-policy checks;
- lint: 0 errors, 177 warnings, 1 hint;
- debug APK assembly passed;
- debug AndroidTest APK assembly passed;
- 465 native Skia frames;
- 31 production surfaces;
- 15 configurations;
- 341 Compose preview instances;
- device acceptance = false.

The cloud/native matrix is layout evidence only. It does not establish final optical quality, real Android text rasterization, real hinge behavior, TalkBack behavior, Readium/Pdfium interaction, full-motion behavior, Paper/Slide mid-drag quality, or long-session comfort.

---

# Realm-by-realm review

## Threshold

### Improvements verified in source
- current volume and return action are promoted ahead of secondary world systems;
- hero artifact is larger than recent books;
- stacked large-text layout places title/action before the cover;
- long Persian title fixture exists;
- 200% text handling is explicitly tested;
- ornament density was reduced versus earlier framed/dashboard composition;
- RTL navigation chevron is direction-aware;
- resume action retains a 52dp minimum target.

### Remaining risk
The top-level adaptive class and the abbreviated doorway decision still use `LocalConfiguration.current.screenWidthDp/screenHeightDp`, while newer realm work uses actual `LocalWindowInfo.containerSize` for constrained-window decisions.

This is not proven broken, but the app now has two different layout-size authorities. Real split-window/fold posture can expose disagreements.

### Mixed-script gap
`ThresholdWhisperCard` applies ordinary shell typography to source-derived preserved title/body/detail. A Persian preserved passage inside an English shell can therefore inherit the Latin family/tracking instead of the already-authored `withVeilContentScript(...)` path.

Classification: **P1 UI correctness**.

---

## Library / Grayfog Archive

### Improvements verified
- search and retrieval now precede world-history utilities;
- Gallery, Shelves and Index have distinct grammar;
- status filters remain available in Shelves;
- view mode has a dedicated control rail;
- collection / series / sorting instruments wrap;
- Series is directly retrievable without scrolling into Archive wings;
- Gallery record measure grows with font scale;
- Shelf objects share a continuous datum instead of isolated mini-cards;
- Index uses continuous ruled rows;
- 48dp utility targets are explicitly protected;
- Book Detail reading action moved into identity hierarchy;
- current actions are quieter and less gold-filled;
- empty/no-result state is explicitly separated.

### Remaining mixed-script gaps
The design system correctly introduced `withVeilContentScript` for imported metadata, but several dynamic Library strings bypass it:
- current Collection selection text;
- Collection dropdown labels;
- Series dropdown labels;
- Archive wing names.

These values can be user-authored or publication-derived and can contain Persian while the shell locale is Latin.

Classification: **P1 UI correctness**.

### Device-only
- real cover ratio crop;
- dense large libraries;
- IME/search restoration;
- actual Persian baselines;
- three-mode visual hierarchy;
- TalkBack nested record/actions;
- foldable posture.

---

## Book Detail

### Improvements verified
- artifact/identity only pair when enough readable width remains after text scaling;
- Read/Continue is adjacent to title identity instead of buried below progress/history;
- secondary actions are quieter;
- history/provenance/delete gates remain;
- full screen is scrollable.

No source-level blocker found.

Device gate remains for real covers, very long identity, deletion focus and tactile hierarchy.

---

## Reader Sanctuary / Appearance

### Improvements verified
- publication remains visually dominant;
- Reader appearance is instrument-like rather than another world dashboard;
- Paged/Scroll and Paper/Slide/None remain semantically separated;
- constrained layouts can disclose preview rather than forcing it permanently onscreen;
- accessibility-sized choices stack;
- Persian sample/prose shaping is supported;
- theme/material controls are visually quieter than the earlier gold-fill direction.

Current Grayfog continuation only changes the Appearance presentation/disclosure boundary in ReaderScreen; navigation ownership and draft settlement are intentionally preserved.

### Important limit
The native Grayfog frame matrix uses Reduced Motion. It does **not** establish full-motion Reader quality or Paper/Slide behavior.

This is now coupled to the page-engine arbitration and should not be visually judged through the old Paper runtime.

---

## Notes / Highlights / Reader Notebook

### Improvements verified
- repeated perimeter framing reduced;
- source title/category hierarchy improved;
- actions wrap and keep target size;
- editor and destructive dialogs scroll;
- exact locator callbacks and atomic Note ownership remain outside the visual refactor.

### Mixed-script defects
Two ReaderNotebook dialogs render the source quote with:
`MaterialTheme.typography.bodyMedium`
without `withVeilContentScript`.

A Persian quote inside an English UI therefore bypasses the dedicated Persian shell family.

Archive bookmark labels also render publication/user content without the script-aware typography helper.

Classification: **P1 UI correctness**.

---

## Observatory

### Improvements verified
- map is a dark instrument field rather than a generic framed graph;
- selected relationship is stronger than context relationships;
- connection ledger can expose all relationships;
- disclosure resets on source selection;
- exact return-to-reading callback is tested;
- source-derived graph remains the truth; no invented nodes/edges.

### Mixed-script risk
Shared passage terms are inserted into a localized sentence and rendered with ordinary body typography. Terms can be source-derived Persian text even when shell locale is English.

Classification: **P1 UI correctness**.

### Device-only
The dense 24-volume / 276-edge fixture proves layout survival, not touch discrimination or optical graph quality.

---

## Castle

### Improvements verified
- actual chambers/maps precede diagnostic text;
- architectural two-pane layout now reserves a readable adjacent record;
- large text restores reading order;
- optional descriptive prose condenses under vertical/text pressure;
- duplicate doorway framing was reduced;
- foldable-width source/native fixture exists.

No source-level P0 found.

Device-only: real hinge posture, corridor/chamber scale and spatial feeling.

---

## Path / Ritual

### Improvements verified
- journey/ascent is primary, identity secondary;
- wide layout can use adjacency;
- large text restores sequential order;
- ceremony remains scrollable and cancellable;
- Reduced Motion keeps the state transition legible.

No source-level blocker found.

---

## Sanctum / Profile

### Improvements verified
- permanent records are calmer and less loot-like;
- ordinary equip/title controls are quieter;
- Profile Settings remains reachable in native layout tests;
- dossier/stat grids adapt rather than forcing rigid columns;
- source history remains unchanged.

No source-level blocker found.

---

## Navigation / chrome / system bars

### Improvements verified
- offline deterministic shell fonts;
- Persian typography has first-class family and zero tracking;
- imported Arabic metadata has a dedicated script-aware helper;
- nav target minimum is 56dp;
- rail becomes wider at high text scale and scrolls;
- tabs retain semantics;
- system-bar contrast is realm/window aware;
- dialog-window system bars have dedicated tests.

### Remaining quality note
11sp navigation labels are now materially better than the earlier tiny labels, but final legibility and truncation at real 200% system font settings remain device evidence, not source proof.

---

## Material / decorative surface

Grayfog reduced:
- mottle;
- oxidation;
- fibre opacity;
- speck opacity.

That is the correct direction for Sanctuary readability.

The old Paper renderer received only material-surface tuning in the Grayfog work; page-engine geometry/ownership should now be governed by the canonical GPU arbitration, not by further tuning of the legacy runtime.

---

# Cross-cutting strengths

The strongest Grayfog work is not ornament. It is product hierarchy:

1. reading action moved earlier;
2. retrieval moved before archive lore;
3. Gallery/Shelves/Index became genuinely different modes;
4. Book Detail became artifact-first and action-clear;
5. large text changes composition instead of merely enlarging text;
6. Persian/Arabic is treated as authored typography;
7. world screens use spatial adjacency only when readable;
8. controls increasingly behave as instruments rather than decorative cards;
9. Reduced Motion is part of the design contract;
10. decorative material no longer competes as strongly with reading.

This matches the literary direction: **clarity first, mystery second**.

---

# Defect inventory

## P0
No new source-level UI/UX P0 blocker found in Grayfog #371.

The major P0 remains outside the shell: the GPU Paper host initialization defect documented in the page-engine arbitration.

## P1 — close before visual freeze
1. Mixed-script typography leak in Threshold preserved whisper content.
2. Mixed-script typography leak in Library Collection / Series / Archive Wing dynamic labels.
3. Mixed-script typography leak in ReaderNotebook source quote previews.
4. Mixed-script typography leak in Archive bookmark/user labels.
5. Mixed-script typography risk in Observatory source-derived shared passage terms.
6. Consolidate/verify one window-size authority for top-level adaptive classes on fold/multi-window. Treat as device-backed until proven wrong.

## P2 / visual-device polish
1. final cover-to-title balance;
2. Persian baseline/weight optical tuning;
3. nav label truncation at real accessibility scale;
4. Observatory dense-edge clarity;
5. Castle room/corridor rhythm;
6. Settings background-art balance in short landscape;
7. long-session decorative texture comfort;
8. full-motion realm transition timing.

## DEVICE-REQUIRED
- real Android screenshots against the user's reference boards;
- physical font rasterization;
- TalkBack / focus order / touch exploration;
- IME;
- actual foldable posture;
- system bars on OEM devices;
- real EPUB/PDF;
- selection / TTS / Notes / Focus Guide / Lookup;
- Paper / Slide mid-drag;
- long-session readability.

---

# Reference-board judgment

The current Grayfog source is directionally aligned with the supplied visual references:
- deep blue-black architecture;
- brass registration;
- parchment as a special material, not every surface;
- editorial hierarchy;
- archival/library identity;
- restrained celestial geometry;
- artifact-driven book presentation.

It intentionally does **not** copy the reference board's maximum framing density or turn every row into an ornate card. That restraint is desirable for a real reading product.

However, source and non-golden native frames are insufficient to certify **"exactly like the reference or better."** That statement requires current Android screenshots from the actual app and a side-by-side Arena visual review.

---

# Arena next-step decision

## STEP A — UI correctness freeze pass

One small, reversible commit on the canonical convergence branch:

- apply script-aware typography to every source/user-authored dynamic string identified above;
- add regression tests for Persian metadata while UI locale is English;
- normalize or explicitly document the top-level window-size authority;
- no redesign;
- no new illustration;
- no Reader navigation changes;
- no Paper renderer changes.

Acceptance:
- existing 713+ suite stays green;
- new script tests pass;
- lint 0 errors;
- reading-policy 39/39;
- debug and AndroidTest APK assemble;
- `git diff --check`.

After this commit, declare:

**GRAYFOG SHELL: SOURCE-FROZEN FOR PAGE-ENGINE CONVERGENCE**

## STEP B — Canonical GPU Paper convergence

Immediately after the UI freeze:
- repair the v2 GPU initialization P0;
- port v1 correctness invariants only;
- retain exactly one Paper runtime renderer;
- preserve the frozen Grayfog shell;
- keep Slide/Paged/Scroll distinct;
- keep PDF out of Paper.

## STEP C — device Arena

Only after a canonical APK exists:
- capture actual Threshold, Library, Book Detail, Reader, Appearance, Archive, Observatory, Castle and Settings;
- compare side-by-side with supplied references;
- evaluate Persian at 100/130/150/200%;
- evaluate normal/high contrast;
- evaluate real Paper/Slide motion;
- then reopen only evidence-backed visual polish.

---

# Final Arena ruling

**Do not spend the next cycle adding more UI decoration.**

The highest-value next move is:

**close the small remaining UI correctness gaps → freeze Grayfog → fix and converge the canonical GPU Paper engine → perform real-device visual arbitration.**

This keeps the product baseline stable while the highest-risk defining feature — the physical Paper experience — is rebuilt correctly.
