#!/usr/bin/env python3
"""Reject old build-time crypto even when Dependency Review's delta is empty."""
import json
import re
import sys
from pathlib import Path
from urllib.parse import unquote

BC_PACKAGE = re.compile(r"^pkg:maven/org\.bouncycastle/(bcprov-jdk18on|bcutil-jdk18on|bcpkix-jdk18on)@([^?]+)")


def verify(snapshot, expected_sha):
    if snapshot.get("sha") != expected_sha:
        raise ValueError("Dependency snapshot does not belong to the expected source SHA")
    manifests = snapshot.get("manifests", {})
    if not manifests or not any(m.get("resolved") for m in manifests.values()):
        raise ValueError("Dependency snapshot is empty")
    versions = set()
    for name, manifest in manifests.items():
        for dependency in manifest.get("resolved", {}).values():
            match = BC_PACKAGE.match(unquote(dependency.get("package_url", "")))
            if not match:
                continue
            artifact, version = match.groups()
            if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?", version):
                raise ValueError(f"Unrecognized build crypto version: {artifact}:{version}")
            numeric = tuple(int(part) for part in version.split("."))
            numeric += (0,) * (3 - len(numeric))
            if numeric < (1, 85, 0):
                raise ValueError(f"Unpatched build crypto: {name} -> {artifact}:{version}")
            versions.add(version)
    if len(versions) > 1:
        raise ValueError(f"Build crypto family is not aligned: {sorted(versions)}")
    return sorted(versions)


def main():
    directory, expected_sha = Path(sys.argv[1]), sys.argv[2]
    snapshots = sorted(directory.glob("*.json"))
    if not snapshots:
        raise ValueError("No generated dependency snapshot to verify")
    for path in snapshots:
        versions = verify(json.loads(path.read_text()), expected_sha)
        print(f"PASS: {path.name}; build crypto={','.join(versions) or 'not resolved'}")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, json.JSONDecodeError) as error:
        print(f"Dependency graph verification failed: {error}", file=sys.stderr)
        sys.exit(1)
