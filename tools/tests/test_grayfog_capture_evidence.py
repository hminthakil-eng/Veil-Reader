"""Offline synthetic capture archive tests; no APK/device/book content needed."""
from __future__ import annotations

import io
import json
import struct
import sys
import tarfile
import tempfile
import unittest
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_grayfog_capture_evidence import (
    EvidenceError, check_archive, changed_sample_fraction, expected_filenames,
    main, read_png_pixels,
)


def chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)


def fixture_png(tint: int = 0, *, color: int = 2, filter_mode: int = 0) -> bytes:
    width, height = 96, 96
    bpp = 3 if color == 2 else 4
    lines = bytearray()
    for y in range(height):
        lines.append(filter_mode)
        for x in range(width):
            lines.extend(((x * 13 + tint) % 256, (y * 7 + tint * 2) % 256,
                          ((x ^ y) * 9 + tint) % 256))
            if bpp == 4:
                lines.append(255)
    body = zlib.compress(bytes(lines), 6)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, color, 0, 0, 0)) +
            chunk(b"IDAT", body) + chunk(b"IEND", b""))


def tar_fixture(dest: Path, overrides: dict[str, bytes | None] | None = None,
                *, duplicate: str | None = None, symlink: bool = False,
                bad_name: str | None = None):
    overrides = overrides or {}
    names = expected_filenames()
    base, large = fixture_png(0), fixture_png(45)
    with tarfile.open(dest, "w") as archive:
        for name in sorted(names):
            if name in overrides and overrides[name] is None:
                continue
            blob = overrides.get(name, large if "-200-" in name else base)
            info = tarfile.TarInfo("grayfog-review/" + name)
            info.size = len(blob)
            archive.addfile(info, io.BytesIO(blob))
            if name == duplicate:
                archive.addfile(info, io.BytesIO(blob))
        if symlink:
            link = tarfile.TarInfo("grayfog-review/book.png")
            link.type = tarfile.SYMTYPE
            link.linkname = "../../personal-data"
            archive.addfile(link)
        if bad_name:
            info = tarfile.TarInfo(bad_name)
            info.size = len(base)
            archive.addfile(info, io.BytesIO(base))


class GrayfogCaptureEvidenceTest(unittest.TestCase):
    def test_expected_corpus_is_complete_and_stable(self):
        self.assertEqual(120, len(expected_filenames()))
        self.assertIn("archive-search-fa-200-contrast.png", expected_filenames())
        self.assertIn("dialog-book_detail-fa-100-standard.png", expected_filenames())

    def test_valid_export_has_three_actual_pixel_differences(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "captures.tar"
            tar_fixture(archive)
            result = check_archive(archive)
            self.assertEqual("PASS", result["status"])
            self.assertEqual(120, result["capture_count"])
            self.assertEqual(3, len(result["font_scale_pixel_change"]))
            self.assertFalse(result["physical_device_pass"])

    def test_missing_capture_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "captures.tar"
            tar_fixture(archive, {"threshold-empty-fa-130-contrast.png": None})
            with self.assertRaisesRegex(EvidenceError, "screenshot.*missing"):
                check_archive(archive)

    def test_duplicate_filename_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "captures.tar"
            tar_fixture(archive, duplicate="threshold-empty-fa-130-contrast.png")
            with self.assertRaisesRegex(EvidenceError, "duplicate screenshot"):
                check_archive(archive)

    def test_tar_symlinks_and_path_traversal_are_rejected(self):
        for extra in ("symlink", "traversal"):
            with self.subTest(extra=extra), tempfile.TemporaryDirectory() as tmp:
                archive = Path(tmp) / "captures.tar"
                tar_fixture(archive, symlink=extra == "symlink",
                            bad_name="grayfog-review/../private/secret.png" if extra == "traversal" else None)
                with self.assertRaises(EvidenceError):
                    check_archive(archive)

    def test_corrupt_png_crc_is_rejected(self):
        good = fixture_png()
        with self.assertRaisesRegex(EvidenceError, "CRC"):
            read_png_pixels(good[:-1] + b"!")

    def test_invalid_filter_is_rejected(self):
        with self.assertRaisesRegex(EvidenceError, "filter"):
            read_png_pixels(fixture_png(filter_mode=5))

    def test_identical_font_sizes_are_rejected(self):
        good = fixture_png()
        self.assertEqual(0.0, changed_sample_fraction(good, good))
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "captures.tar"
            tar_fixture(archive, {"threshold-entrance-fa-200-standard.png": good})
            with self.assertRaisesRegex(EvidenceError, "unexpectedly alike"):
                check_archive(archive)

    def test_failed_validation_reports_fail_and_no_physical_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "missing.tar"
            report = Path(tmp) / "report.json"
            self.assertEqual(1, main(["--archive", str(archive), "--report", str(report)]))
            data = json.loads(report.read_text())
            self.assertEqual("FAIL", data["status"])
            self.assertFalse(data["physical_device_pass"])

    def test_supports_rgb_and_rgba_android_bitmaps(self):
        for color in (2, 6):
            (width, height), samples = read_png_pixels(fixture_png(15, color=color))
            self.assertEqual((96, 96), (width, height))
            self.assertEqual(4096, len(samples))


if __name__ == "__main__":
    unittest.main()
