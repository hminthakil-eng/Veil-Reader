"""Mutation-based regression tests for the source workflow permission boundary."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_reader_supply_chain import WORKFLOW, CANONICAL, SYFT, check, main


class ReaderSupplyChainContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = WORKFLOW.read_text(encoding="utf-8")

    def test_current_workflow_is_safe(self):
        self.assertEqual([], check(self.original))
        self.assertEqual(0, main())

    def test_missing_canonical_branch_on_pr(self):
        broken = self.original.replace(
            f'branches: [ "{CANONICAL}", "grand-forge/tts-listening-mode-v1"',
            'branches: [ "grand-forge/tts-listening-mode-v1"', 1
        )
        self.assertIn("pull_request must include canonical Reader branch", check(broken))

    def test_missing_canonical_branch_on_push(self):
        original = f'branches: [ "{CANONICAL}", "grand-forge/arena-app-hardening-v1"'
        self.assertIn(original, self.original)
        broken = self.original.replace(original,
            'branches: [ "grand-forge/arena-app-hardening-v1"', 1)
        self.assertIn("push must include canonical Reader branch", check(broken))

    def test_rejects_global_write(self):
        self.assertIn("default workflow permission must be contents:read",
            check(self.original.replace("permissions:\n  contents: read",
                                        "permissions:\n  contents: write", 1)))

    def test_rejects_pr_write(self):
        broken = self.original.replace(
            "  pr-source-sbom:\n    name: pr-source-sbom\n    if: github.event_name == 'pull_request'\n    permissions:\n      contents: read",
            "  pr-source-sbom:\n    name: pr-source-sbom\n    if: github.event_name == 'pull_request'\n    permissions:\n      contents: write"
        )
        self.assertIn("PR job must explicitly be contents:read", check(broken))

    def test_rejects_pr_graph_submission(self):
        broken = self.original.replace(
            "  dependency-graph:\n",
            "      - uses: gradle/actions/dependency-submission@v6\n\n  dependency-graph:\n", 1
        )
        self.assertIn("PR job must not submit dependency graph or obtain write token", check(broken))

    def test_rejects_unrestricted_graph_job(self):
        broken = self.original.replace(
            "    if: github.event_name != 'pull_request'\n",
            "    if: always()\n", 1
        )
        self.assertIn("privileged graph job must exclude pull_request", check(broken))

    def test_requires_pinned_pr_inventory(self):
        broken = self.original.replace(SYFT, "anchore/sbom-action@v0", 1)
        self.assertIn("PR source inventory must use pinned Syft CycloneDX", check(broken))

    def test_requires_trusted_graph_submission(self):
        broken = self.original.replace("gradle/actions/dependency-submission@v6",
                                       "gradle/actions/setup-gradle@v6", 1)
        self.assertIn("trusted graph job must submit resolved Gradle graph", check(broken))


if __name__ == "__main__":
    unittest.main()
