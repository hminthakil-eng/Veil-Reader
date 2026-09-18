#!/usr/bin/env python3

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("check_performance_budget.py")
SPEC = importlib.util.spec_from_file_location("check_performance_budget", MODULE_PATH)
assert SPEC and SPEC.loader
budget = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(budget)


SAMPLE = {
    "benchmarks": [
        {
            "name": "EMULATOR_coldStartupBaselineProfile",
            "className": "com.veilreader.benchmark.StartupBenchmark",
            "metrics": {
                "timeToInitialDisplayMs": {
                    "minimum": 410.0,
                    "maximum": 520.0,
                    "median": 460.0,
                    "runs": [410.0, 460.0, 520.0],
                }
            },
            "sampledMetrics": {},
        },
        {
            "name": "EMULATOR_pageTurns",
            "className": "com.veilreader.benchmark.ReaderFrameBenchmark",
            "metrics": {
                "frameCount": {
                    "minimum": 80.0,
                    "maximum": 90.0,
                    "median": 85.0,
                    "runs": [80.0, 85.0, 90.0],
                }
            },
            "sampledMetrics": {
                "frameOverrunMs": {
                    "P50": -8.0,
                    "P90": -2.0,
                    "P95": -0.5,
                    "P99": 5.0,
                    "runs": [[-8.0, -2.0, -0.5, 5.0]],
                },
                "frameDurationCpuMs": {
                    "P50": 5.0,
                    "P90": 8.0,
                    "P95": 10.0,
                    "P99": 15.0,
                    "runs": [[5.0, 8.0, 10.0, 15.0]],
                },
            },
        },
    ]
}


class PerformanceBudgetTest(unittest.TestCase):
    def write_result(self, root: Path) -> Path:
        path = root / "sample-benchmarkData.json"
        path.write_text(json.dumps(SAMPLE), encoding="utf-8")
        return path

    def test_finds_nested_macrobenchmark_json(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            nested = root / "outputs" / "device"
            nested.mkdir(parents=True)
            result = self.write_result(nested)

            self.assertEqual([result.resolve()], budget.find_result_files([root]))

    def test_suffix_matching_accepts_emulator_prefixed_test_names(self):
        check = {
            "classSuffix": ".ReaderFrameBenchmark",
            "benchmarkNameSuffix": "pageTurns",
        }
        self.assertTrue(budget.matches(SAMPLE["benchmarks"][1], check))

    def test_physical_style_checks_pass_for_good_sample(self):
        checks = [
            {
                "id": "startup",
                "classSuffix": ".StartupBenchmark",
                "benchmarkNameSuffix": "coldStartupBaselineProfile",
                "metricGroup": "metrics",
                "metric": "timeToInitialDisplayMs",
                "stat": "median",
                "max": 600.0,
            },
            {
                "id": "frame_p95",
                "classSuffix": ".ReaderFrameBenchmark",
                "benchmarkNameSuffix": "pageTurns",
                "metricGroup": "sampledMetrics",
                "metric": "frameOverrunMs",
                "stat": "P95",
                "max": 0.0,
            },
            {
                "id": "frame_count",
                "classSuffix": ".ReaderFrameBenchmark",
                "benchmarkNameSuffix": "pageTurns",
                "metricGroup": "metrics",
                "metric": "frameCount",
                "stat": "median",
                "min": 20.0,
            },
        ]
        passes, failures = budget.evaluate(SAMPLE["benchmarks"], checks)
        self.assertEqual(3, len(passes))
        self.assertEqual([], failures)

    def test_budget_violation_fails(self):
        checks = [
            {
                "id": "too_strict",
                "classSuffix": ".StartupBenchmark",
                "benchmarkNameSuffix": "coldStartupBaselineProfile",
                "metricGroup": "metrics",
                "metric": "timeToInitialDisplayMs",
                "stat": "median",
                "max": 300.0,
            }
        ]
        passes, failures = budget.evaluate(SAMPLE["benchmarks"], checks)
        self.assertEqual([], passes)
        self.assertEqual(1, len(failures))
        self.assertIn("too_strict", failures[0])


if __name__ == "__main__":
    unittest.main()
