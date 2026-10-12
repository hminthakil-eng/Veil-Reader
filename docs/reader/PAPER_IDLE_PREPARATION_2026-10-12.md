# Reader polish — avoid repeated adjacent-page work

Base inspected: `7a365c4e5a016e0e885f1fee678d2e5c6a87c7b0`, active native
Paper PR #509. Main remains the integrated baseline, not a release approval.

## Source-confirmed defect and change

After a reversed/canceled fold, the Reader preparation effect restarts when
`PaperCurlState.active` becomes false. The live origin/revision has not changed,
but `PaperPreviewNavigator.prepare` previously navigated and captured each
adjacent direction again. It replaced already-valid immutable page pixels and
could allocate two full-resolution adjacent bitmaps plus a frozen origin.

The preparer now computes missing directions before any Readium JavaScript,
navigation, paint wait or destination allocation. A hit must match source
revision, logical origin, viewport dimensions and unrecycled bitmap ownership.
A complete hit returns immediately; a partial hit only prepares the missing
direction. A source invalidation, chapter change or viewport change requires
fresh preparation. Active gestures continue pinning their existing destination.

Idle source/adjacent preparation also yields while the appearance panel is open,
native text selection is active, or renderer preferences are settling. Closing
the panel or ending selection restarts preparation. The separate preview host
remains attached and has no progress, input or accessibility ownership.

No capture resolution, navigation mode, release gate or performance limit was
changed. This removes unnecessary work; a measured frame improvement requires
new exact-source benchmark evidence.

## Competitor-informed acceptance

| Area | Reference and Veil requirement | Current work / next gate |
|---|---|---|
| Page movement | Apple's documented Curl/Fast Fade/Scroll choices; Veil must preserve distinct Paged/Paper/Slide/Scroll behavior | Existing real-book LTR/RTL fold, cancel, commit and chapter-pixel tests remain mandatory; physical #408 open |
| Reading controls | Apple documents text, spacing, theme and brightness controls; Veil needs accessible controls with the actual book visible | Yield preparation during appearance changes and selection; preserve location and current mode |
| Listening | Amazon documents Assistive Reader and synchronized highlighting | Audible Pause/voice-stability #481 and neural-engine #484 remain open; this patch does not certify speech |
| Position trust | Apple documents automatic place saving | Existing exact-source storage/durability gates must pass; canceled previews must never become saved progress |
| Responsiveness | Android recommends investigating actual slow rendering and frame work | Base run 38244456395 fails relative frame limits: p95 300 vs 200 ms, p99 350 vs 200 ms; retain those limits and remeasure |

Primary references (read 2026-10-12):
- https://support.apple.com/guide/iphone/read-books-iphc1af7c57/ios
- https://www.aboutamazon.com/news/books-and-authors/kindle-accessibility-features-for-all-readers
- https://developer.android.com/topic/performance/rendering/inspect-gpu-rendering
- https://github.com/readium/kotlin-toolkit/blob/develop/docs/guides/navigator/navigator.md

These are documented features and platform guidance, not a matched-device
quality ranking. Superiority to Apple Books or Kindle remains unproven.

## Verification

- Local: 39 reading-policy checks PASS; canonical Paper contract PASS;
  six canonical guard regressions PASS; `git diff --check` PASS.
- Four added JVM regressions cover canceled-source reuse, partial preparation,
  stale chapter/location/revision/viewport, invalidation and recycled ink.
- Android JVM/build/lint, actual-book instrumentation and frame gates: pending
  exact implementation SHA. Record receipts below after completion.
- Windows fallback compilation could not resolve the current AndroidX/zoom
  dependencies from its offline cache; this is an environment failure, not an
  Android compilation result. No physical phone was connected through ADB.
- Physical smoothness, subjective paper feel and public release remain
  UNVERIFIED. PR #509 stays draft; native production rollout stays OFF.
