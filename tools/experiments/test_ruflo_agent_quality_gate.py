"""Adversarial tests for Ruflo report screening, including a prior real-model failure."""
import unittest

from ruflo_agent_quality_gate import evaluate, HEAD_SHA, BASE_SHA, WARNINGS_HEADER

GOOD = f"""# RFC: Verified-snapshot investigation

## CI investigator
Observation: GitHub accepted the Gradle submission, but the exact dependency
comparison did not expose a complete head snapshot.
The source HEAD SHA is {HEAD_SHA}; base SHA is {BASE_SHA}.
The exact API to query is
GET /repos/hminthakil-eng/Veil-Reader/dependency-graph/compare/{BASE_SHA}...{HEAD_SHA}?per_page=5
Require {WARNINGS_HEADER} to be present, valid base64 and empty.
Hypothesis (unproven): the head workflow submitted the correct SHA under the
wrong ref. In a separate PR compare refs/pull/509/merge with
refs/heads/reader/native-paper-rebuild-20261011 using exact checkout.

## Skeptical reviewer
Reject absent/invalid headers or warnings, and block release until a complete
comparison and independent policy review are successful.
Alternative explanation: ingestion delay or deduplication collision.
No production policy may be bypassed.
"""

# This captures the concrete mistake from the real Qwen2.5-Coder 3B report.
REAL_MODEL_FAILURE = f"""# Ruflo engineering-quality cloud pilot

## CI investigator
Confirmed Observation: dependency review failed for {HEAD_SHA}.
Hypothesis: the compare API does not have the exact HEAD snapshot.
Proposed Experiment: ref refs/pull/509/merge
GET /repos/{{owner}}/{{repo}}/compare/{{base}}...{{head}}

## Skeptical reviewer
Alternative causes include latency. Reject incomplete snapshots.
GET /repos/{{owner}}/{{repo}}/compare/{{base}}...{{head}}
"""


class GateTest(unittest.TestCase):
    def test_good_sample_is_only_static_pass_and_never_approved(self):
        result = evaluate(GOOD)
        self.assertEqual(result["status"], "PASS_STATIC_ONLY", result)
        self.assertTrue(result["static_checks_passed"])
        self.assertFalse(result["approved_for_merge"])

    def test_actual_model_error_rejected(self):
        result = evaluate(REAL_MODEL_FAILURE)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("wrong commits-compare API cited as a dependency snapshot check", result["issues"])
        self.assertFalse(result["approved_for_merge"])

    def test_correct_endpoint_elsewhere_does_not_excuse_wrong_one(self):
        result = evaluate(GOOD + "\nGET /repos/owner/repo/compare/x...y\n")
        self.assertEqual(result["status"], "FAIL")

    def test_missing_base_sha_is_rejected(self):
        result = evaluate(GOOD.replace(BASE_SHA, "wrong-sha"))
        self.assertEqual(result["status"], "FAIL")

    def test_missing_warning_header_is_rejected(self):
        result = evaluate(GOOD.replace(WARNINGS_HEADER, "generic-warning"))
        self.assertEqual(result["status"], "FAIL")

    def test_missing_second_reviewer_is_rejected(self):
        result = evaluate(GOOD.split("## Skeptical reviewer")[0])
        self.assertEqual(result["status"], "FAIL")

    def test_repeated_second_reviewer_is_rejected(self):
        result = evaluate(GOOD + "\n## Skeptical reviewer\nReviewer duplicated.")
        self.assertEqual(result["status"], "FAIL")

    def test_only_merge_ref_is_rejected(self):
        result = evaluate(GOOD.replace("refs/heads/reader/native-paper-rebuild-20261011", "refs/pull/509/merge"))
        self.assertEqual(result["status"], "FAIL")

    def test_missing_fail_closed_is_rejected(self):
        result = evaluate(GOOD.replace("Reject", "Allow").replace("block", "permit").replace("No production policy may be bypassed.", "No concerns."))
        self.assertEqual(result["status"], "FAIL")


if __name__ == "__main__":
    unittest.main()
