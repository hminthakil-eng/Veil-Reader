# APK-informed Paper preparation ownership

## Baseline and evidence

Live integration: `grand-forge/p0-kindle-reader-quality-20261008` (#429).
Active Reader/UI development: `grand-forge/visual-foundation-reader-components-v1-20261009`
at `450303d9fc23941c4a1b4a1030fb5d64b0452065` (#436). That SHA's Android CI,
storage and benchmark workflows completed successfully; device acceptance is a separate gate.

Reuse the two bounded commits from #439 (PixelCopy temporary-buffer isolation and
attempt-only diagnostics) on the active Reader base. No rollout promotion or production merge.
Existing local checkouts with unrelated TTS work are untouched.

The `analyze-reader-apks` skill's expanded static reference identifies separate
Moon+ `NewCurl3D` geometry/texture/render operations: `startCurl` invokes rectangle,
mesh and bitmap setup; `setCurlPos` invokes `Mesh3D.curl` and requests rendering.
Sample SHA-256: `d676b16ff18c2496ab35d1306fd195e7fba491bc8260cfd368ab973c7bc09d08`.
This supports investigating independent image ownership rather than replacing Veil
physics. It does not prove Moon+ performance or identify every cause of Veil's
reported invisible turn. No proprietary source or binary assets are copied.

## Verified Veil code defect

`MaterialPageEngineState.prepareSnapshot` chose a reusable slot then suspended in
hardware capture. `nextWritableSnapshotBufferSlot` excluded GPU uploads but not
warm preparations. A simultaneous `begin` could choose that same CPU bitmap. A
late capture could overwrite the displayed/uploaded sheet before the existing
post-capture `active` check rejected its result. Refreshing an already prepared
bitmap was also consumable while a second preparation was writing it.

## Changes and acceptance

| Gap | Change | Regression |
|---|---|---|
| Suspended preparation has no CPU ownership | Lease its slot through return/cancellation; exclude it from immediate capture and prepared consumption | Begin during delayed capture uses a different bitmap; late red capture cannot change blue active pixels |
| Concurrent preparations select the same slot | Select only non-preparing/non-uploading slots; fail closed when both busy | Two captures have distinct targets; cancelling one makes only its slot available |
| Hidden/released source can publish or fallback late | Non-reactive preparation generation advances on idle release; reject before fallback and publication | Release while delayed hardware failure is pending performs no obsolete software capture |
| Existing prepared image is refreshed during begin | Prepared consumption requires an unleased preparation slot | Begin during refresh cannot consume the writable image |

Production WebView visual-readiness behavior remains the default; its suspend seam
allows deterministic engine tests without timing sleeps or a real PixelCopy driver.
Keep PAGED/PAPER CURL/SLIDE/SCROLL dispatch, geometry, navigation, sensory behavior,
RTL handling, durability and release feature gates unchanged.

## Validation boundaries

Local source guard and its six regression tests, plus 39 reading-policy checks,
passed. Gradle wrapper could not fetch Gradle 9.6.0 because its distribution
endpoint is unreachable from this runtime; Android tests were not run locally. The four new Robolectric engine regressions require Android CI; local
Android toolchain availability and remote exact-head results must be reported
separately. A passing test/build does not establish device-level paper quality.

Device gate: same book/device, first turn and immediate drag during preparation,
slow finger tracking, release/cancel, next chapter, rapid turns, orientation and
pause/resume. Collect actual presentation/frame evidence before claiming GREEN.
