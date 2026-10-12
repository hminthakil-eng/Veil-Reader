import unittest
from verify_build_dependency_graph import verify


class BuildGraphTest(unittest.TestCase):
    def graph(self, versions=("1.85",)):
        return {"sha": "tested", "manifests": {"settings.gradle.kts": {"resolved": {
            str(i): {"package_url": f"pkg:maven/org.bouncycastle/bcprov-jdk18on@{version}"}
            for i, version in enumerate(versions)}}}}

    def test_old_settings_resolution_cannot_hide_behind_patched_project_resolution(self):
        with self.assertRaises(ValueError):
            verify(self.graph(("1.85", "1.80.2")), "tested")

    def test_snapshot_source_and_empty_graph_are_not_success_evidence(self):
        with self.assertRaises(ValueError):
            verify(self.graph(), "other-head")
        with self.assertRaises(ValueError):
            verify({"sha": "tested", "manifests": {}}, "tested")

    def test_patched_graph(self):
        self.assertEqual(["1.85"], verify(self.graph(), "tested"))


if __name__ == "__main__":
    unittest.main()
