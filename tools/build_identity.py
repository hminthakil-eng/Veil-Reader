#!/usr/bin/env python3
"""Prepare build identity, then verify it survived APK packaging unchanged."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ASSET = Path("app/src/main/assets/veil-build.json")
APK_ASSET = "assets/veil-build.json"


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()


def clean_revision(root):
    revision = git(root, "rev-parse", "HEAD")
    if git(root, "diff", "HEAD", "--name-only"):
        raise ValueError("Tracked source is dirty; refusing to label it as a clean commit")
    untracked_source = git(
        root,
        "ls-files",
        "--others",
        "--exclude-standard",
        "--",
        "app/src",
        "benchmark/src",
        "buildSrc",
    ).splitlines()
    if any(path != ASSET.as_posix() for path in untracked_source):
        raise ValueError("Untracked source cannot be attributed to this commit")
    return revision


def prepare(root):
    identity = {
        "schema": 1,
        "commit": clean_revision(root),
        "branch": os.environ.get("CM_BRANCH") or git(root, "rev-parse", "--abbrev-ref", "HEAD"),
        "workflow": os.environ.get("CM_WORKFLOW_ID", "local"),
        "build_id": os.environ.get("CM_BUILD_ID", "local"),
        "prepared_at_utc": datetime.now(timezone.utc).isoformat(),
    }
    target = root / ASSET
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(identity, indent=2) + "\n", encoding="utf-8")
    return identity


def verify(root, apk, output):
    expected = json.loads((root / ASSET).read_text(encoding="utf-8"))
    if expected["commit"] != clean_revision(root):
        raise ValueError("HEAD changed after identity preparation")

    with zipfile.ZipFile(apk) as archive:
        actual = json.loads(archive.read(APK_ASSET))

    if actual != expected:
        raise ValueError("APK identity does not match this build; stale APK rejected")

    digest = sha256(apk)
    output.mkdir(parents=True, exist_ok=True)
    artifact = output / ("Veil-Reader-" + expected["commit"][:12] + "-debug.apk")
    shutil.copy2(apk, artifact)

    evidence = {
        **expected,
        "artifact": artifact.name,
        "sha256": digest,
        "size_bytes": artifact.stat().st_size,
        "identity_verified": True,
    }
    (output / "build-identity.json").write_text(
        json.dumps(evidence, indent=2) + "\n",
        encoding="utf-8",
    )
    return evidence


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("prepare", "verify"))
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument(
        "--apk",
        type=Path,
        default=Path("app/build/outputs/apk/debug/app-debug.apk"),
    )
    parser.add_argument("--output", type=Path, default=Path("build-evidence"))
    args = parser.parse_args()
    root = args.root.resolve()
    result = (
        prepare(root)
        if args.action == "prepare"
        else verify(root, root / args.apk, root / args.output)
    )
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
