import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

import build_identity as identity


class BuildIdentityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-q")
        self.git("config", "user.email", "fixture@example.invalid")
        self.git("config", "user.name", "Test Fixture")
        (self.root / "source.txt").write_text("original")
        self.git("add", "source.txt")
        self.git("commit", "-qm", "fixture")
        self.apk = self.root / "app.apk"

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args], text=True)

    def package(self, data):
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr(identity.APK_ASSET, json.dumps(data))

    def test_current_identity_and_hash_are_preserved(self):
        prepared = identity.prepare(self.root)
        self.package(prepared)
        result = identity.verify(self.root, self.apk, self.root / "evidence")
        self.assertEqual(prepared["commit"], self.git("rev-parse", "HEAD").strip())
        self.assertEqual(hashlib.sha256(self.apk.read_bytes()).hexdigest(), result["sha256"])
        copied = self.root / "evidence" / result["artifact"]
        self.assertEqual(self.apk.read_bytes(), copied.read_bytes())

    def test_stale_apk_from_same_commit_is_rejected(self):
        prepared = identity.prepare(self.root)
        self.package({**prepared, "build_id": "previous-build"})
        with self.assertRaisesRegex(ValueError, "stale APK"):
            identity.verify(self.root, self.apk, self.root / "evidence")
        self.assertFalse((self.root / "evidence").exists())

    def test_missing_asset_is_rejected(self):
        identity.prepare(self.root)
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("unrelated", "data")
        with self.assertRaises(KeyError):
            identity.verify(self.root, self.apk, self.root / "evidence")

    def test_tracked_changes_are_rejected(self):
        (self.root / "source.txt").write_text("changed")
        with self.assertRaisesRegex(ValueError, "dirty"):
            identity.prepare(self.root)

    def test_head_change_after_prepare_is_rejected(self):
        prepared = identity.prepare(self.root)
        self.package(prepared)
        (self.root / "source.txt").write_text("new revision")
        self.git("commit", "-qam", "next")
        with self.assertRaisesRegex(ValueError, "HEAD changed"):
            identity.verify(self.root, self.apk, self.root / "evidence")

    def test_untracked_app_source_is_rejected(self):
        source = self.root / "app/src/main/java/Unexpected.kt"
        source.parent.mkdir(parents=True)
        source.write_text("class Unexpected")
        with self.assertRaisesRegex(ValueError, "Untracked source"):
            identity.prepare(self.root)


if __name__ == "__main__":
    unittest.main()
