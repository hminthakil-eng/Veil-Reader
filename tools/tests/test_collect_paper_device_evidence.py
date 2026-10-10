"""Non-device guards for opt-in local Paper capture."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from collect_paper_device_evidence import PACKAGE_RE, SHA_RE, online_devices, safe_name


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


if __name__ == "__main__":
    unittest.main()
