# Veil Reader rebuild — architectural proposal

Status: PROPOSED / owner review required before implementation planning.
Date: 2026-10-18. Owner: Eyad. Workflow: Superpowers.

## Intent and success

Rebuild Veil Reader into a polished, dependable offline Android book reader with an original archival mystery identity and optional reading-linked world. The owner requests a full-force rebuild using relevant skills. Prior owner feedback says page turning has become good on the phone: preserve that achievement and identify the exact accepted APK before changing its engine. That feedback is subjective acceptance, not evidence for an arbitrary newer SHA.

Success means a real imported book can be resumed, read, customized, annotated, searched, listened to and safely closed without losing work. Visual depth supports retrieval and reading. The optional world must never impair page interaction, privacy, accessibility or persistence. No claim of superiority to competitors without comparative evidence.

## Observed source and limits

- Canonical main inspected at `811f0d7d521fc11f110638a100c315f698d52fd5` in a complete Git checkout.
- Android app and benchmark Gradle modules; Kotlin/Compose; declared Readium **3.4.0** dependencies. README's 3.3.0 capability paragraph is stale.
- `ReaderScreen.kt`: 6,786 lines. Existing `ReaderViewModel`, writer leases, session tracking and mode handoff already exist; extend these instead of introducing competing owners.
- `LocalLibraryRepository.kt`: 1,616 lines. Room and DataStore remain the established durable stores.
- `quality/physical-device-status.json` says UNVERIFIED and has no executable SHA. Roadmap live-status is dated October 11 and is historical context, not current acceptance.
- PR #509 remains a separate draft native-paper candidate; observed head `e37869bb68e0497b5cbb9ca9f9b56c4e53286020`. Do not treat its earlier CI receipts as current-head verification.
- PR #542 is a separate Reborn Home visual slice. Its own review says visual RED.
- PR #545/#546 are separate station experiments. #546 reports scoped tests/build success, but physical GPU execution, performance and visual acceptance remain open.
- Research PRs #550–#552 are sources of proposals, not installed capabilities or product improvements.
- Sandbox inventory: Java 17 available; repository documentation requires JDK 21 and Android SDK 37. No `adb` found on PATH. No Android build or device test performed in this design task.

## Alternatives

| Approach | Advantage | Cost/risk | Decision |
|---|---|---|---|
| Incremental architectural rebuild | Reuses working publication, data and interaction behavior; small reversible changes | Requires explicit ownership and staged integration | Recommended |
| Entirely new application | Maximum structural freedom | Reimplements migrations, rendering, annotations, accessibility and lifecycle; highest regression risk | Reject for this rebuild |
| Visual-only reskin | Fast visible changes | Leaves reader coupling and durability risks; cannot meet overall request | Insufficient |

## Architecture and ownership

Keep the current Android technology and dependency versions until a separately justified upgrade passes checks. Start with package-level seams, not a speculative many-module conversion.

| Boundary | Responsibility | Ownership/contract |
|---|---|---|
| Publication host | Open publication, native Readium navigation, locator and layout events | Readium remains the sole pagination authority; Android views stay lifecycle-bound |
| Reader session | Active book/open generation, accepted locator sequence, progress and engaged reading | Extend existing ReaderViewModel and writer leases; one writer per active generation |
| Interaction coordination | Resolve turn/selection/zoom/chrome/settings/audio ownership | Explicit transitions; stale callbacks cannot commit; cancellation leaves locator unchanged |
| Rendering adapters | Paged, Paper, Slide and Scroll presentation | Four distinct modes; no silent substitution; report unavailable capability explicitly |
| Appearance | Global defaults, future per-book overrides, live preview | Effective values derived once; cancellation restores prior committed values and locator |
| Reader surfaces | Chrome, contents, search, annotation, appearance and listening sheets | Compose renders state; surface components do not own persistence or pagination |
| Library/archive | Import, duplicate detection, covers, metadata, retrieval and resume | Existing repository/storage remains canonical; cover processing stays asynchronous |
| Listening | Playback, provider readiness, stable voice and interruptions | Reuse existing TTS pipeline; one playback controller; explicit model availability |
| Optional world | Stars, constellations, castle/island and progression | Existing game state remains isolated; consumes committed reading summaries only |

Initial scope is one independently reviewable subsystem: reader interaction and surface boundaries. Library, per-book appearance persistence, neural listening and world rendering get their own spec/plan/implementation cycles after this foundation. This document is an umbrella direction, not permission to implement every subsystem together.

### Reader transition contract

Reuse existing mechanisms where equivalent. Model transitions around idle, preparing, interacting, settling, applying appearance and closing. These names describe responsibilities; do not add a redundant second state machine if current types already represent them.

1. Open requests receive an instance/generation identity. Later requests invalidate older completions.
2. A turn captures its source and destination identity plus layout/theme epoch. Held/reversed gestures do not persist progress.
3. Release requests one semantic Readium navigation. Only the confirmed accepted destination may advance the canonical progress sequence.
4. Mode or appearance changes finish/cancel current interaction first and invalidate incompatible snapshots. Preview views never write progress.
5. Selection and PDF zoom retain gesture ownership until their action ends. Back closes the active surface before leaving the book.
6. Close invalidates incoming callbacks and follows the existing durable write/lease contract. Save failure remains visible and recoverable. A success message requires a committed write.

### Data and migration

The first subsystem changes no Room schema, backup format, file location or application identity. Future per-book preferences require a separate explicit migration and backup compatibility design. Cached covers, snapshots and GPU resources are disposable; notes, locators and reading sessions are not. Never uninstall the user's existing app to work around a signer mismatch.

## Visual direction

Preserve the agreed Grayfog identity: deep blue-black archival shell, restrained brass details, readable parchment reading surface, editorial typography and subtle astronomical motifs. Keep navigation labels plain and localized.

Home leads with the current book and one clear resume action; Library emphasizes retrieval, series, collections and real progress; Reader is quiet with center-tap controls and a compact appearance sheet showing the actual book. No invented progress or unfinished feature presented as working.

The Sefirah library proposal gives each imported book a stable star identity; groups can form constellations without replacing a searchable accessible list. Fog and motion obey reduced-motion, power and lifecycle settings. Exact constellation thresholds and 3D assets belong to the world spec. Station experiments remain opt-in until visual/device evidence passes. Product text must not expose engine/framework names.

## Verification and acceptance

Every receipt identifies base/head SHA, build variant, package, signer, artifact hash, fixture and device where applicable. Existing repository budgets remain authoritative; do not relax them to produce a pass.

| Area | Required evidence before promotion |
|---|---|
| Baseline identity | Reconcile main, #509, #542 and #546 ancestry; identify the owner's accepted APK/SHA without overwriting any branch |
| Regression behavior | Existing 39 reading-policy checks, relevant reader/session tests, full unit/lint/build checks; no skipped failures |
| Reader modes | Real LTR and RTL EPUB: 100 forward/back turns per mode, cancel/reverse, chapter boundaries, first/last page; exactly one settled navigation |
| Durability | Abrupt process death after accepted navigation/notes, background/close races and reopen; latest committed state restored; no double rewards |
| Layout | Real FA/EN locale, 320dp/390dp/tablet, 100%/200% font, landscape, themes; no lost locator or clipped primary action |
| Accessibility | TalkBack focus/actions, selection/zoom ownership, reduced motion; controls usable without atmospheric graphics |
| Performance | Existing physical budget: startup median <=600 ms; frame overrun p95 <=0 ms/p99 <=8 ms; CPU p95 <=16.7 ms/p99 <=24 ms. Emulator guardrails are separate |
| World handoff | 30 reader/world round trips with locator retained; rendering resources released while reading; reader benchmark regression blocks promotion |
| Listening | Stable selected voice, pause while preparing, foreground/background/lockscreen/audio interruption; actual audible evidence separate from source tests |
| Visual quality | Actual app screenshots and motion on the accepted phone, explicit owner review; static fixture or CI success alone insufficient |

Initial interaction-boundary extraction must be behavior-preserving. Scope tests to genuinely risky transitions and existing regressions; do not create tests that only assert file layout or new type names.

## Work packages and sequencing

These are delivery boundaries, not the detailed implementation plan (that follows design approval).

1. Recover a source-qualified baseline and compare behavior against the owner-accepted page-turn build. Capture current failures before edits.
2. Extract one reader seam at a time: interaction ownership, transient surfaces and appearance coordination. Preserve native publication/progress ownership and existing contracts.
3. Prove a complete import → resume → read → appearance → annotate → close → process restore journey in the isolated review build.
4. Design and rebuild Home/Library surfaces with native renders, meaningful empty/error states and physical visual review.
5. Independently specify and deliver per-book settings, listening quality and archive retrieval gaps, each with storage/privacy/device gates appropriate to its scope.
6. Independently specify and deliver the optional stars/constellations world; admit richer 3D only after measured benefit and resource budgets.

Each package is reversible and gets a source review and exact-head checks. Reuse licensed existing work after verifying scope and ancestry; do not merge all drafts wholesale.

## Skills and collaboration

Use Superpowers brainstorming → written design review → writing-plans → approved execution method → verification/review. Apply relevant Android/reader analysis, debugging, accessibility and visual skills when their tasks arise. Do not load unrelated writing, manga or media skills merely to increase a count.

Gemini participation requires a real available connection and a recorded response; no simulated peer review. No Gemini consultation occurred in this task. Parallel execution, if selected after planning, must give workers separate ownership and retain one integration authority.

## Rollback and release boundary

Use a dedicated branch and reversible feature gates for behavior-changing experiments. Keep the current reader available while evaluating candidates. A proposed branch is never automatically the accepted installation baseline. No force pushes, branch deletion, bulk merges, app-data resets or public release as part of design approval.

## Decisions requested

Approve or amend the recommended incremental architecture, visual direction and reader-foundation-first sequence. Written-spec approval permits creation of the detailed first-subsystem implementation plan; execution method is selected after that plan is reviewable. The rebuild is not yet implemented and no new APK is claimed.
