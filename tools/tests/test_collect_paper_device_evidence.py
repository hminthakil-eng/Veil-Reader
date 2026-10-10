"""Non-device guards for opt-in local Paper capture."""
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from collect_paper_device_evidence import PACKAGE_RE, SHA_RE, online_devices, safe_name, main


class DeviceCaptureGuards(unittest.TestCase):
    def test_requires_exact_commit(self):
        self.assertTrue(SHA_RE.fullmatch("a" * 40))
        self.assertIsNone(SHA_RE.fullmatch("abc"))
        self.assertIsNone(SHA_RE.fullmatch("g" * 40))

    def test_rejects_package_command_injection(self):
        self.assertTrue(PACKAGE_RE.fullmatch("com.veilreader.app.forgeqa"))
        for sample in ("com.veilreader;rm -rf", "bad space", "../app", "com..app"):
            self.assertIsNone(PACKAGE_RE.fullmatch(sample))

    def test_only_authorized_online_devices(self):
        output = ("List of devices attached\nAA11\tdevice product:foo\nBB22\tunauthorized\n"
                  "CC33\toffline\nDD44\tdevice\n")
        self.assertEqual(online_devices(output), ["AA11", "DD44"])

    def test_safe_path_suffix(self):
        self.assertEqual(safe_name("serial/../evil:name"), "serial_.._evil_name")


    def test_mismatch_prevents_recording_and_remains_unverified(self):
        with tempfile.TemporaryDirectory() as tmp:
            local = Path(tmp) / "qa.apk"
            local.write_bytes(b"test artifact")
            out = Path(tmp) / "private"
            def fake_run(args, **kwargs):
                return "List of devices attached\nAA11\tdevice\n" if args == ["adb", "devices"] else "available"
            with patch("collect_paper_device_evidence.run", side_effect=fake_run), \
                 patch("collect_paper_device_evidence.verify_installed_apk",
                       return_value={"match": False, "status": "MISMATCH"}), \
                 patch("builtins.input", side_effect=AssertionError("video must not start")):
                code = main(["--source-sha", "a" * 40, "--apk", str(local),
                             "--verify-installed-apk", "--video", "--output", str(out)])
            self.assertEqual(code, 2)
            manifest = json.loads(next(out.rglob("manifest.json")).read_text())
            self.assertEqual(manifest["status"], "UNVERIFIED")
            self.assertEqual(manifest["installed_apk_byte_identity"]["status"], "MISMATCH")

    def test_missing_installed_binary_prevents_capture(self):
        with tempfile.TemporaryDirectory() as tmp:
            local = Path(tmp) / "qa.apk"
            local.write_bytes(b"test")
            out = Path(tmp) / "private"
            def fake_run(args, **kwargs):
                return "List of devices attached\nAA11\tdevice\n" if args == ["adb", "devices"] else "available"
            with patch("collect_paper_device_evidence.run", side_effect=fake_run), \
                 patch("collect_paper_device_evidence.verify_installed_apk",
                       side_effect=RuntimeError("pull failed")), \
                 patch("builtins.input", side_effect=AssertionError("video must not start")):
                self.assertEqual(main(["--source-sha", "a" * 40, "--apk", str(local),
                                       "--verify-installed-apk", "--video", "--output", str(out)]), 2)
            manifest = json.loads(next(out.rglob("manifest.json")).read_text())
            self.assertEqual(manifest["installed_apk_byte_identity"]["status"], "UNVERIFIED")

    def test_binary_match_does_not_mark_physical_quality_green(self):
        with tempfile.TemporaryDirectory() as tmp:
            local = Path(tmp) / "qa.apk"
            local.write_bytes(b"test")
            out = Path(tmp) / "private"
            def fake_run(args, **kwargs):
                return "List of devices attached\nAA11\tdevice\n" if args == ["adb", "devices"] else "available"
            with patch("collect_paper_device_evidence.run", side_effect=fake_run), \
                 patch("collect_paper_device_evidence.verify_installed_apk",
                       return_value={"match": True, "status": "MATCH", "source_commit_verified": False}):
                self.assertEqual(main(["--source-sha", "a" * 40, "--apk", str(local),
                                       "--verify-installed-apk", "--output", str(out)]), 0)
            manifest = json.loads(next(out.rglob("manifest.json")).read_text())
            self.assertEqual(manifest["installed_apk_byte_identity"]["status"], "MATCH")
            self.assertFalse(manifest["source_identity_verified_against_installed_apk"])
            self.assertEqual(manifest["physical_acceptance"], "NOT_REVIEWED")
            self.assertEqual(manifest["status"], "UNVERIFIED")


if __name__ == "__main__":
    unittest.main()
