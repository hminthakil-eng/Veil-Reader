# Slice 1C — Reader Settings Architecture

Status: **DRAFT / stacked on Slice 1B**
Parent: `ux/slice1b-reader-sanctuary`

## Goal

Make Reader settings understandable, format-aware and reversible without introducing a second preference system.

## Architecture decision

- Keep `ReaderAppearance` as the single EPUB appearance model.
- Keep `SettingsStore` / DataStore as the single durable source of truth.
- Keep `LocalLibraryRepository.loadAppearance/saveAppearance` as the screen-facing persistence path.
- Do not create per-book preference storage in this slice.
- Do not expose controls that have no effect for the active format.

## UX structure

### EPUB — Quick
- Theme presets
- Text size
- Continuous scroll

### EPUB — Advanced
- Line height
- Page margins
- Publisher styling
- Reset EPUB settings

### PDF
- Settings remains reachable from Reader chrome.
- EPUB typography controls are hidden.
- The sheet explains the supported on-page PDF gestures instead of presenting controls that do nothing.

## Why this slice exists

The previous sheet mixed basic and advanced controls and made format behavior ambiguous. In particular, EPUB appearance settings do not apply to PDF pages. Slice 1C makes that boundary explicit rather than simulating unsupported behavior.

## Intentionally unchanged

- Readium engine
- EPUB navigator
- PDF navigator
- progress / locator persistence
- Room schema
- DataStore keys
- per-book preference model
- PDF zoom implementation
- page-turn implementation

## Verification gates

- [ ] assembleDebug
- [ ] unit tests
- [ ] lint
- [ ] EPUB settings opens
- [ ] Quick controls visible by default
- [ ] Advanced controls hidden by default
- [ ] Advanced controls reveal on demand
- [ ] reset persists through existing DataStore path
- [ ] PDF settings opens without EPUB typography controls
- [ ] no Reader crash / resume regression

## Next candidate slice

**Slice 1D — PDF Interaction Reliability**

Only after 1C is green:
- verify pinch zoom / pan behavior;
- verify fit/readability expectations;
- map actual Pdfium/Readium capabilities;
- fix root cause of zoom regressions without inventing fake settings.
