#!/usr/bin/env python3
"""Normalize AndroidX Macrobenchmark JSON into a compact Veil Reader performance snapshot."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
from pathlib import Path

from check_performance_budget import (
    find_result_files,
    flatten_benchmarks,
    load_json,
    matches,
    read_metric,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "results",
        nargs="+",
        type=Path,
        help="Benchmark result file(s) or directories containing *-benchmarkData.json",
    )
    parser.add_argument("--mode", choices=("smoke", "physical"), required=True)
    parser.add_argument("--budgets", type=Path, default=Path("performance/budgets.json"))
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--commit", default=os.environ.get("GITHUB_SHA", "unknown"))
    parser.add_argument("--ref", default=os.environ.get("GITHUB_REF_NAME", "unknown"))
    parser.add_argument("--run-id", default=os.environ.get("GITHUB_RUN_ID", "unknown"))
    parser.add_argument("--device", default=os.environ.get("VEIL_PERF_DEVICE", "unspecified"))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    files = find_result_files(args.results)
    if not files:
        raise SystemExit("No *-benchmarkData.json files found.")

    config = load_json(args.budgets)
    checks = config.get("modes", {}).get(args.mode)
    if not isinstance(checks, list) or not checks:
        raise SystemExit(f"No budgets configured for mode {args.mode}.")

    benchmarks = flatten_benchmarks(files)
    metrics: dict[str, dict[str, object]] = {}

    for check in checks:
        candidates = [benchmark for benchmark in benchmarks if matches(benchmark, check)]
        if not candidates:
            raise SystemExit(f"Missing benchmark for {check['id']}.")
        benchmark = candidates[-1]
        value = read_metric(benchmark, check)
        metrics[str(check["id"])] = {
            "value": value,
            "metricGroup": check["metricGroup"],
            "metric": check["metric"],
            "stat": check["stat"],
            "className": benchmark.get("className", ""),
            "benchmarkName": benchmark.get("name", ""),
            "sourceFile": benchmark.get("_sourceFile", ""),
        }

    payload = {
        "schemaVersion": 1,
        "mode": args.mode,
        "commit": args.commit,
        "ref": args.ref,
        "runId": str(args.run_id),
        "device": args.device,
        "capturedAtUtc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "metrics": metrics,
    }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Wrote {args.output} with {len(metrics)} metrics.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
