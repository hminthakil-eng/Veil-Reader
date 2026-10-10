# Grand Forge live master matrix

Evidence baseline: b8ec35a1; continuation on
grand-forge/p0-kindle-reader-quality-20261008. Detailed source evidence, task
fields, file-level execution graph and rollback are in
[the execution ledger](GRAND_FORGE_EXECUTION_2026-10-08.md) and its linked
archaeology. This is a scoped source audit, not a completed whole-app/device audit.
Competitor columns describe the user-supplied behavioral benchmark, not APK
measurements performed in this session. No benchmark APK or attached research
files were present in this workspace. No proprietary implementation was copied.

Paths below are relative to app/src/main/java/com/veilreader/app unless noted.

| Capability | Kindle | Moon+ | Veil | Best Reference | Veil Gap | Root Cause | Target | Priority | Files | Tests | Status |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Progress durability | Continuity benchmark | Resume benchmark | Atomic checkpoint + ordered Room | Existing Veil journal | Failure-path/device acceptance | Physical evidence missing | Accepted state survives abrupt kill | P0 | data/LocalLibraryRepository.kt, ui/reader/ReaderViewModel.kt | ReaderCloseDurabilityTest, RoomRuntimeRepositoryInstrumentedTest, fault injection | YELLOW exact06a94b57 CI |
| Stable publication identity | Ecosystem identity | Import depth | Content fingerprint already exists | Existing Veil import commit | Rename/relink/restore matrix incomplete | End-to-end identity evidence missing | Preserve user data across source changes | P0 | data/LocalLibraryRepository.kt | RoomRuntimeRepositoryInstrumentedTest | YELLOW |
| Precise navigation targets | Precise passage navigation | Search/bookmark controls | Patched fine-anchor jump admission | Existing Readium navigation | Physical search/highlight target acceptance | Coarse position equality swallowed precise targets | Admit paragraph/viewport jumps; retain tolerant settlement | P0 | ui/reader/ReaderNavigationTransaction.kt | Three new tests, pending CI | YELLOW3539c9c9 |
| Navigation transactions | Reading continuity | Search/navigation depth | Existing transaction/session gates | Existing Veil ownership model | Complete exploration vs anchor audit | Mixed navigation semantics need characterization | Exploration preserves reading anchor | P0 | ui/reader/ReaderNavigationTransaction.kt, ui/screens/ReaderScreen.kt | ReaderNavigationTransactionTest | YELLOW |
| Input ownership | Predictable interaction | Gesture flexibility | Single arbiter; static lifetime fixed | Existing Veil arbiter | Stale selection destroy releases new owner | Unconditional false callback | Exact-owner release | P0 | ui/screens/ReaderSelectionActionMode.kt | ReaderSelectionActionModeTest | YELLOW automated pass; device pending |
| Paper availability | Physical reading benchmark | Transition choice | GPU exists, release disabled | Existing canonical engine | Disabled-mode remap unexplained | Failure notice requires presented Paper | Explicit reason, static mode truthful | P0 | ui/screens/ReaderAppearancePolicy.kt, ReaderScreen.kt | ReaderTransitionAvailabilityTest | YELLOW automated pass; device pending |
| Paper performance | Smooth turns | Multiple transitions | Snapshot/upload/presentation fences | Existing Veil traces | First turn/100/500 turns/high-refresh proof | No physical hardware | Stable frame/memory budgets on device | P0 | ui/reader/material/, benchmark/ | PaperWarmupBenchmark, ReaderFrameBenchmark | BLOCKED physical |
| Fixed-layout transition | Layout-aware behavior | Format depth | Static paging policy | Existing Veil capability policy | No physical leaf/spread engine | Whole-view capture is not one leaf | Explicit unavailability until correct engine | P0 | ui/screens/ReaderAppearancePolicy.kt | ReaderTransitionAvailabilityTest | HOLD engine |
| Adaptive columns | Layout maturity | Column customization | New viewport/font-size policy | Existing Readium renderer | Physical output and complete text-measure policy pending | Mapper previously delegated AUTO/TWO without Veil font scale | Single column when text would be cramped; preserve requested setting | P0 | ui/screens/AdaptiveTypesettingPolicy.kt, ReaderScreen.kt | AdaptiveTypesettingPolicyTest (6 cases; CI pending) | YELLOW 1fe42055 pending |
| Typography/RTL | Typesetting maturity | Advanced settings | Script-aware capability restrictions exist | Readium + Veil policy | Rendered corpus/extreme settings evidence | Permanent output matrix incomplete | Readable language-aware layouts | P0 | ui/screens/ReaderAppearancePolicy.kt, ReaderScreen.kt | ReaderPreferencesTest, rendered fixtures pending | YELLOW |
| Failed semantic progress | Durable continuity | Resume trust | Failed journal now rejected | Existing AtomicFile/Room | Device storage exhaustion, warning/close UX | Cache/accepted outcome ignored failed journal | Preserve accepted state and allow retry | P0 | data/LocalLibraryRepository.kt, ui/reader/ReaderViewModel.kt, ReaderLocatorPolicy.kt, ui/screens/ReaderScreen.kt | Two Room and three dedup tests, latest CI pending | YELLOW3539c9c9 |
| Bookmark create | One-action saved place | Notebook depth | Ordered Room commit patched | Existing Bookmark domain | Device/UX and semantic duplicate identity | Earlier optimistic cache acknowledgement | Durable one-action create | P0 | data/LocalLibraryRepository.kt, data/db/Daos.kt, ui/screens/ReaderScreen.kt | AnnotationDurabilityInstrumentedTest (3 creation cases) | YELLOW |
| Bookmark delete | Reliable saved places | Notebook depth | Ordered deletion patched | Existing Room observer | Device/UX acceptance | Earlier optimistic removal | Retain record on failure | P0 | data/LocalLibraryRepository.kt, ui/VeilApp.kt, ui/screens/ReaderScreen.kt | AnnotationDurabilityInstrumentedTest (2 deletion cases) | YELLOW |
| Selection/notes | Contextual tools | Notes/highlights | Existing semantic locator + quote | Readium selection/Room | Selection replacement async races; output QA | Multiple async lifecycle owners | Durable stable semantic tools | P0 | ui/screens/ReaderSelectionActionMode.kt, ReaderNotebook.kt | selection/note regressions | YELLOW |
| Dictionary/translation | Immediate lookup | Provider breadth | PROCESS_TEXT adapter | Android interoperability | Independent provider/result contracts absent | Intent primitive only | Truthful configured optional providers | P0 | ui/screens/ReaderLookup.kt | ReaderLookupTest | RED |
| Publication search | Contextual navigation | Search depth | Existing notebook search | Readium search | Exploration anchor/session query audit | Integration not fully accepted | Stable results/context/return | P0 | ui/screens/ReaderNotebook.kt | search policy/tests | YELLOW |
| TTS | Assistive reading | Filtering/voice depth | Extensive foreground/service/neural infrastructure | Readium/Android/Media3 | Text filters, release sleep timer, physical playback | Partial foreground/service integration | Reliable independent listening session | P0 | ui/reader/tts/ | ReaderTtsSessionTest, backend instrumentation | YELLOW foreground; BLOCKED background |
| PDF core | Fixed layout | Professional PDF tools | Native Pdfium + fixture tests | Existing native navigator | Large/password/rotation/device acceptance | Missing complete document/device matrix | Professional PDF core before tools | P0 | ui/screens/ReaderPdfInput.kt, ReaderScreen.kt | ReaderPdfReliabilityInstrumentedTest, native fixture | YELLOW |
| PDF authored annotation | Annotation benchmark | Markup breadth | Not implemented | Licensed API evaluation needed | Durable coordinates/tools/export absent | Renderer support ≠ authoring support | Core first; annotation P1 | P1 | future controller below native renderer | Representative PDF fixtures required | RED |
| Accessibility/adaptive | Assistive reading | Customization | Existing semantic/key/Compose tests | Android accessibility | Actual TalkBack, 200%, foldable output proof | No physical UX evidence | Non-drag accessible reading | P0 | ui/screens/, ui/reader/ | accessibility instrumentation | YELLOW |
| Library retrieval/large library | Ecosystem retrieval | Power search | Existing Room/search/cover shell | ReadEra simplicity | 1k/10k measured retrieval/cover budgets | Scale evidence incomplete | Retrieval-first bounded pipeline | P1 | ui/screens/LibraryScreen.kt, data/ | library tests/benchmarks | YELLOW |
| Backup/restore | Continuity | Portable backup | Staged backup/restore exists | Existing transactional restore | Expanded preferences round trip incomplete | Schema coverage gap | Validated preview/merge/commit | P1 | data/LibraryExport.kt, LocalLibraryRepository.kt | RoomRuntimeRepositoryInstrumentedTest | RED gap |
| Cloud sync | Semantic continuity | Integrations | Release disabled | Semantic entity model | Conflict/tombstone/device proof missing | Not product accepted | Local durability first | P1 | feature/VeilFeatureGates.kt | conflict/offline matrix required | HOLD |

## Feature evidence matrix

Historical automated evidence below is against06a94b57. New executable3539c9c9 is under verification and does not inherit those results. Older baseline references apply only to b8ec35a1. Physical QA and
emulator instrumentation are separate columns. No row is GREEN.

| Feature | Implemented | Integrated | Unit | Instrumented | Device | UX | Perf | Release | Status |
|---|---|---|---|---|---|---|---|---|---|
| Progress journal | Yes | Yes | Current961 suite pass | 36 abrupt-process samples pass | Missing | Missing | Baseline smoke only | Enabled | YELLOW |
| Bookmark creation | Patched | Reader action | Current suite pass | New Room cases pass | Missing | Missing | Latency unmeasured | Enabled | YELLOW |
| Bookmark deletion | Patched | Reader + Archive | Current suite pass | New Room cases pass | Missing | Missing | Latency unmeasured | Enabled | YELLOW |
| Selection exact owner | Patched | Existing callback | 3/3 pass | Compiled; UI case not run | Missing | Missing | Constant-time guard; unmeasured | Enabled | YELLOW |
| Transition availability notice | Patched | Ready Reader snackbar | 3/3 pass | Compiled; notice UI not run | Missing | Missing | No rendering work; unmeasured | Notice enabled | YELLOW |
| Adaptive reading columns | Patched | Reader initial/apply/close paths | Six new cases, CI pending | Not run | Missing | Missing | Reflow cost unmeasured | Awaiting build | YELLOW |
| Failed progress checkpoint | Patched | Save/warning/close | Three dedup cases pending | Two failure cases passed at1e81; latest pending | Missing | Missing | Existing journal path; pending metrics | Awaiting current build | YELLOW |
| Precise jump admission | Patched | Existing jump callers | Three cases pending | Compiling | Missing | Missing | Constant-time; unmeasured | Awaiting current build | YELLOW |
| Idle Paper preparation lifecycle | Patched | Existing warm effects | Three engine/provider cases pending | Compiling | Missing | Missing | Current benchmark pending | GPU disabled | YELLOW |
| Foreground timer and listening text | Patched | Default listening path | Four timer cases pending; existing text characterization | Compiling | Missing audible results | Missing | Current smoke is not long-session battery evidence | Awaiting current build | YELLOW |
| GPU Paper | Substantial | Debug/benchmark | Baseline pass | Simulated evidence | Missing | Missing | No hardware traces | Disabled | BLOCKED |
| Background TTS | Substantial | Debug review | Baseline pass | Baseline backend | Missing audible playback | Missing | Battery/thermal missing | Disabled | BLOCKED |
| Neural/network TTS | Partial/gated | Review/adapter | Baseline coverage | Partial | Missing | Missing | Missing | Disabled | HOLD |
| PDF editor | Incomplete | Gated | Partial | Partial | Missing | Missing | Missing | Disabled | HOLD |
| Cloud sync/live manga | Partial | Gated | Partial | Partial | Missing | Missing | Missing | Disabled | HOLD |

## Anti-pattern registry

These are design constraints derived from the supplied benchmark brief and source
defects, not independently measured competitor complaints.

| ID | Anti-pattern / evidence class | Veil rule | Enforcement / task |
|---|---|---|---|
| AP-01 | Settings dumping / supplied Moon+ concern | Quick controls, progressive advanced settings | Preserve existing settings hierarchy; no new persistent controls in this wave |
| AP-02 | Fake Paper fallback / source-confirmed risk | Static, Slide, Paper and Scroll remain distinct | Rollout policy tests + explicit unavailable notice; gate unchanged |
| AP-03 | Optimistic important-state acknowledgement / Veil source defect | Commit before saved feedback | Ordered bookmark create/delete + failure injection |
| AP-04 | Hidden selection/navigation ownership / Veil source defect | Only current owner can release reservation | ActionMode replacement lifecycle tests |
| AP-05 | Feature existence sold as completion / evidence risk | Require every applicable acceptance gate | This evidence matrix; physical status file stays UNVERIFIED |
| AP-06 | Clutter inside reading canvas / product constraint | Short dismissible state feedback; no persistent decoration | Reuse existing Reader snackbar |
| AP-07 | Network text sharing without consent / product constraint | Optional explicit adapters; local reading remains offline | Network TTS/Cloud gates remain disabled |
| AP-08 | Physical Next hard-coded to left / RTL constraint | Semantic Next routes by publication progression | Existing directional tests; actual RTL Paper remains BLOCKED |


## Wave 1 automated acceptance record

- Executable source: 06a94b57f17b13edd4c8e1cc513ac868bdaa0d24. Subsequent
  documentation/CI-retention changes do not change the application or benchmark
  source trees; they are not represented as a newly device-tested APK.
- [Android CI37752444851](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37752444851):
  961 unit tests, zero failures/errors/skips; instrumentation compilation, schema
  verification, lint (zero errors;174 existing warnings), debug/release APK/AAB,
  optimized archive verification and performance-harness compilation PASS.
- [Storage37752444933](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37752444933):
  163 instrumented tests, zero failures/errors/skips; includes all five new
  bookmark acknowledgement/duplicate/failure/deletion regressions.
- [Durability37752444930](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37752444930):
  six scenarios ×six cycles =36 abrupt-process samples PASS. These cover locator
  commits/preview cancellation, not optical GPU Paper or bookmark process-kill UI.
- [Performance37752445136](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37752445136):
  generation, packaged Baseline Profile verification, release archive and absolute
  smoke budgets PASS. Manual dispatch skipped the PR relative-delta gate.
- Unit coverage:955 →961; instrumentation coverage:158 →163. Lint:174 warnings
  and zero errors before and after. No failures in the exercised suites.
- User's S24 Ultra is a manual-install target, not an attached device. Device,
  TalkBack, rendered RTL, optical Paper, audible TTS, battery and thermal evidence
  remain UNVERIFIED. No feature is GREEN and no risky gate is promoted.

| Emulator metric | b8ec35a1 baseline | 06a94b57 current | Interpretation |
|---|---:|---:|---|
| Cold-start TTID median |811.338ms|912.747ms|Higher observed sample; budget6000ms passes |
| Reader gfx P95, median across runs |200ms|225ms|Higher observed sample; budget400ms passes |
| Reader gfx P99, median across runs |200ms|250ms|Higher observed sample; budget600ms passes |
| Reader gfx frame count, median |77.5|71|Coverage signal; not a speed metric |

Before/after runs used separate uncontrolled API35/lavapipe CI environments.
These measurements establish neither a speedup nor causality for the increase.
Current raw reader data reports100% jank, P50=200ms, P95 runs200–250ms and P99
runs250–300ms. That is not production performance acceptance. Same-device S24
traces and a representative 60/90/120Hz matrix remain required. The historical
baseline was recovered from job113192735910 logs. Current startup raw scenarios
were missing from the artifact; the independent CI-retention patch fixes future
capture paths and remains unexecuted/YELLOW.

Debug APK SHA-256:
be7d21606bf0e11bfe13f1a5a6d94254a080e01427c65e953a171c49a5da9948.
[Exact test artifact11539007013](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37752444851/artifacts/11539007013).
This is a debug test APK, not a signed production release. See the
[S24 Ultra protocol](GRAND_FORGE_S24_ULTRA_QA_2026-10-08.md).

## Reader experience follow-up

User S24 feedback: “nothing changed”; the cover screenshot is not evidence of
Paper/text/layout acceptance. Previous build mainly changed durability and
ownership. Do not describe it as a completed visible Reader upgrade.
New executable source1fe42055 adds adaptive reflowable EPUB columns and an EN/FA
settings explanation. [Exact-source CI](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37758765459)
is pending; previous961/storage163/process36 results do not verify this new code.
No feature gate promotion, cover replacement or whole-Reader redesign.

## Current P0 continuation verification boundary

Executable3539c9c9da47b394abe77783d4dedc83b1534946 includes adaptive
columns, failure-safe progress acknowledgement/retry, precise navigation admission,
lifecycle-fenced prewarming, foreground timer and active Listening Mode text.
These are independent reversible commits, not a completed whole-app release.

Exact-source checks are running:
- Android: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37761477427
- Storage: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37761480606
- Abrupt process: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37761483780
- Performance: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37761680929

The earlier1e81f532 storage and process runs passed, but they do not certify
the later navigation/Paper/TTS source. Intermediate superseded Android runs were
cancelled; they are not pass evidence or failed test results.
ReadingAnchor/ExplorationLocator separation remains RED: current Notebook search
still shares committed programmatic navigation. Paper hardware, audible playback,
TalkBack/RTL/rendered text, full application journey and release promotion remain
unaccepted. All seven risky release gates remain false.

## Verified3539c9c9 batch, prior to packaging/window correction

- Android37761477427:980 unit tests in170 suites, zero failures/errors/skips;
  instrumentation compilation, Room schema, debug APK, optimized release APK/AAB,
  archive verification and performance-harness compilation pass. Lint0 errors/175
  warnings included one new ConfigurationScreenWidthHeight warning, subsequently
  corrected with actual LocalWindowInfo dimensions; that later source is still testing.
- Storage37761480606:165 instrumented tests, zero failures/errors/skips.
- Fault injection37761483780:36 abrupt-process samples pass.
- Performance37761680929: profile generation, benchmark execution, generated-profile
  release packaging, packaged-profile verification and absolute smoke budgets pass.
  Raw separate startup and Reader JSONs are retained, verifying the earlier retention fix.
- Unpaired software-emulator observations, not causal speedup evidence: baselineb8
  TTID811.338ms / ReaderP95200ms / P99200ms;06a912.747ms /225ms /250ms;
 3539 TTID743.829ms / ReaderP95200ms / P99200ms.3539 raw ReaderP50 median150ms,
  highest reported per-runP99250ms,100% jank in all six samples. Startup profiled
  median743.829ms, max792.782ms; uncompiled median798.110ms, max873.008ms.
  API35 x86_64 lavapipe/independent runners; no physical production acceptance.
-3539 debug APK SHA-256:4a515740c16f042bb8d1d0208ac57706aad613a350494f68f19ebd672c860cf0.
  Its signing certificate differs from the previous06a APK; it cannot update that
  installed package. It is not the planned S24 delivery artifact.

Device delivery now targets an isolated com.veilreader.app.forgeqa installation
with an explicit QA/source label, private non-production key cache and certificate
pin guard. Normal app identity and all seven risky gates retain their defaults.
The first QA run failed configuration before tests: generated label resources were
disabled.03423a36 explicitly enables them only for QA. Normal Android run37764746924 passed. QA run37765266653 passed980 units,
lint0 errors/174 warnings and164 of165 Android tests; its SAF UI test searched
hard-coded original-package resource IDs after installing the separate QA package.
efa3fb20 resolves IDs using instrumentation.targetContext.packageName for both
portrait and rotated Reader checks. QA rerun37768309693 passed980 units,165 Android tests, lint0 errors/174 warnings
and package/signature/hash verification. [Installable artifact](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37768309693/artifacts/11547406062) is version
0.10.0-forge-efa3fb20, APK SHA-256 e45956c76cd3720a12e7a1035ccf3ae91c92d6a3d0563c1543f808e1b2632449.
Physical-device acceptance remains UNVERIFIED. The public QA certificate is pinned;
runner cache save passed, later cache restore/update acceptance is pending.
ReadingAnchor/exploration separation remains RED, as do complete rendered/script
and physical-reader acceptance. No row is GREEN.

## 2026 design brief — verified reuse decisions

Magic UI repository metadata and LICENSE.md confirm MIT licensing, copyright
Magic UI,2026-10-08. Its web/React animation components are reference material;
nothing was copied into native Compose or the reading canvas in this wave.
Reuse restrained shell hierarchy/feedback concepts independently; preserve
reduced-motion and avoid animation on repeated reading actions.

| Insight | Evidence / decision | Veil target / priority / status |
|---|---|---|
| Script-aware typography | New original LTR/RTL corpus passes EPUBCheck5.4.0; rendered acceptance absent | Existing Readium shaping/fallback first, P0/YELLOW |
| Variable-font optical axes | Font and navigator-specific support needs verification; not an automatic legibility guarantee | Opt-in policy/corpus before implementation, P1/HOLD |
| Hinge-aware layout | Actual window sizing is integrated; hinge avoidance not verified | Characterize separating hinge and text measure, P0/YELLOW |
| Focus mask/sensory restraint | Existing ruler/haptic/reduced-motion paths need reachable-device audit | Reuse controls, keep user ownership, P0/YELLOW |
| Eye/hand-position inference | Proposal is not measured device capability; inferred grip/cognition can be wrong | Optional adapter only after hardware/privacy/UX evidence, P2/HOLD |
| Narrative-driven reflow/audio | Proposed sentiment behavior can disturb position and calm reading; no measured benefit | No automatic layout/theme change while reading, P2/HOLD |
| Bionic text | No clinical efficacy evidence supplied; script shaping/accessibility need testing | Optional research, no dyslexia-treatment claim, P2/HOLD |
| OS emergency notification replacement | Application canvas does not own system emergency alerts | Respect OS handling; no substitute dismissal flow, BLOCKED |
| Local AI/vector search | Local execution is not zero latency or automatically private; lifecycle/security need evidence | Optional offline adapter, P2/HOLD |
| EPUB4.0 / e-ink90Hz claims | Supplied brief is not a verified standard/hardware benchmark; official current validator reports EPUB3.4 rules | Publication/device capability evidence, HOLD |
| Calm metrics | Do not infer comprehension/focus depth from page cadence without validity evidence | Actual time/session events, separate Reader/gamification domain, P1/YELLOW |

P0 annotation/selection/listening continuity remains ahead of speculative motion
and AI. Fixtures are structurally validated, not visual goldens. No GREEN claim.

## Current repository truth — 2026-10-10

This section supersedes the historical verification boundaries above. Canonical a67586cf includes the subsequent navigation-session/semantic-reason/input-ownership work; the earlier statement that no ReadingAnchor/exploration separation exists no longer describes this head. A remaining repeated-scroll observation/final-snapshot defect is isolated in PR 446, not yet integrated.

| Feature | Implemented | Integrated | Unit | Instrumented | Device | UX | Perf | Release | Status |
|---|---|---|---|---|---|---|---|---|---|
| Durable highlight/note/delete acknowledgement | Yes | Reader + Archive | Full QA suite passes | New transaction regressions pass in QA suite | Pending | Pending S24 | No measured physical delta | Core available | YELLOW |
| Async selection cleanup ownership | Yes | Reader tools | Owner-fence regressions pass | QA integration suite passes | Pending | Pending rapid reselection/TalkBack | Unmeasured | Core available | YELLOW |
| Foreground TTS listening checkpoint | Yes, last publication | QA defaults to foreground system owner | Session/controller regressions pass | DataStore restoration test passes | Pending audible/kill QA | Pending | Start latency/storage cost unmeasured | Foreground core; service/neural/network gates OFF | YELLOW |
| Reading anchor/exploration + preview input ownership | Yes | Canonical a67586cf | Navigation/input regressions pass | Full QA integration suite passes | Pending | Pending scroll/RTL/TalkBack | Unmeasured | Core available | YELLOW; residual PR 446 |
| Atomic journal failure acknowledgement, GF-DUR-006 | Yes | 38a6bf10 | New rename-failure regression awaiting CI | Repository ordering/retry regression awaiting CI | Pending | Existing storage failure UI | Extra sync/readback cost awaiting evidence | Existing core path, no gate change | YELLOW |
| Original multilingual typography corpus | Yes | QA fixture workflow | Determinism + EPUBCheck pass | No rendered corpus tests claimed | Pending | Rendered acceptance pending | Pending | Fixtures, not product renderer | YELLOW |

Verified artifact: [a67586cf Forge QA](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37878688504/artifacts/11594068417). Full QA results: 1009 units / 174 suites and 174 Android tests, no reported failures/errors/skips. Independent APK verification confirms the pinned certificate; CI successfully restored its key cache. Actual phone update remains unverified. Physical status JSON remains untouched.

Current new-code verification: QA run 38027688503 and process-kill run 38027688094 target 38a6bf10; performance run targets the same executable source with the later documentation commit. Local 39 policy checks, canonical Paper contract plus 4 guard tests, and 22 performance/release parser tests pass. No Android test is claimed from these local checks.

PR 446 hosted build originally aborted its native Robolectric worker with `Class not found: java/nio/LongBuffer`; its incomplete 1016-test report includes one skipped test and is not a pass. A rerun preserves the original failure record; no test was excluded, weakened or moved to legacy graphics. Its storage, 144-sample abrupt-process and emulator performance lanes passed independently. Full-build success is required before integration.

No feature gate changes, physical speedup claims or GREEN promotions. Separate visual-foundation and Paper-release branches are not part of the delivered QA artifact.
## 2026-10-09 durability continuation

| Capability | Kindle | Moon+ | Veil | Best Reference | Veil Gap | Root Cause | Target | Priority | Files | Tests | Status |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Same-position final snapshot durability | Reading continuity benchmark | Resume benchmark | Stronger duplicate persistence integrated | Existing Veil atomic journal | Android/device evidence pending | Location-only dedup swallowed final checkpoint | Persist once before acknowledgment | P0 | ReaderLocatorPolicy.kt, ReaderViewModel.kt, debug probe, existing fault-injection lane | 19 policy tests PASS; two process-kill scenarios added, not executed | YELLOW |

| Feature | Implemented | Integrated | Unit | Instrumented | Device | UX | Perf | Release | Status |
|---|---|---|---|---|---|---|---|---|---|
| Duplicate final snapshot upgrade | Yes | ViewModel source | PASS focused JVM | Probe added, pending execution | Unverified | Unverified | Unverified | Unverified | YELLOW |

Task, evidence, risks and continuation graph: [durability execution](GRAND_FORGE_DURABILITY_2026-10-09.md).

## 2026-10-10 repository-truth correction

Earlier RED statements about unconditional Notebook/programmatic progress commits
are historical and superseded: ReaderNavigationSessionState plus explicit semantic
commit policies are integrated on a67586cf. Search/TOC/reference exploration and
final-snapshot suppression are implemented; acceptance remains YELLOW pending
Android/device execution. Android SDK availability was also rechecked: the official
package is platforms;android-37.0, which installed successfully without SDK downgrade.

Durability escalation now explicitly avoids granting page-turn credit for an
unchanged location. A sixth regression covers that accounting boundary.

## Final source — 2026-10-10

| Feature | Implemented | Integrated | Unit | Instrumented | Device | UX | Perf | Release | Status |
|---|---|---|---|---|---|---|---|---|---|
| Stronger same-location checkpoint | Yes | Real ViewModel | PASS, final Reader/input suite 145 | Compiles; kill scenarios pending | Unverified | Unverified | Unverified | Unverified | YELLOW |
| Repeated-scroll exploration protection | Yes | Both Reader recording sites | New regression PASS; old behavior FAIL | Compiles; real stream pending | Unverified | Unverified | Unverified | Unverified | YELLOW |
| Forge verification routing | Four existing lanes include target | YAML route checks PASS | Existing tooling PASS | Hosted execution pending | N/A | N/A | Hosted pending | No promotion | YELLOW |

Earlier whole-suite result: 1,015 tests / 174 suites PASS before final pacing and
exploration corrections; not claimed as exact-final-source evidence. Final source
passes 145 affected Reader/input tests and app/instrumentation compilation.

Final-source local lint, debug APK and instrumentation compilation PASS with no
detectors disabled. Affected suite: 145 tests / 18 suites, zero failures or skips.
Device/UX/performance/release gates remain unverified; no capability is GREEN.

## Current delivery and combined source — 2026-10-10 terminal boundary

GF-DUR-006 is verified in software on38a6bf10: 1010 units / 174 suites,175 Android tests, no failures/errors/skips, lint0 errors/174 warnings and108 abrupt-process cases. [Install verified QA ZIP](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38027688503/artifacts/11660629953), extract `VeilReader-ForgeQA-38a6bf10.apk`; actual SHA256796291c81ec9be4b6e7b1d3196cd0dcb4dbbc8f4734a9ce8090fe7be166befbb independently matches its manifest. APK v2 signature matches pinned QA certificate. Physical installation/UX/performance remain UNVERIFIED; status YELLOW.

PR446 is merged into the canonical branch at8bae5d3f after all four hosted checks passed. Its full rerun produced1016 units /174 suites with no failures/errors/skips, and raw kill evidence has144 successful cases across8 scenarios. The native-worker failure from the first attempt remains retained. GF-DUR-007 at4ee2a2be fixes same-location failure rollback found in combined review; regression execution remains pending. Combined executable88ce3971 is running QA38029096124, normal/release Android38029166135 and process-kill38029098844. These later changes are not in the delivered38a6 APK. Duplicate manual process run38029095743 was cancelled in favor of the automatic exact-source run, not counted as a failure/pass.

Draft evidence PR447 remains separate. Its7a0bbc3b process run38028207347 passed108 cases; derived footer count matches raw records. Retained90 durable checkpoint timing samples and0 failed events. Nearest-rank journal latency P506.146ms /P9510.120ms /maximum12.279ms. Those timings include Store.write sync/readback, exclude total navigation/UI latency, and have no retained paired pre-change baseline. Hosted software emulator, not S24 acceptance. Benchmark retention validation is still running.

The tested38a6 executable's documentation-only successor2f9c27bb passed benchmark/profile packaging checks: startup TTID median606.847ms; Reader P50 median121ms, P95/P99 medians150ms, maximum reported per-runP99200ms. Jank100% on lavapipe; no physical improvement or production performance GREEN is inferred. All seven risky release gates remain OFF. Physical status JSON is unchanged.
