# Grand Forge — Open-Source / Research / Tooling Intake
Date: 2026-10-11
Status: RESEARCH + FIRST ISOLATED IMPLEMENTATION; not a product acceptance certificate
Canonical development base at intake: `grand-forge/p0-kindle-reader-quality-20261008` (`9c65997d891b2b19ccba19e5729ce82bbdff9cce`). The default `main` is not the active Reader baseline.

## Non-negotiable acceptance rules
- Observe → Predict → Reuse → Integrate → Automate → Build → Verify → Learn.
- Keep the current proven Readium 3.4.0 EPUB/PDF reading owner, Room durable state, Media3 TTS transport and native Compose UI. Do not layer a second navigator/persistence owner on them.
- Evaluate licensed open-source code for reuse; analyze proprietary Kindle/Moon+ behavior without transplanting closed-source assets, source, DRM, licenses, voice models or keys.
- Each behavior is product-accepted only after source SHA + exact-head CI + emulator AND real-device proof. A screenshot-only or unit-only pass is not enough for GPU Paper or neural audio.
- Maintain isolated PAGED / SLIDE / PAPER_CURL / SCROLL modes. Never impersonate PAPER with slide.
- No tracking/cloud/network requirement for offline reading. New external providers require explicit user selection and privacy disclosures.

## Study sources and practical Veil decisions

| Reference | Upstream / source | Evidence and lesson | Use and licensing gate |
|---|---|---|---|
| Readium Kotlin Toolkit | https://github.com/readium/kotlin-toolkit | Kotlin publication, locator, navigator, text extraction, PDF, RTL, TTS. Veil already uses 3.4.0. | KEEP; use official APIs, verify Pdfium locator migration/scroll defaults and newer image tap support. |
| Readest | https://github.com/readest/readest ; https://github.com/readest/readest/blob/main/apps/readest-app/docs/architecture.md | Separates reader engine, navigation, notes, parallel reading, library/store responsibilities. Uses Foliate-based engine, React and Tauri; not a native Compose drop-in. | STUDY code boundary & UX; rewrite as native contracts only, review repository license before any actual copy. |
| Foliate JS | https://github.com/johnfactotum/foliate-js | Parser/renderer/search/overlay/progress responsibilities exposed as separate modules. JS/web security and CSP assumptions differ from Android. | STUDY abstractions; do not replace Readium with WebView-in-WebView. |
| Anx Reader | https://github.com/anxcye/anx-reader | Flutter/Riverpod reader using Foliate JS; UI, note and AI interaction patterns. Repository documents license changes across versions. | STUDY interaction flows; pin commit + verify license per file before reuse. |
| KOReader | https://github.com/koreader/koreader | Advanced document layouts, formats, plugins, dictionaries and e-ink workflows. | REFERENCE only unless compatible AGPL obligations deliberately approved. |
| Librera | https://github.com/foobnix/LibreraReader | Multi-format Android document/reading subsystems incl. MuPDF and native libs. | REFERENCE; GPL/AGPL and native transitive obligations may preclude direct reuse. |
| Irurueta Curl | https://github.com/albertoirurueta/irurueta-android-gl-curl | Native 3D front/back texture curl. Already a debug-only lab in draft #479. | TEST on physical devices, not connected to EPUB until proven. |
| eschao PageFlip | https://github.com/eschao/android-PageFlip | GL 3D mesh / fold edge / shadows. Independent debug lab candidate in draft #479. | TEST dependencies, native rendering and gestures; no production binding yet. |
| Sherpa ONNX | https://github.com/k2-fsa/sherpa-onnx | Offline TTS native inference, model loaders and Kotlin JNI. Native-PCM architecture planned in draft #485. | Continue separate neural branch; verify transitive licenses/models and physical audio. |
| Android Baseline Profiles | https://developer.android.com/topic/performance/baselineprofiles/overview | Speed up first-launch and hot code paths with measured profiles. | Keep existing benchmark harness; verify generated profile packaging instead of relying on a green compile. |
| Android performance measurement | https://developer.android.com/topic/performance/measuring-performance | Measure actual frame times, memory and jank on physical devices across 60/90/120 Hz. | Per-route, per-device trace with exact build identity. |
| Readium EPUB image viewer | https://readium.org/kotlin-toolkit/latest/guides/navigator/epub-image-viewer/ | Readium 3.4 exposes experimental image tap targets. | Existing Veil image tap + full-screen zoom already implemented. Improve it rather than recreate. |

## Tooling inventory, grounded in repository and live integrations
- REPO-LOCAL: `.agents/skills/veil-android-engineer`, `veil-reader-ui`, `veil-reader-product-designer`, `veil-release-guardian`, `veil-world-class-app-development`, `systematic-debugging`, `test-driven-development`, `verification-before-completion`, `dispatching-parallel-agents`, `impeccable`.
- ACTIVE IN CHAT: GitHub code/PR/branch/CI connector; Context7 developer-documentation connector; public web/documentation search.
- CODEBASE PILOTS: Context7 PR #476, UI Skills PR #477, Android skill pack #453; avoid merging stacked plugin tooling into the app or enabling it in runtime without proof.
- Mobbin UI reference plugin is available, but its current connector call returned a **paid-plan blocker**. No Mobbin screen evidence is claimed.
- Remote Desktop Commander registered Windows desktop is **offline** at intake; cannot claim device/build/debug from that machine.
- Playwright CLI supports website/browser smoke testing; it does not certify native Android gestures, GPU deformation, TTS audio or real-device jank. Supabase is unnecessary until optional sync is explicitly approved.
- Commercial reference APKs are test fixtures for observed UI/capability behavior only; no proprietary code copy.

## Gap decisions from actual canonical source
- `app/build.gradle.kts` already pins Readium **3.4.0**, Compose **1.12.1**, Media3 **1.11.1**, Room **2.8.5**. An upgrade recommendation without source check would be wrong.
- `PdfiumLocatorCompat.kt` already provides migration of pre-3.4 PDF locators and a tagged persistence version; do not duplicate migration.
- `ReaderBookSearchDialog.kt` and `ReaderPublicationSearch.kt` already implement bounded Readium search, search pagination and dedicated search UX. Search remains a gated feature; complete certification, don't invent a second parser.
- `ReaderImageViewer.kt` already implements tap-to-open, full-screen illustration view, pinch/drag, zoom buttons and accessibility labels. A documented micro-gap: no double-tap-to-detail/fit affordance.
- `VeilFeatureGates.kt` intentionally holds GPU Paper, neural TTS, sync and other risky features off in production. Do not flip release flags as an acceptance shortcut.
- Canonical automated proof on P0 GPU repair draft #468 succeeded for three CI lanes at its exact head; physical tactile acceptance is still absent.
- Independent ready-made curl lab draft #479 has a failed Storage Instrumentation CI run; its physical evidence is also unverified.
- Draft #485 in-process neural audio contract passed exact-head CI/Storage/Performance, but does not yet include finished native JNI + AudioTrack model playback.

## First isolated engineering slice
Branch: `grand-forge/research-driven-image-ux-20261011`.
Files: `ReaderImageTransform.kt`, `ReaderImageViewer.kt`, `ReaderImageTransformTest.kt`.
- Add double-tap zoom toward the actual touch centroid (2.5x), then a second double-tap reset to fit.
- Reuse existing clamped aspect-ratio transform and keep pinch/pan and the explicit zoom buttons intact.
- Do not touch Readium's page gestures, cover handling, persistent location, database or privacy surfaces.
- Regression tests: off-centre content stability; second-tap reset; unmeasured viewport; letterboxed wide illustration.
- ACCEPTANCE NOT CLAIMED: exact-head compile/tests, physical tap/drag, accessibility and 60/120Hz checks remain required.

## Evidence-led next engineering waves
1. P0 physical Paper arena: debug lab #479 vs native #468; inspect GL mesh vs slide; compare genuine ink deformation, cancel/commit, RTL/spine, memory and jank. No production rollout before proof.
2. P0 audible TTS: finish #480 Pause intent, then #485 native JNI/AudioTrack behind debug gate; tests on English/Persian/mixed EPUB, voice consistency, focus and continuous 30-minute audio. Keep external engine experimental.
3. P0 durability: process kill at chapter boundary and exact-position restoration; disallow phantom preview commits.
4. P1 annotated study: selection, dictionary/translation provider, notebook export and return-to-passage on a large real EPUB.
5. P1 appearance and Persian: true live font preview, custom fonts, RTL bidirectional selection and TalkBack/large type/line geometry.
6. P1 PDF and large books: links, OCR-free native text when available, zoom, scanned documents, vector clarity and cover/spine stress.
7. P2 library: 1k-10k books, series retrieval, cover cache pressure, backup and restore; optional OPDS/Calibre only with explicit credential isolation.
8. P2 Grayfog/Castle: premium visual motion design that stays outside uninterrupted reading, original asset provenance and deterministic gaming progress.

## Quality gates per wave
- Verify exact branch, SHA and compatibility with the canonical base.
- Run relevant unit tests + Android CI + Storage Instrumentation + Performance Benchmarks on that exact commit.
- Preserve screenshots/trace for failed states as well as success; no sibling-branch GREEN transfer.
- Test screen reader, RTL, 200% text and reduced motion when UI is touched.
- Require Samsung/Pixel physical video or equivalent proof for finger-driven GL and real audio; CI green is only TECH GREEN.
- Keep draft until owner review and evidence signoff. Revert by reverting scoped commits.

This document is an execution ledger; it does not imply work across all waves is completed.
