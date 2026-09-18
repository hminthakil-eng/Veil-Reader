#!/usr/bin/env python3
"""Validate AndroidX Macrobenchmark JSON results against Veil Reader performance budgets."""

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


def find_result_files(inputs: list[Path]) -> list[Path]:
    found: set[Path] = set()
    for item in inputs:
        if item.is_file() and item.name.endswith("-benchmarkData.json"):
            found.add(item.resolve())
        elif item.is_dir():
            found.update(path.resolve() for path in item.rglob("*-benchmarkData.json"))
    return sorted(found)


def flatten_benchmarks(files: list[Path]) -> list[dict[str, Any]]:
    benchmarks: list[dict[str, Any]] = []
    for path in files:
        payload = load_json(path)
        for benchmark in payload.get("benchmarks", []):
            if isinstance(benchmark, dict):
                copy = dict(benchmark)
                copy["_sourceFile"] = str(path)
                benchmarks.append(copy)
    return benchmarks


def matches(benchmark: dict[str, Any], check: dict[str, Any]) -> bool:
    class_name = str(benchmark.get("className", ""))
    benchmark_name = str(benchmark.get("name", ""))
    return class_name.endswith(str(check["classSuffix"])) and benchmark_name.endswith(
        str(check["benchmarkNameSuffix"])
    )


def read_metric(benchmark: dict[str, Any], check: dict[str, Any]) -> float:
    group_name = str(check["metricGroup"])
    metric_name = str(check["metric"])
    stat = str(check["stat"])
    group = benchmark.get(group_name)
    if not isinstance(group, dict):
        raise KeyError(f"missing metric group {group_name}")
    metric = group.get(metric_name)
    if not isinstance(metric, dict):
        raise KeyError(f"missing metric {metric_name}")
    value = metric.get(stat)
    if not isinstance(value, (int, float)):
        raise KeyError(f"missing numeric stat {stat}")
    return float(value)


def evaluate(
    benchmarks: list[dict[str, Any]],
    checks: list[dict[str, Any]],
) -> tuple[list[str], list[str]]:
    passes: list[str] = []
    failures: list[str] = []

    for check in checks:
        check_id = str(check["id"])
        candidates = [benchmark for benchmark in benchmarks if matches(benchmark, check)]
        if not candidates:
            failures.append(f"{check_id}: benchmark result not found")
            continue

        # AndroidX may emit multiple result files. Use the newest/last matching record so a rerun
        # doesn't fail because an older report is still present in the output directory.
        benchmark = candidates[-1]
        try:
            value = read_metric(benchmark, check)
        except KeyError as error:
            failures.append(f"{check_id}: {error}")
            continue

        bounds: list[str] = []
        failed = False
        if "min" in check:
            minimum = float(check["min"])
            bounds.append(f">= {minimum:g}")
            if value < minimum:
                failed = True
        if "max" in check:
            maximum = float(check["max"])
            bounds.append(f"<= {maximum:g}")
            if value > maximum:
                failed = True

        detail = (
            f"{check_id}: {value:.3f} ({' and '.join(bounds)}) "
            f"[{benchmark.get('className', '')}.{benchmark.get('name', '')}]"
        )
        (failures if failed else passes).append(detail)

    return passes, failures


def write_summary(mode: str, passes: list[str], failures: list[str]) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    status = "PASS" if not failures else "FAIL"
    lines = [
        f"## Veil Reader performance budget — {mode}",
        "",
        f"**Result:** {status}",
        "",
        "### Checks",
    ]
    lines.extend(f"- ✅ {item}" for item in passes)
    lines.extend(f"- ❌ {item}" for item in failures)
    lines.append("")
    lines.append(
        "Emulator smoke budgets are guardrails only. Physical-device budgets are the product-performance gate."
    )
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "results",
        nargs="+",
        type=Path,
        help="Benchmark JSON file(s) or directories containing *-benchmarkData.json",
    )
    parser.add_argument(
        "--mode",
        choices=("smoke", "physical"),
        default="smoke",
        help="Budget set to enforce",
    )
    parser.add_argument(
        "--budgets",
        type=Path,
        default=Path("performance/budgets.json"),
        help="Budget configuration file",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    files = find_result_files(args.results)
    if not files:
        print("FAIL: no *-benchmarkData.json files were found.", file=sys.stderr)
        return 2

    config = load_json(args.budgets)
    checks = config.get("modes", {}).get(args.mode)
    if not isinstance(checks, list) or not checks:
        print(f"FAIL: no budgets configured for mode {args.mode}.", file=sys.stderr)
        return 2

    benchmarks = flatten_benchmarks(files)
    passes, failures = evaluate(benchmarks, checks)

    print(f"Veil Reader performance budget mode: {args.mode}")
    for item in passes:
        print(f"PASS: {item}")
    for item in failures:
        print(f"FAIL: {item}", file=sys.stderr)

    write_summary(args.mode, passes, failures)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
