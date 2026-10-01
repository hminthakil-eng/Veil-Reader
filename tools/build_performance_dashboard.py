#!/usr/bin/env python3
"""Build Veil Reader performance history, dashboard and early-warning trend signals."""

from __future__ import annotations

import argparse
import json
import math
import os
from pathlib import Path
from typing import Any


SPARKS = "▁▂▃▄▅▆▇█"


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def sparkline(values: list[float]) -> str:
    if not values:
        return ""
    low = min(values)
    high = max(values)
    if math.isclose(low, high):
        return SPARKS[0] * len(values)
    chars = []
    for value in values:
        ratio = (value - low) / (high - low)
        index = min(len(SPARKS) - 1, max(0, round(ratio * (len(SPARKS) - 1))))
        chars.append(SPARKS[index])
    return "".join(chars)


def regression_percent(first: float, last: float) -> float:
    denominator = abs(first)
    if denominator < 1e-9:
        return 0.0 if math.isclose(first, last) else float("inf")
    return (last - first) / denominator * 100.0


def trend_status(values: list[float], rule: dict[str, Any], min_points: int) -> tuple[str, str]:
    if len(values) < min_points:
        return "learning", f"{len(values)}/{min_points} points"

    recent = values[-min_points:]
    transitions = [b - a for a, b in zip(recent, recent[1:])]
    sustained_worse = all(change >= 0 for change in transitions) and any(change > 0 for change in transitions)

    if "warnIncrease" in rule:
        increase = recent[-1] - recent[0]
        threshold = float(rule["warnIncrease"])
        if sustained_worse and increase > threshold:
            return "watch", f"+{increase:.3f} > +{threshold:.3f}"
        if increase < -threshold:
            return "improving", f"{increase:.3f}"
        return "stable", f"{increase:+.3f}"

    threshold = float(rule["warnRegressionPercent"])
    percent = regression_percent(recent[0], recent[-1])
    if sustained_worse and percent > threshold:
        return "watch", f"+{percent:.2f}% > +{threshold:.2f}%"
    if percent < -threshold:
        return "improving", f"{percent:.2f}%"
    return "stable", f"{percent:+.2f}%"


def read_history(path: Path | None) -> dict[str, Any] | None:
    if path is None or not path.is_file():
        return None
    return load_json(path)


def append_snapshot(
    history: dict[str, Any] | None,
    snapshot: dict[str, Any],
    max_points: int,
) -> tuple[dict[str, Any], dict[str, Any] | None]:
    mode = snapshot.get("mode")
    device = snapshot.get("device")
    if not mode or not device:
        raise ValueError("snapshot must contain mode and device")

    previous: dict[str, Any] | None = None
    if history is None:
        snapshots: list[dict[str, Any]] = []
    else:
        if history.get("mode") != mode:
            raise ValueError("history mode does not match snapshot mode")
        if history.get("device") != device:
            raise ValueError("history device does not match snapshot device")
        snapshots = [item for item in history.get("snapshots", []) if isinstance(item, dict)]
        if snapshots:
            previous = snapshots[-1]

    key = (snapshot.get("commit"), snapshot.get("runId"))
    snapshots = [
        item
        for item in snapshots
        if (item.get("commit"), item.get("runId")) != key
    ]
    snapshots.append(snapshot)
    snapshots = snapshots[-max_points:]

    return {
        "schemaVersion": 1,
        "mode": mode,
        "device": device,
        "snapshots": snapshots,
    }, previous


def baseline_is_compatible(previous: dict[str, Any], current: dict[str, Any]) -> bool:
    previous_metrics = previous.get("metrics", {})
    current_metrics = current.get("metrics", {})
    if not isinstance(previous_metrics, dict) or not isinstance(current_metrics, dict):
        return False
    return set(current_metrics).issubset(previous_metrics)


def build_dashboard(
    history: dict[str, Any],
    policy: dict[str, Any],
) -> tuple[str, dict[str, Any], list[str]]:
    mode = str(history["mode"])
    snapshots = history["snapshots"]
    rules = policy.get("modes", {}).get(mode, [])
    window = int(policy.get("window", 5))
    min_points = int(policy.get("minPoints", 3))
    alerts: list[str] = []
    rows: list[dict[str, Any]] = []

    for rule in rules:
        metric_id = str(rule["id"])
        points: list[tuple[dict[str, Any], float]] = []
        for snap in snapshots[-window:]:
            entry = snap.get("metrics", {}).get(metric_id)
            value = entry.get("value") if isinstance(entry, dict) else None
            if isinstance(value, (int, float)):
                points.append((snap, float(value)))

        values = [value for _, value in points]
        status, delta = trend_status(values, rule, min_points)
        if status == "watch":
            alerts.append(metric_id)

        rows.append({
            "id": metric_id,
            "status": status,
            "current": values[-1] if values else None,
            "delta": delta,
            "sparkline": sparkline(values),
            "points": len(values),
        })

    latest = snapshots[-1] if snapshots else {}
    status_icon = {
        "watch": "⚠️",
        "stable": "✅",
        "improving": "↘️",
        "learning": "🧪",
    }

    lines = [
        f"## Veil Reader performance trend — {mode}",
        "",
        f"**Device:** {history.get('device', 'unknown')}",
        f"**Latest commit:** `{latest.get('commit', 'unknown')}`",
        f"**History points:** {len(snapshots)}",
        "",
        "| Metric | Current | Window change | Trend | Status |",
        "|---|---:|---:|:---:|---|",
    ]

    for row in rows:
        current = "—" if row["current"] is None else f"{row['current']:.3f}"
        status = str(row["status"])
        lines.append(
            f"| `{row['id']}` | {current} | {row['delta']} | {row['sparkline'] or '—'} | "
            f"{status_icon.get(status, '')} {status} |"
        )

    lines.append("")
    if alerts:
        lines.append(
            "**Early warning:** sustained degradation detected before the absolute budget was necessarily crossed: "
            + ", ".join(f"`{item}`" for item in alerts)
        )
    else:
        lines.append("No sustained early-warning degradation is currently detected.")

    dashboard = {
        "schemaVersion": 1,
        "mode": mode,
        "device": history.get("device"),
        "latestCommit": latest.get("commit"),
        "historyPoints": len(snapshots),
        "alerts": alerts,
        "metrics": rows,
    }
    return "\n".join(lines) + "\n", dashboard, alerts


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--history", type=Path)
    parser.add_argument("--snapshot", type=Path, required=True)
    parser.add_argument("--policy", type=Path, default=Path("performance/trend-policy.json"))
    parser.add_argument("--output-history", type=Path, required=True)
    parser.add_argument("--markdown", type=Path, required=True)
    parser.add_argument("--json", type=Path, required=True)
    parser.add_argument("--baseline-output", type=Path)
    parser.add_argument("--max-points", type=int, default=30)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    snapshot = load_json(args.snapshot)
    history, previous = append_snapshot(read_history(args.history), snapshot, args.max_points)
    policy = load_json(args.policy)
    markdown, dashboard, alerts = build_dashboard(history, policy)

    for path in (args.output_history, args.markdown, args.json):
        path.parent.mkdir(parents=True, exist_ok=True)

    args.output_history.write_text(
        json.dumps(history, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    args.markdown.write_text(markdown, encoding="utf-8")
    args.json.write_text(
        json.dumps(dashboard, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    if args.baseline_output and previous is not None:
        if baseline_is_compatible(previous, snapshot):
            args.baseline_output.parent.mkdir(parents=True, exist_ok=True)
            args.baseline_output.write_text(
                json.dumps(previous, indent=2, sort_keys=True) + "\n",
                encoding="utf-8",
            )
        else:
            print(
                "Previous performance snapshot uses an incompatible metric schema; "
                "relative delta is skipped until a compatible main baseline exists."
            )

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write(markdown)

    if alerts:
        print("TREND WATCH: " + ", ".join(alerts))
    else:
        print("TREND OK: no sustained early-warning degradation")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
