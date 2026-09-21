# Slice 1D — PDF Interaction Reliability

Status: **DRAFT / stacked on Slice 1C**
Parent: `ux/slice1c-reader-settings-architecture`

## Goal

Make PDF zoom reliable without forking the reader engine or adding controls that do not affect the actual PDF surface.

## Audit findings

- Readium Kotlin Toolkit: `3.4.0`.
- Readium PDF adapter uses `AndroidPdfViewer 3.2.8` over `PdfiumAndroid 1.9.8`.
- `PdfiumDefaults` has no zoom preference; zoom is owned by the embedded `PDFView`.
- AndroidPdfViewer already enables swipe, double-tap and scale gestures by default.
- `DragPinchManager.onScale()` updates the real `PDFView` zoom.
- Double-tap cycles through mid zoom, max zoom and reset.
- Readium's first-render `fitToWidth()` is not repeatedly called after every pinch; the repeated-reset hypothesis was rejected.
- Readium's directional navigation adapter does not replace PDF pinch scaling.

## Slice decision

Preserve native pinch, double-tap and pan. Add a small reliability layer in Reader chrome using the same live `PDFView`:

- Zoom out (`−`)
- current zoom percentage
- Fit width
- Zoom in (`+`)

The controls are visible only for PDF and disappear with Reader chrome, preserving the Reader Sanctuary.

## Architecture

- No PDF engine fork.
- No alternate renderer.
- No Room/DataStore migration.
- No persisted zoom state in this slice.
- `AndroidPdfViewer 3.2.8` is declared directly only because Slice 1D intentionally calls the same PDFView API already brought transitively by Readium.

## Verification evidence

- ✅ `:app:assembleDebug`
- ✅ `:app:testDebugUnitTest`
- ✅ `:app:lintDebug`
- ✅ PDF opens on emulator with no FATAL/ANR
- ✅ Reader chrome exposes Zoom out / Fit width / Zoom in
- ✅ initial zoom exposed as 100%
- ✅ Zoom in: 100% → 135%
- ✅ Zoom out: 135% → 100%
- ✅ Fit width: returns/holds 100% for the QA fixture
- ✅ EPUB path exposes no PDF zoom controls

## Deliberately not claimed

- The emulator automation does not synthesize a true two-finger pinch.
- Native pinch/double-tap support was verified by dependency/source audit; direct controls were hands-on verified on emulator.
- Persisting zoom across a close/reopen is out of scope.

## Next candidate slice

**Slice 1E — Reader Motion & Page-Turn Reliability**

Focus: replace the current slide-like EPUB page transition with the best supported, testable reading transition without breaking scroll mode, RTL, progress or selection.