#!/usr/bin/env python3
"""Fail closed if Dependency Review's actual comparison has snapshot warnings.

The upstream action currently logs a retry timeout and still returns success when
its dependency-graph comparison lacks a head/base snapshot. It emits those warnings
in the `comment-content` output as an HTML heading, even on successful steps.
"""

import re
import sys

_REVIEW = re.compile(r"<h1(?:\s[^>]*)?>\s*Dependency Review\s*</h1>", re.I)
_WARNINGS = re.compile(r"<h2(?:\s[^>]*)?>\s*Snapshot Warnings\s*</h2>", re.I)


def validate(summary: str) -> tuple[bool, str]:
    if not summary.strip():
        return False, "Missing Dependency Review output; no complete comparison proof"
    if not _REVIEW.search(summary):
        return False, "Malformed Dependency Review output; cannot verify comparison"
    if _WARNINGS.search(summary):
        return False, "Dependency graph snapshot warnings; comparison is incomplete"
    return True, "Dependency Review reported no snapshot warnings"


def main() -> int:
    valid, message = validate(sys.stdin.read())
    print(message if valid else f"::error::{message}")
    return 0 if valid else 1


if __name__ == "__main__":
    raise SystemExit(main())
