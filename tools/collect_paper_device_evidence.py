#!/usr/bin/env python3
"""Opt-in local Android evidence capture for Veil Reader Paper physical QA.

This tool never installs, uninstalls, force-stops, modifies app data, uploads files,
or marks a physical device as PASS. Screen contents and logs remain on the operator's PC.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

from paper_apk_identity import verify_installed_apk

SHA_RE = re.compile(r"^[0-9a-fA-F]{40}$")
PACKAGE_RE = re.compile(r"^[a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z][a-zA-Z0-9_]*)+$")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def online_devices(adb_output: str) -> list[str]:
    return [line.split("\t", 1)[0] for line in adb_output.splitlines()
            if "\tdevice" in line and line.split("\t", 1)[1].startswith("device")]


def safe_name(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]", "_", value)


def run(argv: list[str], timeout: int = 20, binary: bool = False):
    result = subprocess.run(argv, capture_output=True, timeout=timeout, check=False)
    if result.returncode != 0:
        err = result.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(argv[:3])}; {err[-350:]}")
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-sha", required=True, help="40-character executable source commit SHA")
    parser.add_argument("--package", default="com.veilreader.app.forgeqa")
    parser.add_argument("--apk", type=Path, help="Exact locally installed APK file; optional for a manually installed ZIP")
    parser.add_argument("--verify-installed-apk", action="store_true",
                        help="Read-only pull and SHA-256 comparison of the installed single-base APK against --apk")
    parser.add_argument("--serial", help="ADB serial; mandatory when multiple devices are connected")
    parser.add_argument("--output", type=Path, default=Path("paper-device-evidence"))
    parser.add_argument("--video", action="store_true", help="Explicitly opt into a local screen recording")
    parser.add_argument("--logcat", action="store_true", help="Explicitly opt into collecting recent logs (may contain personal data)")
    parser.add_argument("--duration", type=int, default=60, help="Video duration 10-180 seconds")
    args = parser.parse_args(argv)

    if not SHA_RE.fullmatch(args.source_sha):
        parser.error("--source-sha must be exactly 40 hexadecimal characters")
    if not PACKAGE_RE.fullmatch(args.package):
        parser.error("--package must be a standard dotted Android package name")
    if not 10 <= args.duration <= 180:
        parser.error("--duration must be between 10 and 180 seconds")
    if args.apk is not None and (not args.apk.is_file() or args.apk.suffix.lower() != ".apk"):
        parser.error("--apk must name an existing .apk file (extract any downloaded ZIP first)")
    if args.verify_installed_apk and args.apk is None:
        parser.error("--verify-installed-apk requires --apk from the extracted QA artifact")

    devices = online_devices(run(["adb", "devices"]))
    if args.serial:
        if args.serial not in devices:
            parser.error("Selected ADB device is not online/authorized")
        serial = args.serial
    elif len(devices) == 1:
        serial = devices[0]
    else:
        parser.error("Connect and authorize exactly one device or specify --serial")

    adb = ["adb", "-s", serial]
    folder = args.output / f"paper-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{safe_name(serial)}"
    folder.mkdir(parents=True, exist_ok=False)
    props = {
        "model": ["shell", "getprop", "ro.product.model"],
        "android_release": ["shell", "getprop", "ro.build.version.release"],
        "api_level": ["shell", "getprop", "ro.build.version.sdk"],
        "locale": ["shell", "getprop", "persist.sys.locale"],
        "screen_size": ["shell", "wm", "size"],
        "screen_density": ["shell", "wm", "density"],
        "animator_scale": ["shell", "settings", "get", "global", "animator_duration_scale"],
        "transition_scale": ["shell", "settings", "get", "global", "transition_animation_scale"],
        "font_scale": ["shell", "settings", "get", "system", "font_scale"],
        "package_location": ["shell", "pm", "path", args.package],
    }
    observations = {}
    for name, cmd in props.items():
        try:
            observations[name] = run(adb + cmd)
        except (RuntimeError, subprocess.TimeoutExpired) as exc:
            observations[name] = f"UNAVAILABLE: {type(exc).__name__}"

    manifest = {
        "source_sha_requested": args.source_sha.lower(),
        "source_identity_verified_against_installed_apk": False,
        "device_serial_local_only": serial,
        "package": args.package,
        "utc": datetime.now(timezone.utc).isoformat(),
        "properties": observations,
        "status": "UNVERIFIED",
        "physical_acceptance": "NOT_REVIEWED",
        "files": {},
        "privacy": "Local only. Screen capture/logs may contain personal reading content; review before sharing.",
    }
    if args.apk:
        manifest["local_apk_sha256"] = sha256(args.apk)
        manifest["local_apk_path"] = str(args.apk)

    if args.verify_installed_apk:
        # Hash-match only proves *byte identity* with the supplied local artifact.
        # It cannot establish the executable's source commit without build provenance.
        try:
            comparison = verify_installed_apk(adb, args.package, args.apk, run)
            manifest["installed_apk_byte_identity"] = comparison
            if not comparison["match"]:
                (folder / "manifest.json").write_text(
                    json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
                print("ERROR: Installed APK differs from supplied QA artifact; capture stopped.", file=sys.stderr)
                return 2
        except (ValueError, RuntimeError, subprocess.TimeoutExpired, OSError) as exc:
            manifest["installed_apk_byte_identity"] = {
                "status": "UNVERIFIED",
                "error_type": type(exc).__name__,
            }
            (folder / "manifest.json").write_text(
                json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            print(f"ERROR: Installed APK identity could not be verified ({type(exc).__name__}). "
                  "Capture stopped.", file=sys.stderr)
            return 2

    if args.video:
        remote = f"/sdcard/Movies/veil_paper_{datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S')}.mp4"
        print("Opt-in recording. Open a test EPUB and exercise first drag, cancel, commit, cover and chapter transitions.")
        input("Press Enter to start device screen recording (contents stay local): ")
        try:
            run(adb + ["shell", "screenrecord", "--time-limit", str(args.duration), remote], timeout=args.duration + 35)
            target = folder / "paper-motion.mp4"
            run(adb + ["pull", remote, str(target)], timeout=100)
            manifest["files"][target.name] = sha256(target)
        finally:
            # Delete only the tool-created temporary recording on the phone, never user files.
            try:
                run(adb + ["shell", "rm", remote], timeout=20)
            except (RuntimeError, subprocess.TimeoutExpired):
                print("Warning: temporary recording might remain on the phone:", remote, file=sys.stderr)

    for filename, cmd in (("gfxinfo.txt", ["shell", "dumpsys", "gfxinfo", args.package]),):
        try:
            target = folder / filename
            target.write_text(run(adb + cmd, timeout=35), encoding="utf-8")
            manifest["files"][filename] = sha256(target)
        except (RuntimeError, subprocess.TimeoutExpired):
            manifest["files"][filename] = "UNAVAILABLE"

    if args.logcat:
        print("Logcat can contain book titles, paths, text and other sensitive data. Nothing is uploaded.")
        target = folder / "logcat-private.txt"
        target.write_text(run(adb + ["logcat", "-d", "-t", "2000"], timeout=45), encoding="utf-8")
        manifest["files"][target.name] = sha256(target)

    (folder / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (folder / "REVIEW.txt").write_text(
        "UNVERIFIED: manually inspect screen video and enter per-case PASS/FAIL in the issue.\n"
        "Cases: cover first tap/drag; first text page; finger tracking; cancel/reverse; "
        "release across chapter; 100 turns; RTL; rotation; background; 60/120 Hz; TalkBack.\n"
        "Check actual deformation, contact shadow, no destination flash, no skipped locator, "
        "and no Slide fallback. Record limitations and exact executable APK version/signature.\n"
        "Do NOT upload logs/video containing personal book text without reviewing and consenting.\n",
        encoding="utf-8",
    )
    print("Evidence folder:", folder)
    print("Result remains UNVERIFIED until device/optical review is performed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
