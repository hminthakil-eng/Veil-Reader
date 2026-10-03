# Codex handoff — W39 Focus Guide

## Canonical branch
`alpha/w39-focus-guide-v1`

Base:
`alpha/w38-reader-hardware-keys-v1`

Current head:
Use the current HEAD of `alpha/w39-focus-guide-v1`; do not rely on a pinned SHA because handoff hardening may advance the branch.

## Goal
Continue Veil Reader competitor-informed hardening using independent clean-room implementations. Preserve all existing Reader ownership contracts and do not regress current W36–W38 behavior.

## W39 implemented so far
- Persistent `ReaderFocusGuideSettings`
- Modes: OFF / WINDOW / LINE
- Normalized vertical position, band height and dim strength
- Non-intercepting overlay in Reader
- Settings UI + English/Persian strings
- Persistence through DataStore
- Unit tests for normalization/geometry
- Instrumented durability test
- Toggle behavior preserves last active focus-guide mode durably across process recreation
- R&D gap matrix updated

## Critical interaction contracts
1. Focus Guide must never own touch input.
2. Text selection, EPUB links/footnotes, PDF zoom/pan, Paper Curl, Slide and static paged navigation remain authoritative.
3. OFF must be visually and behaviorally identical to pre-W39 Reader.
4. Invalid/legacy persisted values fail calm.
5. Accessibility/touch exploration must not be obscured by a semantically interactive overlay.
6. Reduced motion does not disable the guide because it is static, but no decorative animation should be added.

## Required Codex work
1. Clone repo and checkout `alpha/w39-focus-guide-v1`.
2. Run:
   - `./gradlew testDebugUnitTest`
   - `./gradlew assembleDebug`
   - relevant instrumentation compile task if emulator is unavailable
3. Fix all compile/test failures found on W39 and stacked W37/W38 changes.
4. Verify quick toggle preserves the user's prior WINDOW/LINE mode after restart; this was a late W39 UX fix.
5. Review W39 overlay order relative to:
   - PaperCurlOverlay
   - SlidePageOverlay
   - ReaderModeHandoffOverlay
   - image viewer/loading overlays
   - chrome
   Ensure the guide is visually useful but never covers blocking/error/loading UI incorrectly.
6. Add tests for:
   - OFF produces no band
   - LINE clamps to narrow band
   - WINDOW bounds at min/max vertical positions
   - malformed persisted enum/value fallback
   - toggle OFF → last active mode and active → OFF
7. Audit color/contrast on PAPER, SEPIA, DUSK, OLED and high-contrast shell.
8. Do not merge until a real build/test step executes successfully.

## Known upstream CI issue
GitHub-hosted Android CI / Storage / Performance frequently terminate before any step allocation with `steps=null`. Treat this as infrastructure failure, not test evidence.

## Next research after W39 is green
Priority order:
1. PDF annotation capability audit
2. TTS engine using supported Readium/Android APIs
3. Auto-scroll with pause-on-touch and accessibility policy
4. Dictionary routing
5. Chapter/book time remaining
6. E-Ink mode
7. Reading calendar/year statistics

## Clean-room rule
Do not copy Moon+ DEX, resources, assets, fonts, native libraries, layouts, identifiers or proprietary implementation code. Reimplement observed behavior independently using Veil/Readium/Android APIs.

## Cloud verification — 2026-10-03

See [W39_CLOUD_VERIFICATION.md](W39_CLOUD_VERIFICATION.md) for reproducible toolchain assumptions,
fixes, regression coverage, and the exact instrumentation limitation.

- 537 unit tests passed; debug APK, instrumentation APK and lint passed.
- Official Gradle 9.6.0 wrapper added; JDK 21 is required by Robolectric SDK 37.
- Focus removal is immediate and rapid toggles acknowledge the latest preference draft.
- Hardware presses retain ownership through handler disposal/replacement and spoken accessibility.
- Instrumentation targets compile, but unstable software emulation did not provide a valid runtime pass.
- PR #364 must remain unmerged; device interaction/visual/process-kill QA is still explicit.
