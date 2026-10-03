# W42 — owned offline TTS foundation

Branch: `alpha/w42-tts-foundation-v1`, based on W41. This slice supplies the speech foundation; it does **not** expose a complete user-facing TTS feature. Controls, persisted preferences, paragraph/chapter navigation, sleep timer and notification/media-session behavior remain W43 work. No background playback is claimed.

## Capability and ownership

Pinned Readium 3.4.0 public `Publication.content(start).iterator()` supplies actual text segments, languages and source locators. Veil does not parse EPUB HTML independently, synthesize PDF text, replace the publication engine or write speech progress into the Reader's locator store. Protected/restricted publications and PDF are conservatively excluded.

Readium's HTML iterator explicitly requires a CSS selector to start within a resource. Without one it starts at the resource boundary; progression alone does not establish paragraph precision. W43 must present that boundary honestly and verify the actual current Navigator locator before offering speech from the current position. Source locators have paragraph/segment granularity: no exact word highlight or exact word resume is claimed.

The separately published Readium media-TTS 3.4.0 source was audited before choosing an adapter. Factory and media-session eager collectors have scope ownership concerns, Android initialization waits without a bounded deadline, and missing selected voices can fall back through `setLanguage`. These are source observations, not device leak measurements. W42 uses the existing shared content API and an explicitly owned Android backend without adding dependencies.

## Safety

Android 11+ package visibility declares the TTS service query, so installed engine discovery is permitted without broad package visibility.

Speech binds only after explicit start and a nonempty content segment. Voice selection requires an installed, offline voice of the requested language; missing packs fail without network/default-language fallback. Rate/pitch and voice identifiers normalize safely. UTF-16 bounded chunks preserve Unicode and surrogate pairs across Persian, Arabic, mixed script and CJK. No whole-book queue is created.

A Reader owner has one cancellable speech session, a private Handler and generation/request tokens. Old initialization/completion callbacks cannot control a new request or Reader. Initialization is limited to five seconds; synthesis has a ten-minute watchdog, with 1,000-character chunks. Pause replays the current bounded chunk, because Android does not expose a trustworthy word resume point.

Audio-focus loss (including ducking) and becoming-noisy events pause and release ownership. Backgrounding pauses immediately. Selection, modals, appearance, notes, image loading/viewer, renderer preference transitions, close and spoken accessibility block new speech; active speech checks these conditions every 500 ms. No automatic resume occurs. Close cancels and joins source work before publication release. Engine, receiver, focus and callbacks are disposed by their owner; platform cleanup failures cannot skip subsequent cleanup.

No decorative motion, touch layer, permanent overlay, renderer replacement, Room change or progress writer is introduced. Entering Reader does not bind a speech service or request audio focus.

## Verification

Final Gradle results are recorded below after completion. Unit coverage includes deterministic offline voice selection, missing packs, CJK scripts, malformed preferences, Unicode bounds, explicit start, pause/replay, old callbacks, independent Reader owners, interruptions, accessibility/background gates, bounded initialization, empty content and broken content factories.

Instrumentation compilation includes the existing W39/W41 suites. There is no usable Android device: adb has none, /dev/kvm is absent, and prior software-emulator attempts never provided a usable package manager. No audible speech, voice-installation, Bluetooth/call interruption, TalkBack, process-kill, pixels or performance measurement is claimed. A real-device TTS test and current-locator content fixture remain gates for user-facing W43 controls.

Final unchanged-source command: `testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`, JDK 21, `--no-daemon --max-workers=1`: **BUILD SUCCESSFUL**, 4m 36s, 92 tasks (25 executed, 67 up-to-date). **564 unit tests, zero failures/errors/skips**. Both APKs build/package; native TTS binding/cleanup target compiles but is not executed. Lint: **0 errors, 152 warnings**, unchanged baseline count. No compiler warning was emitted. Policy: **39 pass**; performance-tool unit checks: **15 pass**. Debug APK: **46,302,261 bytes**; test APK: **1,451,239 bytes**. The packaged debug DEX includes the TTS backend and manifest includes the service query. `git diff --check` passes. No device performance measurement is claimed.

Two intermediate builds were cancelled while superseded source revisions were undergoing lint; they are not reported as successful lint evidence. The final pass above uses fixed source and includes the manifest query and native test.
