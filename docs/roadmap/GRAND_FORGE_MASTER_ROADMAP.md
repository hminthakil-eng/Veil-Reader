# VEIL READER — GRAND FORGE MASTER ROADMAP
**Version:** 2026-10-11 / 1.0
**Canonical development trunk:** [main](https://github.com/hminthakil-eng/Veil-Reader/tree/main)
**Baseline SHA (before this documentation):** `9c65997d891b2b19ccba19e5729ce82bbdff9cce`
**Branch rule:** newest **integrated and evidence-qualified** development work must live on main. A new draft branch does not become main by having a newer date. Previous main snapshots are immutable numbered `archive/NNNN` branches.
**Product acceptance:** NOT GREEN. Developer-integrated is not the same as physically tested or release ready.
**Status register:** [LIVE_STATUS.md](LIVE_STATUS.md) · **Branch/archive policy:** [BRANCH_AND_ARCHIVE_POLICY.md](BRANCH_AND_ARCHIVE_POLICY.md)
**UI research:** [2026-10-11 UX evidence](../design/GRAND_FORGE_WORLDCLASS_UI_UX_RESEARCH_2026-10-11.md) lives currently in draft PR #489; link becomes canonical only after review/merge.
**Principle:** Observe → Predict → Reuse → Integrate → Automate → Build → Verify → Learn.

## A. How to operate and measure the plan

Every task has: ID, severity, goal, evidence, dependency, owner role, implementation strategy, tests, device/UX gates, rollback, status. NEVER interpret a passing Gradle run as a PASS for tactile quality, audio, battery, accessibility or product completeness.

The lifecycle of each task is:
1. BACKLOG (requirement accepted; no implementation known)
2. INVESTIGATING (reproduce on exact SHA/device; note hypothesis vs fact)
3. IN_PROGRESS (code/PR exists, not yet verified)
4. SOURCE_VERIFIED (exact-head code tests pass, review accepted; not user acceptance)
5. INTEGRATED (landed in main; not necessarily hardware or release certified)
6. DEVICE_VERIFIED (real-device runtime evidence links, not UX judgement alone)
7. PRODUCT_ACCEPTED (independent UX/a11y/perf sign-off, with known debt)
8. RELEASED (artifact signature/version/rollout and release gates)
Other statuses: BLOCKED (name owner/unblocking event), REJECTED (explicit reason), SUPERSEDED (link successor), HOLD (scope intentionally deferred).

Status changes require date, branch and exact SHA, PR or issue, workflow run, device fixture/hash if physical, acceptance result and owner. Closed or merged PR means **INTEGRATED** only if the change is actually reachable in main; CI alone cannot progress beyond SOURCE_VERIFIED. Discovered regressions reopen tasks. A removed feature needs compatibility/deprecation notes. For READ, TTS, PDF, storage and fonts, maintain separate SOURCE / DEVICE / PRODUCT / RELEASE columns.

## B. Source of truth and latest verified evidence

At 2026-10-11, GitHub main was old Sept 20 SHA cbf27c8. It was fast-forwarded (0 commits behind) to the established Reader integration head 9c65997d on 2026-10-11. Previous HEAD was preserved at archive/0001 BEFORE the move. The integration line was 3,980 commits ahead of that historical main. No forced rewrite, merge of untested experiments or public release was performed.

**Known integrated PRs (review source and acceptance separately):**
- #451 persisted-locator retry origin; #452 Readium in-book search core; #455 reader search UI; #461 search/input fence.
- #467 Paper gesture ownership and release-only semantic commit; #474 visual GPU overlay touch-through and accessibility; #475 Paper eligibility alignment.
- #473 PDF SAF/Downloads test fixture.
- A later merge head 9c65997d only had Reader Durability Fault Injection direct-SHA workflow visible in the GitHub run-by-head lookup; predecessor feature branches have separate CI evidence. Never claim all CI runs on merged main exact SHA unless checked.

**Active branches/PRs NOT part of the integrated release baseline:** #468 physical-deformation gate, #479 OSS curl experiment, #480 TTS pause fix, #482 Kokoro bridge, #485 in-process neural PCM, #487 illustration zoom, #488 appearance sheet, #489 UX research, #466 screenshot evidence, #464 English/Persian resource parity. These are independent or stacked; do not merge by PR number or newest date.

**Open product blocker:** [#408 physical Reader acceptance](https://github.com/hminthakil-eng/Veil-Reader/issues/408), which requires Samsung/Pixel physical movement, RTL, reduced motion, 60/90/120Hz evidence. Paper visual result still described by product owner as slide-like despite emulator green. TTS physical report: Pause during loading unreliable and narrator changes; draft #480 does not prove audible device acceptance.

## C. 0 — Release discipline, risk and project OS (P0)

| ID | Outcome / exact work | Dependencies / execution | Evidence required | State |
|---|---|---|---|---|
| GOV-01 | Maintain ONE canonical `main`; never make latest PR head canonical merely because newest | Original main preserved at archive/0001; compare refs before moves | exact ancestry and SHA, CI, rollback snapshot | INTEGRATED policy baseline |
| GOV-02 | Archive stable prior trunk versions using increasing 4-digit archives; write manifest | no open PR, no active workflow dependency, no external checkout needing old branch name | full SHA + time + reason + predecessor; non-destructive branches | IN_PROGRESS |
| GOV-03 | Publish permanent roadmap, evidence ledger and single command issue | This document, LIVE_STATUS and GitHub epic issue | daily dated updates, no invented pass/reject | IN_PROGRESS |
| GOV-04 | Correct PR base filters + protection for main, required checks, reviewer policy, Code Owners | workflow audit / admin settings | branch protection read currently returned 403; no claim protection applied | BLOCKED: admin settings evidence |
| GOV-05 | Single source capability matrix; retained features vs missing/hidden/disabled | #312 #313 #316 + canonical app; do not create duplicate app implementations | exported before/after matrix with actual device capture | PLANNED |
| GOV-06 | Freeze main on critical regressions and reversible promotion of reviewed slices | Android+storage+performance+security and device gates by scope | failure budget, exact SHA, rollback procedure | POLICY |
| GOV-07 | Maintain provenance/license boundary on competitor analysis and art/assets | #428 #441 and APK comparison R&D | licenses, no proprietary source copying, clean-room notes | IN_PROGRESS |

## D. 1 — The physical reading engine (P0 blocker; Reader/Android/GPU/QA)

### RDR-01 Material paper curl: non-slide physical deformation
Problem: touch, GPU sheet acquisition and navigation may pass in emulator but appear as sliding or non-moving on real hardware.
- Reuse source in main (#467/#474/#475); characterize controlled source/next/back textures, viewport identity, geometry, actual shadow, crease, thickness, backside, curvature, resistance and touch velocity.
- Compare isolated PR #479 Apache-2.0 CurlTextureView laboratory against current #468, under matching hardware, same manually operated gestures. No unlicensed Kindle/Moon source.
- Strictly distinguish Static Paged / Slide / Curl / Scroll with no silent Paper→Slide fallback.
- Gesture states: IDLE → CLAIM → PREPARE → PRESENT → DRAG → RELEASE/CANCEL → COMMIT ONCE → SETTLE → IDLE. No Readium navigation on an uncommitted finger move. Every cancelled turn reverts exactly.
- Inputs: first turn, SVG/raster cover, chapter boundary, center tap, links, selection, 14 consecutive turns, rapid reverse, pause/resume, rotation, app background and process death.
- Evidence: exact installed APK SHA/signature, device/API/display Hz, EPUB fixture hash, 60/90/120Hz where supported, real screen recording of slow corner turn and 100-turn stress, GPU trace/fps/memory, RTL page direction, visual review. Issue #408 is acceptance owner.
- Branch strategy: maintain both candidates independently; integrate only technically and visually superior one behind safe gate; failing optics => BLOCKED.

### RDR-02 Readers and navigation
- Preserve typed semantic progress, TOC, title/subchapter, search/links/footnotes and previous position; open in <2 taps from Home is design target.
- Page/Slide/Curl/Scroll switching must be reversible, with persisted actual effective mode and truthful disabled reason; existing user-requested unavailable mode cannot be shown as running.
- Simultaneous owners: image zoom (#487), annotation selection, Reader Chrome, PDF pan, bottom sheets, TalkBack, keyboard/volume, hardware back. Build pairwise gesture contention tests; no hidden duplicate listener.
- Success: no phantom turn, zero orphan gesture owners, correct first turn, explicit chapter-boundary behavior, same committed locator after fast backgrounding.

### RDR-03 EPUB source/renderer and rescue
- Reuse Readium; handle WebView renderer death (#375), malformed HTML, missing resources, large 1,467-entry EPUB, SVG and image covers, tables, mixed script and footnotes.
- Provide accessible recovery banner with retry and last committed position, no blank page or misleading continued reading.
- Characterization tests before any renderer rearchitecture.

## E. 2 — Crash-safe progress and data trust (P0; Data/Android/Security)

| ID | Objective | Work and no-regression constraints | Final acceptance |
|---|---|---|---|
| DATA-01 | Durable semantic position | One authoritative Room journal and atomic recovery checkpoint; commit on page turn, jump, previous-location return, close; coalesce only scroll previews | abrupt kill right after committed turn; same book location; no preview persistence; issue #409/#417 |
| DATA-02 | Bookmark/highlight/note acknowledgment | Ack only after ordered Room write; preserve failed drafts; retry without duplicate or phantom success | failed disk/IO injection and process kill; Reader → Archive → Return to Passage |
| DATA-03 | Global and per-book appearance | Current app-wide appearance is GLOBAL. Define persisted layered overrides, migration, backups and reset contract before adding scope selector | no cross-book leakage, revert/undo, restore both old and new backups |
| DATA-04 | Backup/import integrity | Room transaction/order plus partial failure recovery, no content duplication, file access expiration semantics | corrupt/oversized archives, dropped permission, process loss |
| DATA-05 | Search/index fidelity | Reuse Readium semantic cursor; match large EPUB text and titles; progress unaffected by Search UI lifecycle | correct sentence locator, EN/FA mixed script, no gesture interception regression |

## F. 3 — Sanctuary UI/UX and Persian/RTL (P0/P1; UX/Accessibility/Art)

### UX-01 Reading appearance console — P0 / current #488
Current PR #488 replaces opaque full-screen dialog with partial Material3 sheet showing the REAL book behind it, compact theme controls, font scale, 4 distinct modes, Advanced controls preserved. Its exact head 4cfedd6 shows successful Android CI / Storage Instrumentation / Performance Benchmarks in initial research, but no physical visual acceptance.
- Design rule: Aa one tap, most common change one additional action; avoid 2-level technical forms, fake specimen previews and hidden expert capabilities.
- Quick: theme, font size, brightness, transition. Advanced: typography, page geometry, interactions, material/publisher behavior. Future per-book scope ONLY with real persistence.
- Acceptance: actual book visible, no locator drift through reflow, modal captures focus and releases it, 200% text, EN/FA contrast, portrait/landscape/tablet, TalkBack, reduced motion, no phantom scroll or lost final setting.

### UX-02 Grayfog identity, from Home to Archive
- Color: deep ink-blue archive shell, restrained warm brass accents; typography hierarchies and quiet minimal parchment Sanctuary. Text contrast wins over decoration.
- One authored visual story through Threshold, Archive, Reader, Castle, Ritual, Sanctum. No pasted random motifs, cluttered headings or persistent bottom navigation on immersive Reader.
- Home: Continue Reading hero → Recents → Collections → Achievements. One primary Resume CTA with accessible target.
- Archive: cover grid/list, author/series/status filtering, collections and full-text search. Handle 1k/3k large library and very long names. Make empty states beautiful but actionable.
- Book Detail: Resume/Read first, factual metadata, chapter contents, notes; never invent publisher/series/reading facts.
- Annotations: highlight/note/search/export and Back to Passage; no dead-end Archive.
- Accessibility: WCAG text contrast target >=4.5:1 for regular and 3:1 for large; Compose touch targets >=48dp. Actual colors must be computed. Respect screen reader focus, 200% font, and reduction of motion; avoid Latin letter spacing on Persian/Arabic joined scripts.
- QA: #466 120-frame emulator capture gate (not a user/device approval), #464 FA/EN resource parity, actual screenshots matching owner-provided Grayfog reference, multiple font scales.

### UX-03 Adaptive reader for all books and form factors
- Reflowable EPUB, fixed layout, PDF, comic/manga need separate capability-aware UI.
- Phone portrait, landscape, tablet/foldable, window resizing and safe insets; right/left reading direction derives from publication semantics not app language.
- Dark mode and OLED preserve images/illustrations and links; no inverted photos or disappearing footnotes.
- Font families: separate display/heading identity from sustained reading; Persian+English font research with real licensing and shaping tests; avoid any promise of bundled font without output.

## G. 4 — Audiobook-grade TTS (P0 audible; Audio/Android/Data)

### TTS-01 Reliable playback / voice stability
- Start, Pause, Resume, Stop during LOADING, speaking, chapter transition, lock/unlock, headset unplug, phone call, process restore.
- No narrator replacement unless user explicitly changes it. Clear user-visible error and fallback; never silently start network audio or a different provider.
- Distinguish visual Reader location from listening checkpoint. Playback locks existing MediaSession ownership and does not create a second progress authority.
- PR #480 is DRAFT; backend tests do not imply audible quality.
- Exit: signed build on physical Android with audible capture, 10 repeated pause-in-loading trials, background and Bluetooth; zero unwanted speech after Pause and no unsolicited voice change.

### TTS-02 Quality: local neural in-process
- #482 exploratory bridge and #485 in-process PCM stack are not product-ready. Model licensing, supply chain hash, ABI, CPU/memory/battery, on-demand model storage, audio latency, stream cancellation and pause ownership are all gates.
- Compare several consented local models by intelligibility, pronunciation (English + Persian if supported), expressive quality and memory footprint under matched passage content.
- Offline system TTS fallback with truthful labeling. Don't bundle a 300MB engine or hide a second app requirement. Do not mark neural provider available until native inference is running in-app.
- Exit: naturalness human listening review, reproducible latency/energy, clean pause, punctuation/RTL, correct active text and model provenance.

## H. 5 — PDF, search, tools and everyday reliability (P1)

| ID | Outcome | Implementation / source | Acceptance |
|---|---|---|---|
| PDF-01 | Reliable PDF reading | retain Pdfium/Readium owner, #415; zoom, fit width/page, links, thumbnail jump, scan behavior, rotation | real 10–500-page PDFs, large files, password errors/hostile files, no blank screen |
| PDF-02 | PDF annotation (when real) | coordinates and durable text/ink annotations, backup/restore; never conflate rendering embedded annotation with authoring | roundtrip annotate/export and page mapping |
| TOOL-01 | Selection actions | define/translate/copy/share/web search, privacy clear for online actions | multi-script selection, accurate quote/locator, failure and retry |
| TOOL-02 | Book search | integrated #452/#455/#461 source, advanced filters/TOC, return location | pagination and accurate deep-link; no stale search/hardware owner |
| TOOL-03 | Image zoom | #487 draft double tap and pinch/pan, match bitmap cover vs SVG behavior | zero conflict with selection/chrome/Curl, large images and accessibility alternative |
| TOOL-04 | Library quality | import duplicate detection, damaged format UX, author/title normalization, index/sort and large collections | no data loss or long freezes during 3k-book import/search |
| TOOL-05 | Offline-first | file handling, optional account/sync later, no forced sign-in | airplane mode all core functions, deterministic import and reopen |

## I. 6 — Eyad Studio signature: meaningful world, not decoration (P2)

- REALM-01 Threshold: quiet premium home, Continue Reading priority, contextual collection discovery.
- REALM-02 Archive: physical literary archive with notes/reading history/Time Capsules retrieved factually.
- REALM-03 Castle: camp → hut → house → castle; reading-derived progression, furniture/crafting/relics with no punishing streak systems. One authoritative event ledger, no accidental farming from cancelled turns.
- REALM-04 Observatory/Ritual/Sanctum: rare earned narrative environment reveals, skip and reduced-motion alternatives, user controls atmospheric effects. Separate visual world GPU lifetime from Reader lifetime.
- REALM-05 Original art and motion: licensed/source-verified art, coherent palette/materials, restrained motion grammar across routes and error states; don't imitate proprietary Lord of the Mysteries artwork or UI exactly.
- REALM-06 Accessibility and performance for every unlocked scene, offline cache, no data loss, exact factual milestones.

## J. 7 — QA, security and release hardening (P0-P2)

- QA-01 Android CI, storage instrumentation, Reader durability, benchmark, lint, dependency/SBOM, 120-frame visual evidence gate; workflow run binds to SAME SHA. Repo must not claim GREEN from an absent artifact.
- QA-02 Device matrix: Pixel + Samsung flagship + lower-end Android + tablet; 60/90/120 Hz if available; English/Persian publication crossing interface locale, TalkBack, 100%/200% text, reduced motion, dark/OLED. Make signed APK identity and video mandatory.
- QA-03 Runtime: cold startup, interrupted import, first turn, 100-turn endurance, 30m listening, background memory/battery, Bluetooth audio, process death, rotation, no silent fallback or infinite spinners.
- QA-04 Security: model/source download pinning, bounds checks for decompression, font/EPUB/PDF untrusted markup, URI permissions, backup encryption when appropriate, no partner telemetry without opt-in.
- QA-05 Release lane: maintain existing release provenance and signing, baseline/startup profiles, apk/aab reproducibility, R8 shrinker, rollback plan; **main is development integration, NOT permission to public release**. Archive an immutable previous HEAD before every major accepted promotion. Protect main with required checks when repository admin settings available.
- QA-06 Product owner compares reference photos and real recordings. Clearly separate SOURCE_VERIFIED, DEVICE_VERIFIED, PRODUCT_ACCEPTED and RELEASED. A red optical issue overrules three green emulator lanes.

## K. Acceptance order and implementation waves

| Wave | Exit conditions | Do next only when |
|---|---|---|
| W0 Truth/main/archive | previous main snapshot preserved; only main canonical; tracker updated; open branches inventoried | audit owner approves source chain; no blind archive of PR source |
| W1 Stop critical reading pain | #408 Paper phone test; #480 audible Pause fix; DATA-01 crash-safe progress | exact-head CI + device evidence, still no release |
| W2 Reader console + input | #488 visual reader sheet + #487 gestures; 200% + RTL; no navigator regressions | screenshot review and accessibility tests |
| W3 Library/tools | Search, import, Book Detail, archive notes, per-book settings model | main app is reliable and reversible |
| W4 PDF + Listening | PDF owned tools, complete listening ownership and neural model QA | proven privacy, storage, runtime |
| W5 Signature world | authored Castle/Archive/Observatory; fair gamification, custom fonts | core Reader is not obstructed |
| W6 Release | physical, performance, security, a11y, signed artifact | all required P0 gates accept, not just CI |

## L. Daily maintenance contract

At least once per day and at every critical merge/promotion:
1. Read repository default/main HEAD, open PRs, merged PRs, recent CI, relevant issues and physical evidence manifest.
2. Re-evaluate status by exact SHA; never count passing CI on a different branch or an old build. Newly merged PRs source count as integrated, NOT product accepted.
3. Update LIVE_STATUS.md with timestamp, HEAD and delta: DONE in main, IN_PROGRESS, BLOCKED, REJECTED/SUPERSEDED, NEXT. Update command-center issue with the same evidence.
4. New confirmed P0 defects get named issue + owner role, reproduction, severity, rollback and test.
5. Never auto-merge experimental/shadow PRs or delete branches. Human product/device gate holds.
6. Before major promotion, create next unused `archive/NNNN` snapshot from prior main, validate fast-forward ancestry and exact expected SHA; migrate reviewed code to main. Old noncanonical active work branches remain until dependencies and open PRs are resolved. Document why a branch was archived.
7. Report failures to update honestly; absence of CI or a screenshot is UNKNOWN, not PASS.

**References:** #408 #409 #415 #417 #428 #429 #466 #467 #468 #474 #475 #479 #480 #482 #485 #487 #488 #489.