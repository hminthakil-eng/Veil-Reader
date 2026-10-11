#!/usr/bin/env python3
"""Grand Forge pre-work/post-stage GitHub receipt gate. Python stdlib only.

A receipt proves record existence, NOT whether physical or product claims are true.
"""
import datetime as dt
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

CUTOFF = dt.datetime(2026, 10, 11, 9, 32, 42, tzinfo=dt.timezone.utc)
STAGE_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{2,79}$")
ISSUE_RE = re.compile(r"(?im)^Grand Forge work issue:\s*#([1-9]\d*)\s*$")
STAGE_LINE_RE = re.compile(r"(?im)^Grand Forge stage:\s*([A-Za-z0-9._-]{3,80})\s*$")
END_STATUSES = {"DONE_SOURCE", "BLOCKED", "TEST_FAILED", "REJECTED", "SUPERSEDED", "NEEDS_REWORK"}
DOCS = ("docs/roadmap/GRAND_FORGE_MASTER_ROADMAP.md",
        "docs/roadmap/LIVE_STATUS.md",
        "docs/roadmap/BRANCH_AND_ARCHIVE_POLICY.md")


class GateError(Exception):
    pass


def timestamp(s):
    return dt.datetime.fromisoformat(s.replace("Z", "+00:00"))


def mark(action, stage):
    return "<!-- grand-forge-receipt:v1:" + action + ":" + stage + " -->"


def validate(fields):
    action, stage = fields["action"], fields["stage"]
    if action not in ("preflight", "stage_end") or not STAGE_RE.fullmatch(stage):
        raise GateError("Invalid action or stage identifier")
    if not fields["issue"].isdigit() or int(fields["issue"]) < 1:
        raise GateError("Work issue number must be positive")
    for key in ("objective", "plan", "checks", "rollback", "next_step"):
        value = fields[key].strip()
        if len(value) < 12 or len(value) > 3000:
            raise GateError(key + " must have substantive content (12-3000 chars)")
    if action == "preflight" and fields["status"] != "PLANNED":
        raise GateError("Pre-work outcome must be PLANNED")
    if action == "stage_end" and fields["status"] not in END_STATUSES:
        raise GateError("Unrecognized conclusion; this workflow never confers device/product GREEN")
    if fields["status"] == "DONE_SOURCE" and not re.search(
            r"https://github[.]com/\S+|[0-9a-f]{40}", fields["evidence"]):
        raise GateError("DONE_SOURCE needs a GitHub artifact URL or exact 40-character commit")
    if any(len(v) > 6000 or any(ord(c) < 32 and c not in "\n\t" for c in v)
           for v in fields.values()):
        raise GateError("Unsafe or oversized input")


def api(method, path, payload=None):
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    token = os.environ.get("GH_TOKEN", "")
    if not re.fullmatch(r"[\w.-]+/[\w.-]+", repo) or not token:
        raise GateError("Missing repo or GH_TOKEN")
    req = urllib.request.Request(
        "https://api.github.com/repos/" + repo + path,
        data=json.dumps(payload).encode() if payload is not None else None,
        method=method,
        headers={"Accept": "application/vnd.github+json", "Authorization": "Bearer " + token,
                 "X-GitHub-Api-Version": "2022-11-28", "Content-Type": "application/json",
                 "User-Agent": "Veil-Reader-Grand-Forge"})
    try:
        with urllib.request.urlopen(req, timeout=20) as result:
            return json.loads(result.read().decode())
    except urllib.error.HTTPError as e:
        raise GateError("GitHub " + path + " returned HTTP " + str(e.code)) from e


def issue_comments(number):
    result = []
    for page in range(1, 6):
        chunk = api("GET", "/issues/%d/comments?per_page=100&page=%d" % (number, page))
        if not isinstance(chunk, list):
            raise GateError("Comments response malformed")
        result += chunk
        if len(chunk) < 100:
            return result
    raise GateError("More than 500 comments; expand pagination before use")


def trusted(comment, marker):
    author = (comment.get("user") or {}).get("login", "")
    assoc = comment.get("author_association", "")
    return marker in (comment.get("body") or "") and (
        author == "github-actions[bot]" or assoc in ("OWNER", "MEMBER", "COLLABORATOR"))


def pr_check(event):
    pr = event["pull_request"]
    created = timestamp(pr["created_at"])
    if created < CUTOFF:
        return "::warning::Legacy PR: receipt gate grandfathered; human review still required"
    if pr["base"]["ref"] != "main":
        raise GateError("New PR must target main; shadow/stacked verification requires explicit review")
    body = pr.get("body") or ""
    im, sm = ISSUE_RE.search(body), STAGE_LINE_RE.search(body)
    if im is None or sm is None:
        raise GateError("Missing Grand Forge work issue: #N / Grand Forge stage: ID lines")
    issue, stage = int(im.group(1)), sm.group(1)
    rows = issue_comments(issue)
    pre = [r for r in rows if trusted(r, mark("preflight", stage))
           and timestamp(r["created_at"]) <= created]
    if not pre:
        raise GateError("A trusted PRE-WORK receipt must predate PR opening")
    if not pr.get("draft", False):
        done = [r for r in rows if trusted(r, mark("stage_end", stage))
                and timestamp(r["created_at"]) >= timestamp(pre[-1]["created_at"])]
        if not done or "**Outcome:** DONE_SOURCE" not in done[-1].get("body", ""):
            raise GateError("Ready PR must have stage-end DONE_SOURCE receipt with source evidence")
    return "PASS: issue #%d stage %s (product/device gates still independent)" % (issue, stage)


def record(fields):
    validate(fields)
    if os.environ.get("GITHUB_REF") != "refs/heads/main":
        raise GateError("Dispatch ledger only from trusted main")
    current = api("GET", "/branches/main")["commit"]["sha"]
    if not re.fullmatch("[0-9a-f]{40}", current):
        raise GateError("Invalid main HEAD")
    if current != os.environ.get("GITHUB_SHA"):
        raise GateError("main moved after workflow dispatch; rerun against newest version")
    for path in DOCS:
        api("GET", "/contents/" + urllib.parse.quote(path, safe="/") + "?ref=main")
    issue = int(fields["issue"])
    item = api("GET", "/issues/%d" % issue)
    if item.get("state") != "open" or "pull_request" in item:
        raise GateError("Issue must be open and not a PR")
    comments = issue_comments(issue)
    pre = [r for r in comments if trusted(r, mark("preflight", fields["stage"]))]
    completed = [r for r in comments if trusted(r, mark("stage_end", fields["stage"]))]
    if fields["action"] == "preflight":
        if pre and (not completed or timestamp(completed[-1]["created_at"]) <
                    timestamp(pre[-1]["created_at"])):
            raise GateError("Stage preflight already open. Complete it first.")
        if pre:
            raise GateError("Stage ID used before; new work needs a new stage ID.")
    elif not pre or completed:
        raise GateError("Cannot finish without previous preflight, or stage already ended")
    action = fields["action"]
    text = "\n\n".join([
        mark(action, fields["stage"]),
        "### Grand Forge " + ("PRE-WORK" if action == "preflight" else "POST-STAGE")
        + " | " + fields["stage"],
        "**Outcome:** " + fields["status"],
        "**Canonical main checked:** " + current,
        "**Work issue:** #" + fields["issue"],
        "**Objective:** " + fields["objective"],
        "**Plan / performed:** " + fields["plan"],
        "**Checks / acceptance:** " + fields["checks"],
        "**Rollback / risk:** " + fields["rollback"],
        "**Evidence:** " + (fields["evidence"] or "Not established yet"),
        "**Next step:** " + fields["next_step"],
        "This receipt is a record, not a product/device/release approval.",
    ])
    posted = api("POST", "/issues/%d/comments" % issue, {"body": text})
    if action == "stage_end" and issue != 495:
        api("POST", "/issues/495/comments", {"body":
            "### Stage " + fields["stage"] + " | " + fields["status"] +
            "\nIssue: #" + str(issue) + "\nReceipt: " + posted["html_url"] +
            "\nNext: " + fields["next_step"]})
    return "Posted: " + posted.get("html_url", "")


def main():
    try:
        if sys.argv[1:] == ["pr-check"]:
            data = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
            print(pr_check(data))
        elif sys.argv[1:] == ["record"]:
            keys = ("action", "issue", "stage", "status", "objective", "plan",
                    "checks", "rollback", "evidence", "next_step")
            print(record({k: os.environ.get("GF_" + k.upper(), "").strip() for k in keys}))
        else:
            raise GateError("Expected pr-check or record")
        return 0
    except (GateError, ValueError, KeyError, OSError) as exc:
        print("::error::Grand Forge stage gate: " + str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
