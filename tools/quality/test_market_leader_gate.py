"""Unit tests for the evidence gate, not for the Android application."""
from __future__ import annotations
import copy
import json
from pathlib import Path
import sys
import unittest

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from market_leader_gate import evaluate

ROOT = HERE.parents[1]
FIXTURE = json.loads((ROOT / "quality/market-leader/benchmark-contract.json").read_text(encoding="utf-8"))

class MarketLeaderGateTest(unittest.TestCase):
    def test_current_contract_remains_blocked_without_device_or_visual_data(self):
        report = evaluate(FIXTURE, ROOT)
        self.assertEqual(report["decision"], "BLOCKED")
        self.assertFalse(report["release_ready"])
        self.assertFalse(report["integrity_errors"])
        self.assertEqual(len(report["source_checks"]), 5)
        self.assertTrue(all(x["static_match"] for x in report["source_checks"]))
        self.assertGreaterEqual(len(report["unresolved"]), 8)

    def test_cannot_claim_pass_without_evidence(self):
        changed = copy.deepcopy(FIXTURE)
        changed["tests"][0]["status"] = "measured-pass"
        report = evaluate(changed, ROOT)
        self.assertTrue(any("Unproven PASS" in e for e in report["integrity_errors"]))
        self.assertFalse(report["release_ready"])

    def test_global_gate_cannot_turn_green_without_proof(self):
        changed = copy.deepcopy(FIXTURE)
        changed["global_gates"]["android_device_evidence"] = "measured-pass"
        report = evaluate(changed, ROOT)
        self.assertTrue(any("Unproven global PASS" in e for e in report["integrity_errors"]))
        self.assertFalse(report["release_ready"])

    def test_duplicate_cases_are_invalid(self):
        changed = copy.deepcopy(FIXTURE)
        changed["tests"].append(copy.deepcopy(changed["tests"][0]))
        report = evaluate(changed, ROOT)
        self.assertTrue(any("duplicate" in e for e in report["integrity_errors"]))

if __name__ == "__main__":
    unittest.main()
