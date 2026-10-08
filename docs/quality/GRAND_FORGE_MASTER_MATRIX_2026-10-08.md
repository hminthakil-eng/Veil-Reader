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
| Progress durability | Continuity benchmark | Resume benchmark | Atomic checkpoint + ordered Room | Existing Veil journal | Failure-path/device acceptance | Physical evidence missing | Accepted state survives abrupt kill | P0 | data/LocalLibraryRepository.kt, ui/reader/ReaderViewModel.kt | ReaderCloseDurabilityTest, RoomRuntimeRepositoryInstrumentedTest, fault injection | YELLOW baseline CI |
| Stable publication identity | Ecosystem identity | Import depth | Content fingerprint already exists | Existing Veil import commit | Rename/relink/restore matrix incomplete | End-to-end identity evidence missing | Preserve user data across source changes | P0 | data/LocalLibraryRepository.kt | RoomRuntimeRepositoryInstrumentedTest | YELLOW |
| Navigation transactions | Reading continuity | Search/navigation depth | Existing transaction/session gates | Existing Veil ownership model | Complete exploration vs anchor audit | Mixed navigation semantics need characterization | Exploration preserves reading anchor | P0 | ui/reader/ReaderNavigationTransaction.kt, ui/screens/ReaderScreen.kt | ReaderNavigationTransactionTest | YELLOW |
| Input ownership | Predictable interaction | Gesture flexibility | Single arbiter; static lifetime fixed | Existing Veil arbiter | Stale selection destroy releases new owner | Unconditional false callback | Exact-owner release | P0 | ui/screens/ReaderSelectionActionMode.kt | ReaderSelectionActionModeTest | YELLOW patch |
| Paper availability | Physical reading benchmark | Transition choice | GPU exists, release disabled | Existing canonical engine | Disabled-mode remap unexplained | Failure notice requires presented Paper | Explicit reason, static mode truthful | P0 | ui/screens/ReaderAppearancePolicy.kt, ReaderScreen.kt | ReaderTransitionAvailabilityTest | YELLOW patch |
| Paper performance | Smooth turns | Multiple transitions | Snapshot/upload/presentation fences | Existing Veil traces | First turn/100/500 turns/high-refresh proof | No physical hardware | Stable frame/memory budgets on device | P0 | ui/reader/material/, benchmark/ | PaperWarmupBenchmark, ReaderFrameBenchmark | BLOCKED physical |
| Fixed-layout transition | Layout-aware behavior | Format depth | Static paging policy | Existing Veil capability policy | No physical leaf/spread engine | Whole-view capture is not one leaf | Explicit unavailability until correct engine | P0 | ui/screens/ReaderAppearancePolicy.kt | ReaderTransitionAvailabilityTest | HOLD engine |
| Typography/RTL | Typesetting maturity | Advanced settings | Script-aware capability restrictions exist | Readium + Veil policy | Rendered corpus/extreme settings evidence | Permanent output matrix incomplete | Readable language-aware layouts | P0 | ui/screens/ReaderAppearancePolicy.kt, ReaderScreen.kt | ReaderPreferencesTest, rendered fixtures pending | YELLOW |
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

“Baseline pass” applies only to b8ec35a1, never to a new patch. Physical QA and
emulator instrumentation are separate columns. No row is GREEN.

| Feature | Implemented | Integrated | Unit | Instrumented | Device | UX | Perf | Release | Status |
|---|---|---|---|---|---|---|---|---|---|
| Progress journal | Yes | Yes | Baseline pass | Baseline fault injection pass | Missing | Missing | Baseline smoke only | Enabled | YELLOW |
| Bookmark creation | Patched | Reader action | Existing baseline | New cases pending | Missing | Missing | Latency unmeasured | Enabled | YELLOW |
| Bookmark deletion | Patched | Reader + Archive | Existing baseline | New cases pending | Missing | Missing | Latency unmeasured | Enabled | YELLOW |
| Selection exact owner | Patched | Existing callback | Three new cases pending | Not run | Missing | Missing | Constant-time guard; unmeasured | Enabled | YELLOW |
| Transition availability notice | Patched | Ready Reader snackbar | Three policy cases pending | Not run | Missing | Missing | No rendering work; unmeasured | Notice enabled | YELLOW |
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
