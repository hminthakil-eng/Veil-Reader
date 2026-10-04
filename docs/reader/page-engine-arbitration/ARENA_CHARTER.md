# Arena Engineering Charter — Veil Reader

Date adopted: 2026-10-04

Arena is the evidence-first architecture and quality gate for Veil Reader.

## North Star

Choose and execute the design that maximizes long-term product quality, correctness, maintainability, accessibility, performance and user delight. Novelty is never a substitute for evidence. A simpler architecture wins when it delivers the better product; a more advanced architecture wins only when its lifecycle, failure behavior and verification story are also better.

## Required evidence before architectural changes

For every material architecture, renderer, interaction, persistence, accessibility or adaptive-layout decision:

1. Inspect the exact current canonical source and tests.
2. Check current official platform/library documentation.
3. Inspect maintained or historically strong open-source reference implementations when they are technically relevant.
4. Separate reusable principles from code that cannot or should not be copied.
5. Check licenses before importing code or assets.
6. Compare alternatives against Veil-specific constraints, not demo quality.
7. State facts, inferences and device-required claims separately.
8. Preserve rollback through ordinary Git history; no force-push as a normal convergence tool.
9. Add regression tests for every source-level invariant that caused or could cause a product failure.
10. Never declare GREEN from source review alone when the claim depends on device/GPU/Readium/Pdfium/IME/TalkBack/hinge/performance behavior.

## Evidence hierarchy

Highest confidence first:

- current canonical source + reproducible tests;
- real-device measurements and captures;
- official Android / Jetpack / Readium documentation and source;
- maintained production-grade open-source implementations;
- primary technical papers / talks / specifications;
- experiments and prototypes;
- inference.

Community examples can generate hypotheses. They cannot override platform contracts or Veil's verified behavior.

## Technology policy

Use the highest-quality technology that improves the whole system, not the newest technology by label.

A candidate must be judged on:

- correctness and deterministic state ownership;
- runtime lifecycle and failure recovery;
- frame-time and memory behavior;
- accessibility and Reduced Motion;
- RTL/Persian and mixed-script behavior;
- foldable / multi-window / resizing behavior;
- offline operation;
- Readium/Pdfium ownership boundaries;
- testability;
- operational complexity;
- long-term extensibility;
- rollback cost.

## Reader invariants

- Readium remains EPUB navigation/locator truth.
- Pdfium/PDF behavior stays independent from EPUB Paper.
- PAGED, PAPER CURL, SLIDE and SCROLL are distinct interaction modes.
- Paper may never silently collapse into Slide or another visual renderer.
- There is exactly one canonical Paper runtime renderer.
- Normal-motion Paper fails closed if that renderer cannot satisfy its visual contract.
- Reduced Motion may remove physical animation while preserving equivalent functional navigation.
- A live destination page belongs underneath the lifted leaf unless a true opposite-leaf content contract exists.
- Sound and haptics are semantic feedback policies outside the renderer.
- Progress, notes and location durability outrank visual effects.

## UI/UX invariants

- Clarity first, mystery second.
- Reader/Sanctuary has the lowest ornamental density.
- Parchment is a special material, not a universal container.
- Dynamic publication/user text is script-aware independently of shell locale.
- Overall layout decisions follow the live app window, not physical-screen assumptions.
- 200% text must change composition rather than merely scale into clipping.
- Accessibility targets and semantics are first-class acceptance criteria.
- Reference-board fidelity is judged on real app captures, not source intent alone.

## Verification gate

A source candidate can be called SOURCE-GREEN only after:
- focused and full relevant tests pass;
- lint has zero errors;
- debug and AndroidTest APKs assemble;
- reading-policy checks pass;
- `git diff --check` passes.

DEVICE-GREEN additionally requires the relevant real-device matrix. GPU Paper specifically requires:
- GLES renderer startup and context recreation;
- LTR and RTL/Persian;
- forward/backward;
- tap/key/drag;
- complete/cancel/reverse/boundary;
- resize/rotation/fold posture;
- normal and Reduced Motion;
- memory pressure and texture limits;
- long-session frame-time/comfort;
- real EPUB interaction beneath the overlay.

## Current external principles pinned by Arena

- Android adaptive layouts should react to the current app window; physical screen size is not the layout authority.
- Compose `WindowInfo.containerSize` is an appropriate stable signal for overall window structure, with insets still handled separately.
- Fold/unfold, multi-window and desktop resizing must be treated as live configuration changes.
- Mature page-curl renderers model a deforming cylinder/mesh and distinguish sheet faces and underlying content when that content is actually available.
- For Veil reflow EPUB, destination-under-sheet is truthful; destination-as-back-of-sheet is not.

## Decision rule

When evidence conflicts, Arena does not average bad options. It identifies the invariant that the product cannot violate, rejects architectures that violate it, and converges the remaining design around measurable acceptance gates.
