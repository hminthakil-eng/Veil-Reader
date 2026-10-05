#!/bin/sh
set -eu

fail() {
  echo "CANONICAL PAPER CONTRACT FAILED: $1" >&2
  exit 1
}

legacy_draw="app/src/main/java/com/veilreader/app/ui/screens/PaperCurlDraw.kt"
legacy_geometry="app/src/main/java/com/veilreader/app/ui/screens/PaperCurlGeometry.kt"
gpu="app/src/main/java/com/veilreader/app/ui/reader/material/GpuMaterialPageCurlView.kt"
engine="app/src/main/java/com/veilreader/app/ui/reader/material/MaterialPageEngine.kt"
input="app/src/main/java/com/veilreader/app/ui/screens/PaperCurlInputListener.kt"

[ ! -e "$legacy_draw" ] || fail "legacy Canvas PaperCurlDraw runtime returned"
[ ! -e "$legacy_geometry" ] || fail "legacy Canvas PaperCurlGeometry runtime returned"
[ -f "$gpu" ] || fail "canonical GPU renderer is missing"
[ -f "$engine" ] || fail "canonical Material Page engine is missing"

if grep -nE 'uBackTexture|uHasBackTexture|destinationInk|backSnapshot|captureBack\('   "$gpu" "$engine" "$input"; then
  fail "destination-as-back-leaf path returned"
fi

grep -q 'glGenTextures(1, ids, 0)' "$gpu" ||
  fail "canonical Paper renderer must keep one leaf texture"

grep -q 'shouldAllowPaperNavigation' "$input" ||
  fail "normal-motion Paper fail-closed gate is missing"

grep -q 'GpuMaterialPageRendererStatus.READY' "$input" ||
  fail "Paper input no longer requires renderer readiness"

grep -q 'GpuMaterialPageRendererStatus.INITIALIZING' "$gpu" ||
  fail "GPU initialization lifecycle state is missing"

snapshot="app/src/main/java/com/veilreader/app/ui/reader/material/MaterialPageSnapshot.kt"
model="app/src/main/java/com/veilreader/app/ui/reader/material/GpuPageCurlModel.kt"
benchmark_activity="app/src/benchmark/java/com/veilreader/app/benchmark/BenchmarkReaderActivity.kt"

[ -f "$snapshot" ] || fail "Paper snapshot freshness contract is missing"
[ -f "$model" ] || fail "Paper GPU geometry model is missing"

grep -q 'awaitMaterialPageSourceVisualReady' "$snapshot" ||
  fail "Paper warm capture no longer waits for WebView visual readiness"

grep -q 'MATERIAL_PAGE_PREPARED_SNAPSHOT_MAX_AGE_NANOS' "$snapshot" ||
  fail "Paper warm snapshot age bound is missing"

grep -q 'gpuCylinderXForFreeEdge' "$model" ||
  fail "finger-to-free-edge geometry constraint is missing"

grep -q 'gpuMaterialFrameMatchesRendererGeneration' "$gpu" ||
  fail "stale GL-context frame fence is missing"

# READY must not reset retries; a successfully drawn frame may do so. Scope
# this guard to the readiness callback instead of rejecting both owners.
ready_callback=$(sed -n '/onRendererReady = { ready ->/,/onRendererFailure = {/p' "$gpu")
[ -n "$ready_callback" ] || fail "GPU readiness callback is missing"
if printf '%s\n' "$ready_callback" | grep -Eq 'rendererFailureCount[.]intValue[[:space:]]*=[[:space:]]*0'; then
  fail "GPU recovery counter is reset on READY and can retry forever"
fi

grep -q 'paper.gpu.texture_upload' "$gpu" ||
  fail "GPU texture upload trace is missing"

grep -q 'PageTurnStyle.PAPER' "$benchmark_activity" ||
  fail "benchmark Reader is not exercising canonical Paper"

grep -q 'awaitSheetPresented' "$input" || fail "Paper navigation lost the acquired-buffer barrier"
grep -q 'eglPresentationTimeANDROID' "$gpu" || fail "Paper acquired-buffer timestamp correlation is missing"
grep -q 'gpuMaterialSheetPresentationMatches' "$gpu" || fail "Paper presentation context/viewport fencing is missing"

echo "Canonical Paper source contract: PASS"
