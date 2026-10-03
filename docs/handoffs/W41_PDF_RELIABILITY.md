# W41 — PDF reliability through the existing renderer

Base: W40 `alpha/w40-reader-cloud-baseline-v1`, which starts at current W39 `7d3d7e28`.
Working branch: `alpha/w41-pdf-reliability-v1`. The previous independently published PDF audit is
included unchanged except for a clearly separated W41 follow-through note. No Readium/Pdfium replacement,
private-handle borrowing, database migration or parallel reading-position engine was introduced.

## User-visible behavior and discovered defects

- Embedded PDF annotations now render through `PdfiumEngineProvider.Listener` and the public
  configurator. Covers retain upstream's annotation-free behavior. No annotation editing/list UI is
  shown: the real engine/wrapper boundary remains explicit in the A/B/C audit.
- Internal links use the native document destination to construct the standard one-based Readium
  `page` fragment. Readium alone maps document indices to RTL-reversed displayed indices. Source-page
  fragments, progression, title and text are not copied into destination locators.
- The native viewer invokes its app tap callback before testing links. PDF-only tap deferral lets
  native links cancel pending chrome actions. There is no touch overlay or custom link hit testing.
  Pan, pinch, double tap, input keys and EPUB motion/tap ownership retain their existing delegates.
- PDF link jumps use the existing previous-location transaction and carry the expected document page.
  A stale source emission cannot settle the jump. The same authoritative locator stream captures a
  reached link destination before UI debounce; a subsequent native swipe cannot erase that checkpoint.
  Close/ON_PAUSE/ON_STOP can commit an actually reached
  PDF destination before the 500ms UI debounce, preserving the normal durable flush sequence without
  fake page-turn XP. Unreached destinations are not persisted as if completed.
- Pending tap work is removed by its Reader owner. A disposed or background owner cannot dispatch a
  deferred tap, and a stale factory link callback cannot address a newer session.
- External PDF links follow the existing EPUB HTTP(S) policy, with browsable routing and calm failure
  if no activity handles the request. File/content/script/intent URIs are not launched from book links.
- PDF bookmarks show localized document page numbers rather than only percentage/title context.
- `PDFView.zoomTo` only sets a field in the resolved viewer. Slider/immediate reduced-motion paths
  now preserve the viewport center and explicitly refresh tiles. Slider movement reuses cached pixels
  while dragging and requests tiles on release, avoiding a new render queue for every slider tick.
  Reset's label and animated/immediate target agree even if renderer limits differ from defaults.
- Zoom mirror/acquisition polling is foreground lifecycle-bound. Malformed renderer zoom limits cannot
  create NaN/infinite/empty Compose slider ranges. No zoom settings schema or invented viewport restoration
  was added.

## Tests and evidence

Final unchanged-source verification: `testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest
:app:lintDebug` with JDK 21, Gradle 9.6.0, `--no-daemon --max-workers=1` completed successfully in
5m 27s (92 tasks; 20 executed, 72 up-to-date). **550 unit tests, 0 failures/errors/skips**.
Both APKs compile/package successfully; lint has **0 errors, 152 warnings**, matching W40 by ID/count.
No compiler warning/error was emitted. Debug APK: **46,302,261 bytes (44.16 MiB)**;
instrumentation APK: **1,412,955 bytes**. Policy checks: **39 pass**. Performance-tool tests: **15 pass**.
Committed Room schemas are unchanged and `git diff --check` passes. Instrumentation is compiled,
not run, because no usable device is available. No measured device-performance improvement is claimed.

The intermediate lint pass was cancelled after more than 13 minutes of CPU work in a Kotlin UAST
comment traversal on a superseded source revision. It is not recorded as a lint pass or app failure.
Final verification uses a fresh Gradle process and stable source.

New focused regressions cover native-link cancellation of chrome, pending tap disposal/background,
interaction ownership changing before deferred dispatch, native-to-Readium page addressing, malformed
indices, source-data removal and locator JSON round-trip, page labels, safe external routing, finite
zoom limits, stale source emissions, fast lifecycle destination flush and expired/ordinary transaction
isolation, and StateFlow conflation when a native swipe immediately follows a link. Existing W37/W38/W39 and locator/session durability suites are rerun in full.

`ReaderPdfNativeFixtureTest` uses a 1,835-byte, independently generated PDF with three pages, a real
internal link and an explicit highlight appearance. It opens/closes its own native document and recycles
both bitmaps. It checks actual Pdfium destination indices and annotation rendering flags on-device;
**compilation is not execution**. Host `pdfinfo` confirms three unencrypted PDF 1.4 pages, 612×792 points,
no JavaScript/forms and valid parse. Host parsing is not a Pdfium runtime pass.

The existing end-to-end PDF harness now imports that fixture and adds a real native-link jump and
previous-location return before rotation. Its obsolete layout-label expectations and radio-as-toggle
assumption were corrected; node traversal/click/read helpers release pooled AccessibilityNodeInfo
objects on API 26–32, including unmatched nodes and queued siblings. These harness fixes compile but
do not constitute a device pass.

## Accessibility, RTL, appearance and ownership

No interactive annotation overlay, extra permanent chrome, decorative motion, new renderer semantics
or EPUB-only control was introduced. Custom PDF chrome taps remain gated by Reader selection/accessibility/modal policy, and TalkBack
hardware-key exclusion is preserved. The native PDF accessibility surface is not expanded or
represented as a successful TalkBack link-navigation pass. PDF bookmark labels are English/Persian resources and use document page numbers,
independent of locale and displayed RTL order. Zoom controls keep their existing 48dp practical targets,
localized state descriptions and reduced-motion immediate path. Actual large-text/TalkBack/rotated/RTL
Reader rendering is still a device gate, not inferred from source inspection.

## Performance and remaining limitations

No device frame/jank/memory improvement is claimed. Tap deferral is one cancellable main-loop callback
per PDF tap, not a frame loop; stale pending work is removed. Zoom polling stops outside RESUMED and
unchanged mirror values do not cause new state notifications. Slider tile loading occurs on release.
No publication file hashing, PDF parsing or cover extraction was moved onto the UI thread.

This cloud has no KVM or attached device, and its two software-emulator attempts were unusable. Run
compiled native fixtures and the existing `ReaderPdfReliabilityInstrumentedTest` on a usable device.
Then verify link origin/return/re-return, links in both RTL and LTR, zoom/pan before/after jumps, rotation
while zoomed, process recreation, scanned/large/encrypted/corrupt files, all palettes/high contrast,
large text and TalkBack. Pdfium's own internal zoom animator lacks a public general cancel API; this
slice does not reflect private animation state or promise cancellation of an in-flight native double-tap
animation. Adapter resets still open a new document and may reset zoom by upstream design; exact native
viewport process restoration remains unproven, not faked with saved screen coordinates.

No automatic merge. Next approved slice after this build is green: supported offline TTS foundation,
with a capability check before introducing controls or background services.
