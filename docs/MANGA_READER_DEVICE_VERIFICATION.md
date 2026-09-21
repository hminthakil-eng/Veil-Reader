# Manga Reader device verification

This harness validates the integrated Manga Reader without a live source or the production library.

## Automated emulator gate

The existing `Storage Instrumentation` workflow is reused. It already runs:

```bash
./gradlew :app:connectedDebugAndroidTest --stacktrace
```

To run only the Manga Reader device matrix:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.veilreader.app.manga.reader.verification.MangaReaderDeviceVerificationInstrumentedTest
```

The matrix covers:

- complete offline reopen;
- Activity recreation;
- RTL swipe-right forward navigation;
- LTR swipe-left forward navigation;
- Paged -> Webtoon mode preservation;
- durable progress reopen through the isolated verification Room database;
- partial-offline chapter-boundary blocking;
- extreme 360x12000 local image selection into ZoomImage subsampling;
- no page-turn leakage while ZoomImage owns gestures;
- large-image recreation without an OOM/crash.

The verification Activity and database exist in the debug build only.

## Manual true process-death gate

This gate deliberately disables durable progress. The only valid way to recover the changed page/mode after the OS kills the process is the reader SavedState path.

Install the debug APK, then launch:

```bash
adb shell am start \
  -n com.veilreader.app/.manga.reader.verification.MangaReaderVerificationActivity \
  --ez durable_progress false \
  --ez reset_progress true \
  --ei start_page 1
```

1. Move from page 2 to page 3.
2. Tap the center and switch to Webtoon or LTR so there is more than one state value to verify.
3. Press Home so the app is backgrounded.
4. Ask Android to kill the background app process:

```bash
adb shell am kill com.veilreader.app
```

5. Return to the existing Veil task from Recents. Do not clear the task and do not use `force-stop`.

Pass criteria:

- the same logical chapter is restored;
- page/item index is the changed value, not the original `start_page=1`;
- Paged/Webtoon and RTL/LTR restore to the values selected before process death;
- there is no crash, blank reader or source/network request.

`am force-stop` is intentionally not used because it clears/changes lifecycle semantics and is not a valid SavedState restoration test.

## Manual extreme-image visual gate

Launch:

```bash
adb shell am start \
  -n com.veilreader.app/.manga.reader.verification.MangaReaderVerificationActivity \
  --ez extreme true \
  --ez reset_progress true
```

The first page is generated locally as a 360x12000 PNG with horizontal markers every 512 px.

Pass criteria:

- the page opens without OOM or app restart;
- zoom/pan stays under ZoomImage ownership and does not turn the Manga page;
- text/marker edges sharpen as higher-resolution tiles become available;
- no persistent seams, black bands or large blank regions appear while panning;
- rotation/recreation returns to a usable extreme page;
- tapping the renderer can still reveal Veil reader chrome.

For memory diagnostics during this gate:

```bash
adb shell dumpsys meminfo com.veilreader.app
```

Record PSS before opening the extreme fixture, after it appears, after zoom/pan, and after leaving the reader. This is diagnostic evidence, not a fixed pass/fail memory threshold because device heaps differ.

## Partial-offline manual gate

```bash
adb shell am start \
  -n com.veilreader.app/.manga.reader.verification.MangaReaderVerificationActivity \
  --ez partial_offline true \
  --ez reset_progress true
```

The first chapter exposes only two cached pages. Advancing at its final cached page must stay in that chapter and show the reconnect message rather than silently entering the next chapter.
