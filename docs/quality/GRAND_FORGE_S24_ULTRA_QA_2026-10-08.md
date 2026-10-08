# Veil Reader S24 Ultra verification

Target phone reported by user: Galaxy S24 Ultra.
Connection: user will install APK and test manually; no remote connection established.
Executable source: 06a94b57f17b13edd4c8e1cc513ac868bdaa0d24.
APK SHA-256: be7d21606bf0e11bfe13f1a5a6d94254a080e01427c65e953a171c49a5da9948.
Build artifact: Android CI37752444851 / artifact11539007013.
APK artifact: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/37752444851/artifacts/11539007013.
Automated evidence: 961 unit tests and163 instrumented tests pass;36 abrupt-process samples pass.
Release APK/AAB compilation/archive verification and performance smoke/profile budgets pass.
This is a debug test APK; physical acceptance is still UNVERIFIED.
Android/One UI version, model variant, observed refresh rate: not recorded.
Status: UNVERIFIED. This document is a test protocol, not test results.

Use the exact-head debug APK only after automated checks pass. Do not substitute
an older APK or call the debug-only Paper review a release-enabled capability.

## First verification: current Wave 1 changes

1. Import a real reflowable EPUB. Open it, move to a recognizable sentence and
   create a bookmark. Confirm “saved” appears. Immediately force-stop Veil from
   Android App info, reopen the book and return to the bookmark. The saved
   sentence/locator must match. Record each result, not just “app opened.”
2. Tap Bookmark repeatedly at the same location. Notebook must contain exactly
   one bookmark, with one successful creation and explicit duplicate feedback.
   Repeat after reopening the app.
3. Delete that bookmark from Reader Notebook. Once it disappears, force-stop and
   reopen. It must remain deleted. Repeat using Archive deletion.
4. Select text, dismiss and immediately select different text. Open and close
   Notebook/appearance overlays. Selection must block accidental page-turn
   routing while its current toolbar is active, and navigation must work when
   the current toolbar closes. Test touch and TalkBack separately.
5. With an existing saved Paper preference and Paper unavailable, opening a
   publication must explain actual simple paging. Dismissing the notice must
   not change the saved preference or cause persistent notices. A fixed-layout
   EPUB requested as Paper/Slide/Scroll must explain its static mode.

Storage rejection is covered by SQLite-trigger instrumentation; do not fill or
damage the user's phone storage to reproduce it. Real device actions above verify
integration and lifecycle behavior but do not replace deterministic fault tests.

## Physical acceptance after the above

- Display: Standard then Adaptive motion smoothness; record observed refresh rate.
- Reader: portrait/landscape, ordinary and accessibility-large font, Paper/Night.
- Language: actual Persian/Arabic EPUB plus mixed Persian-English content.
- TalkBack: Bookmark, saved/failure message, Notebook return/remove, selection;
  logical focus must survive opening/dismissing each overlay.
- Paper debug review: first turn, slow drag, cancel, reverse, 100 uninterrupted
  alternating turns, rotation, background/foreground. Record optical behavior
  separately from frame traces. Release GPU gate remains disabled.
- System TTS: audible first utterance, pause/resume, next/previous, language voice
  routing, headphone/Bluetooth interruption and position restoration. Background
  and neural review do not earn release promotion without their full acceptance.
- PDF: representative large and RTL documents, zoom/pan/links/search, rotation,
  resume; authored markup remains unavailable.

## Results template

| Test | Build/APK hash | Phone/OS | Settings | Observed result | Evidence | Status |
|---|---|---|---|---|---|---|
| Bookmark create → process stop → resume | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| Rapid duplicate bookmarks | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| Reader + Archive delete → process stop | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| Selection replacement + input ownership | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| Unavailable transition explanation | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| TalkBack / RTL / large font | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| Paper physical / memory / battery | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |
| Audible system/background/neural TTS | Pending | S24 Ultra / unknown | Pending | Not executed | None | UNVERIFIED |

## Next P0 build —3539c9c9 (not the historical APK above)

Build/tests/APK hash pending exact-source CI37761477427. Do not install the
historical06a APK to verify these new changes. User feedback on that previous
build was “nothing changed”; the cover screenshot does not validate text/turning.

- Open an actual text chapter. In Appearance, test Auto/Two columns in portrait,
  landscape, split screen, maximum Veil font size and Android large text. Narrow
  or large-text layouts should use one column and preserve the saved preference;
  returning to eligible width should restore it. PDF/fixed-layout should be unchanged.
- Open Listening Mode with default foreground/system TTS. Its active semantic text
  should be visible. Select10 minutes: the timer should show active immediately,
  continue without restarting the current utterance and pause at expiry. Replace
  with15, or choose Off, to test cancellation without restarting playback.
- Open a search result/highlight later in the same chapter/position chunk; the
  navigator should receive the precise target. Identical positions remain no-ops.
- Paper review: background during idle/visual preparation, return, cancel/commit
  first turn; repeat. It must preserve ownership and prepare current pixels on
  resume. A manual visual check is not memory/frame/battery proof.
- Storage failure feedback and blocked successful close require a safe injected
  test environment; do not deliberately fill the personal phone's storage.

Physical status for every new item remains UNVERIFIED.
