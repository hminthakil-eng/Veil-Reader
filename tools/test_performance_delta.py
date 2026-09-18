#!/usr/bin/env python3

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("check_performance_delta.py")
SPEC = importlib.util.spec_from_file_location("check_performance_delta", MODULE_PATH)
assert SPEC and SPEC.loader
delta = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(delta)


def snapshot(**values):
    return {
        "schemaVersion": 1,
        "mode": "smoke",
        "commit": "test",
        "device": "emulator",
        "metrics": {key: {"value": value} for key, value in values.items()},
    }


class PerformanceDeltaTest(unittest.TestCase):
    def test_percent_regression_passes_within_limit(self):
        baseline = snapshot(startup=100.0)
        current = snapshot(startup=109.0)
        rules = [{"id": "startup", "maxRegressionPercent": 10.0}]

        passes, failures = delta.evaluate(baseline, current, rules)

        self.assertEqual(1, len(passes))
        self.assertEqual([], failures)

    def test_percent_regression_fails_over_limit(self):
        baseline = snapshot(startup=100.0)
        current = snapshot(startup=111.0)
        rules = [{"id": "startup", "maxRegressionPercent": 10.0}]

        passes, failures = delta.evaluate(baseline, current, rules)

        self.assertEqual([], passes)
        self.assertEqual(1, len(failures))
        self.assertIn("11.00%", failures[0])

    def test_absolute_increase_handles_negative_frame_overrun(self):
        baseline = snapshot(frame=-4.0)
        current = snapshot(frame=1.0)
        rules = [{"id": "frame", "maxIncrease": 4.0}]

        passes, failures = delta.evaluate(baseline, current, rules)

        self.assertEqual([], passes)
        self.assertEqual(1, len(failures))
        self.assertIn("increase 5.000", failures[0])

    def test_zero_baseline_does_not_hide_regression(self):
        self.assertEqual(float("inf"), delta.regression_percent(0.0, 1.0))
        self.assertEqual(0.0, delta.regression_percent(0.0, 0.0))

    def test_missing_metric_fails_closed(self):
        baseline = snapshot(startup=100.0)
        current = snapshot(other=100.0)
        rules = [{"id": "startup", "maxRegressionPercent": 10.0}]

        passes, failures = delta.evaluate(baseline, current, rules)

        self.assertEqual([], passes)
        self.assertEqual(1, len(failures))
        self.assertIn("missing from current", failures[0])


if __name__ == "__main__":
    unittest.main()
