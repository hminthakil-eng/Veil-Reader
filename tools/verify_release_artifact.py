#!/usr/bin/env python3
"""Verify optimized Veil Reader release artifacts without installing them."""

from __future__ import annotations

import argparse
import pathlib
import sys
import zipfile

MAX_BASELINE_PROFILE_BYTES = 1_500_000


class VerificationError(RuntimeError):
    pass


def _require(names: set[str], predicate, description: str) -> str:
    for name in names:
        if predicate(name):
            return name
    raise VerificationError(f"Missing {description}")


def verify_artifact(path: pathlib.Path, require_profile: bool) -> dict[str, object]:
    if not path.is_file():
        raise VerificationError(f"Artifact does not exist: {path}")
    if path.stat().st_size <= 0:
        raise VerificationError(f"Artifact is empty: {path}")

    suffix = path.suffix.lower()
    if suffix not in {".apk", ".aab"}:
        raise VerificationError(f"Unsupported artifact type: {path}")

    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())

        if suffix == ".apk":
            _require(names, lambda n: n == "AndroidManifest.xml", "APK AndroidManifest.xml")
            _require(names, lambda n: n == "resources.arsc", "APK resources.arsc")
            _require(names, lambda n: n.startswith("classes") and n.endswith(".dex"), "APK DEX")
            profile_path = "assets/dexopt/baseline.prof"
            native_prefix = "lib/"
        else:
            _require(names, lambda n: n == "BundleConfig.pb", "AAB BundleConfig.pb")
            _require(
                names,
                lambda n: n == "base/manifest/AndroidManifest.xml",
                "AAB base manifest",
            )
            _require(
                names,
                lambda n: n.startswith("base/dex/classes") and n.endswith(".dex"),
                "AAB DEX",
            )
            profile_path = "BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof"
            native_prefix = "base/lib/"

        profile_size = None
        if require_profile:
            if profile_path not in names:
                raise VerificationError(
                    f"Baseline Profile not packaged at expected path: {profile_path}"
                )
            profile_size = archive.getinfo(profile_path).file_size
            if profile_size <= 0:
                raise VerificationError("Packaged Baseline Profile is empty")
            if profile_size >= MAX_BASELINE_PROFILE_BYTES:
                raise VerificationError(
                    f"Packaged Baseline Profile is too large: {profile_size} bytes"
                )

        abis = sorted(
            {
                name[len(native_prefix):].split("/", 1)[0]
                for name in names
                if name.startswith(native_prefix) and "/" in name[len(native_prefix):]
            }
        )

    return {
        "path": str(path),
        "bytes": path.stat().st_size,
        "baseline_profile_bytes": profile_size,
        "abis": abis,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifacts", nargs="+", type=pathlib.Path)
    parser.add_argument("--require-profile", action="store_true")
    args = parser.parse_args()

    try:
        for artifact in args.artifacts:
            result = verify_artifact(artifact, args.require_profile)
            print(
                "VERIFIED "
                f"{result['path']} "
                f"bytes={result['bytes']} "
                f"baseline_profile_bytes={result['baseline_profile_bytes']} "
                f"abis={','.join(result['abis']) if result['abis'] else 'none'}"
            )
    except (VerificationError, zipfile.BadZipFile) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
