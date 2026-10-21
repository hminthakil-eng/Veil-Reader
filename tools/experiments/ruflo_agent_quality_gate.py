#!/usr/bin/env python3
"""Conservative static quality gate for experimental Ruflo PR-509 review reports.

This is a smoke-quality screen, NOT a replacement for human/engineering review.
"""
import argparse
import json
import re
from pathlib import Path

CORRECT_API = '/dependency-graph/compare/'
WRONG_API_RE = re.compile(r'/repos/\{[^}]+\}/\{[^}]+\}/compare/', re.I)


def evaluate(content: str) -> dict:
    errors = []
    if not re.search(r'^##\s+CI investigator\s*$', content, re.M):
        errors.append('missing investigator section')
    if not re.search(r'^##\s+Skeptical reviewer\s*$', content, re.M):
        errors.append('missing independent reviewer section')
    if WRONG_API_RE.search(content):
        errors.append('wrong GitHub commits-compare endpoint proposed for dependency snapshot checks')
    if CORRECT_API not in content:
        errors.append('correct dependency-graph compare endpoint not provided')
    if 'e37869bb68e0497b5cbb9ca9f9b56c4e53286020' not in content:
        errors.append('exact source head SHA missing')
    if 'refs/pull/509/merge' in content and 'refs/heads/' not in content:
        errors.append('PR merge ref repeated without considering an exact source-head branch ref')
    if not re.search(r'fail.closed|block|reject|forbid|must not|no bypass', content, re.I):
        errors.append('explicit fail-closed acceptance condition not established')
    if not re.search(r'unproven|unknown|hypothesis|alternative|not prove', content, re.I):
        errors.append('hypothesis-versus-evidence distinction missing')
    return {'status': 'FAIL' if errors else 'PASS', 'quality_approved': not errors,
            'issues': errors, 'disclaimer': 'Static screen only: passing is not approval to merge or deploy.'}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('report', type=Path)
    args = parser.parse_args()
    result = evaluate(args.report.read_text(encoding='utf-8'))
    print(json.dumps(result, ensure_ascii=False, indent=2))
    raise SystemExit(1 if result['issues'] else 0)


if __name__ == '__main__':
    main()
