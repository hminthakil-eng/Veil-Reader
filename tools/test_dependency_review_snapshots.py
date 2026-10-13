import base64
import unittest
from email.message import Message

from verify_dependency_review_snapshots import (
    WARNINGS_HEADER,
    comparison_fetcher,
    snapshot_warnings,
    verify_review_summary,
    wait_for_snapshots,
)


class SnapshotReadinessTest(unittest.TestCase):
    def headers(self, warning=""):
        return {WARNINGS_HEADER: base64.b64encode(warning.encode()).decode()}

    def wait(self, responses, timeout=20):
        elapsed = [0]
        values = iter(responses)

        def sleep(seconds):
            elapsed[0] += seconds

        wait_for_snapshots(lambda: next(values), timeout=timeout,
                           clock=lambda: elapsed[0], sleep=sleep)
        return elapsed[0]

    def test_complete_empty_delta_can_be_ready(self):
        self.assertEqual(0, self.wait([self.headers()]))

    def test_missing_head_then_ingestion_is_ready(self):
        self.assertEqual(10, self.wait([
            self.headers("No snapshots were found for the head SHA"), self.headers(),
        ]))

    def test_missing_base_times_out_instead_of_proceeding(self):
        with self.assertRaisesRegex(ValueError, "incomplete after 20s"):
            self.wait([self.headers("No snapshots were found for the base SHA")] * 3)

    def test_other_snapshot_warnings_also_prevent_success(self):
        with self.assertRaisesRegex(ValueError, "incomplete after 0s"):
            self.wait([self.headers("The detectors do not match")], timeout=0)

    def test_missing_or_malformed_readiness_is_not_success(self):
        for headers in ({}, {WARNINGS_HEADER: "invalid*"},
                        {WARNINGS_HEADER: base64.b64encode(b"\xff").decode()}):
            with self.subTest(headers=headers), self.assertRaises(ValueError):
                snapshot_warnings(headers)

    def test_real_http_header_casing_and_empty_value(self):
        headers = Message()
        headers["X-GitHub-Dependency-Graph-Snapshot-Warnings"] = ""
        self.assertEqual("", snapshot_warnings(headers))
        headers.replace_header("X-GitHub-Dependency-Graph-Snapshot-Warnings",
                               self.headers("No snapshots were found for the head SHA")[WARNINGS_HEADER])
        self.assertEqual("No snapshots were found for the head SHA", snapshot_warnings(headers))

    def test_action_success_with_snapshot_warnings_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "incomplete"):
            verify_review_summary("<h2>Snapshot Warnings</h2><blockquote>No head snapshot</blockquote>")

    def test_missing_action_result_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "did not provide"):
            verify_review_summary("")
        verify_review_summary("<h2>Dependency Review</h2><p>No vulnerable changes</p>")

    def test_branch_names_cannot_replace_exact_source_shas(self):
        with self.assertRaisesRegex(ValueError, "exact base and head"):
            comparison_fetcher("owner/repo", "main", "a" * 40, "test-token", "https://api.github.com")


if __name__ == "__main__":
    unittest.main()
