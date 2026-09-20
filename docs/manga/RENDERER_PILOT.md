# Manga Renderer / Zoom Pilot

Status: controlled pilot; no production dependency selected
Date: 2026-09-20

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

Known risk:
- Telephoto issue #167 remains open for custom Coil Fetcher + Keyer combinations.
- Veil currently uses standard Coil ImageRequest values with request headers for online pages,
  and file URIs for durable offline pages, so this issue is not automatically a blocker.
- those exact Veil paths MUST be tested before adoption.

## Why SSIV stays as fallback

Mihon's maintained fork is specifically optimized for very large images and tile/subsampling
workloads. Its upstream design has long-running production experience and supports huge images
without decoding the full image into memory.

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
- known issue reviewed: #167 custom Coil fetcher/keyer + subsampling

Mihon SSIV:
- canonical reviewed fork: `mihonapp/subsampling-scale-image-view`
- license: Apache-2.0
- active maintenance observed in 2026
- exact commit must be pinned at implementation time.

## Exit criteria

No renderer dependency becomes permanent until:
License -> dependency resolution -> debug/release compile -> device fixture matrix ->
memory/jank comparison -> reader regression matrix are green.
