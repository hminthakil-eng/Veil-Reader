"""Safety and determinism tests for the non-executing APK inventory."""

import hashlib
from pathlib import Path
import tempfile
import unittest
import zipfile

from reader_reference_inventory import UnsafeArchive, inventory, report, to_markdown


class ReferenceInventoryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "sample.apk"

    def build(self, members):
        with zipfile.ZipFile(self.path, "w") as archive:
            for name, content in members:
                archive.writestr(name, content)

    def test_counts_and_hash_are_deterministic(self):
        self.build([
            ("classes.dex", b"fake"),
            ("classes3.dex", b"another"),
            ("lib/arm64-v8a/libreader.so", b"ELF?"),
            ("assets/fonts/Book.ttf", b"font"),
            ("assets/hyphenation/en", b"patterns"),
            ("assets/background/paper.png", b"image"),
            ("META-INF/APP.RSA", b"signer"),
        ])
        first, second = inventory(self.path), inventory(self.path)
        self.assertEqual(first, second)
        self.assertEqual(
            first["sha256"],
            hashlib.sha256(self.path.read_bytes()).hexdigest(),
        )
        self.assertEqual(2, len(first["dex_files"]))
        self.assertEqual(
            ["libreader.so"],
            first["native_libraries_by_abi"]["arm64-v8a"],
        )
        self.assertEqual(1, len(first["font_paths"]))
        self.assertEqual(1, first["hyphenation_entry_count"])
        self.assertEqual(1, first["background_image_count"])
        self.assertIn(
            "not runtime feature evidence",
            to_markdown(report([self.path])).lower(),
        )

    def test_refuses_path_traversal(self):
        self.build([("../secret.dex", b"data")])
        with self.assertRaises(UnsafeArchive):
            inventory(self.path)

    def test_refuses_backslash_and_absolute_entries(self):
        for name in ("\\tmp\\bad.so", "/absolute/file.so"):
            self.build([(name, b"a")])
            with self.assertRaises(UnsafeArchive):
                inventory(self.path)

    def test_refuses_duplicate_entries(self):
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            self.build([
                ("assets/a.png", b"a"),
                ("assets/a.png", b"b"),
            ])
        with self.assertRaises(UnsafeArchive):
            inventory(self.path)

    def test_deduplicates_identical_apks_by_content_not_name(self):
        self.build([("classes.dex", b"fake")])
        other = self.path.parent / "copy.apk"
        other.write_bytes(self.path.read_bytes())
        result = report([self.path, other])
        self.assertEqual(1, len(result["duplicate_sha256"]))
        self.assertEqual(2, len(result["apks"]))


if __name__ == "__main__":
    unittest.main()
