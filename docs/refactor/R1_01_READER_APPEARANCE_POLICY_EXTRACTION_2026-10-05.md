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
