#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-}"
case "$MODE" in
  storage|performance) ;;
  *) echo "Usage: $0 {storage|performance}" >&2; exit 2 ;;
esac

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK_ROOT" ]] || { echo "ANDROID_HOME or ANDROID_SDK_ROOT is required." >&2; exit 1; }
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$SDK_ROOT/platform-tools/adb"
for tool in "$SDKMANAGER" "$AVDMANAGER"; do
  [[ -x "$tool" ]] || { echo "Missing Android SDK tool: $tool" >&2; exit 1; }
done

API_LEVEL="${VEIL_EMULATOR_API_LEVEL:-35}"
IMAGE="system-images;android-${API_LEVEL};default;x86_64"
"$SDKMANAGER" --install platform-tools emulator "$IMAGE" </dev/null >/dev/null
[[ -x "$EMULATOR" && -x "$ADB" ]] || { echo "Android emulator toolchain was not installed." >&2; exit 1; }

TEMP_ROOT="$(mktemp -d)"
export ANDROID_AVD_HOME="$TEMP_ROOT/avd"
export ANDROID_USER_HOME="$TEMP_ROOT/android-user"
mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME" build/reports

SERIAL="emulator-5554"
AVD_NAME="veil_ci_${MODE}"
EMULATOR_PID=""
EMULATOR_LOG="build/reports/emulator-${MODE}.log"
LOGCAT_LOG="build/reports/logcat-${MODE}.log"
LOCK_FILE="${RUNNER_TEMP:-/tmp}/veil-emulator-5554.lock"
exec 9>"$LOCK_FILE"
flock -n 9 || { echo "$SERIAL is already owned by another process." >&2; exit 1; }

cleanup() {
  status=$?
  set +e
  if (( status != 0 )); then
    "$ADB" -s "$SERIAL" logcat -d >"$LOGCAT_LOG" 2>&1
  fi
  if [[ -n "$EMULATOR_PID" ]] && kill -0 "$EMULATOR_PID" 2>/dev/null; then
    "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1
    sleep 1
    kill "$EMULATOR_PID" >/dev/null 2>&1
  fi
  [[ -n "$EMULATOR_PID" ]] && wait "$EMULATOR_PID" 2>/dev/null
  rm -rf "$TEMP_ROOT"
  return "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

"$ADB" start-server >/dev/null
if "$ADB" devices | awk 'NR > 1 { print $1 }' | grep -Fxq "$SERIAL"; then
  echo "Refusing to reuse an existing $SERIAL." >&2
  exit 1
fi

printf 'no\n' | "$AVDMANAGER" create avd   --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_6 >/dev/null

"$EMULATOR"   -avd "$AVD_NAME" -port 5554   -no-window -no-audio -no-boot-anim -no-metrics -no-snapshot -wipe-data   -gpu lavapipe -accel on -memory 2048 -partition-size 6144   >"$EMULATOR_LOG" 2>&1 &
EMULATOR_PID="$!"

assert_emulator_alive() {
  if [[ -z "$EMULATOR_PID" ]] || ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    echo "Android emulator process exited unexpectedly." >&2
    tail -200 "$EMULATOR_LOG" >&2 || true
    return 1
  fi
}

timeout 180 "$ADB" -s "$SERIAL" wait-for-device
assert_emulator_alive

booted=false
for _ in $(seq 1 120); do
  assert_emulator_alive
  if [[ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
    booted=true
    break
  fi
  sleep 2
done
[[ "$booted" == true ]] || { echo "Emulator did not finish booting." >&2; exit 1; }

for _ in $(seq 1 20); do
  if "$ADB" -s "$SERIAL" shell setprop debug.sf.luma_sampling 0 >/dev/null 2>&1; then break; fi
  assert_emulator_alive
  sleep 1
done

stable_samples=0
stable_system_server=""
for _ in $(seq 1 60); do
  assert_emulator_alive
  system_server="$("$ADB" -s "$SERIAL" shell pidof system_server 2>/dev/null | tr -d '\r' || true)"
  available_kb="$("$ADB" -s "$SERIAL" shell df -k /data 2>/dev/null | tr -d '\r' | awk 'NR == 2 { print $4 }' || true)"
  healthy=false
  if [[ "$available_kb" =~ ^[0-9]+$ ]] &&
     (( available_kb >= 524288 )) &&
     [[ -n "$system_server" ]] &&
     "$ADB" -s "$SERIAL" shell cmd package list packages android >/dev/null 2>&1 &&
     "$ADB" -s "$SERIAL" shell cmd activity get-config >/dev/null 2>&1 &&
     "$ADB" -s "$SERIAL" shell cmd settings get global window_animation_scale >/dev/null 2>&1; then
    healthy=true
  fi

  if [[ "$healthy" == true && "$system_server" == "$stable_system_server" ]]; then
    stable_samples=$((stable_samples + 1))
  else
    stable_samples=0
    stable_system_server="$system_server"
  fi
  (( stable_samples >= 6 )) && break
  sleep 5
done
(( stable_samples >= 6 )) || {
  echo "Android services failed to stabilize before tests." >&2
  "$ADB" -s "$SERIAL" shell df -h /data >&2 || true
  tail -200 "$EMULATOR_LOG" >&2 || true
  exit 1
}

"$ADB" -s "$SERIAL" shell settings put global window_animation_scale 0
"$ADB" -s "$SERIAL" shell settings put global transition_animation_scale 0
"$ADB" -s "$SERIAL" shell settings put global animator_duration_scale 0
export ANDROID_SERIAL="$SERIAL"

run_gradle() {
  gradle --no-daemon --max-workers=2 --no-parallel "$@"
}

case "$MODE" in
  storage)
    run_gradle :app:connectedDebugAndroidTest --stacktrace
    ;;
  performance)
    run_gradle :app:generateBaselineProfile --stacktrace       -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile       -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
    assert_emulator_alive
    run_gradle :benchmark:connectedBenchmarkBenchmarkAndroidTest --stacktrace \
      -Pandroid.testInstrumentationRunnerArguments.class=com.veilreader.benchmark.StartupBenchmark,com.veilreader.benchmark.ReaderFrameSmokeBenchmark
    ;;
esac

assert_emulator_alive
echo "Veil Android emulator gate passed: mode=$MODE api=$API_LEVEL renderer=lavapipe"
