#!/usr/bin/env python3
import pathlib
import tempfile
import unittest
import zipfile
import importlib.util

MODULE_PATH = pathlib.Path(__file__).with_name("verify_release_artifact.py")
SPEC = importlib.util.spec_from_file_location("verify_release_artifact", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ReleaseArtifactVerifierTest(unittest.TestCase):
    def _zip(self, suffix: str, entries: dict[str, bytes]) -> pathlib.Path:
        directory = pathlib.Path(tempfile.mkdtemp())
        path = directory / f"artifact{suffix}"
        with zipfile.ZipFile(path, "w") as archive:
            for name, data in entries.items():
                archive.writestr(name, data)
        return path

    def test_apk_requires_packaged_profile_when_requested(self):
        apk = self._zip(
            ".apk",
            {
                "AndroidManifest.xml": b"manifest",
                "resources.arsc": b"resources",
                "classes.dex": b"dex",
            },
        )
        with self.assertRaises(MODULE.VerificationError):
            MODULE.verify_artifact(apk, require_profile=True)

    def test_apk_accepts_nonempty_profile(self):
        apk = self._zip(
            ".apk",
            {
                "AndroidManifest.xml": b"manifest",
                "resources.arsc": b"resources",
                "classes.dex": b"dex",
                "assets/dexopt/baseline.prof": b"profile",
                "lib/arm64-v8a/libreader.so": b"native",
            },
        )
        result = MODULE.verify_artifact(apk, require_profile=True)
        self.assertEqual(result["baseline_profile_bytes"], 7)
        self.assertEqual(result["abis"], ["arm64-v8a"])

    def test_aab_accepts_official_profile_location(self):
        aab = self._zip(
            ".aab",
            {
                "BundleConfig.pb": b"bundle",
                "base/manifest/AndroidManifest.xml": b"manifest",
                "base/dex/classes.dex": b"dex",
                "BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof": b"profile",
            },
        )
        result = MODULE.verify_artifact(aab, require_profile=True)
        self.assertEqual(result["baseline_profile_bytes"], 7)


if __name__ == "__main__":
    unittest.main()
