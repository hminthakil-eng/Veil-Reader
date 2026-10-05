# R1.01 Slice 1 — Reader Appearance Policy Extraction

Date: 2026-10-05

## Scope

Extract pure appearance/format/language policy from `ReaderScreen.kt` into
`ReaderAppearancePolicy.kt`.

Moved:
- material-page availability fallback;
- fixed-layout appearance fallback;
- appearance capability model;
- RTL typography classification;
- CJK typography classification.

## Explicit non-changes

This slice does not move or alter:
- Reader session state;
- locator/progress persistence;
- Readium navigator ownership;
- Paper/Slide input;
- coroutine jobs;
- selection/notes;
- TTS;
- PDF zoom;
- Chrome;
- lifecycle behavior.

Existing tests continue to reference the same package-level symbols.

## Reason

The first decomposition slice should reduce ReaderScreen responsibility without
changing the state machine. Pure policy is the lowest-risk boundary and provides a
template for later ownership-based extraction.


## Slice 2 — pure interaction policy

Also extracted into `ReaderInteractionPolicy.kt`:
- boundary-feedback rate policy;
- locator suppression while Paper/Slide preview owns the visual;
- final navigator snapshot policy;
- context-control selection (Appearance vs PDF View);
- back disposition;
- Reader chrome auto-hide policy.

This slice also owns no side effects. The actual cancellation, close, navigation,
gesture and persistence operations remain in ReaderScreen / their existing owners.


## Slice 3 — page presentation primitives

Extracted into `ReaderPresentation.kt`:
- Reader canvas color;
- boundary pulse rendering;
- page atmosphere rendering;
- renderable viewport guard.

The viewport-change preview-cancel decision moved to
`ReaderInteractionPolicy.kt`.

This still does not move Reader mutable state or side-effect ownership.
