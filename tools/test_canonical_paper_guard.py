#!/usr/bin/env python3
"""Exercise the real source guard against positive and mutated source fixtures."""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GPU = "app/src/main/java/com/veilreader/app/ui/reader/material/GpuMaterialPageCurlView.kt"
READER = "app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt"

class CanonicalPaperGuardTest(unittest.TestCase):
    def run_guard(self, mutation=None, source=GPU):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shutil.copytree(ROOT / "app/src/main", root / "app/src/main")
            shutil.copytree(ROOT / "app/src/benchmark", root / "app/src/benchmark")
            path = root / source
            if mutation:
                path.write_text(mutation(path.read_text()))
            return subprocess.run(["sh", str(ROOT / "tools/verify-canonical-paper.sh")],
                                  cwd=root, capture_output=True, text=True)

    def test_real_frame_success_may_reset_retry_budget(self):
        result = self.run_guard()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_reintroducing_paper_static_fallback_is_rejected(self):
        result = self.run_guard(
            lambda text: text.replace(
                "paperListener?.performDiscreteTurn(direction) == true",
                "performDirectPagedTurn()"
            ),
            source=READER,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("exclusive Paper owner", result.stderr)

    def test_legacy_paper_static_fallback_path_is_rejected(self):
        result = self.run_guard(
            lambda text: text.replace(
                "fun paperModeSelected(): Boolean =",
                "fun paperNeedsStaticFallback(): Boolean = false\\n            "
                "fun paperModeSelected(): Boolean =",
            ),
            source=READER,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("StaticPaged or directional fallback", result.stderr)

    def test_ready_only_reset_is_rejected(self):
        result = self.run_guard(lambda s: s.replace("onRendererReady = { ready ->",
            "onRendererReady = { ready ->\n                    rendererFailureCount.intValue = 0"))
        self.assertNotEqual(0, result.returncode)
        self.assertIn("reset on READY", result.stderr)

    def test_missing_buffer_timestamp_contract_is_rejected(self):
        result = self.run_guard(lambda s: s.replace("eglPresentationTimeANDROID", "removedTimestampContract"))
        self.assertNotEqual(0, result.returncode)
        self.assertIn("timestamp correlation is missing", result.stderr)

    def test_missing_readiness_owner_is_rejected(self):
        result = self.run_guard(lambda s: s.replace("onRendererReady = { ready ->", "removed = { ready ->"))
        self.assertNotEqual(0, result.returncode)
        self.assertIn("callback is missing", result.stderr)

if __name__ == "__main__":
    unittest.main()
