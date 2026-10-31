"""Negative controls for fail-closed Dependency Review output validation."""

import importlib.util
import pathlib
import unittest

MODULE = pathlib.Path(__file__).resolve().parents[1] / "scripts/ci/check_dependency_review_summary.py"
spec = importlib.util.spec_from_file_location("dependency_gate", MODULE)
gate = importlib.util.module_from_spec(spec)
assert spec and spec.loader
spec.loader.exec_module(gate)


class DependencyReviewSummaryGateTest(unittest.TestCase):
    def test_clean_comparison_passes(self):
        self.assertTrue(gate.validate("<h1>Dependency Review</h1><p>No issues found.</p>")[0])

    def test_missing_output_fails(self):
        self.assertFalse(gate.validate("")[0])

    def test_non_review_output_fails(self):
        self.assertFalse(gate.validate("<h2>Unrelated</h2>")[0])

    def test_missing_head_snapshot_fails(self):
        summary = ("<h1>Dependency Review</h1><h2>Snapshot Warnings</h2>"
                   "<blockquote>No snapshots were found for the head SHA.</blockquote>")
        self.assertFalse(gate.validate(summary)[0])

    def test_missing_base_snapshot_fails(self):
        summary = ("<h1>Dependency Review</h1><h2>Snapshot Warnings</h2>"
                   "<blockquote>No snapshots were found for the base SHA.</blockquote>")
        self.assertFalse(gate.validate(summary)[0])

    def test_timeout_with_warnings_fails(self):
        summary = ("<h1>Dependency Review</h1><h2>Snapshot Warnings</h2>"
                   "<p>Retry timeout exceeded. Proceeding...</p>")
        self.assertFalse(gate.validate(summary)[0])

    def test_case_and_html_attributes(self):
        summary = '<h1 class="x">Dependency Review</h1><h2 id="y">SNAPSHOT WARNINGS</h2>'
        self.assertFalse(gate.validate(summary)[0])


if __name__ == "__main__":
    unittest.main()
