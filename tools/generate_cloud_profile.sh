#!/usr/bin/env bash
# Use the existing connected-device Baseline Profile path from any CI host.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${EW_API_TOKEN:?Set EW_API_TOKEN as a Codemagic secret; never commit it}"
if [ "${VEIL_CLOUD_FREE_QUOTA_CONFIRMED:-}" != "yes" ]; then
  echo 'Confirm sufficient unused free quota in both accounts before setting VEIL_CLOUD_FREE_QUOTA_CONFIRMED=yes.' >&2
  exit 2
fi
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:?Android SDK is required}}"
export PATH="$SDK_ROOT/platform-tools:$PATH"
mkdir -p profile-evidence .ci-tools
git rev-parse HEAD > profile-evidence/source-commit.txt

# Refuse to use an unrelated device. This job owns one cloud session only.
adb start-server
if adb devices | awk 'NR > 1 && NF {found=1} END {exit !found}'; then
  echo 'ADB already contains a device; refusing ambiguous profile generation.' >&2
  exit 1
fi

# Pin the executable, not the mutable launcher or its latest-version lookup.
CLI=.ci-tools/ew-cli-1.4.2.jar
curl --fail --location --retry 3 --max-time 180 \
  https://maven.emulator.wtf/releases/wtf/emulator/ew-cli/1.4.2/ew-cli-1.4.2.jar \
  --output "$CLI"
java -jar "$CLI" --version
python3 - "$CLI" <<'PY'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1])
pathlib.Path('profile-evidence/cli-sha256.txt').write_text(hashlib.sha256(p.read_bytes()).hexdigest() + '\n')
PY

java -jar "$CLI" start-session --device model=Pixel7,version=35,gpu=auto \
  --max-time-limit 30m --adb-binary "$SDK_ROOT/platform-tools/adb" \
  > profile-evidence/cloud-session.log 2>&1 &
SESSION_PID=$!
cleanup() {
  kill -TERM "$SESSION_PID" 2>/dev/null || true
  wait "$SESSION_PID" 2>/dev/null || true
}
trap cleanup EXIT
deadline=$((SECONDS + 180))
serial=''
while [ "$SECONDS" -lt "$deadline" ]; do
  if ! kill -0 "$SESSION_PID" 2>/dev/null; then
    echo 'Cloud session exited before ADB became ready; inspect cloud-session.log.' >&2
    exit 1
  fi
  devices="$(adb devices | awk 'NR > 1 && $2 == "device" {print $1}')"
  count="$(printf '%s\n' "$devices" | awk 'NF {n++} END {print n+0}')"
  if [ "$count" -gt 1 ]; then
    echo 'Multiple ADB devices detected; refusing to choose one.' >&2
    exit 1
  fi
  if [ "$count" -eq 1 ] && [ "$(adb -s "$devices" shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; then
    serial="$devices"
    break
  fi
  sleep 2
done
[ -n "$serial" ] || { echo 'Cloud ADB boot deadline exceeded.' >&2; exit 1; }
export ANDROID_SERIAL="$serial"
adb -s "$serial" shell getprop ro.build.fingerprint > profile-evidence/device-fingerprint.txt
./gradlew :app:generateBaselineProfile -Pveil.profile.connected=true --stacktrace \
  2>&1 | tee profile-evidence/generation.log

python3 - <<'PY'
import pathlib, shutil
root = pathlib.Path('app/src')
for name in ('baseline-prof.txt', 'startup-prof.txt'):
    files = list(root.glob('*/generated/baselineProfiles/' + name))
    if not files or any(p.stat().st_size == 0 for p in files):
        raise SystemExit('Missing or empty generated source: ' + name)
    for p in files:
        dest = pathlib.Path('profile-evidence') / p
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(p, dest)
        print(f'Generated {p}: {p.stat().st_size} bytes')
PY

# The first refresh can stop here with sources retained as artifacts for review.
# A release verification run requires those exact files already in HEAD.
python3 tools/verify_release_profile.py --sources-only
