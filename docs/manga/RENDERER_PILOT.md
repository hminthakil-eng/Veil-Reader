# Manga Renderer / Zoom Pilot

Status: controlled pilot; no production dependency selected
Date: 2026-09-21

## Decision

Primary pilot candidate:
- Telephoto 0.19.0
- artifact: `me.saket.telephoto:zoomable-image-coil3:0.19.0`
- license: Apache-2.0

Fallback / reference candidate:
- Mihon fork of SubsamplingScaleImageView
- repository: `mihonapp/subsampling-scale-image-view`
- license: Apache-2.0
- pin an immutable commit if adopted; do not depend on a moving JitPack branch.

Do not use the old Maven Central 3.10.0 artifact as the preferred fallback.
It is much older than the actively maintained Mihon fork.

## Why Telephoto is first

- Compose-native API fits Veil's reader UI.
- official Coil 3 integration
- automatic large-image sub-sampling
- pinch, double-tap, quick zoom and fling
- nested-scroll compatibility
- zoom/pan state restoration
- direct file/image support is aligned with Veil's online-cache and durable-offline paths.

Known risks:
- Telephoto issue #167 remains open for custom Coil Fetcher + Keyer combinations.
- Telephoto issue #165 remains open for large progressive JPEGs in HorizontalPager. The maintainer
  reproduced the same OOM behavior with Coil AsyncImage, so this is not currently proven to be
  Telephoto-specific. The observed failure is overlapping native-memory-heavy progressive JPEG
  decodes, especially when pager preloading keeps multiple giant pages alive.
- Veil currently uses standard Coil ImageRequest values with request headers for online pages,
  and file URIs for durable offline pages, so #167 is not automatically a blocker.
- hard gates now include: exact Veil request paths, giant progressive JPEGs, beyond-viewport
  preloading <= 1 during stress testing, and disk-cached preview strategy before full-resolution
  zoom/subsampling.

## Host-side pilot evidence — 2026-09-21

- Upstream still reports Telephoto 0.19.0 as the latest release.
- Veil resolves `me.saket.telephoto:zoomable-image-coil3:0.19.0` successfully.
- Telephoto's declared Coil 3.2.0 resolves upward to Veil's existing Coil 3.6.3.
- Baseline comparison confirmed Compose Runtime/Foundation 1.12.0 and Kotlin stdlib 2.4.20 were
  already selected on #175 before Telephoto; that dependency-version drift is not caused by this pilot.
- With Telephoto present but not wired into production navigation, Veil passed debug and release/R8
  builds plus the full host gate: policy 39, performance 4/5/6, unit, AndroidTest compile and lint.
- `TelephotoMangaPagePilot` compiles against Veil's real ImageRequest builder, including
  request-header and file-URI-capable inputs, while `MangaReaderScreen` remains on AsyncImage.
- Debug APK delta observed locally was about +503 KiB (+1.23%).
- A dedicated pilot keep-rule is now committed so the next retained release/R8 pass measures actual
  Telephoto shrinker compatibility instead of allowing the unused pilot to be fully dead-stripped.
- The pilot branch caps paged-reader `beyondViewportPageCount` at 1 to reduce overlapping giant-image decodes during stress testing.
- Physical-device memory/gesture acceptance remains mandatory before any production adoption.

## Why SSIV stays as fallback

Mihon's maintained fork is specifically optimized for very large images and tile/subsampling
workloads. Its upstream design has long-running production experience and supports huge images
without decoding the full image into memory. The fork was still active on 2026-08-16; the latest
reviewed commit is `94915e6f73b3d13187d3c794079e75eb6fd4974f`.

Tradeoff:
- View-based, not Compose-native.
- integration requires AndroidView/lifecycle/recycling care.
- dependency distribution is less clean than Telephoto's Maven Central artifact.

## Pilot fixtures

The device test set MUST include all of the following.

### Source paths
1. normal HTTPS page without custom headers
2. HTTPS page requiring Referer/header
3. durable offline `file:` page
4. Coil disk-cache hit
5. cache miss followed by reload

### Image shapes
1. ordinary portrait manga page
2. landscape spread
3. 4K page
4. 8K page
5. approximately 20k x 20k stress image
6. very tall webtoon image
7. transparent PNG
8. WebP
9. corrupted/truncated image

### Reader interactions
- pinch zoom
- double-tap zoom
- quick-scale / double-tap-drag when supported
- pan at zoom > 1
- fling
- zoom reset on page change
- orientation change while zoomed
- RTL page swipe while image is at minimum zoom
- pan without accidentally turning page while zoomed
- nested scroll in Webtoon mode
- process recreation / state restoration
- accessibility content description

## Performance gates

A renderer may pass only if:

- zero OOM/crash across the fixture set
- large images are sub-sampled/tiled rather than fully retained in memory
- no visible zoom snap-back
- double-tap and pinch remain deterministic
- page swipe and zoom/pan gesture arbitration is correct
- offline file pages work without an extra network copy
- custom-header online pages work through Veil's standard ImageRequest path
- page changes release renderer resources predictably
- long Webtoon reading does not retain off-screen full-resolution pages
- orientation/process restoration does not corrupt page or zoom state

Record for both candidates:
- peak process memory / PSS
- Java heap
- native heap
- frame timing/jank during zoom
- first-display latency
- time to first full-quality tile
- repeated 50-page navigation memory trend

Prefer the candidate with the more stable worst-case behavior, not merely the best average.

## Integration boundary

`MangaReaderViewModel` owns:
- chapter/page identity
- reader preferences
- preload state
- durable progress

Renderer owns only:
- display
- zoom/pan state
- image decode/subsampling
- gesture arbitration local to the current page

The renderer MUST NOT persist library progress or source state.

## Telephoto adoption gate

Adopt Telephoto if:
1. standard Coil 3 + custom HTTP headers works with sub-sampling,
2. direct offline file URI works with sub-sampling,
3. pager gesture arbitration passes RTL/LTR tests,
4. 8K/20k stress cases do not regress memory,
5. release/R8 build is clean.

If any of those fail materially, test the pinned Mihon SSIV fallback.

## Provenance

Telephoto:
- canonical repository: `saket/telephoto`
- license: Apache-2.0
- latest reviewed release: 0.19.0
- release commit shown upstream: `7acc50f`
- known issues reviewed: #167 custom Coil fetcher/keyer + subsampling; #165 large progressive-JPEG pager OOM

Mihon SSIV:
- canonical reviewed fork: `mihonapp/subsampling-scale-image-view`
- license: Apache-2.0
- active maintenance verified through 2026-08-16
- latest reviewed commit: `94915e6f73b3d13187d3c794079e75eb6fd4974f`
- exact immutable commit must be pinned if adopted.

## Exit criteria

No renderer dependency becomes permanent until:
License -> dependency resolution -> debug/release compile -> device fixture matrix ->
memory/jank comparison -> reader regression matrix are green.
