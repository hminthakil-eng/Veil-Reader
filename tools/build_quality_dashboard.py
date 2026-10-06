#!/usr/bin/env python3
import argparse
import json
import os
import re
import sys
import time
import urllib.request
from pathlib import Path

API = "https://api.github.com"

REQUIRED_CHECK_NAMES = (
    "build",
    "room-on-android",
    "benchmark-smoke",
    "dependency-graph",
    "dependency-review",
)

def api_get(path: str, token: str):
    req = urllib.request.Request(
        API + path,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "veil-reader-quality-dashboard",
        },
    )
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)

def latest_by_name(check_runs):
    result = {}
    for item in check_runs:
        name = item.get("name", "unknown")
        previous = result.get(name)
        if previous is None or item.get("id", 0) > previous.get("id", 0):
            result[name] = item
    return result

def normalize(check):
    if not check:
        return {"status": "missing", "conclusion": None, "url": None}
    return {
        "status": check.get("status"),
        "conclusion": check.get("conclusion"),
        "url": check.get("html_url"),
    }

def required_checks_settled(checks):
    return all(
        checks.get(name, {}).get("status") == "completed"
        for name in REQUIRED_CHECK_NAMES
    )

def source_status_for(items):
    if all(item["status"] == "completed" and item["conclusion"] == "success" for item in items):
        return "GREEN-SOURCE"
    if any(
        item["status"] == "completed" and item["conclusion"] != "success"
        for item in items
    ):
        return "RED"
    return "YELLOW"

def load_checks(repo, sha, token, wait_seconds=0, poll_seconds=15):
    deadline = time.monotonic() + max(0, wait_seconds)
    while True:
        payload = api_get(f"/repos/{repo}/commits/{sha}/check-runs?per_page=100", token)
        checks = latest_by_name(payload.get("check_runs", []))
        if required_checks_settled(checks) or time.monotonic() >= deadline:
            return checks
        time.sleep(max(1, poll_seconds))

def run_id_from_check(check):
    if not check:
        return None
    url = check.get("html_url") or check.get("details_url") or ""
    match = re.search(r"/actions/runs/(\d+)", url)
    return int(match.group(1)) if match else None

def artifact_present(repo, run_id, token, artifact_name):
    if not run_id:
        return False
    payload = api_get(f"/repos/{repo}/actions/runs/{run_id}/artifacts", token)
    return any(a.get("name") == artifact_name and not a.get("expired", False)
               for a in payload.get("artifacts", []))

def load_physical(path, sha):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    evidence_sha = data.get("sha")
    fresh = bool(evidence_sha) and evidence_sha == sha
    return data, fresh

def conclusion_icon(value):
    return {
        "success": "🟢",
        "failure": "🔴",
        "cancelled": "⚪",
        "skipped": "⚪",
        "neutral": "⚪",
        None: "🟡",
    }.get(value, "🟡")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--token", default=os.environ.get("GITHUB_TOKEN"))
    parser.add_argument("--physical", required=True)
    parser.add_argument("--wait-seconds", type=int, default=0)
    parser.add_argument("--poll-seconds", type=int, default=15)
    parser.add_argument("--out-json", required=True)
    parser.add_argument("--out-md", required=True)
    args = parser.parse_args()

    if not args.token:
        raise SystemExit("GITHUB_TOKEN is required")

    checks = load_checks(
        args.repo,
        args.sha,
        args.token,
        wait_seconds=args.wait_seconds,
        poll_seconds=args.poll_seconds,
    )

    android = normalize(checks.get("build"))
    storage = normalize(checks.get("room-on-android"))
    performance = normalize(checks.get("benchmark-smoke"))
    dependency_graph = normalize(checks.get("dependency-graph"))
    dependency_review = normalize(checks.get("dependency-review"))

    storage_run_id = run_id_from_check(checks.get("room-on-android"))
    screenshot_artifact = artifact_present(
        args.repo,
        storage_run_id,
        args.token,
        "veil-reader-grayfog-shell-review",
    ) if storage_run_id else False

    # A successful benchmark-smoke necessarily passed the packaged Baseline Profile
    # verification step because that workflow is sequential and fail-fast.
    profile_packaging = performance["conclusion"] == "success"

    physical, physical_fresh = load_physical(args.physical, args.sha)

    core = [android, storage, performance, dependency_graph, dependency_review]
    source_status = source_status_for(core)

    device_status = (
        "GREEN-DEVICE"
        if physical_fresh and physical.get("status") == "GREEN-DEVICE"
        else "UNVERIFIED"
    )

    result = {
        "repo": args.repo,
        "sha": args.sha,
        "source_status": source_status,
        "device_status": device_status,
        "product_status": "UNVERIFIED",
        "checks": {
            "android_ci": android,
            "storage_instrumentation": storage,
            "performance_benchmarks": performance,
            "dependency_graph": dependency_graph,
            "dependency_review": dependency_review,
        },
        "evidence": {
            "grayfog_capture_artifact": screenshot_artifact,
            "baseline_profile_packaged": profile_packaging,
            "physical_device": physical,
            "physical_evidence_matches_sha": physical_fresh,
        },
        "rules": {
            "green_source_is_not_green_device": True,
            "green_device_is_not_green_product": True,
            "product_green_requires_manual_acceptance": True,
        },
    }

    Path(args.out_json).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out_json).write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")

    rows = [
        ("Android CI", android),
        ("Storage Instrumentation", storage),
        ("Performance Benchmarks", performance),
        ("Dependency Graph", dependency_graph),
        ("Dependency Review", dependency_review),
    ]
    md = []
    md.append("# Veil Reader — SHA-linked Quality Dashboard")
    md.append("")
    md.append(f"- SHA: `{args.sha}`")
    md.append(f"- Source status: **{source_status}**")
    md.append(f"- Device status: **{device_status}**")
    md.append("- Product status: **UNVERIFIED**")
    md.append("")
    md.append("| Gate | Status | Result |")
    md.append("|---|---|---|")
    for name, item in rows:
        icon = conclusion_icon(item["conclusion"])
        md.append(f"| {name} | {item['status']} | {icon} {item['conclusion'] or 'pending/missing'} |")
    md.append("")
    md.append("## Evidence")
    md.append("")
    md.append(f"- Grayfog rendered capture artifact: **{'YES' if screenshot_artifact else 'NO'}**")
    md.append(f"- Packaged Baseline Profile proof: **{'YES' if profile_packaging else 'NO'}**")
    md.append(f"- Physical evidence matches SHA: **{'YES' if physical_fresh else 'NO'}**")
    md.append("")
    md.append("## Contract")
    md.append("")
    md.append("Automated checks may promote a revision only to **GREEN-SOURCE**.")
    md.append("Physical interaction/rendering evidence is required for **GREEN-DEVICE**.")
    md.append("Product acceptance is manual and is never inferred by CI.")
    md.append("")

    Path(args.out_md).write_text("\n".join(md), encoding="utf-8")
    print("\n".join(md))

if __name__ == "__main__":
    main()
