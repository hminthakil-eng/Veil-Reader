#!/usr/bin/env python3
"""Lightweight Company OS duplicate-work preflight.

Designed for GitHub Actions issue events. Uses only Python stdlib.
It never decides that an issue is a duplicate automatically; it reports
possible matches for the human/SID preflight gate.
"""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.request
from dataclasses import dataclass
from difflib import SequenceMatcher
from pathlib import Path

API = "https://api.github.com"
STOP = {
    "the","a","an","and","or","to","of","in","for","on","with","from","by","as",
    "is","are","be","this","that","it","we","our","new","work","task","issue",
    "company","eyad","studio","pilot","add","update","build","design","research",
}


@dataclass
class Candidate:
    repo: str
    number: int
    title: str
    body: str
    url: str
    state: str


def tokens(text: str) -> set[str]:
    words = re.findall(r"[a-z0-9][a-z0-9_+.-]{1,}", text.lower())
    return {w for w in words if w not in STOP and len(w) > 2}


def score(a: str, b: str) -> float:
    ta, tb = tokens(a), tokens(b)
    if not ta or not tb:
        return 0.0
    jaccard = len(ta & tb) / len(ta | tb)
    seq = SequenceMatcher(None, " ".join(sorted(ta)), " ".join(sorted(tb))).ratio()
    return 0.70 * jaccard + 0.30 * seq


def request_json(url: str, token: str):
    req = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "eyad-company-os-reuse-gate",
        },
    )
    with urllib.request.urlopen(req, timeout=20) as response:
        return json.load(response)


def post_comment(repo: str, number: int, token: str, body: str) -> None:
    data = json.dumps({"body": body}).encode()
    req = urllib.request.Request(
        f"{API}/repos/{repo}/issues/{number}/comments",
        data=data,
        method="POST",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": "application/json",
            "User-Agent": "eyad-company-os-reuse-gate",
        },
    )
    with urllib.request.urlopen(req, timeout=20):
        pass


def load_repos(path: Path) -> list[str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return list(dict.fromkeys(data["repositories"]))


def fetch_issues(repo: str, token: str, max_pages: int = 3) -> list[Candidate]:
    out: list[Candidate] = []
    for page in range(1, max_pages + 1):
        url = f"{API}/repos/{repo}/issues?state=all&per_page=100&page={page}"
        rows = request_json(url, token)
        if not rows:
            break
        for row in rows:
            if "pull_request" in row:
                continue
            out.append(
                Candidate(
                    repo=repo,
                    number=row["number"],
                    title=row.get("title") or "",
                    body=row.get("body") or "",
                    url=row.get("html_url") or "",
                    state=row.get("state") or "",
                )
            )
    return out


def main() -> int:
    token = os.environ.get("GITHUB_TOKEN")
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not token or not event_path:
        print("GITHUB_TOKEN and GITHUB_EVENT_PATH are required", file=sys.stderr)
        return 2

    event = json.loads(Path(event_path).read_text(encoding="utf-8"))
    issue = event.get("issue") or {}
    current_repo = (event.get("repository") or {}).get("full_name")
    current_number = issue.get("number")
    title = issue.get("title") or ""
    body = issue.get("body") or ""

    if not title.startswith("[WORK]") and "Preflight:" not in body:
        print("Not a canonical Company OS Work Item; scanner skipped.")
        return 0

    query_text = f"{title}\n{body[:8000]}"
    repos = load_repos(Path(__file__).with_name("work-repos.json"))

    matches: list[tuple[float, Candidate]] = []
    for repo in repos:
        try:
            candidates = fetch_issues(repo, token)
        except Exception as exc:
            print(f"warning: failed to scan {repo}: {exc}", file=sys.stderr)
            continue
        for candidate in candidates:
            if repo == current_repo and candidate.number == current_number:
                continue
            s = score(query_text, f"{candidate.title}\n{candidate.body[:8000]}")
            if s >= 0.30:
                matches.append((s, candidate))

    matches.sort(key=lambda x: x[0], reverse=True)
    top = matches[:5]

    if top:
        lines = [
            "## Company OS — Work Reuse Preflight",
            "",
            "Possible existing work matches were found. **Do not start execution until these are reviewed.**",
            "",
        ]
        for s, c in top:
            lines.append(f"- **{s:.0%} similarity** — [{c.repo}#{c.number}: {c.title}]({c.url}) ({c.state})")
        lines += [
            "",
            "Required action: classify each as EXACT_DUPLICATE, SEMANTIC_DUPLICATE, "
            "PARTIAL_OVERLAP, CONFLICT, or DISTINCT, then record the canonical Work ID and reuse decision.",
            "",
            "_This scanner is advisory. SID/Company OS owns the final duplicate/reuse decision._",
        ]
    else:
        lines = [
            "## Company OS — Work Reuse Preflight",
            "",
            "No high-similarity work item was found by the lightweight scanner.",
            "",
            "This **does not replace** the mandatory manual/SID search across docs, decisions, registries, PRs and external solutions.",
        ]

    post_comment(current_repo, current_number, token, "\n".join(lines))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
