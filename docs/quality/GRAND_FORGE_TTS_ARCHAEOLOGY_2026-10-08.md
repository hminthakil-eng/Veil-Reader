# Grand Forge — TTS archaeology

Source baseline: `3cc162aeadd5c4ced809e26e110946244cc74560`, inspected 2026-10-08. Documentation-only specialist investigation. No production/test changes, test execution, audio playback, device QA, or benchmark execution were performed by this specialist. Findings are code evidence, not product acceptance. No feature below is GREEN.

## Ownership and release truth

| Owner | Responsibility | Evidence |
|---|---|---|
| ReadiumTtsContent | Authored content/language/locators; Readium ICU sentence tokenizer; bounded UTF-16 splitting | ReaderTtsContent.kt:35–144 |
| ReaderTtsSession | Sole speech session; serial fencing; pending read; current segment; previous history; pause/focus policy | ReaderTtsSession.kt:43–427 |
| AndroidReaderTtsBackend | Main-thread Android engine; offline voice validation; audio focus/noisy interruption; fenced Binder callbacks | AndroidReaderTtsBackend.kt:20–258 |
| ReaderTtsMediaPlayer | Media3 semantic facade; separately opened publication; checkpoint persistence | ReaderTtsMediaPlayer.kt:39–388 |
| ReaderTtsPlaybackService | MediaSession; private custom commands; timer; voice catalog/preview | ReaderTtsPlaybackService.kt:40–401 |
| ReaderTtsServiceController | Activity client; closing controller does not stop service playback | ReaderTtsServiceController.kt:27–410 |
| SettingsStore / ReaderTtsSettings | Global speed/pitch and bounded per-language voice memory | SettingsStore.kt:323–337; ReaderTtsSettings.kt:5–64 |
| ReaderTtsSettingsState | Local pending preference acknowledgement protection | ReaderTtsSettingsState.kt:9–26 |
| Neural model tooling | Verified manifests, bounded extraction, transactional install/recovery, layout/shared-vocoder validation | ReaderTtsModelPackage/ArchiveExtractor/Store/Preparer/Layout/SharedRuntimeAssets |
| ReaderTtsNeuralRuntime | PCM interface and benchmark contract; no executable implementation found | ReaderTtsNeuralRuntime.kt:9–104 |
| ReaderScreen / ListeningMode / Controls | Owner selection and presentation | ReaderScreen.kt:1202–1256,3993–4200; ReaderListeningMode.kt:49–451 |

`BACKGROUND_TTS`, `LOCAL_NEURAL_TTS`, `NETWORK_TTS` are all release-disabled (`VeilFeatureGates.kt:19–38`). Debug review enables background/local-neural gates only. Release uses foreground ReaderTtsSession; debug uses service. A debug gate does not supply an executable neural runtime. Keep gates unchanged until their acceptance evidence exists.

Semantic source → ICU sentences → Android-safe chunks → authored/publication/default locale → offline voice → backend playback is implemented. A configurable cleaning/pronunciation/language-detection pipeline is absent from the inspected TTS domain. Speech checkpoints are intentionally separate from visual Reader progress; do not merge their authority.

## Actionable tasks

### GF-TTS-01 — Exposed timer silently does nothing in release

- Severity: P0.
- Subsystem: TTS UX/capability integration.
- Affected files: ReaderScreen.kt, ReaderListeningMode.kt, ReaderTtsControls.kt, corresponding Compose tests; localized strings if an unavailable explanation is used.
- Observed behavior: release has no service controller. ReaderScreen.kt:4057–4059 passes a nullable controller timer callback; ListeningMode.kt:423–436 forwards it; Controls.kt:194–243 always exposes the duration menu. Selecting a duration therefore does nothing in foreground release.
- Expected behavior: exposed timer actions must have an actual owning implementation; otherwise clearly unavailable.
- Root cause: capability is implicit in a nullable callback, while the control remains enabled.
- Implementation plan: add an explicit timer capability through both presentation components; hide actionable durations or show a localized unavailable state. Genuine foreground timer can be separately implemented with lifecycle semantics.
- Reuse candidate: existing service timer and durable deadline store; mirror existing voiceCatalogSupported gating.
- Regression risks: defaults must fail closed; debug timer and test callers must retain real behavior.
- Tests required: unsupported menu cannot expose actionable durations; supported selection calls once; EN/FA large-text access; service timer expires and clears.
- Performance implications: negligible capability check; no added timer in the minimal fix.
- UX implications: removes false promise without disabling narration.
- Acceptance criteria: no exposed no-op timer; supported timer remains reachable; physical UX verification before GREEN.
- Dependencies: existing owner selection; no gate change.
- Status: RED. Small, high-confidence candidate after Wave 1 reliability work.

### GF-TTS-02 — Foreground listening ignores active sentence

- Severity: P0.
- Subsystem: speech presentation.
- Affected files: ReaderScreen.kt, ReaderListeningMode.kt, presentation tests.
- Observed behavior: Session.kt:175–180 publishes activeText; Screen.kt:4028–4031 reads controller text only, otherwise an empty flow. Foreground release narration cannot display its current semantic sentence.
- Expected behavior: display the active owner's actual bounded text.
- Root cause: presentation source wiring excludes foreground session state.
- Implementation plan: when controller is absent, use collected foreground speechState.activeText. Preserve controller source for service playback.
- Reuse candidate: existing bounded, in-memory session field.
- Regression risks: owner/book replacement must clear stale text; never persist speech text or publish it as MediaMetadata.
- Tests required: foreground PLAYING/PAUSED text, replacement clearing, service path preservation, EN/FA text.
- Performance implications: existing state emission; no additional content extraction.
- UX implications: truthful current listening context.
- Acceptance criteria: displayed text belongs to current owner and semantic segment; device/UX evidence required.
- Dependencies: session state and owner policy.
- Status: RED. Small wiring candidate.

### GF-TTS-03 — Unbounded content preparation

- Severity: P0.
- Subsystem: reliability.
- Affected files: ReaderTtsSession.kt, ReaderTtsSessionTest.kt.
- Observed behavior: first nextContent() at line129 precedes initialization timeout; subsequent and Next-transition reads are also unbounded. Lines404–411 await deferred source.next() without deadline. A hanging source can retain PREPARING indefinitely.
- Expected behavior: bounded visible preparation with actionable TIMEOUT and explicit recovery.
- Root cause: bounds cover initialization/synthesis only.
- Implementation plan: characterize pending-read retention, then introduce content deadline and owner-safe cleanup. Decide retry policy for retained hung reads; do not claim timeout physically interrupts non-cooperative parsing.
- Reuse candidate: existing timeout/problem model, serial fence, source mutex, session-owned deferred.
- Regression risks: naive timeout drops consumed segment, preserves permanently hung deferred, or creates concurrent parsers; close can wait for non-cooperative work.
- Tests required: hung first/later read; retry; pause retains chunk; Next during PREPARING; replacement; close; cancellation distinct from timeout.
- Performance implications: per-read deadline only; measure large-EPUB extraction P50/P95 before choosing limit.
- UX implications: eliminate endless spinner and provide retry.
- Acceptance criteria: bounded visible state, exact semantic locator, no surprise playback; real large-publication measurements required.
- Dependencies: pending-read characterization and content performance baseline.
- Status: RED by missing-bound code evidence; hanging-source regression test pending.

### GF-TTS-04 — Dedicated speech transformation pipeline

- Severity: P0.
- Subsystem: TTS text pipeline.
- Affected files: ReaderTtsContent.kt, ReaderTtsSession.kt, ReaderTtsSettings.kt; new pure pipeline/settings tests.
- Observed behavior: tokenizer and safe chunks exist; no URL/header/page-number/footnote filters, pronunciation/regex/abbreviation rules or language detector found in TTS domain.
- Expected behavior: configurable speech-only transforms with immutable book content and locators.
- Root cause: current boundary implements extraction/chunking rather than configurable speech representation.
- Implementation plan: preserve Readium tokenizer/source locators; introduce a pure transform boundary whose default preserves current speech. Add bounded opt-in rules incrementally, define empty-output advancement, and route languages explicitly.
- Reuse candidate: Readium ICU tokenizer and existing offline voice eligibility; evaluate lawful language components before implementing detection.
- Regression risks: locator drift, empty speech, expensive regex, Persian/mixed-script errors, incorrect repeated-header suppression.
- Tests required: immutable source; EN/FA/AR fixtures; Unicode; empty/URL-only segments; bounded rules; language-routing and exception handling.
- Performance implications: bounded linear passes; benchmark content length/rule count; regex safety budget.
- UX implications: progressive disclosure; explicit user filter choices.
- Acceptance criteria: configuration genuinely affects spoken representation without changing publication; actual playback and RTL fixtures verified.
- Dependencies: GF-TTS-03 and text/voice/device baselines; work reuse gate still needs external candidate/competitor evidence.
- Status: RED.

### GF-TTS-05 — Characterize asynchronous service cleanup ownership

- Severity: P0.
- Subsystem: lifecycle/durability.
- Affected files: ReaderTtsMediaPlayer.kt, ReaderTtsPlaybackService.kt, ReaderTtsCheckpointStore.kt, tests.
- Observed behavior: stop/load/release invoke suspending closeCurrentOwner; shared session/opened fields are read again after awaitClosed (MediaPlayer.kt:258–273,330–359). Stop clears checkpoint/request after suspension without captured generation recheck.
- Expected behavior: stale cleanup cannot clear or close a replacement owner/checkpoint.
- Root cause: candidate race from shared mutable ownership across asynchronous lifecycle jobs; not yet reproduced.
- Implementation plan: first characterize stop→new load, release during open, restore→explicit start, preference-save→stop. If reproduced, detach captured old owner before suspension and fence post-suspend writes or serialize transitions.
- Reuse candidate: existing generation counter and session shutdown barrier.
- Regression risks: premature serialization can wait indefinitely on non-cooperative parser; no broad refactor before tests.
- Tests required: deterministic suspended close/open/save owners; no double-close; latest generation survives; checkpoint ordering.
- Performance implications: never block main thread; benchmark service start/close.
- UX implications: prevent silent stop and book/position mismatch.
- Acceptance criteria: each transaction only touches captured owner; deterministic checkpoint ordering and process recovery.
- Dependencies: injectable ownership characterization seams.
- Status: YELLOW; investigation risk, not a claimed reproduced defect.

### GF-TTS-06 — Earn background release readiness

- Severity: P0.
- Subsystem: release/audio/privacy.
- Affected files: backend/service/controller/media facade, VeilFeatureGates.kt, device harness and evidence docs.
- Observed behavior: substantial Media3 implementation exists and exported service is registered (AndroidManifest.xml:26–34); foreign custom commands rejected (Service.kt:111–122). Platform test explicitly needs no audible speech and covers initialization cancellation/close only (BackendInstrumentedTest.kt:16–39).
- Expected behavior: genuine background audio acceptance before release gate promotion.
- Root cause: executable scaffolding/unit tests are insufficient device/UX/performance evidence.
- Implementation plan: retain implementation and gate; run screen-off/lock/background/headset/Bluetooth/notification/focus/noisy/process matrix. Verify foreground-service promotion before focus on supported Android versions, semantic recovery and no surprise autoplay.
- Reuse candidate: Media3/Android TTS; existing semantic checkpoint and audio focus handling.
- Regression risks: OS/OEM differences, service/controller privacy, missing offline voice, unexpected autoplay.
- Tests required: actual offline speech; transient exact-segment resume; user pause suppresses autoresume; notification controls; kill/restore paused locator; EN/FA engines; exported-controller security.
- Performance implications: first-audio P50/P95/worst, memory/CPU/battery/thermal over long session.
- UX implications: accessible notification/listening controls; truthful unavailable voice/provider.
- Acceptance criteria: Implemented + Integrated + Tested + Device QA + UX QA + Release Enabled all independently evidenced before GREEN.
- Dependencies: GF-TTS-01–05, real devices and installed offline voices.
- Status: BLOCKED on device evidence. Neural/network independently BLOCKED on executable integration, models/consent and measurements; never infer readiness from catalog metadata.

## Verification limits and baseline

Existing tests characterize extraction/locators, Unicode boundaries, offline voice fidelity, pause/source replacement/Next/history/focus, payload/checkpoint serialization, model/archive/recovery safeguards, and accessibility controls. No tests were executed by this audit. CI compilation of instrumentation does not prove its execution. Existing test names do not prove audible background playback, native neural PCM, actual EPUB sentence highlighting, or physical accessibility acceptance.

Before/after metrics: unavailable; no change or benchmark performed. Performance thresholds in neural benchmark model are acceptance contracts, not measured results.

## Exact inspected file inventory

Paths below are repository-relative. Inspection included complete core session/backend/content/controls source, selected ownership and lifecycle sections of larger files, and structural/test-case/assertion review of supporting model/tests. This is not a claim of exhaustive line-by-line verification of every listed file.

### Main source

- app/src/main/java/com/veilreader/app/ui/reader/tts/AndroidReaderTtsBackend.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsBackend.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsCheckpointStore.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsContent.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsMediaPlayer.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelArchiveExtractor.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelLayout.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelPackage.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelPreparer.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelStore.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsNeuralCandidates.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsNeuralRuntime.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsPlaybackContract.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsPlaybackService.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsPolicy.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsProvider.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsServiceController.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsSession.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsSharedRuntimeAssets.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsSleepPolicy.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReaderTtsSleepTimerStore.kt
- app/src/main/java/com/veilreader/app/ui/reader/tts/ReadiumReaderTtsSession.kt
- app/src/main/java/com/veilreader/app/ui/screens/ReaderListeningMode.kt
- app/src/main/java/com/veilreader/app/ui/screens/ReaderTtsControls.kt
- app/src/main/java/com/veilreader/app/ui/screens/ReaderTtsSettingsState.kt
- app/src/main/java/com/veilreader/app/ui/screens/ReaderTtsVoicePresentation.kt
- app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt (TTS integration sections)
- app/src/main/java/com/veilreader/app/domain/ReaderTtsSettings.kt
- app/src/main/java/com/veilreader/app/data/settings/SettingsStore.kt (TTS sections)
- app/src/main/java/com/veilreader/app/feature/VeilFeatureGates.kt
- app/src/main/AndroidManifest.xml (TTS declaration/permissions)

### Unit tests — test structure/case/assertion review

- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelLayoutTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelArchiveExtractorTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsSessionTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsPlaybackContractTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReadiumTtsContentTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsPolicyTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsSleepPolicyTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsReaderOwnershipTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsProviderTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelStoreTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsSharedRuntimeAssetsTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelPreparerTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsModelPackageTest.kt
- app/src/test/java/com/veilreader/app/ui/reader/tts/ReaderTtsNeuralRuntimeTest.kt
- app/src/test/java/com/veilreader/app/ui/screens/ReaderTtsVoicePresentationTest.kt
- app/src/test/java/com/veilreader/app/ui/screens/ReaderTtsSettingsStateTest.kt
- app/src/test/java/com/veilreader/app/domain/ReaderTtsSettingsTest.kt
- app/src/test/java/com/veilreader/app/data/settings/ReaderTtsPreferencesTest.kt
- app/src/test/java/com/veilreader/app/feature/VeilFeatureGatesTest.kt

### Instrumentation source reviewed — not executed

- app/src/androidTest/java/com/veilreader/app/ui/reader/tts/ReaderTtsBackendInstrumentedTest.kt
- app/src/androidTest/java/com/veilreader/app/ui/screens/ReaderTtsControlsAccessibilityTest.kt
- app/src/androidTest/java/com/veilreader/app/ui/screens/ReaderListeningModeAccessibilityTest.kt

SettingsStoreInstrumentedTest.kt was discovered by inventory but not inspected in this specialist pass; do not count it as covered. Physical TalkBack, audio playback, Bluetooth, thermal/battery, actual Persian publication output and release gates remain open.
