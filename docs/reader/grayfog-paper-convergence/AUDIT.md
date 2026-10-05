# Grayfog / canonical GPU Paper convergence — 2026-10-05

This report records source work and verification limits. It does not certify physical-device behavior.

## Repository truth

Fetched `origin` before edits. Inputs:

- Grayfog: `18bcded592fff1b67a5dfc2e78785b3363109e1d`.
- Canonical Paper: `25d2c89402cf3dd13df67d080cf7090dafa225cf`.
- Divergence: canonical has 175 exclusive commits; Grayfog has 11.
- Existing PR #374 was OPEN / Draft, targeting Grayfog, at the canonical SHA above.
- Three canonical Actions jobs failed with `steps=[]`: build `111471051461`, benchmark `111471051267`, storage `111471051373`. No checkout/source step executed. These runs are **INFRA FAILURE**, not source verdicts.
- Latest Grayfog Actions runs `37293329528`, `37293329471`, `37293329452` were successful at the exact Grayfog SHA above.
- No applicable `AGENTS.md` was found in the checkout.

## Classification of the 11 Grayfog commits

| Commit | UI/UX | Reader | Settings | Accessibility | Tests | CI | Docs/assets |
|---|---|---|---|---|---|---|---|
| f0d571e7 | artifact/provenance/type hierarchy | headings/appearance presentation | headings/radio layout | mixed script and semantic hierarchy | detail/design constitution | — | review captures |
| b3137ee1 | — | — | — | — | — | recorded status only | verification |
| d9d3e52f | authored-world composition | ordinary-tap access dock, auto-hide policy | persisted auto-hide switch | semantic tap controls | store/chrome/native review | — | keep artwork/captures |
| 73781d63 | — | PDF review harness | — | native review harness | isolated native workers, PDF probes | storage worker setup | device instructions/evidence |
| ed24d8ad | active-volume composition | PDF harness repair | — | native review | artwork fixtures/cloud review | storage/emulator setup | captures/evidence |
| d4011731 | Castle portals, large-text Gallery | — | — | large-text hierarchy | review/search | — | captures/evidence |
| 231038c9 | retrieval-density refinement | — | — | — | retained verification | artifact retention | captures/evidence |
| 22b9074b | — | — | — | live artwork search labels | Android/native search fixtures | — | verification |
| 751c042d | Castle halls, physical volume at Threshold | — | — | large-text composition | cloud/native review | — | evidence |
| 3686fb77 | compact archive/search result hierarchy | — | — | archive targets | native review | — | handoff/audit |
| 18bcded5 | — | — | — | — | recorded results only | recorded status only | authored-world evidence |

## Merge intent and ownership

Merge commit `80c1d7f1` preserves both histories. No donor branch was wholesale merged.

- `ReaderScreen`: inspected the merged diff despite no textual conflict. Keep canonical input arbitration, GPU Paper overlay, warm capture, navigation/durable-close ownership and lifecycle fencing. Add Grayfog access dock, auto-hide setting, Reduced Motion timing and appearance heading. Dock callbacks remain outside publication input; no new renderer.
- `SettingsStore` / `SettingsScreen`: retain canonical rollout availability and explicit review override; add persisted auto-hide preference and Grayfog semantic controls.
- `Common`: retain live `LocalWindowInfo` sizing with bootstrap Configuration fallback, plus display-title fallback and latest cover/artifact presentation.
- `ReadingNowScreen`: preserve Grayfog active-volume height cap and realm-condensation policy; resolve width/height through canonical sizing authority.
- `CastleScreen`: preserve the latest portal/hall implementation and its large-text branching, with canonical live top-level sizing. Do not nest the older corridor owner inside Grayfog's replacement layout.
- `ObservatoryScreen`: retain new heading semantics and canonical sizing.
- `LibraryScreen`: retain new search-result/collection/cover composition and restore Configuration import used for locale formatting; locale remains separate from structural window sizing.

## Root causes repaired

1. Missing source contracts in canonical: orphaned `materialPageSnapshotScaleIsSafe`, Slide buffer-preparation helper and static page-stack atmosphere helper. Restore narrow helpers in their actual owners. Do not restore `PaperCurlGeometry`, `PaperCurlDraw` or Canvas curl runtime.
2. Source compilation: extra top-level brace in `MainActivity`, missing navigation extension import, missing directional-listener scope, missing locale import, nullable shader bitmap access. AndroidTest directional callers also lacked scope and their fake navigator deliberately threw on `currentLocator`, despite settlement now requiring it.
3. Release geometry: finger edge travel and resisted progress are different quantities. Settling previously replaced the former with the latter on its first frame. Continue from the release edge with continuous, monotonic endpoint mapping for completion/cancel.
4. Clear cancellation: engine, Paper adapter and Slide state could retain `active=true` if canceled during their visual/input barrier. Ownership now clears in `finally`.
5. Discrete preview lifetime: Paper and Slide tap/key turns lacked stored exact origins. Their origins/specs now participate in the existing preview lifecycle; force teardown restores an accepted uncommitted transaction.
6. Coroutine publication race: inline completion could reset a job field before assignment finished, leaving a completed job as the busy owner. Publish lazy jobs before starting them in Paper, Slide and directional navigation.
7. Stale cleanup: an old non-cancellable locator settlement could restore/clear/reset a newer turn. Cleanup requires both the canceled token and the exact cancellation generation; force teardown retires cleanup ownership.
8. GPU host identity: per-instance generation counters collided across replacement views. A shared atomic allocator now distinguishes both host replacement and context recreation.
9. Upload lifetime: canceled/superseded submissions that were never uploaded retained CPU bitmap leases. GL retires obsolete submissions after its reads end; pause retires pending reads after the GL pause barrier. Lease acknowledgements include the upload revision so old acknowledgements cannot release a newer submission of the same bitmap.
10. Shader construction: fragment compile failure leaked an already compiled vertex shader. Program construction owns shaders in `finally` and deletes an unlinked program.
11. Canonical guard: whole-file matching rejected the valid post-draw retry reset. Match the READY callback only; mutation tests prove a READY reset and missing callback remain failures.
12. Native Grayfog search verification: two locale configurations shared one native test worker and the worker aborted with JNI `FloatBuffer` lookup failure. Preserve both tests/real artwork assertions in separate locale-specific classes so `forkEvery=1` isolates their native sandboxes. Isolation alone did not resolve the native worker abort. Cover decode could outlive a canceled composition/native sandbox. Decode now retains structured ownership until started native work exits, scopes the worker context classloader, rejects canceled queued work before allocating, and checks cancellation before publishing. Artwork waits include removed-but-still-decoding covers. Cancellation, exception and queued-work regressions cover this ownership. A combined final gate nevertheless reproduced the English native-worker abort; two isolated runs (including `-Xcheck:jni`) passed. Native runtime warnings and this intermittent failure remain disclosed; decode ownership fixes do not establish that the JNI abort is eliminated.
13. Fixed-layout test contract drift: the prior assertion demanded curl/slide style in the effective spread runtime, although canonical intentionally supports one reflowable sheet. Assert static spread navigation and separately verify that the user's retained requested appearance is unchanged.

## Instrumentation

Existing capture/View.draw, texture-upload/draw trace sections, JankStats phases and benchmark parser contracts are retained. Add debug/benchmark metadata for CPU bitmap/pool bytes, pending uploads, submit-to-first-GPU-draw duration and texture byte estimate. Also log begin-to-acquired-source-buffer duration. GPU draw completion is **not** screen presentation or finger-to-screen latency. Estimates do not certify driver residency or memory stability.

## Passes and limits

| Pass | Source evidence | Remaining physical work |
|---|---|---|
| Correctness | compiler + mode/navigation/locator contracts | actual one-page commit and exact cancel |
| Race | inline job publication, canceled-operation fencing, revision/generation lease fencing | fast input, live GL scheduling/context loss |
| Performance | trace phases, no obsolete-lease work allocation with an empty ledger | 60/90/120Hz pacing, cold-first-turn latency |
| Memory | two CPU slots retained; no write to a leased slot; canceled upload retirement | repeated turns/pressure and driver residency |
| Gesture | body/edge reservation, progression, release thresholds retained; discrete regression tests | top/middle/bottom, slow/flick/reverse and selection arbitration |
| Visual/material | five independent physics/optics/sensory profiles retained; release-edge continuity repaired | visual curl, shadows/backside/material distinctions |
| Accessibility | latest Grayfog semantics, auto-hide opt-out and Reduced Motion preserved | TalkBack/touch exploration and real large text |
| RTL/Persian | progression contracts and Persian native search tests retained | real Persian EPUB progression and optical review |
| Lifecycle | durable-close and restoration paths reviewed; cancel/teardown regressions added | rotation/process death/background/foreground on hardware |
| Code quality | narrow owners restored; canonical guard + build verification | device-backed simplification only after evidence |

Paper/Slide/Paged/Scroll remain separate, PDF stays outside Paper, and normal-motion Paper navigation still fails closed on unavailable GPU/capture. Release rollout remains gated; hardware certification has not been earned.

14. First-frame ordering: a fixed frame-settle interval was not a presentation acknowledgement. Paper now tags its EGL buffer and awaits the exact acquired TextureView buffer for that sheet epoch before changing Readium. Presentation acknowledgements also match context and viewport generations. Canceled/old buffers cannot authorize navigation; a bounded timeout fails closed and is visible in the HUD. The GL host lifecycle listener remains delegated, and only eight tiny presentation records are retained.

**Known verification risk:** acquired-buffer ordering is source-tested, but driver support for `EGL_ANDROID_presentation_time`, cold launch, SurfaceFlinger composition and absence of destination flash still require hardware evidence. A buffer acknowledgement is not an optical finger-to-screen latency measurement. No physical support/performance claim is made.

No claim is made that every requested failure mode has been exhaustively eliminated. Hardware/visual/performance requirements cannot be closed by source tests. Final exact-source gate results are recorded separately in `VERIFICATION.md`.

Historical donor logs had trailing whitespace removed, and `gradlew.bat` is normalized under the existing `.gitattributes` policy; neither changes executable behavior. Lint warnings are retained and reported rather than suppressed.

Verified source commit: `969dbe975665b99af4957bc4743364855c1c6a82`. The existing canonical branch and PR #374 are reused; the local v2 staging name is retired. The source passes the final complete 806-test run, but the earlier intermittent native worker failure remains an open test-runtime risk.
