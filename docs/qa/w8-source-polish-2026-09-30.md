# W8 source polish: illustration inspection and adaptive controls

Date: 2026-09-30
Base: 41381a8a7470dd2c6136345e82104cb3f030e104
Track: alpha/w8-convergence-sync / PR #330
Scope: source development; no APK, AAB, Gradle build, or device verification.

## Fixed behavior

- Image-load cancellation now retains ownership of a decoded bitmap across the IO dispatcher handoff. Untransferred bitmaps are recycled in finally, including stale requests. Successful handoff leaves viewer disposal responsible for the bitmap.
- Illustration resource reading and decoding happen on Dispatchers.IO.
- Pinch zoom preserves the image point under the gesture centroid until a real fitted-image boundary is reached.
- Pan limits use ContentScale.Fit geometry: letterboxed images cannot pan along an axis with no image overflow.
- Image inspection uses measured header, image area, and controls rather than overlays with a fixed 58dp clearance.
- Image area clips its transformed content. Viewport changes reclamp pan.
- Localized zoom slider, percent, and reset allow inspection without a pinch gesture; image semantics have a title fallback.
- Settings typography labels have explicit room, nullable values/reset occupy a separate row, and content is capped at 840dp on wider displays.
- Settings switches expose one full-row toggle target and merged semantics, using the existing localized title and description.
- Width caps precede fillMaxWidth in metadata editing, footnote, note editing, and highlight deletion chambers.

## Completed source checks

- 1072 unique EN resource keys and 1072 matching FA keys; no duplicates or one-sided keys.
- String references resolve in the five modified UI files.
- No conflict markers or whitespace errors in those diffs.
- Reviewed diff and preserved the existing Grayfog palette, art, publication text, and four navigation owners.

## Regression coverage added, not executed

ReaderImageTransformTest has nine cases covering fit reset, wide/tall letterboxing, off-center centroid anchoring, existing pan plus gesture translation, returning to fit, maximum zoom, malformed geometry, and very large decode sampling.

Kotlin compilation and JUnit execution have not run in this environment. No runtime, performance, visual, or accessibility GREEN status is claimed.

## Next execution gate

When build/device work is resumed:
1. Run ReaderImageTransformTest plus existing reader input, close durability, handoff, and preference regressions.
2. Inspect wide/tall/square illustrations at fit through 5x; test rotation and slider/TalkBack operation.
3. Cancel or dismiss during resource reading, decode, and immediate repeat taps; verify memory ownership and no late viewer.
4. Inspect all modified chambers and Settings at compact/expanded widths, landscape, FA/EN, and increased font scale.
5. Compare device captures with the pinned reference boards. This source pass does not establish rendered fidelity.

## Follow-up: note reward integrity

- Reproduced: 80 Persian half-spaces qualified as a substantial note in the original ReadingPolicy.
- qualifiesNote now counts visible Unicode code points, ignoring whitespace, control/format characters, and standalone combining marks. Surrogate pairs count once.
- Existing note text is unchanged. Existing credited rewards are preserved. Only future qualification is affected; forty visible characters remain the threshold.
- Executed the repository's dependency-free Java policy suite using JDK 17 compiler module and runtime. Before change: 39 checks passed. New regression failed against original policy. After fix: all 46 checks passed.
- Coverage includes Unicode space and format padding, short padded notes, interior-space padding, standalone accents, supplementary characters, and Persian letters.
- This Java result does not validate Android UI, Kotlin compilation, image JUnit tests, rendering, memory behavior on a device, or the whole application.
