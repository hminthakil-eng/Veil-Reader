"""Synthetic ADB tests; never connects to or modifies a physical device."""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from paper_apk_identity import file_sha256, single_installed_base_apk, verify_installed_apk


class InstalledApkIdentityTest(unittest.TestCase):
    def test_only_one_base_apk_is_allowed(self):
        base = "package:/data/app/~~abc/com.veilreader.app.forgeqa-xyz/base.apk"
        self.assertEqual(single_installed_base_apk(base), base[len("package:"):])
        for bad in (
            "",
            base + "\npackage:/data/app/x/split_config.arm64_v8a.apk",
            "package:/system/app/Veil/base.apk",
            "package:/data/app/abc/../secret/base.apk",
            "package:/data/app/abc/evil.apk",
            "package:/data/app/abc/bad name/base.apk",
            "package:/data/app/abc\\evil/base.apk",
            "garbage",
        ):
            with self.subTest(path=bad):
                with self.assertRaises(ValueError):
                    single_installed_base_apk(bad)

    def test_match_and_mismatch_are_explicit(self):
        with tempfile.TemporaryDirectory() as tmp:
            local = Path(tmp) / "veil.apk"
            local.write_bytes(b"good local QA binary")
            expected = file_sha256(local)
            def runner_factory(installed):
                def runner(args, timeout=20):
                    if args[-4:-1] == ["shell", "pm", "path"]:
                        return "package:/data/app/~~xyz/com.veilreader.app.forgeqa-x/base.apk"
                    if len(args) >= 3 and args[-3] == "pull":
                        Path(args[-1]).write_bytes(installed)
                        return "1 file pulled"
                    raise AssertionError(args)
                return runner
            same = verify_installed_apk(["adb", "-s", "serial"], "com.veilreader.app.forgeqa",
                                        local, runner_factory(b"good local QA binary"))
            self.assertTrue(same["match"])
            self.assertEqual(same["status"], "MATCH")
            self.assertEqual(same["local_sha256"], expected)
            self.assertFalse(same["source_commit_verified"])
            other = verify_installed_apk(["adb", "-s", "serial"], "com.veilreader.app.forgeqa",
                                         local, runner_factory(b"different APK"))
            self.assertFalse(other["match"])
            self.assertEqual(other["status"], "MISMATCH")
            self.assertNotEqual(other["installed_sha256"], expected)

    def test_pull_failure_does_not_claim_match(self):
        with tempfile.TemporaryDirectory() as tmp:
            local = Path(tmp) / "veil.apk"
            local.write_bytes(b"content")
            def runner(args, timeout=20):
                if "path" in args:
                    return "package:/data/app/a/pkg/base.apk"
                raise RuntimeError("ADB pull failed")
            with self.assertRaises(RuntimeError):
                verify_installed_apk(["adb"], "com.veilreader.app.forgeqa", local, runner)

    def test_rejects_missing_and_non_apk_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                verify_installed_apk(["adb"], "com.veilreader.app.forgeqa",
                                     Path(tmp) / "missing.apk", lambda *a, **kw: "")
            path = Path(tmp) / "not-apk.zip"
            path.write_bytes(b"data")
            with self.assertRaises(ValueError):
                verify_installed_apk(["adb"], "com.veilreader.app.forgeqa", path,
                                     lambda *a, **kw: "")


if __name__ == "__main__":
    unittest.main()
