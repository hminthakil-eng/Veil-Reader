#!/usr/bin/env python3
"""No-network unit tests for the Grand Forge preflight/stage receipt state machine."""
import datetime as dt
import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import roadmap_stage_gate as gate


def fields(action="preflight", status="PLANNED", stage="GF-GOV-08-W1"):
    return dict(action=action, issue="495", stage=stage, status=status,
                objective="Make task status auditable before any work starts.",
                plan="Read current canonical main and all related work before changing files.",
                checks="Exact-head CI, ticket receipt, independent review and rollback.",
                rollback="Revert the isolated governance change without data loss.",
                evidence="", next_step="Record the next stage against issue 495 and current main.")


def comment(action, when="2026-10-11T09:30:00Z", stage="GF-GOV-08-W1", done=False):
    return dict(body=gate.mark(action, stage) + ("\n**Outcome:** DONE_SOURCE" if done else ""),
                created_at=when, user=dict(login="github-actions[bot]"),
                author_association="NONE")


class GateTests(unittest.TestCase):
    def test_accept_complete_preflight(self):
        gate.validate(fields())

    def test_invalid_preflight_outcome_fails(self):
        with self.assertRaises(gate.GateError):
            gate.validate(fields(status="DONE_SOURCE"))

    def test_stage_completion_requires_evidence(self):
        with self.assertRaises(gate.GateError):
            gate.validate(fields(action="stage_end", status="DONE_SOURCE"))

    def test_stage_completion_with_commit_accepted(self):
        record = fields(action="stage_end", status="DONE_SOURCE")
        record["evidence"] = "source sha=" + "a" * 40
        gate.validate(record)

    def test_product_green_not_action_status(self):
        with self.assertRaises(gate.GateError):
            gate.validate(fields(action="stage_end", status="PRODUCT_ACCEPTED"))

    def test_issue_field_rejected_when_not_number(self):
        r = fields()
        r["issue"] = "#495"
        with self.assertRaises(gate.GateError):
            gate.validate(r)

    def test_legacy_pr_warns(self):
        result = gate.pr_check({"pull_request": {"created_at": "2026-10-10T00:00:00Z"}})
        self.assertIn("Legacy PR", result)

    def test_new_pr_requires_metadata(self):
        pr = {"created_at": "2026-10-11T10:00:00Z", "base": {"ref": "main"},
              "body": "Regular PR text", "draft": True}
        with self.assertRaises(gate.GateError):
            gate.pr_check({"pull_request": pr})

    def test_new_pr_requires_preflight_before_open(self):
        pr = {"created_at": "2026-10-11T10:00:00Z", "base": {"ref": "main"},
              "body": "Grand Forge work issue: #495\nGrand Forge stage: GF-GOV-08-W1",
              "draft": True}
        with patch.object(gate, "issue_comments", return_value=[
                comment("preflight", when="2026-10-11T10:05:00Z")]):
            with self.assertRaises(gate.GateError):
                gate.pr_check({"pull_request": pr})

    def test_draft_with_receipt_passes_but_ready_fails_without_finish(self):
        pr = {"created_at": "2026-10-11T10:00:00Z", "base": {"ref": "main"},
              "body": "Grand Forge work issue: #495\nGrand Forge stage: GF-GOV-08-W1",
              "draft": True}
        with patch.object(gate, "issue_comments", return_value=[comment("preflight")]):
            self.assertIn("PASS", gate.pr_check({"pull_request": pr}))
            pr["draft"] = False
            with self.assertRaises(gate.GateError):
                gate.pr_check({"pull_request": pr})

    def test_ready_with_post_stage_source_receipt_passes(self):
        pr = {"created_at": "2026-10-11T10:00:00Z", "base": {"ref": "main"},
              "body": "Grand Forge work issue: #495\nGrand Forge stage: GF-GOV-08-W1",
              "draft": False}
        items = [comment("preflight"), comment("stage_end", "2026-10-11T10:05:00Z", done=True)]
        with patch.object(gate, "issue_comments", return_value=items):
            self.assertIn("PASS", gate.pr_check({"pull_request": pr}))

    def test_untrusted_forged_comment_fails(self):
        r = comment("preflight")
        r["user"]["login"] = "random-user"
        self.assertFalse(gate.trusted(r, gate.mark("preflight", "GF-GOV-08-W1")))

    def test_receipt_workflow_ref_is_pinned(self):
        r = fields()
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/experiment/untrusted"}, clear=True):
            with self.assertRaises(gate.GateError):
                gate.record(r)


    def test_preflight_posts_to_work_and_command_center(self):
        r = fields(stage="UX-01-W9")
        r["issue"] = "496"
        sha = "a" * 40
        calls = []

        def fake_api(method, path, payload=None):
            calls.append((method, path, payload))
            if path == "/branches/main":
                return {"commit": {"sha": sha}}
            if path == "/issues/496":
                return {"state": "open"}
            if path.startswith("/issues/496/comments?"):
                return []
            if path.startswith("/contents/"):
                return {"sha": "b" * 40}
            if path.endswith("/comments") and method == "POST":
                return {"html_url": "https://github.com/hminthakil-eng/Veil-Reader/issues/496#issuecomment-1"}
            raise AssertionError(path)

        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/main", "GITHUB_SHA": sha}):
            with patch.object(gate, "api", side_effect=fake_api):
                self.assertIn("Posted:", gate.record(r))
        written = [path for method, path, _ in calls if method == "POST"]
        self.assertEqual(written, ["/issues/496/comments", "/issues/495/comments"])

    def test_outdated_main_aborts_before_posting(self):
        r = fields(stage="DATA-01-W3")
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/main", "GITHUB_SHA": "b" * 40}):
            with patch.object(gate, "api", return_value={"commit": {"sha": "a" * 40}}):
                with self.assertRaises(gate.GateError):
                    gate.record(r)

if __name__ == "__main__":
    unittest.main()
