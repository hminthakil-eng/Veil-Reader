"""Read-only verification that the installed single-base QA APK equals a local APK.

Does not establish a cryptographic attestation of the source commit, nor prove
physical Paper quality. Does not install, uninstall or modify any device app.
"""
from __future__ import annotations

import hashlib
import tempfile
from pathlib import Path, PurePosixPath
from typing import Callable


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def single_installed_base_apk(pm_path_output: str) -> str:
    """Fail closed on split APK installs and malformed paths.

    The manually installed Forge QA debug APK is expected to have exactly
    one /data/app/.../base.apk path; split installs need a separate manifest.
    """
    lines = pm_path_output.splitlines()
    if len(lines) != 1 or not lines[0].startswith("package:"):
        raise ValueError("Expected exactly one installed base APK; splits/unavailable package are unverified")
    remote = lines[0][len("package:"):]
    if any(ch.isspace() or ord(ch) < 32 for ch in remote):
        raise ValueError("Unsafe whitespace/control character in installed APK path")
    parts = PurePosixPath(remote).parts
    if (not remote.startswith("/data/app/") or ".." in parts
            or "\\" in remote or not remote.endswith("/base.apk")
            or len(parts) < 5):
        raise ValueError("Unexpected installed APK location")
    return remote


def verify_installed_apk(adb: list[str], package: str, local_apk: Path,
                         run_command: Callable[..., str]) -> dict:
    """Read the package location, pull only base.apk into temporary storage,
    hash it locally, compare to the supplied extracted QA artifact.

    Raise on ambiguity or failures. The caller decides whether to record a
    failed verification and must not proceed to sensitive video capture.
    """
    if not local_apk.is_file() or local_apk.suffix.lower() != ".apk":
        raise ValueError("Extracted local APK required")
    if local_apk.stat().st_size > 2 * 1024 * 1024 * 1024:
        raise ValueError("Local APK is above 2 GiB safety ceiling")
    pm_output = run_command(adb + ["shell", "pm", "path", package], timeout=30)
    remote_path = single_installed_base_apk(pm_output)
    expected = file_sha256(local_apk)
    with tempfile.TemporaryDirectory(prefix="veil-paper-apk-check-") as tmpdir:
        pulled = Path(tmpdir) / "installed-base.apk"
        run_command(adb + ["pull", remote_path, str(pulled)], timeout=240)
        if not pulled.is_file():
            raise RuntimeError("ADB pull did not produce an installed APK")
        actual = file_sha256(pulled)
    return {
        "status": "MATCH" if actual == expected else "MISMATCH",
        "local_sha256": expected,
        "installed_sha256": actual,
        "match": actual == expected,
        "installed_path": remote_path,
        "source_commit_verified": False,
        "note": "APK byte identity only. Source commit requires separate trusted build provenance.",
    }
