# Veil Reader — Three-Environment Grand Forge Engineering Plan

**Prepared:** 2026-11-03 | **Status:** proposed architecture and execution plan; not product approval  
**Canonical repository:** `hminthakil-eng/Veil-Reader`, `main` | **Owners:** ChatGPT orchestration/forensics, Antigravity native engineering, AI Studio alternate native implementation/UX, GitHub durable review/CI  
**Policy:** Observe → Predict → Reuse → Integrate → Automate → Build → Verify → Learn.

## 0. Decision and source of truth

Veil is an **offline-first native Android Kotlin/Compose reader**, not a web app rewrite. Main remains canonical; `:app` + `:benchmark`, Readium Kotlin Toolkit 3.4, Room, DataStore, PDFium, Media3, Compose, and current page-engine ownership are retained. Keep four reader modes distinct (PAGED, PAPER CURL, SLIDE, SCROLL). An AI Studio imported-Android project, an Antigravity workspace, and the local desktop are **development surfaces**, never parallel sources of production truth. Every proposed code change is tied to an immutable Git SHA and reviewed via an isolated draft PR. Preserve all existing uncommitted worktrees.

The first release gate is reader reliability and credible evidence, **not feature count**. Build success and synthetic PR merge checks cannot establish exact PR-head validation, tactile page curl, TalkBack, cloud privacy or public release approval. Track SOURCE, DEVICE, PRODUCT, RELEASE separately.

## 1. Immediate findings and existing constraints

- `main` currently carries Android Gradle Plugin 9.4, Kotlin Compose plugin 2.3.21, compile/target SDK 37, min SDK 26, Readium Kotlin 3.4, Room 2.8.5, Media3, Coil3, an existing Baseline Profile module and release shrinker.
- Legacy `README.md` reports Readium 3.3; the current build Gradle file is authoritative at 3.4. Do not change dependencies based on the stale README.
- Existing reader features include import/search, saved progress, highlights, bookmarks, library management and backup; avoid reimplementing them without a demonstrable defect.
- PR #576 has a single 48dp reader chrome width change, but the 4-control weighted row needs 320/360dp + 200% text + TalkBack evidence. An earlier CI forensic comment identified synthetic-merge rather than exact source HEAD checks and missing dependency snapshots.
- PR #575 is a **draft hypothesis** about optimistic imported-book rows and Room observation races. It must be diagnosed and accepted independently; no migration shortcuts.
- Source `main` is development-integrated only. Paper physical acceptance (#408), reliable audible TTS, restoration and accessible visual review are still blockers. Never interpret absence of tests as green.

## 2. Architectural boundaries and invariant owners

| Boundary | Current authority and required contract | Change rule |
|---|---|---|
| Publication & navigator | Readium publication/locator; content and reading direction authoritative | No silent page mode fallback, no web render duplication |
| Reading interaction | One gesture arbiter per active reader session; selection, links, zoom, chrome, keyboard, TalkBack must not compete | Cancel must not persist a turn; release may commit exactly once |
| Durable data | Room for books/annotations/positions, DataStore for preferences; backup explicit | No ack before durable write, deterministic migrations & rollback |
| Library retrieval | Single query/filter/collection state; user-initiated Home shelf navigation wins restoration race | No duplicated filters or fake data in screens |
| PDF and manga | Dedicated format capability adapters and own input/zoom lifecycle | No EPUB-specific assumptions or cross-mode gesture leakage |
| Narration | One audio session owner (Media3 and reader checkpoint state); explicit user choice of provider | Pause while loading must cancel; no unseen cloud calls |
| Optional online features | Provider-neutral interface + clear consent + storage boundaries | Offline default, no credentials in source, opt-out and deletion |
| Experience | Grayfog design tokens and native Compose; real book remains readable | Persian shaping, contrast, 48dp targets, 200% font, reduced-motion |

Use small façade interfaces around format/capability/remote-source differences; avoid mass modularization or Room rewrites until profiling shows an actual coupling/build bottleneck. Keep `:benchmark` and baseline-profile ownership with proven measurements.

## 3. Work allocation and no-conflict protocol

**Antigravity:** Open an exact-SHA, dedicated **native Android** worktree. Inspect repository architecture and repo-specific instructions, implement one bounded Kotlin/Compose+test patch at a time; run Gradle, Robolectric, on-device instrumentation when memory allows, accessibility and perf review, and generate a diff with touched-file list. No default/main write, auto-push or merge. Record any agent-derived code as untrusted until independently reviewed.

**Google AI Studio:** Work on the existing imported `Veil-Reader` **Android app**. Produce an independent UX/feature proposal and preview on its streaming Android emulator. Pin imported revision/file identity before claiming parity; return exact file paths, patch/export, test commands/results, screenshots, font scale and locale. Do not publish, push, overwrite project main, infer that an AI Studio `BUILD SUCCESSFUL` applies to another SHA, or silently introduce proprietary assets.

**ChatGPT / GitHub:** Research upstream licenses and API fit; issue narrow design briefs and test matrices; compare independent proposals; insist on immutable PR-head CI, security snapshots, review and rollback; maintain decision ledger; own final recommendation, not auto-promote. Concurrent Grand Forge agents may edit other branches: check branch/ref before every write.

**Merge contract:** One owner per file range at a time. Separate design/prototype from canonical implementation. Code accepted only after (1) explicit source SHA, (2) tests on exact HEAD, (3) accessibility/RTL screenshots and input-state checks as applicable, (4) dependency/license review, (5) non-regression for book locator/storage/modes, (6) rollback. Any failure => HOLD.

## 4. Feature program (ordered, not promised deliveries)

| Wave | Reusable foundation | Ship-sized vertical slice | Mandatory acceptance |
|---|---|---|---|
| P0A — truth & CI | Existing repo QA/Gradle/Readium | Resolve synthetic merge CI and false-green dependency review (#568–#575) independently, verify #576 exact HEAD | All required jobs and checkout SHA match source; fail-closed dependency evidence |
| P0B — reader trust | Existing Paper/Room/Media3 | One reproducible blocker at a time: durable import→progress, cancelled-turn semantics, physical Paper, TTS pause | Process-death restore, 100-turn/cancel test, owner phone video and audio evidence |
| P0C — premium Reader UX | Compose tokens, Antigravity, AI Studio | Reader chrome and live appearance controls; keep real publication visible and gestures owned | EN/FA 100/150/200%, RTL, TalkBack, 320/360dp, real book visual comparison |
| P1A — organized library | Existing Room/FTS/backup | Search + collections + factual metadata + resume return flows refined, not duplicated | 1k/3k books, filter restoration, no false empty state, resilient SAF |
| P1B — long-form formats | Readium, PDFium; Apache-licensed Mihon patterns | Native webtoon image delivery + page viewer, PDF fit/zoom and image affordances | Large images, long scroll, page locators, gesture priority, memory cap |
| P1C — remote catalog (opt-in) | Readium OPDS; Liseur provider adapters | Read-only catalog browse → user-initiated download → local import; optional network only | Explicit consent, source credentials safe, offline reader unaffected, no remote uploads |
| P1D — knowledge/listening | Readium TTS, Media3, dictionary research | Reliable voice selection/pause, local dictionary/selection actions | No unconsented network request, playback lifecycle, accurate source locator |
| P2 — personal layer | Room outbox design only after data trust | Optional cross-device sync, library backup UX and provenance | Consent default OFF, conflict policy, revocation/delete, restore test |
| P3 — fantasy signature | Existing Castle/reading ledger | Optional original Grayfog atmosphere and book-linked progression | No blocking reader, no fabricated events or manipulative streaks |

No new sync backend, AI inference or network permission is authorized by this plan alone.

## 5. First implementation missions

**Mission A (immediate):** Verify PR #576 at immutable `ba3c5e33949994671d585da4a52ccc753bf0e737` on desktop. Check compile and `ReaderMenuSettingsTest` XML, do not substitute old checkout results. Then render the four-control row in a dedicated UI test fixture at 320dp/360dp, font scales 1.0/1.5/2.0, FA/EN and TalkBack semantics. If width pressure or clipping exists, propose an adaptive compact/overflow treatment in a NEW isolated patch; don't silently merge #576.

**Mission B (Antigravity):** Independently audit native module ownership (Readium, Room, DataStore, PDF, Manga, TTS), current open PR overlaps, library/index hot paths, memory risk and testing. Write `docs/architecture/ANDROID_CAPABILITY_OWNERSHIP_20261103.md` plus one reproducible test proposal, no wholesale source edits before evidence.

**Mission C (AI Studio):** From imported Android app, produce UX study of Reader chrome vs Library entry/resume, screenshot actual streaming emulator, propose a **single file-scoped** small design improvement to Grayfog navigation with RTL/200% constraints. Implement only in its isolated AI Studio checkpoint; export patch + filename + test report. Never claim export is applied to `main`.

**Mission D (research):** Evaluate sources in [GitHub Reuse Matrix](../research/GITHUB_REUSE_MATRIX_20261103.md); benchmark and prototype only the highest-fidelity licensed option. Every reuse decision gets exact upstream version/tag, license/NOTICE, fork burden, security and measurable outcome.

## 6. Testing pyramid and readiness

1. Static: dependency/licensing, Kotlin compile, lint, locale resources, main-thread blocking, code owner.
2. Unit/contract: stable semantic locator, filter restoration, Room writer ack, sync protocol in pure core, touch/visibility/semantics.
3. JVM screenshot: pilot Roborazzi with Robolectric *only if test-plugin compatibility and memory proven*; record/verify separate golden baselines for 1.0/2.0 scale, FA/EN, dark/sepia.
4. Emulator: API 35/37 if resources allow; real text/image EPUB, large PDF, CBZ, warm/cold open, gestures, process kill. Screenshots and logs labeled emulator not physical.
5. Physical: Pixel/Samsung and one lower-end; actual paper deformation at supported refresh rates, TalkBack, audio/Bluetooth, frame pacing, crash recovery. Video/source APK hashes mandatory.
6. Security/release: exact-head CI not merge ref; dependency snapshot nonempty and SHA-correct; R8/ABI/profile, deterministic signed APK, privacy and rollback. No public release while P0 gates fail.

## 7. Operating cadence / artifacts

For each slice: short design decision record → precise file ownership → risk hypotheses → tests-first implementation → independent review → DRAFT PR → exact-head jobs → device/product gate → deliberate merge. For every run record SHA, device/memory, tool/model, altered files, before/after, test commands/log URLs, failure class and next decision. Avoid auto-executing unrelated agents or rewriting existing canonical docs. If desktop memory drops below 1.5GB, do not run AVD alongside Antigravity + AI Studio; prefer JVM tests and alternate sessions.

The current plan is a **proposal and engineering handoff**. None of its future-wave features is claimed implemented, validated or release ready.
