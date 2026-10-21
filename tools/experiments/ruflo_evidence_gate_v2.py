#!/usr/bin/env python3
"""Fail-closed, source-specific evidence check for Ruflo's experimental PR-509 audit.

A PASS is only a syntax/evidence-contract pass; it never authorizes a merge.
"""
import argparse
import json
import re
from pathlib import Path

HEAD = 'e37869bb68e0497b5cbb9ca9f9b56c4e53286020'
BASE = 'bfac95f4b82e23b9f277f985bf3282f3e9c584fb'
HEAD_BRANCH = 'refs/heads/reader/native-paper-rebuild-20261011'
MERGE_REF = 'refs/pull/509/merge'
HEADER = 'x-github-dependency-graph-snapshot-warnings'
EXPECTED_PATH = f'/dependency-graph/compare/{BASE}...{HEAD}'
WRONG_ENDPOINT = re.compile(r'/repos/(?:\{[^}]+\}|[\w.-]+)/(?:\{[^}]+\}|[\w.-]+)/compare/', re.I)


def audit(report: str):
    issues = []
    parts = re.split(r'^## (CI investigator|Skeptical reviewer)\s*$', report, flags=re.M | re.I)
    if len(parts) != 5 or parts[1].lower() != 'ci investigator' or parts[3].lower() != 'skeptical reviewer':
        return {'status': 'FAIL', 'issues': ['expected exactly two separate agent sections in order'], 'merge_authorized': False}
    investigator, reviewer = parts[2], parts[4]
    if not investigator.strip() or not reviewer.strip():
        issues.append('empty agent output')
    if WRONG_ENDPOINT.search(report):
        issues.append('wrong commit-compare endpoint')
    if EXPECTED_PATH not in investigator or EXPECTED_PATH not in reviewer:
        issues.append('both agents must independently cite the exact dependency-graph comparison endpoint')
    if HEADER not in investigator.lower() or HEADER not in reviewer.lower():
        issues.append('both agents must check the dependency snapshot warnings header')
    if not all(x in investigator for x in [HEAD, BASE, MERGE_REF, HEAD_BRANCH]):
        issues.append('investigator missing exact PR SHA/ref facts and proposed alternative head branch')
    if not re.search(r'(?i)observ|evidence|log', investigator) or not re.search(r'(?i)hypothes|unproven|uncertain|not proven|candidate', investigator):
        issues.append('investigator does not distinguish evidence from hypothesis')
    if not re.search(r'(?i)experiment|reproduc|test|verify', investigator):
        issues.append('investigator missing executable verification plan')
    if not re.search(r'(?i)alternative|risk|counter|could also|another cause', reviewer):
        issues.append('reviewer did not challenge investigator independently')
    if not re.search(r'(?im)^\s*(?:decision|verdict)\s*:\s*BLOCK\b', reviewer):
        issues.append('reviewer must explicitly block merge pending exact snapshot readiness')
    if not re.search(r'(?i)missing|incomplete|nonempty|non-empty', reviewer):
        issues.append('reviewer must define rejection for incomplete snapshot evidence')
    if not re.search(r'(?i)empty|no warnings|no warning|warning-free', reviewer):
        issues.append('reviewer must require an empty snapshot-warnings header')
    return {'status': 'PASS' if not issues else 'FAIL', 'issues': issues, 'merge_authorized': False,
            'note': 'A PASS does not prove correctness or approve any repository mutation.'}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('report', type=Path)
    p.add_argument('--expected-status', choices=['PASS', 'FAIL'], default='PASS')
    a = p.parse_args()
    result = audit(a.report.read_text(encoding='utf-8'))
    print(json.dumps(result, indent=2, ensure_ascii=False))
    raise SystemExit(0 if result['status'] == a.expected_status else 1)


if __name__ == '__main__':
    main()
