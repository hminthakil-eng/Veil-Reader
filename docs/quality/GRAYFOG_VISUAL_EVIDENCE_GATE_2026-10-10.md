# Grand Forge Grayfog screenshot evidence gate — 2026-10-10

## The concrete gap

`storage-instrumentation.yml` already runs full emulator UI tests including
`GrayfogShellAccessibilityTest` and `DialogVisualEvidenceTest`. It previously
uploaded PNG artifacts using `if: always()`, while the emulator script treated
the capture TAR as optional. A green storage run could therefore omit the
**reproducible screenshot evidence** (or silently carry incomplete captures).

## Bounded implementation

- `tools/verify_grayfog_capture_evidence.py` reads only the known local
  `build/reports/grayfog-review.tar`, without extracting files.
- Requires **96 Grayfog shell images** (six scenes × EN/FA × 100/130/150/200%
  text × standard/high contrast) and **24 Android Dialog images**
  (three dialogs × EN/FA × 100/200% text × standard/high contrast):
  **120 original fixture captures total**.
- Rejects missing/duplicate filenames, unsafe paths, symlinks, unexpected
  archive entries, implausibly empty or oversized PNGs, corrupt PNG checksums,
  and unsupported encoding in the sampled comparison images.
- Compares decoded RGB pixels at 100% vs 200% in three selected actual exported
  images to reject identical/no-op font-scale captures. This is a small
  **regression oracle**, not a visual approval against a designer's golden.
- Logs a JSON SHA-256 manifest, counts and measured sample changes in
  `build/reports/grayfog-evidence-verification.json`. Upload retains the
  report whether pass or fail.
- No Android app code, dependencies, screenshots of the user's library,
  remote telemetry, proprietary competitor content, secrets or permissions.

## Validation

1. `python3 -m unittest discover -s tools/tests -p test_grayfog_capture_evidence.py -v`
   runs ten synthetic positive/negative tests entirely offline.
2. The new CI step must run **after** emulator cleanup exports the TAR. It
   fails the Storage Instrumentation workflow when the corpus is incomplete,
   even if the Android tests themselves pass.
3. Exact PR-head Storage Instrumentation and Android CI must pass before
   considering this draft. If it exposes missing captures, treat the
   failure as evidence and fix the export or fixture instead of suppressing
   the check or claiming GREEN.
4. Compare reported image hashes and test XML against the exact source SHA.
   The gate verifies images **from an emulator**, not a Samsung physical
   device, optical Paper Curl, page-turn timing, TalkBack, or artistic quality.
   Page-engine physical verification remains independently required.

## Rollback

Revert this isolated PR. Prior review PNG generation and Android
instrumentation behavior are unchanged.
