#!/usr/bin/env python3
"""Compare a Veil Reader performance snapshot against a previous baseline snapshot."""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--current", type=Path, required=True)
    parser.add_argument(
        "--limits",
        type=Path,
        default=Path("performance/delta-budgets.json"),
    )
    parser.add_argument("--mode", choices=("smoke", "physical"), required=True)
    return parser.parse_args()


def regression_percent(baseline: float, current: float) -> float:
    denominator = abs(baseline)
    if denominator < 1e-9:
        return 0.0 if abs(current - baseline) < 1e-9 else float("inf")
    return (current - baseline) / denominator * 100.0


def evaluate(
    baseline: dict[str, Any],
    current: dict[str, Any],
    rules: list[dict[str, Any]],
) -> tuple[list[str], list[str]]:
    baseline_metrics = baseline.get("metrics", {})
    current_metrics = current.get("metrics", {})
    passes: list[str] = []
    failures: list[str] = []

    for rule in rules:
        metric_id = str(rule["id"])
        baseline_entry = baseline_metrics.get(metric_id)
        current_entry = current_metrics.get(metric_id)
        if not isinstance(baseline_entry, dict):
            failures.append(f"{metric_id}: missing from baseline snapshot")
            continue
        if not isinstance(current_entry, dict):
            failures.append(f"{metric_id}: missing from current snapshot")
            continue

        baseline_value = baseline_entry.get("value")
        current_value = current_entry.get("value")
        if not isinstance(baseline_value, (int, float)) or not isinstance(current_value, (int, float)):
            failures.append(f"{metric_id}: non-numeric snapshot value")
            continue

        b = float(baseline_value)
        c = float(current_value)
        reasons: list[str] = []

        if "maxIncrease" in rule:
            max_increase = float(rule["maxIncrease"])
            increase = c - b
            if increase > max_increase:
                reasons.append(f"increase {increase:.3f} > {max_increase:.3f}")

        if "maxRegressionPercent" in rule:
            max_percent = float(rule["maxRegressionPercent"])
            percent = regression_percent(b, c)
            if percent > max_percent:
                reasons.append(f"regression {percent:.2f}% > {max_percent:.2f}%")

        detail = f"{metric_id}: baseline={b:.3f}, current={c:.3f}"
        if reasons:
            failures.append(f"{detail}; " + "; ".join(reasons))
        else:
            passes.append(detail)

    return passes, failures


def write_summary(
    mode: str,
    baseline: dict[str, Any],
    current: dict[str, Any],
    passes: list[str],
    failures: list[str],
) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    lines = [
        f"## Veil Reader PR performance delta — {mode}",
        "",
        f"**Baseline:** `{baseline.get('commit', 'unknown')}` · {baseline.get('device', 'unknown')}",
        f"**Current:** `{current.get('commit', 'unknown')}` · {current.get('device', 'unknown')}",
        "",
        f"**Result:** {'PASS' if not failures else 'FAIL'}",
        "",
        "### Checks",
    ]
    lines.extend(f"- ✅ {item}" for item in passes)
    lines.extend(f"- ❌ {item}" for item in failures)
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def main() -> int:
    args = parse_args()
    baseline = load_json(args.baseline)
    current = load_json(args.current)
    config = load_json(args.limits)
    rules = config.get("modes", {}).get(args.mode)

    if not isinstance(rules, list) or not rules:
        print(f"FAIL: no delta rules configured for mode {args.mode}.", file=sys.stderr)
        return 2
    if baseline.get("mode") != args.mode or current.get("mode") != args.mode:
        print("FAIL: snapshot mode does not match requested delta mode.", file=sys.stderr)
        return 2

    passes, failures = evaluate(baseline, current, rules)

    print(f"Veil Reader performance delta mode: {args.mode}")
    for item in passes:
        print(f"PASS: {item}")
    for item in failures:
        print(f"FAIL: {item}", file=sys.stderr)

    write_summary(args.mode, baseline, current, passes, failures)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
