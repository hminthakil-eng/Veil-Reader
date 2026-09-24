#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -z "${FIREBASE_TESTLAB_CREDENTIALS:-}" ]]; then
  echo "FIREBASE_TESTLAB_CREDENTIALS must point to a Firebase Test Lab service-account JSON file." >&2
  exit 2
fi

if [[ ! -s "$FIREBASE_TESTLAB_CREDENTIALS" ]]; then
  echo "Firebase Test Lab credentials file is missing or empty: $FIREBASE_TESTLAB_CREDENTIALS" >&2
  exit 2
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle 9.6 must be available on PATH." >&2
  exit 2
fi

START_HEAD="$(git rev-parse HEAD)"

bash tools/test-policy.sh
python tools/test_performance_budget.py
python tools/test_performance_delta.py
python tools/test_performance_dashboard.py

gradle :app:generateBaselineProfile \
  -PveilUseFirebaseTestLab=true \
  -PfirebaseTestLabCredentials="$FIREBASE_TESTLAB_CREDENTIALS" \
  --stacktrace

BASELINE="app/src/main/generated/baselineProfiles/baseline-prof.txt"
STARTUP="app/src/main/generated/baselineProfiles/startup-prof.txt"

test -s "$BASELINE"
test -s "$STARTUP"

git add -- "$BASELINE" "$STARTUP"

PROFILE_COMMIT_CREATED=false
if ! git diff --cached --quiet; then
  git config user.name "github-actions[bot]"
  git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
  git commit -m "perf(release): commit generated Baseline Profile"
  PROFILE_COMMIT_CREATED=true
fi

gradle :app:testDebugUnitTest --stacktrace
git diff --exit-code -- app/schemas/
gradle :app:lintDebug --stacktrace
gradle :app:assembleDebug --stacktrace
gradle :benchmark:assembleBenchmarkBenchmark --stacktrace
gradle :app:assembleRelease :app:bundleRelease --stacktrace
python tools/verify_release_profile.py

VERIFIED_HEAD="$(git rev-parse HEAD)"
printf 'verified_commit=%s\n' "$VERIFIED_HEAD" > release-evidence.txt

mapfile -t artifacts < <(
  find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' -print 2>/dev/null
  find app/build/outputs/bundle/release -maxdepth 1 -type f -name '*.aab' -print 2>/dev/null
  find app/src -type f -path '*/generated/baselineProfiles/*.txt' -print 2>/dev/null
)

if [[ "${#artifacts[@]}" -eq 0 ]]; then
  echo "No release/profile artifacts found for evidence capture" >&2
  exit 1
fi

for artifact in "${artifacts[@]}"; do
  hash="$(sha256sum "$artifact" | awk '{print $1}')"
  bytes="$(stat -c%s "$artifact")"
  printf '%s sha256=%s bytes=%s\n' "$artifact" "$hash" "$bytes" >> release-evidence.txt
done

cat release-evidence.txt

if [[ "${VEIL_PUSH_PROFILES:-0}" == "1" && "$PROFILE_COMMIT_CREATED" == "true" ]]; then
  remote="$(git ls-remote origin refs/heads/release/reader-hardening-rc | awk '{print $1}')"
  if [[ "$remote" != "$START_HEAD" ]]; then
    echo "Remote hardening branch moved during verification; refusing non-fast-forward push." >&2
    exit 1
  fi
  git push origin HEAD:release/reader-hardening-rc
fi

echo "Release hardening via Firebase Test Lab: GREEN"
