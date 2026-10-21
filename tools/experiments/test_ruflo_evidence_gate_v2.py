#!/usr/bin/env python3
"""Adversarial regression tests for evidence-based agent quality gate."""
import unittest
from ruflo_evidence_gate_v2 import audit, BASE, HEAD, HEADER, HEAD_BRANCH, MERGE_REF, EXPECTED_PATH

GOOD = f'''## CI investigator
Observation: successful submission differs from visibility in the dependency comparison API.
The base {BASE} and head {HEAD} are exact SHAs; the logs show {MERGE_REF}.
Hypothesis: testing {HEAD_BRANCH} for the head submission could address ref identity, but is unproven.
Experiment: use an isolated branch and test GET /repos/hminthakil-eng/Veil-Reader{EXPECTED_PATH}?per_page=5.
Inspect the {HEADER} for missing or incomplete snapshots. Never weaken the gate.
## Skeptical reviewer
Alternative risk: asynchronous ingestion or wrong correlator could also cause missing snapshots.
The correct call is GET /repos/hminthakil-eng/Veil-Reader{EXPECTED_PATH}?per_page=5.
Check {HEADER}; accept readiness only if it is present and empty and policy review passes.
Decision: BLOCK until the comparison is complete and verified.
'''

class AuditTests(unittest.TestCase):
    def assert_rejected(self, s):
        result = audit(s)
        self.assertEqual('FAIL', result['status'], result)
        self.assertFalse(result['merge_authorized'])

    def test_grounded_candidate_passes_contract_only(self):
        result = audit(GOOD)
        self.assertEqual('PASS', result['status'], result)
        self.assertFalse(result['merge_authorized'])

    def test_wrong_normal_compare_api_rejected_even_with_correct_one(self):
        self.assert_rejected(GOOD + 'GET /repos/{owner}/{repo}/compare/{base}...{head}\n')

    def test_missing_exact_dependency_endpoint_in_reviewer(self):
        before, middle, after = GOOD.partition('## Skeptical reviewer')
        self.assert_rejected(before + middle + after.replace(EXPECTED_PATH, '/dependency-graph/compare/main...branch'))

    def test_missing_header_in_reviewer_rejected(self):
        before, middle, after = GOOD.partition('## Skeptical reviewer')
        self.assert_rejected(before + middle + after.replace(HEADER, 'some-other-header'))

    def test_unproven_ref_alternative_omitted(self):
        self.assert_rejected(GOOD.replace(HEAD_BRANCH, MERGE_REF))

    def test_no_block_decision_rejected(self):
        self.assert_rejected(GOOD.replace('Decision: BLOCK', 'Decision: MERGE'))

    def test_empty_or_missing_reviewer_rejected(self):
        self.assert_rejected(GOOD.split('## Skeptical reviewer')[0])

    def test_copying_only_conclusions_without_proof_rejected(self):
        self.assert_rejected('## CI investigator\nLooks good.\n## Skeptical reviewer\nLooks good.\n')

    def test_previous_bad_model_excerpt_rejected(self):
        from pathlib import Path
        path = Path(__file__).parent / 'ruflo_3b_wrong_endpoint_excerpt.md'
        self.assert_rejected(path.read_text(encoding='utf-8'))

if __name__ == '__main__':
    unittest.main()
