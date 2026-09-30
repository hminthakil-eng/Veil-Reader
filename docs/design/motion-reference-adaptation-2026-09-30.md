# Motion Reference Adaptation — 2026-09-30

## Purpose

Use the strongest interaction ideas surfaced from Skiper UI and Vengeance UI as design references for Veil Reader without introducing React, Next.js, Tailwind, Framer Motion, GSAP, or another web runtime into the Android app.

This slice is a native Jetpack Compose reinterpretation, not a source-code port.

## Applied in this slice

### Book artifact depth

Inspired by interactive/3D book presentation patterns:

- layered book-block depth behind the front cover;
- a restrained page fore-edge and bottom depth edge;
- cover aura remains derived from the publication art or existing Veil artifact state;
- recently opened books receive a short perspective settle instead of a continuous hover loop.

### Progressive material sheen

Inspired by progressive-blur/reveal polish patterns:

- diagonal progressive sheen across the cover;
- subtle edge falloff and spine depth;
- no runtime blur is required, avoiding a high GPU cost in scrolling shelves.

## Veil rules preserved

- Reduced Motion removes the spatial settle.
- Static material depth remains visible with Reduced Motion because it communicates object structure, not animation.
- No ambient infinite animation was added.
- Reader/Sanctuary behavior and renderer ownership are untouched.
- No Room, Readium/PDFium, navigation, persistence, schema, or release configuration changes.
- No third-party dependency was added.
- Publication cover pixels remain untouched; Veil-owned layers render around/over the displayed artifact only.

## Source path

Base: `alpha/w22-open-cancellation-integrity-v1`

Working branch: `design/motion-reference-artifacts-v1`

Primary implementation:

`app/src/main/java/com/veilreader/app/ui/screens/Common.kt`

## Verification status

Source adaptation is committed. Android compilation and device visual QA are intentionally not claimed by this document.

The next visual gate should inspect:

1. Continue Reading hero cover.
2. Recent books on Threshold.
3. Archive Gallery and Shelves.
4. Very dark and very bright cover art.
5. Persian/RTL surfaces.
6. Reduced Motion enabled.
7. Fast shelf scrolling on a mid-range device.

The effect should read as physical depth at a glance but disappear from attention while reading or browsing.
