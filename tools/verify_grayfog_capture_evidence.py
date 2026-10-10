#!/usr/bin/env python3
"""Fail closed on missing/corrupt Grayfog emulator review screenshots.

This examines inert test fixtures from storage CI, not publications, user data,
or physical-device Paper evidence. Uses only the Python standard library.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import struct
import sys
import tarfile
import zlib
from pathlib import Path, PurePosixPath

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
SURFACES = (
    "archive-search", "threshold-entrance", "threshold-resume",
    "threshold-empty", "archive-index", "archive-gallery",
)
DIALOGS = ("book_detail", "ritual", "error")
SCALES = (100, 130, 150, 200)
LANGUAGES = ("en", "fa")
THEMES = ("standard", "contrast")
MAX_CAPTURE_BYTES = 12 * 1024 * 1024
MAX_DIMENSION = 4096
SAMPLE_GRID = 64


def expected_filenames() -> set[str]:
    shell = {
        f"{surface}-{language}-{scale}-{theme}.png"
        for surface in SURFACES for language in LANGUAGES
        for scale in SCALES for theme in THEMES
    }
    dialogs = {
        f"dialog-{surface}-{language}-{scale}-{theme}.png"
        for surface in DIALOGS for language in LANGUAGES
        for scale in (100, 200) for theme in THEMES
    }
    return shell | dialogs


class EvidenceError(ValueError):
    pass


def read_png_pixels(content: bytes) -> tuple[tuple[int, int], list[tuple[int, int, int]]]:
    """Verify CRC+bounded 8-bit RGB/RGBA pixels; retain only a sample grid."""
    if not (PNG_SIGNATURE == content[:8] and len(content) <= MAX_CAPTURE_BYTES):
        raise EvidenceError("bad PNG signature or oversized capture")
    offset = 8
    dimensions: tuple[int, int] | None = None
    bpp = 0
    idat = bytearray()
    saw_end = False
    while offset < len(content):
        if offset + 12 > len(content):
            raise EvidenceError("truncated PNG chunk")
        size = struct.unpack_from(">I", content, offset)[0]
        kind = content[offset + 4:offset + 8]
        end = offset + 12 + size
        if size > MAX_CAPTURE_BYTES or end > len(content):
            raise EvidenceError("invalid/oversized PNG chunk")
        payload = content[offset + 8:offset + 8 + size]
        crc = zlib.crc32(kind + payload) & 0xffffffff
        stored = struct.unpack_from(">I", content, offset + 8 + size)[0]
        if crc != stored:
            raise EvidenceError("corrupt PNG chunk CRC")
        if kind == b"IHDR":
            if dimensions is not None or size != 13:
                raise EvidenceError("duplicate/invalid IHDR")
            width, height, depth, color, compression, filtering, interlaced = struct.unpack(">IIBBBBB", payload)
            if not (1 <= width <= MAX_DIMENSION and 1 <= height <= MAX_DIMENSION):
                raise EvidenceError("unexpected screenshot geometry")
            if depth != 8 or color not in (2, 6) or compression or filtering or interlaced:
                raise EvidenceError("unsupported PNG screenshot color/depth/interlace")
            dimensions = width, height
            bpp = 3 if color == 2 else 4
        elif kind == b"IDAT":
            if dimensions is None:
                raise EvidenceError("IDAT before IHDR")
            if len(idat) + size > MAX_CAPTURE_BYTES:
                raise EvidenceError("excessive compressed PNG data")
            idat.extend(payload)
        elif kind == b"IEND":
            if size != 0 or dimensions is None:
                raise EvidenceError("invalid IEND")
            saw_end = True
            if end != len(content):
                raise EvidenceError("trailing PNG bytes")
            break
        offset = end
    if not saw_end or not idat or dimensions is None:
        raise EvidenceError("incomplete PNG")
    width, height = dimensions
    rowbytes = width * bpp
    expected = (rowbytes + 1) * height
    decoder = zlib.decompressobj()
    try:
        raw = decoder.decompress(bytes(idat), expected + 1)
    except zlib.error as exc:
        raise EvidenceError("invalid compressed PNG") from exc
    if len(raw) != expected or not decoder.eof or decoder.unconsumed_tail or decoder.unused_data:
        raise EvidenceError("truncated or oversized PNG pixel data")

    selected_y = {min(height - 1, i * height // SAMPLE_GRID) for i in range(SAMPLE_GRID)}
    selected_x = [min(width - 1, i * width // SAMPLE_GRID) for i in range(SAMPLE_GRID)]
    samples: list[tuple[int, int, int]] = []
    previous = bytearray(rowbytes)
    for y in range(height):
        start = y * (rowbytes + 1)
        kind = raw[start]
        line = bytearray(raw[start + 1:start + 1 + rowbytes])
        if kind > 4:
            raise EvidenceError("invalid PNG scanline filter")
        if kind == 1:
            for i in range(bpp, rowbytes):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif kind == 2:
            for i in range(rowbytes):
                line[i] = (line[i] + previous[i]) & 255
        elif kind == 3:
            for i in range(rowbytes):
                left = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((left + previous[i]) // 2)) & 255
        elif kind == 4:
            for i in range(rowbytes):
                a = line[i - bpp] if i >= bpp else 0
                b = previous[i]
                c = previous[i - bpp] if i >= bpp else 0
                p = a + b - c
                da, db, dc = abs(p - a), abs(p - b), abs(p - c)
                predictor = a if da <= db and da <= dc else b if db <= dc else c
                line[i] = (line[i] + predictor) & 255
        if y in selected_y:
            samples.extend(tuple(line[bpp * x:bpp * x + 3]) for x in selected_x)
        previous = line
    return dimensions, samples


def changed_sample_fraction(left: bytes, right: bytes) -> float:
    (dim_a, pix_a), (dim_b, pix_b) = read_png_pixels(left), read_png_pixels(right)
    if dim_a != dim_b or len(pix_a) != len(pix_b):
        raise EvidenceError("font-scale captures do not have matching viewport geometry")
    if len(set(pix_a)) < 4 or len(set(pix_b)) < 4:
        raise EvidenceError("capture looks blank or near-uniform")
    changed = sum(any(abs(x - y) >= 16 for x, y in zip(a, b)) for a, b in zip(pix_a, pix_b))
    return changed / len(pix_a)


def check_archive(path: Path) -> dict:
    if not path.is_file():
        raise EvidenceError("Grayfog capture archive is missing")
    if path.stat().st_size > 350 * 1024 * 1024:
        raise EvidenceError("Grayfog archive exceeds bounded size")
    expected = expected_filenames()
    captures: dict[str, bytes] = {}
    with tarfile.open(path, mode="r:") as archive:
        for item in archive:
            name = PurePosixPath(item.name)
            if (name.is_absolute() or ".." in name.parts or
                    not name.parts or name.parts[0] != "grayfog-review"):
                raise EvidenceError("unexpected/unsafe archive member path")
            if item.isdir():
                continue
            if not item.isfile() or len(name.parts) != 2 or name.suffix != ".png":
                raise EvidenceError("unexpected archive content or link")
            if name.name in captures:
                raise EvidenceError("duplicate screenshot filename")
            if name.name not in expected:
                raise EvidenceError("unexpected screenshot (review corpus changed)")
            if not 1000 <= item.size <= MAX_CAPTURE_BYTES:
                raise EvidenceError("empty or oversized capture")
            stream = archive.extractfile(item)
            if stream is None:
                raise EvidenceError("PNG archive entry unreadable")
            captures[name.name] = stream.read(MAX_CAPTURE_BYTES + 1)
            if len(captures[name.name]) != item.size:
                raise EvidenceError("PNG archive entry truncated")
    missing = sorted(expected - captures.keys())
    if missing:
        raise EvidenceError(f"{len(missing)} screenshot(s) missing: {', '.join(missing[:8])}")

    comparisons = {}
    pairs = [
        ("threshold-entrance-fa-100-standard.png", "threshold-entrance-fa-200-standard.png"),
        ("archive-search-en-100-contrast.png", "archive-search-en-200-contrast.png"),
        ("dialog-book_detail-fa-100-standard.png", "dialog-book_detail-fa-200-standard.png"),
    ]
    for a, b in pairs:
        ratio = changed_sample_fraction(captures[a], captures[b])
        if ratio < 0.01:
            raise EvidenceError(f"100% and 200% evidence unexpectedly alike for {a}: {ratio:.4f}")
        comparisons[a.replace("-100-", "-")] = round(ratio, 5)
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return {
        "status": "PASS",
        "scope": "software-emulator-visual-evidence-only",
        "capture_count": len(captures),
        "expected_count": len(expected),
        "archive_sha256": digest.hexdigest(),
        "font_scale_pixel_change": comparisons,
        "physical_device_pass": False,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, default=Path("build/reports/grayfog-review.tar"))
    parser.add_argument("--report", type=Path, default=Path("build/reports/grayfog-evidence-verification.json"))
    args = parser.parse_args(argv)
    try:
        result = check_archive(args.archive)
    except (EvidenceError, OSError, tarfile.TarError) as exc:
        result = {"status": "FAIL", "scope": "software-emulator-visual-evidence-only",
                  "reason": str(exc), "physical_device_pass": False}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{result['status']}: Grayfog screenshot evidence — {result.get('reason', str(result.get('capture_count', 0)) + ' captures')}")
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
