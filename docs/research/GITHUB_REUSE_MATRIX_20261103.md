# GitHub Reuse Matrix — Veil Reader Native Android

**Research check:** 2026-11-03. These are candidates, **not installed dependencies**. Confirm actual tagged release, dependency tree and asset-level license before any code vendoring. Treat other project's user data, cloud/network choices, artwork and UI as nontransferable.

| Source / URL | Source license | Immediate reusable idea | Adopt? | Required experiment / reason |
|---|---|---|---|---|
| [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) | BSD-3-Clause | EPUB/PDF navigator, RTL, search, annotations, TTS, OPDS and Compose navigator roadmap | **REUSE EXISTING** | Veil already pins Readium 3.4.0; investigate OPDS optional adapter or image viewer against pinned API. Do not create second navigation owner. |
| [Liseur](https://github.com/chmouel/liseur) | MIT, fonts independently SIL OFL | Provider-neutral `CatalogSource/FileSource/PositionSync` interfaces, OPDS, calibre-web/Komga, saved reader appearance | **DESIGN CANDIDATE** | Write a Veil-specific optional catalog contract; no imports yet, no always-on network; credentials/URL validation and source identity. |
| [Mihon](https://github.com/mihonapp/mihon) | Apache-2.0 | Locally loaded manga viewers, webtoon reading direction, categories, image-loading and storage/backup UX | **ARCHITECTURE STUDY** | Compare local comic viewer gesture and memory ownership with existing Veil Manga; do not add source scraper ecosystem by default. |
| [Harism Android PageCurl](https://github.com/harism/android-pagecurl) | Apache-2.0; demo assets may have exceptions | Curl geometry and source texture mapping | **EXISTING EXPERIMENT — HOLD** | Already part of a Veil lab; no migration unless physically demonstrable real deformation and source/license NOTICE. |
| [Roborazzi](https://github.com/takahirom/roborazzi) | Apache-2.0 | JVM Robolectric+Compose screenshot testing, UI tree JSON for accessibility review, golden diffs | **PILOT QA** | Confirm Gradle 9.6, AGP 9.4, Robolectric 4.17 and Kotlin plugin interoperability. Add to test-only module after isolated no-network benchmark; avoid >8GB desktop memory. |
| [Now in Android](https://github.com/android/nowinandroid) | Apache-2.0 | Architectural decision records, repository separation, Flow testing, benchmark/baseline profile and offline-first patterns | **PATTERN STUDY** | Audit Veil existing `:app`+ `:benchmark`; modularize only after measured bottlenecks and transition plan. |
| [LocalReader (ChengqianHuang)](https://github.com/ChengqianHuang/Reader) | MIT (per project README) | Native Compose reader, explicit offline privacy and MOBI/KF8 parser | **RESEARCH** | Test fixtures and parser provenance before new formats. Existing EPUB/PDF path remains Readium. |
| [Book's Story](https://github.com/Acclorite/book-story) | GPL-3.0 | Extensive custom themes, SAF, shelves, PDF/TXT/FB2 comparisons | **REFERENCE ONLY** | Do not transplant GPL implementation into Veil without deliberate licensing decision and compatibility review. |
| [KOReader](https://github.com/koreader/koreader) | AGPL-3.0 | typography, scanned-PDF handling, OPDS and ereader interaction ideas | **REFERENCE ONLY** | Behavioral comparisons only; no blind source copying or backend mixing. |
| [Readest](https://github.com/readest/readest) | Verify repo/tag and transitive licenses before reuse | Parallel Read, progress/highlight sync, read-along, dictionary and online/offline library split | **PRODUCT PARITY RESEARCH** | Cross-platform Tauri/Next app is not Veil's native replacement; document workflows and implement native designs independently. |

## Gated shortlist — executable hypotheses

1. **Readium-native OPDS catalog**. Use Liseur's separation of CatalogSource vs FileSource as a concept; design a `RemoteCatalog` domain interface with user-initiated download and import. Consent and safe URL/redirect handling come before code; no new INTERNET permission without approval. Measure pages retrieved and offline behavior. Status BACKLOG.
2. **Robolectric screenshot/test tree pilot**. Prototype Roborazzi in an isolated branch with two deterministic Compose UI fixtures; compare current `androidTest` evidence approach. A test-only dependency is acceptable only with proven exact-head Android CI and no new OOM on 8GB host. Status INVESTIGATE.
3. **Manga vertical reader memory pilot**. Use Mihon's approach only to construct a test matrix for lazy decode, virtualized long panels, restored locator and chapter boundaries; compare with Coil3/ZoomImage already in Veil. Status INVESTIGATE.
4. **Readium PDF/image/TTS capability audit**. Reuse existing native navigator; map missing features by document capability instead of adding independent renderers. Status INVESTIGATE.
5. **Offline-first patterns**. Audit Room + DataStore and optional sync boundary against Now in Android; avoid premature “Clean Architecture” rewrites when existing dependencies suffice. Status INVESTIGATE.

## Provenance / license conditions

- Inspect `LICENSE`, `NOTICE`, contributor guidance, tagged release, maintenance history, min SDK, AGP/Gradle/Kotlin matrix and transitive binaries before any import.
- Compare **exact tag or immutable commit** with current Veil baseline, not moving main. Record snippets and attribution if code is later adapted.
- “Open source” does not mean unrestricted copying; third-party fonts/cover art/icons/model weights have independent rights.
- No third-party APK extraction or proprietary UI/asset copying. No server-side upload of user's book text without explicit purpose and consent.
- Never silently mix cross-platform stacks into the Kotlin app solely to add a feature. Existing Readium architecture and release QA remain authoritative.

## Current immediate action

Implement the **QA evidence contract** for PR #576 first, not a potentially overlapping redesign: ensure exact PR-head checks, then run text-size/RTL/hit-target fixtures. No dependency installs or product features are asserted by this document.
