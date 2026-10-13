#!/usr/bin/env python3
"""Fail closed when GitHub has not ingested the exact dependency snapshots."""
import base64
import binascii
import json
import os
import re
import sys
import time
from urllib.error import URLError
from urllib.request import Request, urlopen


WARNINGS_HEADER = "x-github-dependency-graph-snapshot-warnings"


def snapshot_warnings(headers):
    value = headers.get(WARNINGS_HEADER)
    if value is None:
        raise ValueError("Dependency comparison did not provide snapshot readiness")
    try:
        return base64.b64decode(value, validate=True).decode("utf-8").strip()
    except (binascii.Error, UnicodeDecodeError) as error:
        raise ValueError("Dependency comparison returned invalid snapshot warnings") from error


def wait_for_snapshots(fetch, timeout=600, interval=10, clock=time.monotonic, sleep=time.sleep):
    deadline = clock() + timeout
    while True:
        headers = fetch()
        warnings = snapshot_warnings(headers)
        if not warnings:
            return
        remaining = deadline - clock()
        if remaining <= 0:
            raise ValueError(f"Dependency snapshots are incomplete after {timeout}s: {warnings}")
        print(f"Waiting for exact dependency snapshots: {warnings}", flush=True)
        sleep(min(interval, remaining))


def verify_review_summary(summary):
    if not summary.strip():
        raise ValueError("Dependency Review did not provide a review result")
    # v5 places every comparison snapshot warning in this summary section,
    # including when its retry timeout nevertheless returns a successful check.
    if re.search(r"<h2>\s*Snapshot Warnings\s*</h2>", summary, re.IGNORECASE):
        raise ValueError("Dependency Review used incomplete dependency snapshots")


def comparison_fetcher(repository, base_sha, head_sha, token, api_url):
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Invalid dependency review repository")
    if not all(re.fullmatch(r"[0-9a-f]{40}", sha) for sha in (base_sha, head_sha)):
        raise ValueError("Dependency Review requires exact base and head commit SHAs")
    if not token:
        raise ValueError("Dependency Review requires a GitHub API token")
    url = (
        f"{api_url.rstrip('/')}/repos/{repository}/dependency-graph/compare/"
        f"{base_sha}...{head_sha}?per_page=100"
    )

    def fetch():
        request = Request(url, headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "Cache-Control": "no-cache",
        })
        with urlopen(request, timeout=30) as response:
            if not isinstance(json.load(response), list):
                raise ValueError("Dependency comparison returned an invalid delta")
            return response.headers

    return fetch


def main():
    if sys.argv[1:] == ["--review-summary"]:
        verify_review_summary(os.environ.get("DEPENDENCY_REVIEW_SUMMARY", ""))
        print("PASS: Dependency Review reported no incomplete snapshots")
        return
    if sys.argv[1:]:
        raise ValueError("Unexpected dependency snapshot verification arguments")
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    base_sha = os.environ.get("DEPENDENCY_REVIEW_BASE_SHA", "")
    head_sha = os.environ.get("DEPENDENCY_REVIEW_HEAD_SHA", "")
    fetch = comparison_fetcher(
        repository, base_sha, head_sha, os.environ.get("GH_TOKEN", ""),
        os.environ.get("GITHUB_API_URL", "https://api.github.com"),
    )
    wait_for_snapshots(fetch)
    print(f"PASS: complete dependency snapshots for {repository} {base_sha}...{head_sha}")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, URLError, OSError) as error:
        print(f"Dependency snapshot verification failed: {error}", file=sys.stderr)
        sys.exit(1)
