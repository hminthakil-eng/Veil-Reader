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

echo "Canonical Paper source contract: PASS"
