#!/usr/bin/env python3

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("build_performance_dashboard.py")
SPEC = importlib.util.spec_from_file_location("build_performance_dashboard", MODULE_PATH)
assert SPEC and SPEC.loader
dash = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(dash)


def snap(commit, run, device="device-a", startup=100.0, p95=-4.0):
    return {
        "schemaVersion": 1,
        "mode": "smoke",
        "commit": commit,
        "runId": run,
        "device": device,
        "metrics": {
            "startup_ttid_median": {"value": startup},
            "reader_frame_overrun_p95": {"value": p95},
        },
    }


class PerformanceDashboardTest(unittest.TestCase):
    def test_append_snapshot_keeps_previous_and_deduplicates(self):
        history = {
            "schemaVersion": 1,
            "mode": "smoke",
            "device": "device-a",
            "snapshots": [snap("a", "1"), snap("b", "2")],
        }
        updated, previous = dash.append_snapshot(history, snap("b", "2", startup=110.0), 30)

        self.assertEqual("b", previous["commit"])
        self.assertEqual(2, len(updated["snapshots"]))
        self.assertEqual(110.0, updated["snapshots"][-1]["metrics"]["startup_ttid_median"]["value"])

    def test_metric_schema_compatibility_is_explicit(self):
        previous = snap("a", "1")
        current = snap("b", "2")
        self.assertTrue(dash.baseline_is_compatible(previous, current))

        migrated = snap("c", "3")
        migrated["metrics"] = {
            "startup_ttid_median": {"value": 100.0},
            "reader_gfx_frame_p95": {"value": 18.0},
        }
        self.assertFalse(dash.baseline_is_compatible(previous, migrated))

    def test_device_mismatch_fails_closed(self):
        history = {
            "schemaVersion": 1,
            "mode": "smoke",
            "device": "device-a",
            "snapshots": [snap("a", "1")],
        }
        with self.assertRaises(ValueError):
            dash.append_snapshot(history, snap("b", "2", device="device-b"), 30)

    def test_gradual_percent_regression_triggers_watch(self):
        rule = {"id": "startup_ttid_median", "warnRegressionPercent": 15.0}
        status, detail = dash.trend_status([100.0, 108.0, 116.0], rule, 3)

        self.assertEqual("watch", status)
        self.assertIn("16.00%", detail)

    def test_negative_overrun_uses_absolute_increase(self):
        rule = {"id": "reader_frame_overrun_p95", "warnIncrease": 10.0}
        status, detail = dash.trend_status([-12.0, -6.0, -1.0], rule, 3)

        self.assertEqual("watch", status)
        self.assertIn("+11.000", detail)

    def test_non_monotonic_noise_is_not_sustained_watch(self):
        rule = {"id": "startup_ttid_median", "warnRegressionPercent": 10.0}
        status, _ = dash.trend_status([100.0, 120.0, 108.0], rule, 3)

        self.assertEqual("stable", status)

    def test_dashboard_reports_alerts(self):
        history = {
            "schemaVersion": 1,
            "mode": "smoke",
            "device": "device-a",
            "snapshots": [
                snap("a", "1", startup=100.0, p95=-12.0),
                snap("b", "2", startup=108.0, p95=-6.0),
                snap("c", "3", startup=116.0, p95=-1.0),
            ],
        }
        policy = {
            "window": 5,
            "minPoints": 3,
            "modes": {
                "smoke": [
                    {"id": "startup_ttid_median", "warnRegressionPercent": 15.0},
                    {"id": "reader_frame_overrun_p95", "warnIncrease": 10.0},
                ]
            },
        }

        markdown, data, alerts = dash.build_dashboard(history, policy)

        self.assertEqual(
            ["startup_ttid_median", "reader_frame_overrun_p95"],
            alerts,
        )
        self.assertIn("Early warning", markdown)
        self.assertEqual(3, data["historyPoints"])


if __name__ == "__main__":
    unittest.main()
