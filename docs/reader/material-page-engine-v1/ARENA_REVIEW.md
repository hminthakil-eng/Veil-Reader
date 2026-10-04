> Retained strip-renderer documentation from the existing branch. For the new
> cylindrical mesh, current integration and verification results, see [README.md](README.md)
> and [verification.md](verification.md).

# Arena aggressive review

Status: **SOURCE-HARDENED / NOT YET GREEN**

Reviewed branch: `reader/material-page-engine-v1`
PR: #372

Arena treats visual success as irrelevant if Reader ownership, lifecycle, frame pacing, sensory timing, RTL, or reversibility are weak.

## P0

No obvious source-level P0 remains after the current pass.

This is **not** equivalent to release approval. CI has not executed any build step because GitHub-hosted jobs are still terminating with `steps=[]`.

## P1 findings found and fixed

### Engine ownership could change mid-gesture

Problem: legacy/material selection was read repeatedly from the global rollout gate. A gate change during a turn could start in one engine and clear/commit in another.

Fix: visual-engine ownership is now latched for the whole Paper transaction from `begin` through commit/cancel/clear.

### Review switch was not useful enough for real A/B judgment

Problem: the engine gate existed in source but was not conveniently controllable on a review device.

Fix: debug Settings now exposes a process-local A/B switch and canonical material selector. Release behavior remains legacy by default and the control is not persisted.

### Stale sensory queue

Problem: rapid turns could queue synthesized material sounds and delayed haptic pulses, producing feedback after the gesture that caused them.

Fix: material cues now use generation invalidation. Stale queued audio and second pulses are dropped.

### Sensory timing lag

Problem: completion/cancel cues were emitted after settle animation, causing tactile/audio feedback to arrive too late.

Fix: completion, cancellation and boundary cues now start at release onset.

### Release velocity did not drive physical settle

Problem: release velocity affected threshold/sensory identity but not the normal material spring.

Fix: finger release velocity is normalized to page progress velocity and passed into `Animatable.animateTo(initialVelocity=...)`. Android's Compose API supports this gesture-animation pattern.

### Underdamped page settle

Problem: a damping ratio below critical could preserve hidden springiness even when visible progress was clamped.

Fix: material completion is now critically or slightly over-damped; cancellation is deliberately firmer. Boundary feedback remains the only intentionally bouncing path.

### Curl field read too much like a side roll

Problem: the initial mesh had a nearly vertical crease even for corner pulls.

Fix: pull origin and vertical drag now generate a bounded diagonal crease; the flat page is clipped against the same crease so the transition stays coherent.

### First-frame tone/patina flash

Problem: material tone and patina were configured only after a material turn became active. The first dark-theme frame could momentarily use the light defaults.

Fix: tone/patina are preconfigured while the overlay is idle.

### First-turn bitmap allocation risk

Problem: the first interaction could allocate a full ARGB page bitmap at drag start.

Fix: when Paper mode stabilizes, the selected engine's reusable snapshot buffer is prewarmed off the gesture path. Legacy and Material buffers are not both retained unnecessarily during review switching.

### Live mesh allocation churn

Problem: object-based strip/point geometry would allocate heavily at high refresh rates.

Fix: the live renderer now mutates a bounded reusable primitive mesh buffer. Immutable frame/strip objects remain only for tests and inspection.

## Ten-level quality escalation

Arena then ran ten additional quality passes after the first source-hardening review:

1. projected release with reverse-cancel hysteresis;
2. adaptive mesh density based on curvature/material demand;
3. grazing-angle optical response for glossy vs matte surfaces;
4. material-dependent free-edge rendering;
5. page-coordinate-anchored grain/fibre cues to prevent texture swimming;
6. vertical micro-jitter suppression without temporal drag lag;
7. compile/review hardening, including invalid preview Paint cleanup and duplicate motion configuration removal;
8. subpixel strip overlap to suppress seam artifacts;
9. expanded contracts for release, adaptive mesh, jitter and optical differentiation;
10. material-specific tap lift/settle choreography instead of generic UI easing.

The pass also corrected the pure inspection geometry buffer so adaptive segment counts keep full capacity, and removed the superseded generic tap-duration policy.

## Second ten-level quality escalation

Arena then ran a second ten-pass refinement cycle:

11. sanitized non-finite drag, release, settle and reduced-motion inputs so NaN/Infinity cannot poison renderer state;
12. strengthened finite-frame validation and rejected collapsed perspective quads before matrix mapping;
13. stopped failed perspective mappings from drawing orphaned material shading;
14. latched a stable material-specific mesh budget for the entire turn to prevent topology popping;
15. removed the Paper reveal delay from Reduced Motion discrete turns;
16. stopped prewarming full-page Paper bitmaps when Reduced Motion means the legacy snapshot path is unused;
17. added a restrained material tint to the lifted front face so front surface identity is visible without recoloring the reading canvas;
18. normalized seam overlap, free-edge thickness, crease/body shadow and edge offsets by display density;
19. rejected curl drawing when a stale snapshot would be non-uniformly warped after a resize or rotation;
20. made the new quad validation allocation-free and added contracts for non-finite safety, quad collapse, stable turn tessellation, front-surface material identity and resize safety.

The second cycle deliberately rejected refactors that did not create measurable correctness, physical plausibility, accessibility, lifecycle or frame-quality gains.

## Third ten-level quality escalation

Arena then ran quality levels 21–30:

21. replaced one-sample release velocity with a high-refresh-aware estimator that smooths noisy samples but gives deliberate reversals high trust;
22. softened extreme top/bottom pull origins so corner grabs remain expressive without driving singular geometry;
23. added a material-specific backside ink-transmission policy;
24. reduced front-content dominance on back-facing strips and rebalanced backside paper shade;
25. made material haptics foreground/attachment-safe and gave synthesized material sounds deterministic material/action timbre seeds;
26. enforced one large Paper snapshot buffer across Legacy/Material A/B switching;
27. made the Material renderer honor High Contrast by reducing surface noise while strengthening structural edge/contact cues;
28. density-normalized remaining fibre strokes for Papyrus/Manuscript;
29. added regression contracts for corner stabilization, backside transmission, high-refresh velocity smoothing and reversal response;
30. hoisted material invariants out of the per-strip render loop to reduce repeated per-frame work.

Arena also refined Level 21 after testing: meaningful direction reversals receive higher trust than same-direction noise so reverse-cancel stays responsive.

## Polish and debug pass

A dedicated post-Level-30 debug pass found and fixed lifecycle/state-machine issues that visual review alone would not expose:

- committed tap/key Paper animations are now cancelled cleanly during mode handoff instead of continuing to mutate visual progress after the state was cleared;
- committed drag animations receive the same cancellation treatment during Reader mode changes;
- force-cancel restores the exact captured start locator whenever one exists, even if cancellation races the preview-navigation result flag;
- `cancelPendingTurnAndAwait()` keeps a reference to an already-running completion job so durable handoff actually waits for cancelled work to finish;
- Material review/runtime state now sanitizes display density, inspection progress, vertical bias, pull origin and release velocity against non-finite values;
- dead Paper commit serialization was removed.

The local execution environment could not clone GitHub because outbound DNS/network access is unavailable. GitHub-hosted workflows remain the authoritative build route, but those jobs are still failing before step execution.

## Existing protections retained

- Legacy Paper implementation is still present.
- Production rollout default remains OFF.
- PDF ownership is unchanged.
- Readium navigation/persistence ownership is unchanged.
- Cancel restores the exact starting locator.
- Durable close can synchronously cancel an uncommitted preview.
- Selection and TalkBack ownership remain with the renderer.
- LTR/RTL direction mapping remains the existing Reader contract.
- PAGED / PAPER_CURL / SLIDE / SCROLL remain distinct.
- Slide remains a low-latency translation mode and does not use paper physics.

## P2 / device-judgment items still open

These should not be "fixed" blindly in source without hands-on evidence:

- 26 perspective strip mappings need real 60/90/120 Hz frame timing on representative phones.
- Strip seams/texture filtering need close visual inspection on OLED and high-DPI LCD displays.
- Glossy highlight intensity needs low-brightness review.
- Papyrus fibre visibility must remain perceptible without becoming noisy.
- Material sound balance needs phone-speaker and headphone judgment.
- OEM haptic differentiation needs physical-device judgment; system haptic primitives intentionally remain authoritative.
- Snapshot prewarm memory pressure should be checked during rotation and repeated Reader reopen.
- The renderer/state file is still large enough to justify a later structural split once behavior is proven; avoid refactoring it purely for aesthetics before compile/device evidence.

## CI blocker

Latest inspected workflows still fail before execution:

- Android CI: `steps=[]`
- Performance Benchmarks: `steps=[]`

No Checkout, Gradle, unit test, lint, assemble, or benchmark step starts. This is runner allocation/infrastructure failure, not evidence of a source compile failure.

## Arena verdict

**Do not promote Material Page Engine to production Paper yet.**

The source implementation is materially stronger after this pass and no obvious P0 remains, but promotion requires:

1. a real CI run that executes Gradle;
2. unit + lint + debug + debugAndroidTest gates;
3. real-device A/B review against legacy Paper;
4. frame pacing evidence;
5. material sensory tuning on hardware.

Until those gates exist, keep PR #372 draft and keep the production rollout OFF.
