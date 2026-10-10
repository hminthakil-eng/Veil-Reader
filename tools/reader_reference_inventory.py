#!/usr/bin/env python3
"""Deterministic, non-executing APK reference inventory for Veil Reader R&D.

Inventories only ZIP structure and filenames. It deliberately does not extract,
load, decompile, import, install, or execute competitor code or assets.
Evidence of an asset or a library is not evidence of a working user feature.
"""

from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import sys
import zipfile

MAX_ENTRIES = 30_000
MAX_UNCOMPRESSED_BYTES = 2 * 1024**3
MAX_SINGLE_ENTRY_BYTES = 350 * 1024**2
CHUNK_BYTES = 4 * 1024**2
_FONT_SUFFIXES = {".ttf", ".otf", ".woff", ".woff2"}
_IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp", ".avif"}


class UnsafeArchive(ValueError):
    """Archive is malformed or exceeds conservative non-extraction limits."""


def _safe_name(name: str) -> str:
    if "\\" in name or "\x00" in name or name.startswith("/"):
        raise UnsafeArchive("absolute, backslash or NUL archive member")
    parts = PurePosixPath(name).parts
    if any(part in ("..", ".") for part in parts) or not parts:
        raise UnsafeArchive("unsafe archive member traversal")
    return name


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while block := stream.read(CHUNK_BYTES):
            digest.update(block)
    return digest.hexdigest()


def inventory(path: Path) -> dict:
    """Inventory *metadata only*, never decompressing archive members."""
    if not path.is_file():
        raise FileNotFoundError(path)
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        if len(entries) > MAX_ENTRIES:
            raise UnsafeArchive("too many archive entries")
        if sum(item.file_size for item in entries) > MAX_UNCOMPRESSED_BYTES:
            raise UnsafeArchive("archive exceeds uncompressed-size budget")
        if any(item.file_size > MAX_SINGLE_ENTRY_BYTES for item in entries):
            raise UnsafeArchive("single archive member exceeds size budget")
        seen = set()
        for item in entries:
            name = _safe_name(item.filename)
            if name in seen:
                raise UnsafeArchive("duplicate archive member name")
            seen.add(name)

        names = sorted(item.filename for item in entries if not item.is_dir())
        dex = sorted(
            name for name in names if re.fullmatch(r"classes(?:[1-9][0-9]*)?\.dex", name)
        )
        abi_libs = {}
        for name in names:
            match = re.fullmatch(r"lib/([^/]+)/([^/]+\.so)", name)
            if match:
                abi_libs.setdefault(match.group(1), []).append(match.group(2))
        font_paths = [
            name for name in names if Path(name).suffix.lower() in _FONT_SUFFIXES
        ]
        hyphenation_paths = [
            name for name in names if "/hyphen" in "/" + name.lower()
        ]
        background_paths = [
            name for name in names if
            "background/" in name.lower() and
            Path(name).suffix.lower() in _IMAGE_SUFFIXES
        ]
        signing_files = [
            name for name in names if name.upper().startswith("META-INF/")
            and name.upper().endswith((".RSA", ".DSA", ".EC", ".SF"))
        ]
        return {
            "schema_version": 1,
            "file_name": path.name,
            "sha256": _sha256(path),
            "size_bytes": path.stat().st_size,
            "entry_count": len(entries),
            "total_uncompressed_bytes": sum(item.file_size for item in entries),
            "dex_files": dex,
            "native_libraries_by_abi": dict(sorted(
                (abi, sorted(libs)) for abi, libs in abi_libs.items()
            )),
            "font_paths": font_paths,
            "hyphenation_entry_count": len(hyphenation_paths),
            "background_image_count": len(background_paths),
            "signing_metadata_files": signing_files,
            "resource_extension_counts": dict(sorted(
                Counter(Path(name).suffix.lower() for name in names).items()
            )),
            "evidence_limitations": [
                "ZIP metadata does not prove implementation, runtime behavior, ownership, or licensing.",
                "Signer metadata names do not authenticate the APK's origin.",
                "Proprietary code and fonts are not copied, extracted, or redistributable on this basis.",
            ],
        }


def report(paths: list[Path]) -> dict:
    result = [inventory(path) for path in paths]
    hashes = Counter(item["sha256"] for item in result)
    return {
        "purpose": "Veil Reader clean-room R&D reference inventory",
        "safety": "Metadata-only, no APK code execution or asset extraction",
        "apks": result,
        "duplicate_sha256": sorted(
            sha for sha, count in hashes.items() if count > 1
        ),
    }


def to_markdown(value: dict) -> str:
    lines = [
        "# Veil Reader — APK reference fingerprints",
        "",
        "Read-only archive metadata; **not runtime feature evidence**.",
        "",
        "| APK | SHA-256 | MiB | DEX | Native ABIs | Font files | Hyphenation entries |",
        "|---|---|---:|---:|---|---:|---:|",
    ]
    for app in value["apks"]:
        lines.append(
            "| {} | {} | {:.1f} | {} | {} | {} | {} |".format(
                app["file_name"].replace("|", "\\|"),
                app["sha256"], app["size_bytes"] / 1024**2,
                len(app["dex_files"]),
                ", ".join(app["native_libraries_by_abi"]) or "none",
                len(app["font_paths"]), app["hyphenation_entry_count"]
            )
        )
    if value["duplicate_sha256"]:
        lines.extend([
            "",
            "Repeated identical uploads: " +
            ", ".join(value["duplicate_sha256"])
        ])
    lines.extend([
        "", "## Evidence limits", "",
        "- Neither code nor binaries are copied or executed.",
        "- ZIP content paths alone cannot establish user-visible features or performance.",
        "- Every integration requires provenance, license checks, independent implementation, and real-device acceptance.",
    ])
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, nargs="+", help="One or more local APK paths")
    parser.add_argument("--format", choices=("json", "markdown"), default="markdown")
    args = parser.parse_args(argv)
    try:
        value = report(args.apk)
    except (UnsafeArchive, FileNotFoundError, OSError, zipfile.BadZipFile) as error:
        print(f"APK inventory failed: {error}", file=sys.stderr)
        return 2
    print(
        json.dumps(value, indent=2, sort_keys=True)
        if args.format == "json" else to_markdown(value),
        end=""
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
