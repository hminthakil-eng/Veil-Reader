#!/usr/bin/env python3
"""Exercise the real source guard against positive and mutated source fixtures."""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GPU = "app/src/main/java/com/veilreader/app/ui/reader/material/GpuMaterialPageCurlView.kt"

class CanonicalPaperGuardTest(unittest.TestCase):
    def run_guard(self, mutation=None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shutil.copytree(ROOT / "app/src/main", root / "app/src/main")
            shutil.copytree(ROOT / "app/src/benchmark", root / "app/src/benchmark")
            path = root / GPU
            if mutation:
                path.write_text(mutation(path.read_text()))
            return subprocess.run(["sh", str(ROOT / "tools/verify-canonical-paper.sh")],
                                  cwd=root, capture_output=True, text=True)

    def test_real_frame_success_may_reset_retry_budget(self):
        result = self.run_guard()
        self.assertEqual(0, result.returncode, result.stderr)

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
