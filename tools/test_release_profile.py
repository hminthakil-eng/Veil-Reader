#!/usr/bin/env python3
"""Synthetic verifier fixtures only; these are NOT real generated profiles."""
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


class ProfileGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'tools').mkdir()
        shutil.copyfile(Path(__file__).with_name('verify_release_profile.py'), self.root / 'tools/verify_release_profile.py')
        self.git('init', '-q')
        self.git('config', 'user.name', 'Fixture')
        self.git('config', 'user.email', 'fixture@example.invalid')
        self.git('add', 'tools')
        self.git('commit', '-qm', 'fixture')
        self.directory = self.root / 'app/src/release/generated/baselineProfiles'
        self.directory.mkdir(parents=True)

    def git(self, *args):
        return subprocess.run(['git', *args], cwd=self.root, check=True, capture_output=True)

    def sources(self, commit=True):
        for name in ('baseline-prof.txt', 'startup-prof.txt'):
            (self.directory / name).write_text('HSPLfixture/Example;->start()V\n')
        self.git('add', 'app/src')
        if commit:
            self.git('commit', '-qm', 'synthetic profiles')

    def package(self, kind, payload=b'fixture', entry=None):
        folder, default = {
            'apk': ('apk', 'assets/dexopt/baseline.prof'),
            'aab': ('bundle', 'BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof'),
        }[kind]
        dest = self.root / f'app/build/outputs/{folder}/release/app.{kind}'
        dest.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(dest, 'w') as z:
            z.writestr(entry or default, payload)

    def verify(self, success, fragment, sources_only=False):
        cmd = [sys.executable, 'tools/verify_release_profile.py']
        if sources_only:
            cmd.append('--sources-only')
        result = subprocess.run(cmd, cwd=self.root, capture_output=True, text=True)
        self.assertEqual(result.returncode == 0, success, result.stdout + result.stderr)
        self.assertIn(fragment, result.stdout + result.stderr)

    def test_missing(self):
        self.verify(False, 'No committed non-empty', True)

    def test_empty(self):
        (self.directory / 'baseline-prof.txt').touch()
        self.verify(False, 'Empty generated profile', True)

    def test_wrong_location(self):
        self.sources()
        bad = self.root / 'app/src/debug/baseline-prof.txt'
        bad.parent.mkdir(parents=True)
        bad.write_text('Lfixture/Example;\n')
        self.verify(False, 'Unexpected profile source location', True)

    def test_staged_not_committed(self):
        self.sources(commit=False)
        self.verify(False, 'not committed to git', True)

    def test_modified_after_commit(self):
        self.sources()
        (self.directory / 'baseline-prof.txt').write_text('Lfixture/Different;\n')
        self.verify(False, 'differs from HEAD', True)

    def test_committed_sources(self):
        self.sources()
        self.verify(True, 'package checks have NOT run', True)

    def test_missing_packages(self):
        self.sources()
        self.verify(False, 'No release APK')

    def test_both_packages(self):
        self.sources()
        self.package('apk')
        self.package('aab')
        self.verify(True, 'packaging: GREEN')

    def test_missing_aab_entry(self):
        self.sources()
        self.package('apk')
        self.package('aab', entry='wrong.prof')
        self.verify(False, 'is missing BUNDLE-METADATA')

    def test_empty_apk_payload(self):
        self.sources()
        self.package('apk', b'')
        self.package('aab')
        self.verify(False, 'contains empty')

    def test_oversized_profile(self):
        self.sources()
        self.package('apk', b'x' * 1_500_000)
        self.package('aab')
        self.verify(False, 'oversized')


if __name__ == '__main__':
    unittest.main()
