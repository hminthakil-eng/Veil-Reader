#!/usr/bin/env python3
"""Conservative static screening of Ruflo PR-509 engineering reports.

A passing screen does NOT approve a fix, PR, deployment, or autonomous writes.
"""
import argparse
import json
import re
from pathlib import Path

BASE_SHA = "bfac95f4b82e23b9f277f985bf3282f3e9c584fb"
HEAD_SHA = "e37869bb68e0497b5cbb9ca9f9b56c4e53286020"
API = "/dependency-graph/compare/"
WARNINGS_HEADER = "x-github-dependency-graph-snapshot-warnings"
WRONG_API = re.compile(
    r"/repos/(?:\{[^}]+\}|[A-Za-z0-9_.-]+)/(?:\{[^}]+\}|[A-Za-z0-9_.-]+)/compare/",
    re.IGNORECASE,
)


def evaluate(report: str) -> dict:
    issues = []
    headings = re.findall(r"^##\s+(.+?)\s*$", report, re.MULTILINE)
    if headings.count("CI investigator") != 1:
        issues.append("missing or duplicated investigator")
    if headings.count("Skeptical reviewer") != 1:
        issues.append("missing or duplicated independent reviewer")
    if WRONG_API.search(report):
        issues.append("wrong commits-compare API cited as a dependency snapshot check")
    if API not in report:
        issues.append("correct dependency-graph comparison endpoint missing")
    for label, value in (("base", BASE_SHA), ("head", HEAD_SHA)):
        if value not in report:
            issues.append("exact " + label + " SHA missing")
    if WARNINGS_HEADER not in report.lower():
        issues.append("missing dependency-graph snapshot-warning header validation")
    if "refs/heads/" not in report:
        issues.append("source-head ref hypothesis not considered")
    if not re.search(r"fail.closed|block|reject|must not|no bypass", report, re.I):
        issues.append("fail-closed acceptance is not explicit")
    if not re.search(r"hypothes[ie]s|unproven|not (?:yet )?proven|unknown|alternative", report, re.I):
        issues.append("observations and hypotheses not separated")
    # A passing static check is just a candidate for independent human review.
    return {
        "status": "FAIL" if issues else "PASS_STATIC_ONLY",
        "static_checks_passed": not issues,
        "approved_for_merge": False,
        "issues": issues,
        "disclaimer": "Passing string checks is not source verification, human approval, or license to modify the repo.",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("report", type=Path)
    args = parser.parse_args()
    result = evaluate(args.report.read_text(encoding="utf-8"))
    print(json.dumps(result, ensure_ascii=False, indent=2))
    raise SystemExit(1 if result["issues"] else 0)


if __name__ == "__main__":
    main()
