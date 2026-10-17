# GEMINI 4 / CANVAS — VEIL READER GRAND FORGE

**Prepared 2026-10-17.** Collaboration brief for an interactive Gemini Canvas session + Gemini CLI in the owner's desktop; this file does NOT imply Gemini has received, accepted or executed the task.

## Exact full-source handoff

- Canonical repository: https://github.com/hminthakil-eng/Veil-Reader
- Stable input baseline: `design/market-leaders-rebuild-20261017` at `84cb9ae9183e2534be4cbe9787a9004539b7edc2` (at handoff creation).
- Isolated work/output branch already exists: `collab/gemini4-canvas-handoff-20261017`; DO NOT force push, overwrite, cherry-pick over unrelated local uncommitted changes or merge into main.
- Pending draft PR: https://github.com/hminthakil-eng/Veil-Reader/pull/542
- CI test pass / Forge QA APK + Robolectric render fixture evidence: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39004846278
- Repo contains ~1,337 tracked paths (including non-source files at the recorded revision); consume the full working tree from Git, not copied snippets in a context window.
- If working with Gemini Canvas alone (without repository filesystem or source-file attachment access), ask to attach the repository archive or switch to a code-capable workspace BEFORE proposing modifications. Canvas screenshot / generated HTML is never a Kotlin build.

### Safe desktop start (PowerShell, only after owner computer is online)

```powershell
$repo = 'E:\EyadStudio\Projects\Veil-Reader'
$agent = 'E:\EyadStudio\Projects\Veil-Reader-GeminiCanvas-20261017'
git -C $repo fetch origin --prune
git -C $repo show-ref --verify --quiet 'refs/remotes/origin/collab/gemini4-canvas-handoff-20261017'
if ($LASTEXITCODE -ne 0) { throw 'Handoff branch missing: stop; do not modify default checkout' }
if (Test-Path $agent) { throw 'Destination exists: inspect it, do not overwrite' }
git -C $repo worktree add -b 'work/gemini4-canvas-implementation-20261017' $agent 'origin/collab/gemini4-canvas-handoff-20261017'
git -C $agent status --short --branch
```

Canvas design collaboration: Open the user's Gemini desktop app, select **the available highest-capability Gemini model** and **Canvas** if offered. Do not assume a specific model label exists; display the actual selected model in the execution report. Attach project sources only using the user's authorized account and UI. The README and this file are onboarding, not a substitute for reading code.

### First files to read BEFORE drawing or editing

1. `README.md`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` if present.
2. `app/src/main/java/com/veilreader/app/ui/VeilApp.kt`, `ui/screens/ReadingNowScreen.kt`, `RebornThresholdScreen.kt`, `RebornInvestigationDesk.kt`, `GrayfogThresholdSurfaces.kt`, `ui/theme/Theme.kt`, `DesignConstitution.kt`.
3. `app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt`, `ui/screens/ReaderAppearancePolicy.kt`, `ui/reader/material/MaterialPageEngine.kt`, `domain/Models.kt`.
4. `app/src/main/java/com/veilreader/app/data/LocalLibraryRepository.kt` and durable source-of-truth navigation/locator logic.
5. `app/src/test/java/com/veilreader/app/ui/screens/RebornThresholdScreenTest.kt`, `RebornInvestigationDeskTest.kt`, `GrayfogThresholdSurfacesTest.kt`, `GrayfogRebornRenderCaptureTest.kt`, `app/src/androidTest/java/com/veilreader/app/ui/reader/material/NativePaperRealReaderInstrumentedTest.kt`.
6. `docs/strategy/MARKET_LEADERS_2026_10_17.md`, `docs/architecture/ADR-020-MARKET-LEADER-REBUILD.md`, `docs/design/GRAYFOG_REBORN_SLICE_A_EVIDENCE.md`, `quality/market-leader/benchmark-contract.json`.

## EXACT MESSAGE FOR GEMINI — COPY / ENTER AS ONE CANVAS MISSION

> You are Gemini, acting as the **independent co-lead of a world-class Android ebook reader engineering studio**. Your partner is ChatGPT GPT-6 acting as code reviewer, integration architect and evidence gatekeeper. We do not grant either partner unconditional trust: code, metrics and claims must withstand independent review.
>
> **Repository and context**: First read the COMPLETE Veil Reader repo at https://github.com/hminthakil-eng/Veil-Reader, checkout the existing `collab/gemini4-canvas-handoff-20261017` branch in a separate worktree. This is a real Kotlin 2.3 / Jetpack Compose native Android app with Readium Kotlin Toolkit 3.4, Room, local-first EPUB/PDF, page curl engine, TTS and optional reading-based game. It already has a draft Grayfog redesign and a CI-proven Forge QA debug APK. Inspect actual files and relationships; never infer methods from filenames alone. If Canvas cannot access the repository, stop and ask for repo attachment or run in Gemini CLI code workspace rather than inventing a codebase.
>
> **Vision**: Create a genuinely premium reading sanctuary inspired by the atmosphere of Lord of the Mysteries without reproducing protected artwork, names or text. Benchmark Kindle for usability, Apple Books for calm visual polish, Moon+ for advanced controls, ReadEra for fast offline library, Readest for research and parallel study, Kobo for reading/audio continuity. Deliver something visually original, not generic Material Card stacks and not copycat UI. Avoid spurious awards, inflated metrics, fake progress, invented quotes, simulated graphs, or mandatory gaming.
>
> **Parallel team roles**: Assign (1) Principal Android/Reader Architect to navigation transactions, Readium, offline persistence, RTL/LTR and paper modes; (2) Design Director + Compose UI engineer to Grayfog editorial typography, dark archive shell, restrained brass/ink/parchment, real library/continue surfaces, visual hierarchy, adaptive 320dp/390dp/tablet, Persian and English; (3) Senior Performance/Graphics engineer to physical paper curl and actual GPU page textures; (4) Accessibility, Privacy and Test lead to 200% font, TalkBack, reduced motion, highlight/locator integrity, gestures, sealed clues, UI/runtime benchmarks; and (5) Release engineer for isolated Forge QA, CI diagnostics and PR evidence.
>
> **Mission order**: Phase 0 = independently audit full existing repo (what actually works, known red gates, no assumptions). Phase 1 = rebuild HOME/Continue Reading/Library/Investigation presentation using real data and the existing ViewModel routes, behind debug-only `REBORN_THRESHOLD`; preserve sealed clues, gameVisible setting and all source ownership. Use Canvas for design collaboration and Kotlin Compose for actual app implementation (HTML/CSS preview is not implementation). Phase 2 = refine Reader UI: live appearance quick-sheet, authentic GPU curl separated from PAGED/SLIDE/SCROLL, correct RTL and safe passage return; change engine only when a reproducible defect is shown. Phase 3 = measurement-led TTS, PDF, annotation/research and opt-in fantasy-world experience.
>
> **Quality gates**: Real failing test before a bug fix when practical. Existing CI currently runs 16 scoped Robolectric Compose tests and emits three fixture PNGs. Preserve these, then build new tests. Source code and Robolectric screenshots do NOT prove phone UX. Need real APK, instrumented EPUB RTL/LTR curl video, no lost/duplicated locator after cancel/release/process kill, accessible focus and contrast, 320dp/390dp/600dp, dark/light, 100/200% font, Unicode Persian, keyboard/system bars and reduced motion. Measure p50/p95 frame time and startup only from actual Android Macrobenchmark; do not invent numbers. Release is BLOCKED until a human approves device screenshots.
>
> **Working rules**: Read before write. Make small reversible commits in your isolated branch; do not overwrite the existing user app or force push. Preserve Readium 3.4, Room, media/session ownership and native paper runtime unless evidence justifies migration. No direct copying of AGPL Readest code or commercial assets. Keep skills and docs intact. Label facts vs hypotheses. Clean room behavioral reference only. Do not upgrade toolchains without proof. If free RAM <4GB on desktop, do NOT launch Android emulator or Gradle; use GitHub Actions only and respect user apps.
>
> **Required outputs**: (A) concise source-grounded audit with 10 most consequential issues ranked P0/P1/P2 and links to actual files/lines; (B) interactive Canvas *design* proposal and side-by-side variants for HOME, Reader, Library and Investigation; (C) actual Kotlin code diffs and tests in a named working branch with commit IDs; (D) CI APK and screenshots plus explicit pass/fail log and design rationale; (E) `GEMINI_DELIVERY_REPORT.md` listing changed files, risks, unmeasured gates, assets/licensing and exact test commands. You must NOT announce completion merely for mockups, test stubs or compilation.
>
> **Independent handoff**: When your branch is ready, give me a GitHub compare/PR link and exact commit SHA. ChatGPT will inspect every changed Kotlin/resource/build file, review concurrency/privacy/RTL/Compose recomposition/gesture ownership, independently run test gating where possible and either request fixes or promote behind the feature flag. Treat reviewer rejection as part of the workflow. Start with Phase 0 audit; do not rewrite 1,337 files at once.

## Collaboration protocol (Gemini -> GPT-6)

Every delivery must identify:

- `MODEL`: actual Gemini model label verified in UI/CLI, not assumed.
- `MODE`: Canvas or CLI/agent for each task; distinguish visual prototype from working Compose.
- `BASE_COMMIT`, `BRANCH`, `HEAD_COMMIT`, `CHANGED_FILES`, `DIFF_STAT`.
- `TASK`, `BEHAVIOR_BEFORE`, `BEHAVIOR_AFTER`, `DESIGN_RATIONALE`.
- `EVIDENCE`: named test classes, exact test counts, workflow URL, Android screenshots/video filenames, measured performance only.
- `UNVERIFIED`: device classes/tests/build steps that were NOT run.
- `SECURITY`, `PRIVACY`, `LICENSE` review.
- `REVERT`: single commit/feature flag rollback instructions.

## Reviewer checklist for ChatGPT (do not pre-approve Gemini output)

- Is code buildable Kotlin Compose, not Canvas-generated React/Tailwind embedded in repo?
- Are all callbacks wired to the real publication/book/locator and existing app shell?
- Can you reach features in a test APK with real settings, not only isolated previews?
- Does empty library have a truthful and inviting CTA rather than fake populated data?
- Is real reading position durable on kill, cancellation, stale callback and RTL reverse swipe?
- Does GPU curl bend actual page ink and preserve page content on held/cancel/commit?
- Do sealed clues never leak their quote/notes before explicit reveal; re-seal on ON_STOP?
- Is user `gameVisible` preference respected, including when off?
- RTL/200% font/large surfaces and Text scaling free of clipping and unusable targets?
- Are semantic TalkBack labels, focus order and low/reduced-motion cases handled?
- Are dependencies and licenses unchanged or explicitly justified?
- Do tests run in CI on the actual head and yield artifact/trace paths?
- Is production `main` untouched and release flag still OFF pending user signoff?

## Current blockers / truthful status

- The authorized desktop was OFFLINE at preparation, and the separate Opera connector was disconnected. Consequently **Gemini Canvas was not opened, no prompt was delivered to Gemini, and no Gemini-authored code can yet be reviewed**.
- Earlier attempt to use the Gemini CLI reported unauthenticated, so do not claim Gemini cooperation until it returns a real session and output.
- Figma visual review was partially blocked by Starter tool quota; existing source audit remains Visual RED.
- Prior `Grayfog Reborn Slice A CI` success was for this repo, not proof of visual superiority or the paper engine.

**After the owner reconnects the desktop:** open Gemini app/Canvas (when available), paste the mission, grant access to the Git checkout or attach a verified archive, make Gemini produce patches/PR, then request ChatGPT's independent review. Stop and report if any required integration or model is unavailable.
