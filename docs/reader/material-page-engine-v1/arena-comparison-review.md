# Arena comparison review — 2026-10-04

This applies the repository's Arena review criteria to the cylindrical engine.
An independent Arena tool/skill is not available in this session; this document
records actual inspection, reproduced defects and checks, not an invented external
review or a count of ceremonial polish passes. See [source comparison](reference-comparison.md).

## Findings and disposition

| Priority | Finding | Disposition / evidence |
| --- | --- | --- |
| P1 | Projected whole-mesh clipping leaks occluded front ink into the RTL backside | Reproduced pure RED instead of BLUE in native Skia against the previous renderer. Separate clipped face triangles now pass both-direction regression |
| P1 | New triangle API is not hardware accelerated on supported Android 8/9 phones | Official Android support table checked. API 26–28 uses live-edge, no preview/snapshot or forced software render. Policy guards Paper/PDF/Slide separation |
| P1 | Reflow/rotation could stretch a stale captured publication | Captured viewport check suppresses obsolete mesh while real Reader cancellation/navigation remains authoritative; exact-locator restore regression passes |
| P2 | Diagonal fold can lift the binding endpoint on extreme aspect ratios | Derived binding-reach slope bound; tall/narrow + corner + tilt + RTL sweep protects the flat binding |
| P2 | Reduced/no-capture path can change material during a reserved drag | Transaction profile freezes before capture; reduced-motion preference-change regression passes |
| P2 | Cancel feedback arrives after settle instead of at release | Cue follows exact source restoration and precedes animation; coroutine test checks active settle and one cue only |
| P2 | Invalid release endpoints can escape finite animation guarantees | Normalize captured endpoints once; non-finite input and terminal-state test passes |
| P2 | Idle rendering needlessly modulates publication pixels | Exact resting bitmap draw, no material grain or tint on idle ink; native source/terminal pixel checks pass |
| P2 | New face arrays could churn on repeated turns | Renderer and bounded primitive arrays prepared in material Paper overlay and retained across turns; legacy/live-edge do not allocate them. Buffer identity and source-area tests pass |
| P2 | High Contrast does not calm the new material surface | New renderer observes existing High Contrast theme local and attenuates grain, ghosting and tonal shade, retaining the physical edge |

## Architecture judgment

- Geometry remains an inextensible cylindrical approximation; it is separate from
  face partitioning and material rendering. Front/back ownership is now explicit.
- Navigation preview, exact-locator restoration, durable commit/counting, lifecycle
  cancellation and busy guards remain in the existing Readium adapter.
- PDF and selection/TTS/Notes ownership is not replaced by screenshot logic.
- Two prior Paper renderers are retained. New mesh remains a persistent opt-in.
- Slide remains direct translation and its own timing policy, not paper mechanics.
- Mesh, shader, paths, paints, UVs and colors are reused; no PCM or bitmap generation
  happens on a draw frame. Approximately 0.88 MiB of reserved face arrays is bounded.

## Evidence and acceptance

The [current verification report](verification.md) records exact full-suite, Reader,
lint and APK results. The [gallery](review/gallery.html) was regenerated with the
final production renderer, including RTL/Persian, dark/sepia and release sequences.
The upstream StPageFlip geometry smoke test ran 1,188 accepted finite cases.

No known source-level blocker remains from the scoped comparison. This does not
certify every Reader/device behavior. No connected instrumentation, sustained GPU
trace, real WebView capture, listening test or actuator judgment occurred.

**Verdict: keep the new engine opt-in.** Do not promote on software screenshots.
Use [phone acceptance](review.md), especially API 26/28 live-edge and API 29+ mesh,
60/90/120 Hz phones, rotation while previewing, cancellation/TTS interaction and
speaker/headphone/OEM haptic quality. Existing Reader regressions and both APKs
must remain green; real-device superiority is still required for default promotion.
