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


## Follow-up: cover taps never become clicks

User confirms the latest debug APK ignores swipes, edge taps and center taps on
an image cover. Runtime regression head `5f0cdaa7` fails the actual cover edge-tap
assertion on API 35 lavapipe. The swipe run crosses the cover and advances through
chapters, then fails at the fixture's last page; extend the fixture from six to
twelve chapters so fourteen deliberate forward turns do not hit its boundary.
The reported phone swipe failure is still a separate acceptance requirement.

Readium 3.4.0 primary-source evidence:
`readium/navigator/src/main/assets/readium/scripts/readium-reflowable.js` sends
`Android.onDragEnd` on **every** touchend, including touches with no Start/Move.
If the returned value is true, it calls preventDefault and stops propagation.
`R2BasicWebView.kt` forwards valid End events to the registered input listener.
Paper's no-active-spec End branch returned true even when no drag was reserved.
That suppresses the browser-generated click before image/edge/chrome tap policy.
Return the actual reservation state after reset: retain ownership of short drags,
but leave an unstarted tap End unconsumed. No duplicate native input bridge.

Runtime logs also report `GPU page program link failed: Precisions of uniform
'uShadowPass' differ between VERTEX and FRAGMENT shaders.` The two shader defaults
were highp and mediump, including shared uShadowPass/uSideSign. Give shared
uniforms and varyings explicit matching mediump precision while retaining highp
vertex geometry. Local Mesa surfaceless EGL negative control links old source
with status 0 and the exact precision error; changed source links with status 1
and no error. This is actual shader linking, not a text-only declaration check.
A regression compares effective shared-uniform precision across both stages.

Real-input acceptance now opens and dismisses Reader chrome with center taps on
the cover, edge-taps into chapter one, and checks actual Readium href/progression
after every swipe and settling. Harness/probe/EPUB remain benchmark-only.
Hosted after-fix runtime and phone verification must be recorded separately.


### GPU host hit testing after shader repair

After-fix run 37949200789 at ced8b0ca links/initializes GPU successfully, but
center taps and the first cover swipe both fail with no `gesture_owned` event.
The GPU's AndroidView sits inside PaperCurlOverlay's Box, a separate sibling
subtree above ReaderFragmentHost. AndroidView's sibling sharing is local to its
own layout; its parent Box still wins outer hit testing and excludes Readium.
Android's PointerInputModifierNode documentation explicitly describes layout-local
sharing. Add an inert sharing node to the outer Paper Box: it consumes no events
and forwards/synthesizes none. Real Readium remains the sole event source.
Native AndroidView instrumentation uses the same nested hierarchy and a negative
control without sharing. Existing real-publication cover/gesture tests remain
mandatory after this repair. Storage instrumentation at ced8b0ca also failed;
its job log omitted failure details and artifact download returned HTTP 403.
Print failure XML and crash traces on the next run rather than assuming a flake.

### First real input recovery — 8a73b255

Android run 37967841814 passes 1,031 unit tests with no failures/errors/skips,
zero lint errors, instrumentation compilation, debug/release archives and schema
checks. Performance run 37967842048 passes actual-publication center tap chrome,
cover edge advance and every locator departure/settled-navigation assertion in
six repetitions of fourteen forward swipes. Absolute emulator budgets pass;
relative-delta baseline is missing. This establishes emulator navigation recovery,
not physical-device optical Paper quality or a complete app quality claim.

Storage run 37967841813 executes 182 tests; the new native nested-overlay
negative-control tap/swipe regression passes. The sole failure is SAF import's
accessibility resource-ID lookup. Logcat confirms navigator attachment, readiness
and a rendered-book locator at progression 0.0. Explicitly request
AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS in the test, restoring previous
flags afterwards (Android documents this flag as not set by default). Retain the
same ID/rotation assertions and add native-hierarchy diagnostics if it still fails.
Do not declare the import flow accepted until the new run passes.

The user's supplied book has an inline SVG/image cover followed by navigation
and 1,467 content entries. Add a second independent SVG-container fixture with
our own test artwork; retain the existing img cover test. No user book text or
art is committed to the repository.

Normal debug APK 8a73b255 has signer 1c39a77ca6a3d7974ce891bb5385bdd60e23418abfac0da14c6e1428ad8f8e3d;
the uploaded 7a6bea9 signer is eb1ea9e88162d33a559b8e59be74bc67d7357eb5b83e8e081ca30840c1cd9a45.
They cannot update each other in place. Reuse the existing separate Forge QA
package/build and its pinned non-production signer/cache via the verification PR.
No uninstall, production-data migration, merge or release gate promotion.

### Confirmed native accessibility occlusion — a4c17de1

Storage run 37971059466 still fails the same import lookup with flags=65554
(including explicit ID reporting). Native hierarchy diagnostics show visible
resourcePager and R2WebViews, while the overlay's ViewFactoryHolder remains
accessibility-important despite the GPU child being View.NO. The test setup
change did not repair the product defect. Compose's semantics occlusion prunes
covered sibling bounds for an important interop host; native View.NO is not a
replacement for marking the Compose host decorative.

Apply hideFromAccessibility to the modifier passed to the actual GPU AndroidView
(and software visual overlay), preserving parent input sharing and publication
semantics. Add a native accessibility negative control: native View.NO alone
excludes the publication, but hiding the host restores its content description.
Keep the existing real SAF import/rotation test unchanged. Validate both the
normal and separately signed QA variants; previous navigation smoke cannot
establish this new accessibility repair.
