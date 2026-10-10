#!/usr/bin/env python3
"""Fail-closed source contract for least-privileged Reader supply-chain CI.

This checks narrowly scoped invariants in an existing workflow without needing
a YAML library or network access. GitHub Actions remains the runtime authority.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/dependency-baseline.yml"
CANONICAL = "grand-forge/p0-kindle-reader-quality-20261008"
SYFT = "anchore/sbom-action@66cbf4bc1f1c0d2edc94016e65bc221b6bb0ad6c"


def check(text: str) -> list[str]:
    problems: list[str] = []
    if not text.startswith("name: Dependency Baseline\n"):
        problems.append("wrong/missing workflow identity")
    before_jobs, sep, after_jobs = text.partition("\njobs:\n")
    if not sep:
        return problems + ["missing jobs mapping"]

    pull = re.search(r"(?ms)^  pull_request:\n(?P<body>.*?)(?=^  (?:push|workflow_dispatch):)", before_jobs)
    push = re.search(r"(?ms)^  push:\n(?P<body>.*?)(?=^  (?:pull_request|workflow_dispatch):)", before_jobs)
    for event, match in (("pull_request", pull), ("push", push)):
        if not match or CANONICAL not in match.group("body"):
            problems.append(f"{event} must include canonical Reader branch")

    global_permissions = re.search(
        r"(?m)^permissions:\n(?P<block>(?:^  [^\n]*\n?)*)",
        before_jobs,
    )
    if not global_permissions or not re.search(
        r"(?m)^  contents: read\s*$", global_permissions.group("block")
    ):
        problems.append("default workflow permission must be contents:read")
    if re.search(r"(?m)^  contents: write\s*$", before_jobs):
        problems.append("repository write permission must never be global")

    blocks = {}
    entries = list(re.finditer(r"(?m)^  ([a-z][a-z0-9-]*):\s*$", after_jobs))
    for i, entry in enumerate(entries):
        blocks[entry.group(1)] = after_jobs[entry.end():entries[i + 1].start() if i + 1 < len(entries) else None]

    pr = blocks.get("pr-source-sbom", "")
    graph = blocks.get("dependency-graph", "")
    if not pr:
        problems.append("missing PR-only source inventory job")
    else:
        if "if: github.event_name == 'pull_request'" not in pr:
            problems.append("PR SBOM job must run only for pull_request")
        if not re.search(r"(?m)^    permissions:\n      contents: read\s*$", pr):
            problems.append("PR job must explicitly be contents:read")
        if "dependency-submission@" in pr or "contents: write" in pr:
            problems.append("PR job must not submit dependency graph or obtain write token")
        if SYFT not in pr or "format: cyclonedx-json" not in pr:
            problems.append("PR source inventory must use pinned Syft CycloneDX")
    if not graph:
        problems.append("missing trusted push/manual dependency graph job")
    else:
        if "if: github.event_name != 'pull_request'" not in graph:
            problems.append("privileged graph job must exclude pull_request")
        if not re.search(r"(?m)^    permissions:\n      contents: write\s*$", graph):
            problems.append("graph job needs job-local contents:write")
        if "gradle/actions/dependency-submission@v6" not in graph:
            problems.append("trusted graph job must submit resolved Gradle graph")
        if graph.count(SYFT) != 2:
            problems.append("trusted graph job must preserve pinned JSON and XML Syft outputs")

    return problems


def main() -> int:
    try:
        contents = WORKFLOW.read_text(encoding="utf-8")
    except OSError as error:
        print(f"FAIL: workflow unavailable: {error}", file=sys.stderr)
        return 1
    errors = check(contents)
    for error in errors:
        print("FAIL: " + error, file=sys.stderr)
    if errors:
        return 1
    print("PASS: canonical Reader supply-chain jobs use least-privilege PR and trusted push gates.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
