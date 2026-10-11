"""Non-device guards for opt-in local Paper capture."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from collect_paper_device_evidence import (PACKAGE_RE, SHA_RE, installed_base_apk,
                                           online_devices, parse_device_sha256sum, safe_name)


class DeviceCaptureGuards(unittest.TestCase):
    def test_requires_exact_commit(self):
        self.assertTrue(SHA_RE.fullmatch("a" * 40))
        self.assertIsNone(SHA_RE.fullmatch("abc"))
        self.assertIsNone(SHA_RE.fullmatch("g" * 40))

    def test_rejects_package_command_injection(self):
        self.assertTrue(PACKAGE_RE.fullmatch("com.veilreader.app.paperlab"))
        for sample in ("com.veilreader;rm -rf", "bad space", "../app", "com..app"):
            self.assertIsNone(PACKAGE_RE.fullmatch(sample))

    def test_only_authorized_online_devices(self):
        output = ("List of devices attached\nAA11\tdevice product:foo\nBB22\tunauthorized\n"
                  "CC33\toffline\nDD44\tdevice\n")
        self.assertEqual(online_devices(output), ["AA11", "DD44"])

    def test_safe_path_suffix(self):
        self.assertEqual(safe_name("serial/../evil:name"), "serial_.._evil_name")


    def test_installed_base_apk_requires_explicit_path(self):
        self.assertEqual(
            installed_base_apk("package:/data/app/~~abc/com.veilreader.app.paperlab/base.apk\n"),
            "/data/app/~~abc/com.veilreader.app.paperlab/base.apk",
        )
        self.assertEqual(
            installed_base_apk(
                "package:/data/app/~~abc/com.veilreader.app.paperlab/base.apk\n"
                "package:/data/app/~~abc/com.veilreader.app.paperlab/split_config.arm64_v8a.apk\n"
            ),
            "/data/app/~~abc/com.veilreader.app.paperlab/base.apk",
        )
        for invalid in ("", "not installed", "package:foo\npackage:bar\n"):
            with self.assertRaises(ValueError):
                installed_base_apk(invalid)

    def test_device_sha256_parser_rejects_missing_or_bad_digest(self):
        digest = "a" * 64
        self.assertEqual(parse_device_sha256sum(digest + "  /data/app/base.apk"), digest)
        for bad in ("", "sha256 not available", "123  /data/app/base.apk"):
            with self.assertRaises(ValueError):
                parse_device_sha256sum(bad)


if __name__ == "__main__":
    unittest.main()
