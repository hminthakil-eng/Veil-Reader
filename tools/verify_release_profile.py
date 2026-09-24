#!/usr/bin/env python3
"""Fail release hardening if Baseline Profile source or packaged binary is missing."""

from __future__ import annotations

import sys
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def require_git_tracked(path: Path) -> None:
    relative = path.relative_to(ROOT).as_posix()
    result = subprocess.run(
        ["git", "ls-files", "--error-unmatch", relative],
        cwd=ROOT,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if result.returncode != 0:
        fail(f"Generated profile is not committed to git: {relative}")


source_candidates = sorted(
    p for p in APP.glob("src/**/baseline-prof.txt")
    if "build" not in p.parts and p.is_file() and p.stat().st_size > 0
)
if not source_candidates:
    fail(
        "No committed non-empty baseline-prof.txt found under app/src/. "
        "Run :app:generateBaselineProfile with saveInSrc=true and commit the generated profile."
    )

print("Committed Baseline Profile source:")
for path in source_candidates:
    require_git_tracked(path)
    print(f"  {path.relative_to(ROOT)} ({path.stat().st_size} bytes)")

startup_candidates = sorted(
    p for p in APP.glob("src/**/startup-prof.txt")
    if "build" not in p.parts and p.is_file() and p.stat().st_size > 0
)
if not startup_candidates:
    fail(
        "No committed non-empty startup-prof.txt found under app/src/. "
        "The launcher generator uses includeInStartupProfile=true, so the release hardening gate "
        "requires the generated Startup Profile source as well."
    )

print("Startup Profile source:")
for path in startup_candidates:
    require_git_tracked(path)
    print(f"  {path.relative_to(ROOT)} ({path.stat().st_size} bytes)")

apk_candidates = sorted((APP / "build" / "outputs" / "apk" / "release").glob("*.apk"))
aab_candidates = sorted((APP / "build" / "outputs" / "bundle" / "release").glob("*.aab"))

if not apk_candidates:
    fail("No release APK found. Run :app:assembleRelease first.")
if not aab_candidates:
    fail("No release AAB found. Run :app:bundleRelease first.")


MAX_BASELINE_PROFILE_BYTES = 1_500_000

def require_zip_entry(path: Path, entry: str) -> None:
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        if entry not in names:
            fail(f"{path.name} is missing {entry}")
        info = archive.getinfo(entry)
        if info.file_size <= 0:
            fail(f"{path.name} contains empty {entry}")
        if info.file_size >= MAX_BASELINE_PROFILE_BYTES:
            fail(
                f"{path.name} contains oversized {entry}: {info.file_size} bytes "
                f"(must stay below {MAX_BASELINE_PROFILE_BYTES} bytes)"
            )
        print(f"OK: {path.relative_to(ROOT)} -> {entry} ({info.file_size} bytes)")


for apk in apk_candidates:
    require_zip_entry(apk, "assets/dexopt/baseline.prof")

for aab in aab_candidates:
    require_zip_entry(aab, "BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof")

print("Release Baseline Profile packaging: GREEN")
