# Veil Reader — Executable Feature Inventory

Date: 2026-10-05  
Program: Reforge 100 / R0.02  
Owner: Veil Reader Grand Forge + Arena  
Audited branch: `grand-forge/arena-app-hardening-v1`  
Audited executable head: `a42988e0b5984c5ded76bb08dddf589f798ae7c6`

Current automated gates at this head:
- Android CI — GREEN
- Storage Instrumentation — GREEN
- Performance Benchmarks — GREEN

These gates prove the checked automated scope. They do **not** convert visually/tactually unverified features into product-green.

## Status vocabulary

- **WORKS-AUTO** — implementation is wired and current automated evidence covers the core path.
- **PARTIAL** — meaningful implementation exists, but the user-facing capability is incomplete, weak, or missing important behavior.
- **DEVICE-GATE** — source/automation exists, but real physical interaction/rendering is essential and not proven.
- **USER-BROKEN / REVERIFY** — prior user/runtime evidence says the capability did not work acceptably; current code has changed, so it requires fresh physical proof before any GREEN claim.
- **DISABLED-POLICY** — intentionally not enabled in normal product behavior.
- **DEBUG-ONLY** — review/development capability, not a release feature.
- **PLANNED** — not implemented as a usable product feature.
- **DUPLICATION-RISK** — more than one implementation/model exists or responsibilities overlap enough to require convergence.

A feature is not product-GREEN until it also satisfies the Reforge Definition of Done.

---

# A. Core shell / navigation

| Capability | Status | Current evidence / gap | Reforge action |
|---|---|---|---|
| App shell | WORKS-AUTO | Compose shell routes all main surfaces | Decompose `VeilApp.kt`; retain behavior |
| Bottom navigation | WORKS-AUTO | authored icons, 56dp min targets | Optical Persian/200% pass |
| Navigation rail | WORKS-AUTO | adaptive rail exists | tablet/fold physical proof |
| Edge-to-edge | WORKS-AUTO | enabled in MainActivity | per-realm system-bar audit |
| Route state restore | WORKS-AUTO | SavedStateHandle route model | predictive-back/deep-link matrix |
| Settings overlay/destination | WORKS-AUTO | routed from shell | migrate only if Navigation3 pilot proves value |
| Archive overlay/destination | WORKS-AUTO | routed from shell | predictive-back and state restore |
| Castle chambers | WORKS-AUTO | route-owned destinations exist | spatial UX refinement |
| Predictive Back | PARTIAL | back ownership exists in areas, no full product contract | R1.13 |
| Deep links/import-open | PARTIAL | ACTION_VIEW EPUB/PDF accepted | interrupted open/process-death matrix |

# B. Threshold / Home

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Grayfog authored entrance | WORKS-AUTO | production composable + native captures | reduce recurring visual mass |
| Continue Reading | WORKS-AUTO | current hero resumes book | make first returning-user action |
| Recent books | WORKS-AUTO | source + captures | improve scan density |
| Reading pulse/whispers | WORKS-AUTO | derived from real state | prevent content displacement |
| First-library empty state | WORKS-AUTO | explicit empty behavior | physical/200% polish |
| Threshold responsive policy | WORKS-AUTO | window + font-scale policies | extreme-window optical tuning |
| One-hand return speed | PARTIAL | primary action can sit too low | R3.01–R3.05 |
| Cinematic entrance policy | PARTIAL | atmosphere repeats too strongly | reserve full treatment for meaningful states |

# C. Library / retrieval

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Gallery mode | WORKS-AUTO | explicit mode | reduce oversized record height |
| Shelves mode | WORKS-AUTO | explicit mode | stronger shared shelf datum |
| Index mode | WORKS-AUTO | explicit mode | keep as density benchmark |
| Local search | WORKS-AUTO | normalized relevance search | large-library benchmarks |
| Persian/Arabic normalization | WORKS-AUTO | ya/kaf/marks/digits normalized | mixed-script regression |
| Reading-state filter | WORKS-AUTO | real filter | reduce visual mass |
| Collections | WORKS-AUTO | filter/edit | disclosure/density redesign |
| Series | WORKS-AUTO | filter/edit/sort | disclosure/density redesign |
| Sort | WORKS-AUTO | multiple sorts | compact rail |
| Favorites/status | WORKS-AUTO | durable book fields | visual hierarchy |
| Metadata edit | WORKS-AUTO | title/author/series/collections | invalid/long data polish |
| Duplicate fingerprint detection | WORKS-AUTO | SHA-256 path | duplicate-resolution UX |
| Search/filter restoration | PARTIAL | saveable state exists; full process/IME contract incomplete | R4.10 |
| First-book viewport | PARTIAL | current controls delay content | R4.01 P0 |
| Large-library scale | PARTIAL | architecture is lazy; 1k/10k proof absent | R4.16 |
| Bulk management | PLANNED | no mature separate bulk mode | R4.13 |
| Missing/corrupt volume recovery | PARTIAL | errors exist; relink/inspect/remove flow incomplete | R4.15 |
| Automatic device-wide document discovery | PLANNED | import is explicit | evaluate later, not P0 |
| OPDS/catalog browsing | PLANNED | not a normal product path | after local core |

# D. Import / formats / backup

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| EPUB import | WORKS-AUTO | Readium + app-private copy | physical file-source matrix |
| PDF import | WORKS-AUTO | Readium/Pdfium + app-private copy | physical file-source matrix |
| CBZ local manga | WORKS-AUTO | dedicated CBZ path + backup support | manga device proof |
| MOBI/AZW | PLANNED | unsupported | parser/license research |
| FB2 | PLANNED | unsupported | parser/license research |
| DjVu | PLANNED | unsupported | parser/license research |
| TXT/HTML/Markdown ingestion | PLANNED | no canonical ingestion | normalized Tier C design |
| Local backup | WORKS-AUTO | schema-5 archive incl books/annotations/manga | UX polish |
| Restore | WORKS-AUTO | staged/validated/rollback-oriented restore | restore preview |
| Backup migration schemas 1–5 | WORKS-AUTO | explicit compatibility | preserve tests |
| Backup security/path traversal guards | WORKS-AUTO | bounded extraction/path validation | fuzz/adversarial tests |
| Cloud backup | PLANNED | no account/server path | optional sync program |
| Relink missing publication | PLANNED | no complete user flow | R5.08 |

# E. EPUB Reader core

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Readium EPUB navigation | WORKS-AUTO | canonical engine | preserve |
| Paged static mode | WORKS-AUTO | dedicated owner | physical proof |
| Continuous Scroll | WORKS-AUTO | renderer-owned | physical proof |
| Slide | DEVICE-GATE | dedicated listener/state | tactile/device acceptance |
| Paper Curl | USER-BROKEN / REVERIFY | user previously reported no real curl; canonical GPU engine has since changed and automated gates pass | highest physical acceptance priority |
| Reader input arbitration | WORKS-AUTO | explicit arbiter/tests | keep one-owner contract |
| Reader mode handoff | WORKS-AUTO | explicit settlement/cancel | device interruption matrix |
| Progress persistence | WORKS-AUTO | ordered writes/leases/flush | preserve |
| Background durability | WORKS-AUTO | lifecycle durability path | physical/process-death revalidation |
| Durable close | WORKS-AUTO | close barrier | preserve |
| Startup locator handshake | WORKS-AUTO | session/open gates | preserve |
| Previous location return | WORKS-AUTO | transaction model | user-flow verification |
| Boundary feedback | WORKS-AUTO | sensory/reader path | device haptic tuning |
| Paper GPU context recreation | WORKS-AUTO source | repeated failure reporting hardened | DEVICE-GATE |
| Paper material presets | DEBUG-ONLY / DEVICE-GATE | Glossy/Matte/Parchment/Papyrus/Manuscript review modes | select product policy after device review |
| Paper 60/90/120Hz feel | DEVICE-GATE | benchmark infra exists | physical matrix |
| Paper long-session comfort | DEVICE-GATE | cannot be established from CI | reading-session acceptance |

# F. Reader appearance / power controls

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Paper/Sepia/Dusk/OLED themes | WORKS-AUTO | persisted appearance | optical device pass |
| Font scale | WORKS-AUTO | persisted | mixed-content testing |
| Line height | WORKS-AUTO | persisted | renderer proof |
| Margins | WORKS-AUTO | persisted | renderer proof |
| Publisher styles | WORKS-AUTO | persisted | edge-case publications |
| Font family | WORKS-AUTO | bundled options | user font import missing |
| Font weight | WORKS-AUTO | persisted | font-specific validation |
| Alignment | WORKS-AUTO | persisted | script-safe validation |
| Columns | WORKS-AUTO | capability gated | tablet/fixed-layout proof |
| Hyphenation | WORKS-AUTO | preference exists | language behavior proof |
| Ligatures | WORKS-AUTO | preference exists | script behavior proof |
| Text normalization | WORKS-AUTO | preference exists | publication regressions |
| Paragraph spacing/indent | WORKS-AUTO | persisted | renderer proof |
| Letter/word spacing | WORKS-AUTO | persisted | accessibility/script proof |
| Type scale | WORKS-AUTO | persisted | UI clarity |
| Dark image treatment | WORKS-AUTO | explicit setting | visual proof |
| Paper patina | WORKS-AUTO | explicit setting | reduce fatigue |
| Screen brightness | WORKS-AUTO | system/custom ownership | session timeout/auto-resume missing |
| Live appearance preview | WORKS-AUTO source | settlement logic exists | tactile renderer verification |
| User font import | PLANNED | no file import/substitution | R6.14 |
| Auto-scroll | PLANNED | absent | R6.16 |
| E-Ink mode | PLANNED | absent | R6.15 |
| Pinch typography | PLANNED | absent | R6.17 |
| Edge brightness/font gesture | PLANNED | absent | optional R6.18 |
| Chapter ETA | PARTIAL | reading pace domain exists, no full chapter UX | R6.19 |
| Appearance-dependent chapter page estimate | PLANNED | absent | R6.20 |

# G. Reader annotations / Notebook / knowledge

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Native text selection | WORKS-AUTO source | Android/Readium selection toolbar extension | physical handles/device proof |
| Highlight | WORKS-AUTO | durable model/action | exact return regression |
| Note on highlight | WORKS-AUTO | edit/remove semantics | process-death draft improvement |
| Bookmark | WORKS-AUTO | durable locator | exact return |
| Reader Notebook | WORKS-AUTO | Notes/Bookmarks/Search tabs | hierarchy polish |
| Full-book EPUB search | WORKS-AUTO | Readium searchable service; cancel/limit logic | result performance/keyboard |
| Hidden Archive | WORKS-AUTO | Notes/Highlights/Bookmarks/Echoes/Capsules | taxonomy simplification |
| Echoes | WORKS-AUTO source | derived memory model | product-value/clarity audit |
| Time Capsules | PARTIAL | feature exists; historical truth semantics still evolving | stable sealed snapshot contract |
| Passage revisit memory | WORKS-AUTO | passage visits present | verify exact semantics |
| Dictionary lookup | PARTIAL | only Android ACTION_PROCESS_TEXT routing | layered lookup sheet |
| Translation | PLANNED | no owned provider/path | optional provider |
| Wikipedia/web lookup | PLANNED | no owned path | optional layer |
| X-Ray-like local entity layer | PLANNED | absent | late-stage research |
| Annotation export | PLANNED | backup contains data but no user-facing Markdown/HTML/JSON export | R7.13 |
| Global full-text note/highlight index | PARTIAL | Archive local filtering; no dedicated FTS index | Room FTS evaluation |
| Annotation color/style system | PARTIAL | highlight model exists; mature accessible style taxonomy not proven | R7.03 |

# H. PDF workspace

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| PDF open/render | WORKS-AUTO | Pdfium adapter | physical document matrix |
| Paginated PDF | WORKS-AUTO | explicit PDF preference | device proof |
| Continuous PDF | WORKS-AUTO | explicit PDF preference | device proof |
| Zoom | WORKS-AUTO source | direct PDFView ownership/probe | tactile proof |
| Fit width/reset | WORKS-AUTO source | controls exist | document matrix |
| Legacy locator migration | WORKS-AUTO | migration logic/tests | preserve |
| Embedded annotation rendering | PARTIAL | capability audit says rendering available | device proof |
| Annotation enumeration | PARTIAL | not complete product surface | R8 |
| Annotation authoring/save | PLANNED/PILOT | no production-grade path | AndroidX PDF pilot |
| PDF forms | PLANNED/PILOT | absent | AndroidX PDF pilot |
| PDF OCR | PLANNED/PILOT | absent | AndroidX PDF pilot |
| Two-page tablet/fold | PLANNED/PILOT | absent | AndroidX PDF/adaptive pilot |
| PDF TTS | PLANNED | TTS session rejects non-EPUB | only after extraction/OCR reliability |
| PDF engine modernization | PILOT-ONLY | AndroidX PDF beta candidate | no default switch until parity |

# I. TTS / assistive reading

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| EPUB TTS engine | WORKS-AUTO source | Readium content + Android backend | physical audio proof |
| Play/Pause/Stop | WORKS-AUTO source | user controls present | device/audio-focus proof |
| Speed | WORKS-AUTO source | setting/control | voice matrix |
| Pitch | WORKS-AUTO source | setting/control | voice matrix |
| Offline voice failure handling | WORKS-AUTO source | explicit problems/messages | device proof |
| Next/previous section controls | PARTIAL | not a mature full control surface | R9.01 |
| Read-along highlighting | PLANNED | absent | R9.03 |
| Background playback | PARTIAL/UNPROVEN | no MediaSessionService ownership | Media3 pilot |
| Media notification | PLANNED | absent | Media3 |
| Sleep timer | PLANNED | absent | R9.06 |
| Audio focus policy | PARTIAL | backend exists; full media contract not proven | R9.06 |
| PDF TTS | PLANNED | unsupported | OCR/extraction prerequisite |

# J. Accessibility / input

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| 48dp control discipline | WORKS-AUTO source | widespread min targets | full audit |
| High contrast | WORKS-AUTO | setting/design paths | physical contrast review |
| Reduced motion | WORKS-AUTO source | Reader/design paths exist | full cross-screen completeness |
| Persian/RTL | WORKS-AUTO architecture | resources/script policies/tests | optical/device pass |
| Mixed-script typography | WORKS-AUTO source | withVeilContentScript added widely | more surfaces |
| Focus Guide | WORKS-AUTO source | reader overlay/settings | device a11y proof |
| Tap-grid customization | WORKS-AUTO source | persisted 3x3 model | device conflict proof |
| Volume-key mapping | WORKS-AUTO source | activity-owned dispatcher | TalkBack/system volume arbitration proof |
| Keyboard/mouse navigation | PARTIAL | directional infrastructure exists | full surface matrix |
| TalkBack full-app verification | PARTIAL | semantics + tests, no complete physical audit | R9.10 |
| Switch Access | PARTIAL | targets/semantics foundation | explicit matrix |
| 200% text | PARTIAL | native renders survive, composition is too heavy in places | R2/R3/R4 |
| Color-independent state | PARTIAL | high contrast exists; full semantic audit pending | R9.13 |

# K. Sync / data ecosystem

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Room local source of truth | WORKS-AUTO | core library durable | preserve |
| DataStore app settings | WORKS-AUTO | current settings | preserve |
| Local export/restore | WORKS-AUTO | schema 5 | improve UX |
| Account | PLANNED | absent | optional only |
| Cross-device progress sync | PLANNED | absent | R10 |
| Highlight/note sync | PLANNED | absent | R10 |
| Settings sync | PLANNED | absent | R10 |
| Conflict resolution | PLANNED | absent | per-entity rules |
| Tombstones | PLANNED | absent | sync requirement |
| Durable background queue | PLANNED | no WorkManager sync system | WorkManager 2.12 candidate |
| Publication-file cloud storage | PLANNED / NOT DEFAULT | absent | separate optional scope |

# L. World / gamification

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| XP/levels | WORKS-AUTO source | engine/repository | durability parity audit |
| Daily goal | WORKS-AUTO | setting + stats | UX polish |
| Path system | WORKS-AUTO source | screens/mastery | product clarity |
| Advancement ritual | WORKS-AUTO source | ceremony exists | short/skippable/device proof |
| Castle | WORKS-AUTO source | map/floors/chambers | spatial art pass |
| Castle mutation | WORKS-AUTO source | real-state derived | persistence semantics |
| Observatory | WORKS-AUTO source | atlas/relationships | dense map device proof |
| Profile/dossier | WORKS-AUTO source | reading/game stats | IA polish |
| Sanctum/Treasury | WORKS-AUTO source | earned records/routes | rarity through restraint |
| Reading cycles | WORKS-AUTO | durable records | preserve |
| Reading milestones | WORKS-AUTO | durable records | preserve |
| Reading pace | WORKS-AUTO | pace store/profile | surface useful insights |
| Reading calendar/year view | PLANNED | no mature surface | R11.08 |
| Durable game writes parity | PARTIAL | recent critical events hardened; repository still SharedPreferences-based | migrate philosophy |
| World readability/accessibility | PARTIAL | semantics foundation | nonvisual map contract |

# M. Manga

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| Local Manga hub | WORKS-AUTO source | local/offline-first | visual/device proof |
| CBZ ingest | WORKS-AUTO | dedicated ingestion | file matrix |
| Chapter matching/catalog | WORKS-AUTO source | canonical models | preserve |
| Reader screen | WORKS-AUTO source | integrated Compose surface | device proof |
| Image zoom/subsampling | WORKS-AUTO source | Coil/ZoomImage paths | huge-image memory matrix |
| Manga progress | WORKS-AUTO source | stored/restored | process-death proof |
| Local backup/restore | WORKS-AUTO | schema 5 integrates CBZ chapters | preserve |
| Source health/replacement models | WORKS-AUTO source | framework exists | product UI/value audit |
| Live source providers | DISABLED-POLICY | SourceRegistry normal path is intentionally local/offline; network adapters are not a release promise | keep off until explicit scope |
| Browser challenge handling | PARTIAL/FRAMEWORK | coordinator exists, no broad live-source product contract | hold |
| Manga production parity | PARTIAL | source architecture is extensive; physical UX and live-content path not release-proven | secondary to text Reader |

# N. Performance / release

| Capability | Status | Evidence / gap | Action |
|---|---|---|---|
| R8 minification | WORKS-AUTO | release config | package proof per RC |
| Resource shrink | WORKS-AUTO | release config | package proof |
| Baseline Profile plugin | WORKS-AUTO infra | plugin/profile installer present | real critical-journey profile proof |
| Macrobenchmark module | WORKS-AUTO infra | performance workflow green | physical representative hardware |
| Jank monitoring | WORKS-AUTO source | ReaderJankMonitor | dashboards/budgets |
| Storage instrumentation | WORKS-AUTO | current head green | keep |
| Android CI | WORKS-AUTO | current head green | keep |
| Performance workflow | WORKS-AUTO | current head green | absolute + relative evidence |
| 60/90/120Hz physical matrix | PLANNED/DEVICE-GATE | emulator cannot prove | R12.17 |
| OLED/LCD optical matrix | PLANNED/DEVICE-GATE | not CI-provable | physical QA |
| Battery audit | PARTIAL | no release evidence bundle | R12.13 |
| Leak/StrictMode gate | PARTIAL | some prior targeted fixes; no universal debug contract | R0.09/R12.12 |
| Android 17 compatibility | WORKS-AUTO / DEVICE-GATE | compile/target API 37 matches the final Android 17 SDK; current automation is green, while QPR beta/device behavior still needs an isolated compatibility matrix | keep API 37 production baseline; test QPR betas separately |

---

# Immediate red queue

The following items are the highest-value “exists but not finished / cannot be trusted yet” queue:

1. **Paper Curl — USER-BROKEN / REVERIFY**
   - previous user builds did not deliver real curl;
   - source has been substantially replaced/hardened;
   - automated green is not enough;
   - needs current physical-device evidence before any product claim.

2. **Library first viewport / retrieval density — PARTIAL**
   - functionality is rich but composition makes retrieval slower and uglier than it should be.

3. **Threshold returning-user speed — PARTIAL**
   - strong art, weak recurring-task prioritization.

4. **PDF annotations — PARTIAL**
   - reader is real; professional annotation authoring/save is not.

5. **Dictionary / translation / knowledge — PARTIAL**
   - current lookup is mostly delegation to installed PROCESS_TEXT handlers.

6. **TTS product completeness — PARTIAL**
   - core controls exist; background/media/sleep/read-along/PDF are incomplete.

7. **Sync — PLANNED**
   - major commercial ecosystem hole.

8. **Format breadth — PLANNED**
   - native general reader remains EPUB/PDF.

9. **Game durability parity — PARTIAL**
   - stronger than before but still architecturally weaker than canonical reading persistence.

10. **Accessibility physical acceptance — PARTIAL**
    - strong source/test architecture; complete TalkBack/Switch Access/device proof is missing.

11. **Castle spatial finish — PARTIAL**
    - functional world model exists; visual output still risks reading as panels/dashboard.

12. **Manga product completion — PARTIAL**
    - architecture is extensive; offline/local product and physical UX need full acceptance; live sources intentionally not promised.

---

# Duplication / convergence risks

These are not automatically bugs, but they require ownership clarity:

- Two `BookArtifact.kt` families exist under `ui/books` and `ui/screens`.
- ArchiveScreen and ReaderNotebook intentionally overlap notes/highlights/bookmarks but must retain distinct ownership: global archive vs opened-book notebook.
- Reader appearance exists through Settings and in-reader Appearance; global defaults vs per-session/per-book ownership must remain explicit.
- PDF zoom/layout and EPUB appearance must never share turn-effect ownership.
- Manga has a deep source/reader stack; new provider work must reuse it rather than add another source framework.

---

# R0.02 verdict

**Feature inventory status: COMPLETE for source-level program planning.**

There is no evidence at the audited head for a new source-level P0 data-loss blocker in the reviewed paths. That does **not** mean the app is ready.

The dominant problem is now quantified:

- many core capabilities are **WORKS-AUTO** but visually/physically unfinished;
- a substantial set are **PARTIAL**;
- important commercial capabilities are **PLANNED**;
- Paper remains **USER-BROKEN / REVERIFY** until fresh current-head device proof;
- no feature may be promoted to product-GREEN purely from current CI.

Next canonical task: **R0.03 — install the universal GREEN contract into repository governance and PR review so this distinction cannot be lost again.**


## R0.04 correction — current platform truth

Fresh official verification on 2026-10-05 confirms Android 17 was released as a final platform on 2026-06-16. The earlier planning note that treated API 37 as preview-era was stale and is superseded.

Decision:
- keep `compileSdk = 37`;
- keep `targetSdk = 37`;
- do not lower the production target to API 36 merely because Google Play's minimum submission requirement is API 36;
- use a separate compatibility lane for Android 17 QPR beta/minor-SDK images and behavior;
- pay particular attention to Android 17 target-gated large-screen adaptivity, memory behavior, background audio restrictions, and native dynamic-code-loading requirements.
