# Veil Reader — full rebuild design
Date: 2026-10-18 (Asia/Tehran)
Status: PROPOSED DESIGN — architecture review pending; no runtime implementation in this change.
Repository: hminthakil-eng/Veil-Reader
Design branch: design/full-reader-rebuild-20261018
Design base: main @ 811f0d7d521fc11f110638a100c315f698d52fd5

## 1. Intended outcome
Build a coherent premium offline-first Android reader: fast to resume, comfortable for long reading, dependable with personal books and annotations, and distinctive through Grayfog atmosphere and an optional reading world. Retain working capability, use licensed reusable foundations, and require real evidence before claiming a competitor-quality experience.

Owner requirements already established: optional account; Persian/English and RTL; EPUB/PDF and existing manga support; four distinct reading modes; tactile paper; accessible appearance controls; stable reading position; useful library/search/notes; natural dependable listening; optional Castle/Sefirah progression. The owner recently reported that page turning on the phone became very good. Preserve that success.

Assumptions for review: Android remains the delivery platform; no compulsory account/network, no wholesale parser replacement, no immediate production promotion of experiments. This document proposes the boundaries of the rebuild, not approval of every existing draft feature or game story.

## 2. What was actually checked
- Live GitHub repository metadata: main is the default branch.
- Latest 35 open PR snapshots, including #509, #510, #511, #513, #514, #516, #530–542, #544–552 where returned.
- main ReaderScreen.kt imports and LocalLibraryRepository.kt ownership declaration at the exact base above.
- Existing local navigation source, settings.gradle.kts, roadmap and file inventory at local branch design/apple-reading-flow-20261013 @ 52c179c1. Local sources are older evidence, not proof of current remote behavior.
- Experiment source RebornThresholdScreen.kt and live Station 3D gate plan at ce4b96ed721d8342cc8b59f0c898c1edb5e1cbc1.
- Existing competitor-reference skill's acceptance rubric. These are proposed requirements, not fresh competitor runtime findings.
- No app launch, full code audit, new build, device test, audio listening session or benchmark was performed for this design.

Important discrepancy: docs/roadmap/LIVE_STATUS.md still describes an October 11 snapshot and old negative paper feedback. It must not override the owner's later positive phone report. Obtain installed applicationId/version/APK hash and map it to its source before engine changes.

## 3. Approaches considered
| Approach | Benefit | Cost / decision |
|---|---|---|
| Incremental architectural rebuild, reusing proven core | Preserves books, positions, licensed reader work and regression coverage while allowing major UX change | Recommended; integration and device qualification are explicit work |
| Entirely new Android app and parser | Maximum freedom | Duplicates hard EPUB/PDF/layout/storage problems; migration and feature-loss risk; reject as default |
| Game/3D shell as the primary app runtime | Strong visual freedom | Delays reading, complicates accessibility and lifecycle; keep optional and isolated |

The chosen proposal is a full product rebuild delivered as independently reviewable slices, with one coherent data and navigation model.

## 4. Architecture and ownership
| Boundary | Existing foundation | Contract |
|---|---|---|
| Publication/content | ReadiumEngine.kt, existing EPUB navigator and PDF adapter | Sole publication parsing/navigation owner. Renderer receives canonical locations; never invent page indices as durable IDs |
| Reading session | ReaderViewModel.kt, ReaderScreen.kt, VeilAppViewModel.kt | Exactly one active session ID; stale callbacks cannot write to a new book/session; modal/search/TTS gesture ownership is explicit |
| Durable library | LocalLibraryRepository.kt, Room entities | Room is structured truth; one ordered durable write path for progress/annotations; committed success only after write completion |
| Preferences | Existing SettingsStore/DataStore and ReaderAppearance | Explicit global default and, when implemented, persisted per-book overrides; absence means inherit, not copy defaults |
| UI shell | ui/VeilApp.kt, VeilShellPolicy.kt, theme/Theme.kt | Native Compose actions and semantics; UI derives state from repositories, never parallel book/progress stores |
| Search and notes | ReaderPublicationSearch.kt, ReaderBookSearchDialog.kt, existing notebook | Stable anchors; result jump has return-to-origin; cancellation retires cursors and stale results |
| Listening | Existing ui/reader/tts package and media service | One playback authority, stable voice identity, separate listening checkpoint, deterministic pause/cancel and truthful engine availability |
| Atmosphere | RebornThresholdScreen.kt, StationScenePolicy.kt | Optional decoration cannot own book navigation, block first interaction, or consume reader gestures |
| Reading world | GameRepository.kt / existing Castle; isolated Godot work | Optional rewards from narrowly scoped committed reading events; no book contents or annotations passed to game |

Prefer logical boundaries inside the existing app first. Extract interfaces or Gradle modules only where a real dependency or test seam requires it. Avoid an app-wide rewrite of ReaderScreen.kt in one patch.

### Navigation
Home → resume/book detail; Archive → filter/search/collection → book; Reader → appearance/contents/search/notebook/listening; World → optional progress. Back returns predictably through these routes. Reader hides persistent app navigation. Restore a small route/session description, then reacquire publication from durable state.

### Data and migration
Inventory current database, settings and backup schema before changing any field. Test forward migration with real fixture snapshots and existing annotations. Do not destructively recreate Room. Per-book settings must define inheritance, reset-to-global, backup/import and conflict handling together. Branch rollback does not guarantee binary downgrade compatibility after schema migration; use separate QA data and tested backup/restore.

## 5. Experience design
### Home / Threshold
Victorian archival composition in deep ink, restrained brass, gray fog and a sharp native foreground. One dominant Continue Reading action; real cover/title/progress; accessible Import/Open Library empty state. Recent books and collections follow. No fabricated chapter label, timetable, completion metric or lore. World entry follows the user's visibility preference.

### Archive and Sefirah
Retrieval-first list/grid remains available. Add an optional fog-and-stars view: one stable book ID maps to one star; tap reveals title and opens/resumes the real book. Decorative constellation grouping is deterministic and does not alter collections or reading truth. Exact grouping threshold is a later configurable design decision, not a hidden completion rule. Large libraries use bounded visible geometry and an accessible searchable list equivalent. Reduced motion freezes pulses/fog.

### Sanctuary / Reader
Real reading canvas with appropriate parchment or clean dark/light themes, readable typography and preserved illustrations. Center tap reveals concise controls; appearance preview uses the actual publication; advanced controls remain discoverable. Large text, Persian joining, mixed-direction strings, long chapter titles and narrow screens are core scenarios.

Modes remain independently dispatched: PAGED instant; PAPER curl; SLIDE translation; SCROLL continuous. A failed paper preview must not silently become slide. Report effective capability and offer an explicit user choice when unavailable. Paper acceptance includes drag tracking, believable front/back/shadow, release once, reversal/cancel, first/last page and chapter boundaries. Preserve the owner's liked version until a candidate beats it under matched fixtures.

### Notes, search and listening
Notes expose direct return to passage, editing and reliable export. Search has loading/empty/error/cancel states and stable return position. Listening exposes start/pause/resume/seek/speed/voice and sleep timer; do not promise expressive multi-character narration from a basic TTS engine. Neural models are optional downloads only after license, hash, inference and audible-quality verification.

### World / game
Keep existing Castle and standalone island-bookshop work distinct until a bridge design is accepted. Reading remains useful without the game. Rewards are optional, idempotent and cannot be farmed by duplicate lifecycle events. Do not transmit private titles, URIs, text or notes to game/cloud. Gameplay quality and story are separate acceptance tracks.

## 6. Integration matrix — candidates, not merge instructions
| Candidate | Observed head | Decision / first check |
|---|---|---|
| main | 811f0d7d521fc11f110638a100c315f698d52fd5 | Canonical integration base; re-read live ref before execution |
| #509 native paper | e37869bb68e0497b5cbb9ca9f9b56c4e53286020 | Compare installed liked build and ancestry first; verify exact-head dependency and Android evidence |
| #510 import/progress race | ff622f4fd9e914634aec2f65c12536485211baed | Review relevance against candidate; reproduce immediate import/save durability |
| #514 native reading tools | 169eb7049c7e0fdf72b3b298535f9f2c9ff34d49 | Review UI/session/search behavior and test receipts; preserve existing fixes |
| #511 contrast / #513 SAF / #516 CI | independent stacked PRs | Check which fixes already exist by ancestry/diff; no blanket cherry-pick |
| #541 Word Wise / notes export | 4a0e4076e426efef891a8a2796e623092b115d0a | Evaluate parity and error semantics after core stability; not a reason to replace Readium |
| #542 Reborn shell | 84cb9ae9183e2534be4cbe9787a9004539b7edc2 | Useful presentation wiring; PR self-review says visual RED; redesign before promotion |
| #545 station composition | 47cfef067e900bda99f758f3ff86ec36b97a30d7 | Experimental composition and fallback policy, not accepted finished art |
| #546 Station 3D lab | ce4b96ed721d8342cc8b59f0c898c1edb5e1cbc1 | Debug-only experiment; keep isolated until hardware, lifecycle, art and dependency approval |
| #539 / #540 game | separate mobile/day-two branches | Separate game milestones; not ready-made Android reader integration |
| #547–552 research | independent main-based drafts | Curated reference pool; verify individual licenses and applicability; no automatic dependency intake |

Do not merge by date or PR number. For each candidate record ancestry, overlapping files, unique behavior, license, test coverage, remaining blockers and rollback. Inspect diffs before implementation. Draft status and reported CI are not fresh verification.

## 7. Delivery sequence and exit conditions
### A — baseline qualification (first implementation project)
Identify the owner's accepted APK; capture immutable source/build identity. Reconcile installed experience with main and candidate branches. Produce one retained-capability inventory and choose a single isolated QA integration candidate based on ancestry and evidence. Retain a reproducible reference build and fixture books.
Exit: import → open → four modes → settings → notes/search → background/reopen reproduced with named results. No mandatory source replacement before this.

### B — dependable daily reader
Resolve reproduced session, import, annotation, close/pause and locator defects first. Integrate reviewed native tools and mode controls; prove no paper regression.
Exit: exact-head Android/JVM, storage and lifecycle checks plus on-device matched paper comparison and persistence evidence.

### C — premium unified shell
Rebuild Home/Archive/detail/appearance surfaces with consistent typography, native semantics and authored composition. Deliver an actual Android slice before expanding art.
Exit: screenshot/video comparisons on narrow/normal/wide screens, Persian/English, light/dark, large text, TalkBack and reduced motion; user visual review.

### D — useful library and listening
Improve measured large-library retrieval, notes/search and stable TTS. Define per-book preferences with tested migration. Neural engine quality requires actual audible trials.
Exit: matched fixtures, cancellable search/indexing, export failure handling, voice continuity and background playback checks.

### E — atmosphere and world
Prototype fog/stars and station as optional layers; evaluate GPU vs Compose from measured cost. Integrate only accepted art and finite performance budgets. Design the game reward bridge separately.
Exit: native fallback, interruption-free reading, memory/lifecycle/thermal evidence and game privacy/idempotency checks.

Each is its own spec → implementation plan → code review → verification cycle. This document is the umbrella design; it is not permission to ship all draft subsystems at once.

## 8. Acceptance and evidence contract
| Area | Required scenario / result |
|---|---|
| Data | Import then immediate progress save; background and process death; reopen restores latest committed locator; annotations survive reflow/restore |
| Modes | All four mode selections persist and dispatch correctly; no silent fallback; rapid repeated selection and RTL |
| Paper | Matched slow/fast/reversed drags, cancel/release, both directions, chapter boundaries; no wrong ink, white frame or duplicate turn |
| Input | Selection, search, dialogs and PDF zoom/pan do not accidentally turn a page |
| Appearance | Actual-book preview; locator remains semantically stable across font/theme/orientation changes |
| Library | 0/1/1000-book fixtures; long titles, import cancel, invalid file, removed permission; find/resume after restart |
| Search/notes | Correct book/session ownership; canceled cursor cleanup; return-to-origin; failed export never reports success |
| Audio | Pause during loading and chapter transition, interruption, background/reopen, voice remains chosen, no unwanted speech |
| Accessibility | >=48dp targets; readable labels/focus; 200% font at 320dp; contrast calculations; Persian mixed-script shaping; reduced motion |
| Atmosphere | Native controls available before optional renderer; static/error fallback; hidden/background scene stops work; release isolation reviewed |
| Performance | Measure cold open, first turn, steady frames, memory and thermal behavior on named devices with identical fixtures; establish baseline before budgets |
| Privacy | Offline core works; no content sent to cloud without explicit feature consent; world bridge uses minimal events |
| Release | Exact SHA, successful required checks, APK hash/signature/version, upgrade path, regression receipt and unresolved risks |

For every receipt store source SHA, build flags/applicationId, tool/device/OS/refresh rate, fixture hash, reproduction sequence and artifact link. SOURCE_VERIFIED, DEVICE_VERIFIED, PRODUCT_ACCEPTED and RELEASED are separate states. User feedback is valuable qualitative evidence; it is not a fabricated benchmark.

## 9. Skill and tool routing
Use relevant capabilities at the point they improve the work:
- Superpowers: architectural design, written planning, reversible worktree, meaningful regression-first implementation, review and verification.
- Reader APK Intelligence: provenance-labelled competitor references and four-mode acceptance. Runtime claims require actual runtime evidence.
- Existing repo Apple/native design adapters: retain Android conventions and Grayfog identity; inspect source/license before executing imported tools.
- Product-design/image generation: authored visual exploration and assets only when the visual target is being developed; precise UI layout remains native code.
- Research/Context7: current official APIs and licensed reuse when a concrete dependency decision arises.
- Novel/manga/character skills: original game story and character assets when that project is reached, not in reader persistence.
- Gemini collaboration: only claim a review after an actual connected exchange; no session or contribution is assumed.
- Device/desktop tools: verify access first; no claim of hardware QA from a source-only session.

“All skills” means comprehensive relevant coverage, not indiscriminate plugin execution, dependency upgrades or unreviewed installations.

## 10. Review decision
Recommended: approve the incremental architectural rebuild and start with A, baseline qualification, followed by a written executable implementation plan. Native reading and durable data are protected; the visual world is optional and separately measured.

Current deliverable is this source-grounded design only. No product code changed, app rebuilt, APK generated, CI rerun, PR merged or public release made in this design change.
